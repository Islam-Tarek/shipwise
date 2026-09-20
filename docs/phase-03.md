# Phase 3 notes — Temporal saga orchestration

## What was built
The hand-rolled synchronous saga in order-service was replaced with a **durable Temporal
workflow**. Each checkout now runs as `OrderSagaWorkflow` on task queue `ORDER_SAGA`, executed by a
worker embedded in order-service against a local Temporal dev server.

| Component    | Detail                                                  |
|--------------|---------------------------------------------------------|
| Server       | `temporalio/temporal:1.8.3` dev server, `scripts/start-temporal.sh` |
| Server ports | 7233 gRPC, 8233 Web UI (workflow history)               |
| Persistence  | SQLite file `data/temporal/temporal.db` (survives restarts, gitignored) |
| SDK          | `io.temporal:temporal-sdk:1.37.0` (plain SDK, no Spring starter) |
| Worker       | order-service `TemporalConfig` + `TemporalWorkerLifecycle` (SmartLifecycle) |

## How an order is placed now
`POST /api/orders` (`OrderService.placeOrder`):

1. `createOrder` writes the order row + line items in one JDBI transaction (PENDING).
2. `startOrderSaga` starts workflow `OrderSagaWorkflow(orderId, customerAddress)` and blocks on its
   result (60s cap) — the frontend still gets the final CONFIRMED/FAILED order synchronously.
3. `OrderSagaWorkflowImpl.place` runs the saga purely through activities:
   - `loadLines` (reads items from order DB) → one `reserveStock` per line
   - `chargePayment` (amount recomputed from `order_items`)
   - `createShipment` (carrier DHL)
   - `setOrderStatus CONFIRMED`
4. On any failure it records `FAILED` and runs the compensations in reverse via `Saga`:
   - `refundPayment` + `releaseStock` (restock) for each reserved line
   - the workflow completes successfully carrying `OrderOutcome(orderId, status, paymentId)`;
     the order row is the source of truth for status.

## Retry / nontransient semantics
- All activities use `RetryOptions`: max 3 attempts, 1s initial backoff (×1.5) for
  transport/5xx errors — e.g. kill-test restarts of a downed service absorb the outage.
- Downstream business errors (422) are mapped to **non-retryable** `ApplicationFailure`
  (`INSUFFICIENT_STOCK`, `PAYMENT_DECLINED`, duplicate shipment) so a declined card or a lost
  stock race fails the order immediately — this preserves the payment `failure-rate` mock semantics
  and the "exactly one reservation wins" concurrency guarantee.
- Kill-test evidence (`temporal workflow show -w order-127`): the `createShipment` activity
  retried on `ConnectException` → `ResourceAccessException` before the saga compensated.

## Line of code layout
```
order-service/src/main/java/com/shipwise/order/saga/
  OrderSagaWorkflow.java        # @WorkflowInterface: place(orderId, customerAddress) -> OrderOutcome
  OrderSagaWorkflowImpl.java    # deterministic steps + Saga compensations
  OrderSagaActivities.java      # activity interface
  OrderSagaActivitiesImpl.java  # RestClient calls to inventory/payment/shipping + JDBI status writes
  OrderOutcome.java             # (orderId, status, paymentId)
order-service/.../config/
  TemporalConfig.java           # WorkflowServiceStubs / WorkflowClient / WorkerFactory beans
  TemporalWorkerLifecycle.java  # starts the ORDER_SAGA worker on boot (resilient)
```

## Verification
Same three scripts, unchanged HTTP contract:
- `happy-path.sh` -> `HAPPY PATH OK` (order CONFIRMED, stock 99→97)
- `kill-test.sh` -> `KILL-TEST PASSED` (order FAILED, payment REFUNDED, stock restored)
- `last-unit-test.sh` -> `LAST-UNIT TEST PASSED` (50 parallel checkouts, exactly 1 CONFIRMED / 49 FAILED / 0 left)

All 50+ concurrent runs show up as `OrderSagaWorkflow` executions in the Temporal UI / CLI.