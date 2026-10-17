# Shipwise

Distributed order fulfillment system. Customers browse a product catalog, place orders, and the
backend validates stock and confirms orders transactionally.

![status](https://img.shields.io/badge/phase-5%20(observability)-green)

## Stack

| Layer     | Technology                                  |
|-----------|---------------------------------------------|
| Backend   | Java 25, Spring Boot 4, Spring Cloud Gateway, Maven |
| Data      | PostgreSQL 17 (Docker), JDBI 3, Flyway      |
| Orchestration | Temporal (durable saga), io.temporal:1.37.0 |
| Messaging     | RabbitMQ 4 (Docker) — transactional outbox + publisher confirms |
| Email         | Gmail SMTP (app password, env-driven)        |
| API Docs  | springdoc-openapi (Swagger UI)              |
| Frontend  | React + TypeScript + Vite                   |

## Quickstart

```bash
# 1. infra: postgres + temporal dev server (durable SQLite history) + rabbitmq
./scripts/start-postgres.sh
./scripts/start-temporal.sh     # gRPC :7233, Web UI http://localhost:8233
./scripts/start-rabbitmq.sh     # amqp :5672, management UI http://localhost:15672
./scripts/start-lgtm.sh         # Grafana + Tempo + Loki + Mimir (:3000), OTLP :4318

# 2. Gmail mailbox for confirmation emails (copy into .env; gitignored)
#    MAIL_USERNAME=you@gmail.com  MAIL_PASSWORD=<16-char app password>  MAIL_FROM=you@gmail.com
#    (enable 2FA + an app password in Google account security; never commit .env)

# 3. all 8 services (ports 8080,8081,8082,8083,8084,8085,8086,9000)
./scripts/dev.sh
```

`dev.sh` sources `.env` so the notification service picks up the SMTP mailbox.
Order-confirmation emails are sent via Gmail SMTP (outbox -> RabbitMQ -> SMTP); the
default placeholders fall back to localhost:1025 (a local Mailhog queue) so the stack
still runs without `.env`.

The welcome page is http://localhost:5173 (storefront) and each service exposes Swagger at
`/swagger-ui.html` on its port. Everything routes through the gateway on http://localhost:9000.
Temporal's workflow history is visible at http://localhost:8233.

- Storefront: http://localhost:5173
- Gateway:   http://localhost:9000
- Swagger:   http://localhost:9000/products-service/swagger-ui.html (or per-service port)
- Temporal:  http://localhost:8233 (workflow/activity history, retries, compensations)
- Grafana LGTM: http://localhost:3000 (admin/admin) — traces (Tempo), metrics (Mimir), logs (Loki), OTLP :4318

Phase 2 reference (ports, original saga, verification scripts): [docs/phase-02.md](docs/phase-02.md).
The order checkout saga (reserve -> charge -> ship, with compensation) now runs as a durable
Temporal workflow `OrderSagaWorkflow` on task queue `ORDER_SAGA` inside order-service.
The phase-4 pattern (outbox table, dispatcher, RabbitMQ topology, idempotent consumer,
delivery guarantee) is documented in [docs/phase-04.md](docs/phase-04.md).
The phase-5 pattern (OpenTelemetry traces/metrics/logs over OTLP/HTTP to grafana/otel-lgtm,
manual spans for JDBI, Temporal saga and outbox dispatcher) is documented in
[docs/phase-05.md](docs/phase-05.md).

## Testing the phase-3 guarantees

```bash
./scripts/happy-path.sh     # register -> catalog -> order -> cross-service state check
./scripts/last-unit-test.sh # 50 parallel checkouts of 1 item -> exactly 1 CONFIRMED
./scripts/kill-test.sh      # shipping down mid-saga -> order compensated (refund + restock)
```

All three run unchanged against the Temporal-orchestrated checkout: Temporal retries or
fails nontransient steps, then the saga compensates (restock + refund) and marks the order FAILED.

## Testing the phase-4 guarantees

```bash
./scripts/notification-test.sh  # end-to-end confirmation emails (real Gmail)
#   PART A: order confirmed while broker up -> outbox drained, queue empty,
#           email found in the Gmail inbox via IMAP read-back.
#   PART B: order confirmed while RabbitMQ is DOWN -> CONFIRMED anyway, event
#           buffered in the outbox (sent_at NULL), flushed on recovery, email
#           delivered. Verifies "no event lost, even during an outage".
```

The guarantee: a CONFIRMED order eventually produces exactly one confirmation email.
Order-service writes the event into the `order_outbox` table atomically inside the
`confirmOrder` saga activity; a scheduled dispatcher publishes it with correlated
publisher confirms and clears `sent_at` only after the broker acks (NO_ROUTE is
treated as failure and retried forever). The notification-service consumer is
idempotent (unique `order_id`), retries SMTP transient failures, and re-queues on
exhaustion so an SMTP outage never drops the event.

## Testing the phase-5 guarantees

```bash
./scripts/start-lgtm.sh                     # Grafana on :3000, OTLP on :4318
./scripts/happy-path.sh  # generates end-to-end HTTP -> saga -> outbox -> Rabbit -> SMTP telemetry
```

Then open **Grafana Explore** at http://localhost:3000:
- **Tempo**: search `service.name = "order-service"` to see one trace spanning
  `http post /api/orders` → `RunWorkflow:OrderSagaWorkflow` → activities
  (`ReserveStock`, `ChargePayment`, `CreateShipment`, `ConfirmOrder`) →
  `outbox.flush` → `outbox.publish` → `shipwise.events/order.confirmed send` →
  `order-notifications receive` in notification-service.
- **Loki**: filter `{service_name="order-service"}` — logs carry `trace_id` /
  `span_id` so you can pivot from a log line to its trace.
- **Mimir**: `http_server_requests_seconds_count` per `service_name` for request rates.

All eight services export traces, metrics and logs over OTLP/HTTP only
(`http://localhost:4318`) to the `grafana/otel-lgtm` container.

## API overview (gateway, port 9000)

| Method | Path                    | Service   | Description                    |
|--------|-------------------------|-----------|--------------------------------|
| GET    | /api/products           | product   | paginated catalog              |
| GET    | /api/products/{id}      | product   | product detail                 |
| GET    | /api/categories         | product   | category list                  |
| POST   | /api/users              | auth      | register account               |
| GET    | /api/inventory/{id}     | inventory | current quantity               |
| POST   | /api/orders             | order     | checkout (runs the saga)       |
| GET    | /api/orders/{id}        | order     | order detail with items        |
| POST   | /api/payments           | payment   | charge (mock, ~20% failure)    |
| POST   | /api/shipments          | shipping  | plan a shipment                |
| POST   | /api/notifications      | notification | send test email via SMTP     |

## Roadmap

- [x] Phase 1 — monolith MVP (catalog, orders, storefront)
- [x] Phase 2 — microservices split (gateway + saga with compensation)
- [x] Phase 2.5 — JWT auth
- [x] Phase 3 — Temporal saga orchestration (durable, retriable saga)
- [x] Phase 4 — RabbitMQ event-driven notifications (outbox, real SMTP delivery)
- [x] Phase 5 — Observability: OpenTelemetry (traces+metrics+logs, OTLP/HTTP) + Grafana LGTM
- [ ] Phase 6 — Docker Compose packaging (8 services + postgres + Temporal +
        RabbitMQ + metrics stack)
- [ ] Phase 7 — k3s deployment
- [ ] Phase 8 — Elasticsearch storefront search
- [ ] Phase 9 — Continuous profiling (async-profiler/JFR)
