#!/usr/bin/env bash
set -euo pipefail

docker rm -f shipwise-postgres 2>/dev/null || true

docker run -d --name shipwise-postgres \
  -e POSTGRES_DB=shipwise \
  -e POSTGRES_USER=shipwise \
  -e POSTGRES_PASSWORD=shipwise \
  -p 5433:5432 \
  -v shipwise-pgdata:/var/lib/postgresql/data \
  postgres:17

echo "waiting for postgres to accept connections"
until docker exec shipwise-postgres pg_isready -U shipwise -d postgres >/dev/null 2>&1; do
  sleep 1
done

for db in shipwise_products shipwise_orders shipwise_auth shipwise_inventory shipwise_payment shipwise_shipping shipwise_notification; do
  if ! docker exec shipwise-postgres psql -U shipwise -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$db'" | grep -q 1; then
    docker exec shipwise-postgres createdb -U shipwise "$db"
    echo "created database $db"
  fi
done
echo "postgres is up on localhost:5433 (one database per service)"
