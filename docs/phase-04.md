# Phase 4 notes — RabbitMQ event-driven confirmation emails

## What was built
Visiting the storefront and seeing an order land as `CONFIRMED` was already verified by the phase-3
scripts, but the customer never heard about it: the notification service only had a mock email
endpoint. Phase 4 wires an **event-driven confirmation email** with a **no-loss guarantee**, using
the **transactional outbox pattern** + **RabbitMQ**, and proves delivery end-to-end against a real
Gmail mailbox via IMAP read-back.

| Component    | Detail                                                  |
|--------------|---------------------------------------------------------|
| Broker       | `rabbitmq:4-management`, `scripts/start-rabbitmq.sh`    |
| Broker ports | 5672 amqp, 15672 management UI (shipwise/shipwise)      |
| Topology     | topic exchange `shipwise.events`, routing key `order.confirmed`, durable queue `order-notifications` |
| Outbox       | `order_outbox` table in order-service (same DB as orders) |
| Delivery     | Gmail SMTP (app password + 2FA) via Spring `JavaMailSender`; env-driven `MAIL_*` in `.env` |
| Verification | `notification-test.sh` — IMAP read-back of the actual inbox + a RabbitMQ-outage demo |

## How a confirmation email is produced
1. **Atomic write.** The new saga activity `confirmOrder(orderId)` replaces the old
   `setOrderStatus(CONFIRMED)` step. In **one JDBI transaction** it recomputes the total from
   `order_items`, flips the order to `CONFIRMED`, and INSERTs the
   `OrderConfirmedEvent` (customer, items, total) as a JSONB row into `order_outbox`.
   The event row shares the transaction with the state change, so "order confirmed but event
   missing" is impossible; it also shares it with the FAILED case only in the sense that FAILED
   never calls `confirmOrder`, so failed orders emit no event by construction.
2. **Dispatcher.** `OrderOutboxDispatcher` (`@Scheduled(fixedDelay=1000)`) claims up to 50 unsent
   rows (`FOR UPDATE SKIP LOCKED`), publishes each to Rabbit with **correlated publisher
   confirms**, and clears `sent_at` **only after the broker acks**. There is **no attempt cap**:
   rows stay `sent_at IS NULL` forever until delivered (`attempts` is observability only).
3. **Publisher doesn't trust ACK alone** (lesson from a real drop, below): the producer also
   declares the durable queue + binding, and `CorrelationData.getReturned() != null` (NO_ROUTE)
   is treated as a failure → `markFailed` → retried next tick.
4. **Consumer.** `OrderConfirmedEventListener` on queue `order-notifications` is **idempotent**:
   unique partial index on `notifications.order_id` + `ON CONFLICT DO NOTHING` dedupes redeliveries.
   It retries SMTP transient failures (5 attempts, 1s backoff) and — because
   `default-requeue-rejected: true` — a message that exhausts retries is **requeued, not dropped**,
   so an SMTP outage keeps the message alive until Gmail accepts it.
5. **Guarantee boundary.** Events are committed to Gmail at the SMTP-accept; IMAP read-back then
   proves the email physically arrived. A CONFIRMED order yields *exactly one* email.

## The drop that the design must cover
`notifications` DB (event consumer) and the `order.confirmed` event are emitted by different Gen. On a
**fresh broker restart** an early publish could be ACKed yet silently discarded when the queue is not
yet declared (publisher ACK ≠ routability). Observed as: order CONFIRMED, outbox `sent_at` cleared,
queue empty, no email, no notification row. Fixed by (a) producer-side declaration of the durable
queue + binding and (b) treating `CorrelationData.getReturned()` as failure -> retry forever. The
e2e PART B re-creates the pre-fix scenario under controlled conditions.

## SMTP wiring
- `application.yml` reads `spring.mail.host=${MAIL_HOST:localhost}` (port `:1025`, auth/starttls
  default false) so the stack runs without a mailbox via a local Mailhog queue; `dev.sh` sources
  `.env` and the real Gmail settings kick in.
- Real setup used: Gmail account + 2FA + app password (16 chars, no spaces) with SMTP/IMAP enabled;
  `MAIL_USERNAME/MAIL_PASSWORD/MAIL_FROM` in gitignored `.env`.
- Manual restarts of notification-service must use `set -a; . ./.env; set +a` (or `dev.sh`) — without
  the env the service silently falls back to localhost:1025.

## Line of code layout
```
order-service:
  outbox/OrderOutbox.java            # record (id, orderId, created, sentAt, attempts, payloadJson)
  outbox/OrderOutboxDao.java         # append / claimUnsent / markSent / markFailed
  outbox/OrderOutboxDispatcher.java  # @Scheduled publisher, correlated confirms + getReturned()
  rabbit/OrderEventsConfig.java      # exchange + durable queue + binding beans (producer declares too)
  saga/OrderSagaActivities…Impl*.java# confirmOrder activity (confirmed + outbox in one transaction)
  .../V4__add_order_outbox.sql       # order_outbox + partial index (id) WHERE sent_at IS NULL
notification-service:
  rabbit/NotificationsRabbitConfig.java     # exchange + queue + binding (consumer side)
  rabbit/OrderConfirmedEventListener.java   # dedupe -> SMTP -> record
  .../V2__add_order_id.sql                  # notifications.order_id + unique partial index
scripts:
  start-rabbitmq.sh / stop-rabbitmq.sh      # container mgmt + readiness on :15672
  notification-test.sh                      # full e2e incl. outage demo + IMAP read-back
shipwise-common/.../event/:
  OrderConfirmedEvent.java, OrderConfirmedItem.java   # shared event contract (Jackson 3)
```

## Verification
All green in the same session:
- `happy-path.sh` -> `HAPPY PATH OK`
- `kill-test.sh` -> `KILL-TEST PASSED` (FAILED orders emit no event; no email side-effects)
- `last-unit-test.sh` -> `LAST-UNIT TEST PASSED` (50 parallel: 1 CONFIRMED => exactly 1 email)
- `notification-test.sh` -> `NOTIFICATION-TEST PASSED`
  - `PART A`: broker up — order CONFIRMED, outbox drained (sent_at set), queue empty, email **FOUND**
    in the Gmail inbox via IMAP.
  - `PART B`: broker stopped — order CONFIRMED anyway, outbox buffers (sent_at NULL), on recovery
    the dispatcher flushes and the second email is **FOUND** in the inbox. Proves the outage loses
    nothing.

IMAP read-back notes: the check re-establishes the IMAP connection on every poll (a single transient
TLS hiccup used to kill the whole wait window), and tolerates Gmail delivery latency with a 300s window.