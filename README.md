# Shipwise

Distributed order fulfillment system. Customers browse a product catalog, place orders, and the
backend validates stock and confirms orders transactionally.

![status](https://img.shields.io/badge/phase-2%20(microservices)-green)

## Stack

| Layer     | Technology                                  |
|-----------|---------------------------------------------|
| Backend   | Java 25, Spring Boot 4, Spring Cloud Gateway, Maven |
| Data      | PostgreSQL 17 (Docker), JDBI 3, Flyway      |
| API Docs  | springdoc-openapi (Swagger UI)              |
| Frontend  | React + TypeScript + Vite                   |

## Quickstart

```bash
# start everything: postgres + all 8 services (ports 8080,8081,8082,8083,8084,8085,8086,9000)
./scripts/dev.sh
```

The welcome page is http://localhost:5173 (storefront) and each service exposes Swagger at
`/swagger-ui.html` on its port. Everything routes through the gateway on http://localhost:9000.

- Storefront: http://localhost:5173
- Gateway:   http://localhost:9000
- Swagger:   http://localhost:9000/products-service/swagger-ui.html (or per-service port)

Phase 2 reference (ports, saga, verification scripts): [docs/phase-02.md](docs/phase-02.md).

## Testing the phase-2 guarantees

```bash
./scripts/happy-path.sh     # register -> catalog -> order -> cross-service state check
./scripts/last-unit-test.sh # 50 parallel checkouts of 1 item -> exactly 1 CONFIRMED
./scripts/kill-test.sh      # shipping down mid-saga -> order compensated (refund + restock)
```

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
- [ ] Phase 2.5 — JWT auth
- [ ] Phase 3 — Temporal saga orchestration
- [ ] Phase 4 — RabbitMQ event-driven notifications
- [ ] Phase 5 — Prometheus + Grafana + continuous profiling
- [ ] Phase 6 — Elasticsearch search and centralized logs
- [ ] Phase 7 — Docker Compose packaging
- [ ] Phase 8 — k3s deployment
