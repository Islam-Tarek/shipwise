# Shipwise

Distributed order fulfillment system. Customers browse a product catalog, place orders, and the
backend validates stock and confirms orders transactionally.

![status](https://img.shields.io/badge/phase-1%20(monolith)-green)

## Stack

| Layer     | Technology                                  |
|-----------|---------------------------------------------|
| Backend   | Java 25, Spring Boot 4, Maven               |
| Data      | PostgreSQL 17 (Docker), JDBI 3, Flyway      |
| API Docs  | springdoc-openapi (Swagger UI)              |
| Frontend  | React + TypeScript + Vite                   |

## Quickstart

```bash
# 1. start postgres in docker (port 5433)
./scripts/start-postgres.sh

# 2. run the backend (migrates schema and seeds catalog on first start)
cd backend && mvn spring-boot:run

# 3. run the frontend
cd frontend && npm install && npm run dev
```

- Storefront: http://localhost:5173
- Swagger UI: http://localhost:8080/swagger-ui.html

The first backend start pulls ~120 products with images from dummyjson.com and stores them in
Postgres. Subsequent starts skip seeding when the catalog is already populated.

## API overview

| Method | Path                    | Description                    |
|--------|-------------------------|--------------------------------|
| GET    | /api/products           | paginated catalog, `?name=` search |
| GET    | /api/products/{id}      | product detail                 |
| GET    | /api/categories         | category list                  |
| POST   | /api/orders             | checkout (creates customer if new) |
| GET    | /api/orders/{id}        | order detail with items        |
| GET    | /api/orders?customerId= | order history                  |
| POST   | /api/orders/{id}/cancel | cancel a pending order (restores stock) |
| POST   | /api/customers          | create customer                |

## Roadmap

- [x] Phase 1 — monolith MVP (catalog, orders, storefront)
- [ ] Phase 2 — microservices split
- [ ] Phase 3 — Temporal saga orchestration
- [ ] Phase 4 — RabbitMQ event-driven notifications
- [ ] Phase 5 — Prometheus + Grafana + continuous profiling
- [ ] Phase 6 — Elasticsearch search and centralized logs
- [ ] Phase 7 — Docker Compose packaging
- [ ] Phase 8 — k3s deployment
