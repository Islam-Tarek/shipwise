# Phase 2 notes — microservices split

## What was built
The monolith was split into eight services, each with its own Postgres database on port 5433,
behind a Spring Cloud Gateway on port 9000.

| Service             | Port | Database            | Owns                                        |
|---------------------|------|---------------------|---------------------------------------------|
| product             | 8081 | shipwise_products   | catalog, category slugs                     |
| auth                | 8086 | shipwise_auth       | customer accounts                           |
| inventory           | 8082 | shipwise_inventory  | stock (source of truth)                     |
| payment             | 8083 | shipwise_payment    | payments + refunds                          |
| shipping            | 8084 | shipwise_shipping   | shipment lifecycle                          |
| order               | 8080 | shipwise_orders     | order saga orchestration                    |
| notification        | 8085 | shipwise_notification | outbox of notifications (stub in phase 2) |
| gateway             | 9000 | -                   | `/api/**` routing                           |

Shared code (error handling, DTOs) lives in `shipwise-common`; errors are RFC-7807 ProblemDetail.

## How the saga works (order-service)
`POST /api/orders` runs `OrderService.confirm()`: **reserve** (inventory) → **charge** (payment) →
**ship** (shipping). Deliveries are sequential via Spring `RestClient` (Transactional outbox and
Temporal orchestration are phases 3–4). Each step records a row in its own service so state can be
verified across databases.

On any failure the saga compensates in reverse:
- charge fails → order `FAILED`, reserved stock returned (restock)
- shipping fails → order `FAILED`, payment `REFUNDED`, reserved stock returned

Verified end-to-end with the kill-test: shipping down mid-saga → order FAILED, payment REFUNDED,
inventory restored to its pre-order count.

## Data ownership rules
- **inventory is the only stock authority.** It pushes quantity changes to product's display copy
  (`stock_quantity`) after every reserve/restock/upsert, and re-pushes the whole catalog at startup.
- **product is the only catalog authority.** `external_id` == dummyjson product id; inventory keys
  stock rows by that id.
- **order snapshots** product name/price at checkout; it never reads product at saga time.
- **payment and shipping** store stateful rows (SUCCEEDED/REFUNDED, PLANNED/SHIPPED/CANCELLED) that
  order could cross-check, and refunds guard against double-charging.

## Concurrency guarantee
`reserve` is `UPDATE stock SET quantity = quantity - :qty WHERE product_id = :id AND quantity >= :qty`
- an atomic conditional update. Proven by `scripts/last-unit-test.sh`: with one unit left, 50
  parallel checkouts yield exactly **1 CONFIRMED, 49 FAILED, 0 stock left**.

## Seeding and consistency gotchas
- dummyjson returns different snapshots over time (id ranges shifted 117→194, category slugs
  changed). Product and inventory seed from **the same live snapshot** so `external_id` == stock
  product_id; both re-seed cleanly after a `TRUNCATE`. The product seeder now derives categories
  from the snapshot too rather than assuming a fixed list.
- `TRUNCATE ... CASCADE` removes seed data without resetting Flyway history; flyway won't re-run
  already-applied migrations, so any seed data lost to truncation is only restored by the app's
  seeder (another reason the seeder must be self-sufficient).

## Spring Cloud on Boot 4
- Use the **2025.1.x (Oakwood)** train, not 2025.0.x (that is Boot 3.5 and brings relocated,
  incompatible classes).
- Gateway uses `spring-cloud-starter-gateway-server-webflux` (5.x artifact rename) plus
  `spring-boot-web-server`.
- Route properties moved to `spring.cloud.gateway.server.webflux.routes`; the old
  `spring.cloud.gateway.routes` name binds but is silently ignored.

## Verification
`./scripts/dev.sh` builds everything and starts all eight services. Then:

```bash
./scripts/happy-path.sh         # register -> catalog -> order -> cross-DB verification
./scripts/last-unit-test.sh     # concurrency: exactly 1 of 50 wins the last unit
./scripts/kill-test.sh          # shipping down mid-saga -> compensated (refund + restock)
```

Test-state note: payment has a configurable mock failure rate
(`--payment.failure-rate=0.2` default); kill-test pins it to 0 so the injected failure is
deterministically the shipping outage.

## Moving on
- Phase 2.5: minimal JWT auth on order creation.
- Phase 3: Temporal saga (durable, timeouts, retries) replacing hand-rolled compensation.
- Phases 4–8 unchanged per README roadmap.