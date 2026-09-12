# Shipwise

Distributed order fulfillment system. Customers browse a product catalog, place orders, and the
backend validates stock and confirms orders transactionally.

![status](https://img.shields.io/badge/phase-3%20(temporal%20saga)-green)

## Stack

| Layer     | Technology                                  |
|-----------|---------------------------------------------|
| Backend   | Java 25, Spring Boot 4, Spring Cloud Gateway, Maven |
| Data      | PostgreSQL 17 (Docker), JDBI 3, Flyway      |
| Orchestration | Temporal (durable saga), io.temporal:1.37.0 |
| API Docs  | springdoc-openapi (Swagger UI)              |
| Frontend  | React + TypeScript + Vite                   |

## Quickstart

```bash
# 1. infra: postgres + temporal dev server (durable SQLite history)
./scripts/start-postgres.sh
./scripts/start-temporal.sh     # gRPC :7233, Web UI http://localhost:8233

# 2. all 8 services (ports 8080,8081,8082,8083,8084,8085,8086,9000)
./scripts/dev.sh
```

The welcome page is http://localhost:5173 (storefront) and each service exposes Swagger at
`/swagger-ui.html` on its port. Everything routes through the gateway on http://localhost:9000.
Temporal's workflow history is visible at http://localhost:8233.

- Storefront: http://localhost:5173
- Gateway:   http://localhost:9000
- Swagger:   http://localhost:9000/products-service/swagger-ui.html (or per-service port)
- Temporal:  http://localhost:8233 (workflow/activity history, retries, compensations)

Phase 2 reference (ports, original saga, verification scripts): [docs/phase-02.md](docs/phase-02.md).
The order checkout saga (reserve -> charge -> ship, with compensation) now runs as a durable
Temporal workflow `OrderSagaWorkflow` on task queue `ORDER_SAGA` inside order-service.

## Testing the phase-3 guarantees

```bash
./scripts/happy-path.sh     # register -> catalog -> order -> cross-service state check
./scripts/last-unit-test.sh # 50 parallel checkouts of 1 item -> exactly 1 CONFIRMED
./scripts/kill-test.sh      # shipping down mid-saga -> order compensated (refund + restock)
```

All three run unchanged against the Temporal-orchestrated checkout: Temporal retries or
fails nontransient steps, then the saga compensates (restock + refund) and marks the order FAILED.

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
| POST   | /api/notifications      | notification | send (mock, 202 accepted)    |

## Roadmap

- [x] Phase 1 — monolith MVP (catalog, orders, storefront)
- [x] Phase 2 — microservices split (gateway + saga with compensation)
- [x] Phase 2.5 — JWT auth
- [x] Phase 3 — Temporal saga orchestration (durable, retriable saga)
- [ ] Phase 4 — RabbitMQ event-driven notifications (real SMTP delivery)
- [ ] Phase 5 — Operability: Prometheus + Grafana + Loki centralized logs
- [ ] Phase 6 — Docker Compose packaging (8 services + postgres + Temporal +
        RabbitMQ + metrics stack)
- [ ] Phase 7 — k3s deployment
- [ ] Phase 8 — Elasticsearch storefront search
- [ ] Phase 9 — Continuous profiling (async-profiler/JFR)
