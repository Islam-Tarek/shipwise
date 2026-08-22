# Phase 1 notes — monolith MVP

## What was built
Single Spring Boot service owning the whole catalog/order domain, plus a React storefront.
Postgres runs in Docker (port 5433 to avoid clashing with a local instance), schema managed by
Flyway, data access with JDBI SQL objects, catalog seeded from dummyjson.com on first startup.

## Decisions worth remembering

- **JDBI over JPA**: explicit SQL keeps queries visible and avoids lazy-loading surprises.
  Row mapping uses `ConstructorMapper` registered per record; snake_case columns match camelCase
  records via `SnakeCaseColumnNameMatcher`. Lesson learned: `@BindBean` does not work with Java
  records (no getters) - use explicit `@Bind` parameters.
- **Money as NUMERIC(12,2)** mapped to BigDecimal, never floating point.
- **unit_price snapshot** in order_items: product prices change, orders must remember what was paid.
- **Atomic stock check**: decrement runs as a conditional UPDATE
  (`WHERE stock_quantity >= :quantity`) inside the checkout transaction, so overselling fails
  cleanly and rolls back the whole order.
- **Idempotent seeding**: products carry the dummyjson id as `external_id`; seeder skips when the
  table already has rows.
- **Order status as varchar + check constraint**, not a Postgres enum, because later phases add
  saga states and altering check constraints is far easier than altering enums.
- **Spring Boot 4 gotcha**: auto-configuration is modularized - Flyway needs its own
  `spring-boot-flyway` dependency, having flyway-core alone is not enough.
- **Port 5433** for the dockerized Postgres because a local Postgres 16 was already bound to 5432.

## Endpoints
See README or Swagger UI (`/swagger-ui.html`).
