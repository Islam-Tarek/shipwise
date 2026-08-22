#!/usr/bin/env bash
set -euo pipefail

docker rm -f shipwise-postgres 2>/dev/null || true

docker run -d --name shipwise-postgres \
  -e POSTGRES_DB=shipwise \
  -e POSTGRES_USER=shipwise \
  -e POSTGRES_PASSWORD=shipwise \
  -p 5432:5432 \
  -v shipwise-pgdata:/var/lib/postgresql/data \
  postgres:17

echo "waiting for postgres to accept connections"
until docker exec shipwise-postgres pg_isready -U shipwise -d shipwise >/dev/null 2>&1; do
  sleep 1
done
echo "postgres is up on localhost:5432 (db=shipwise user=shipwise pass=shipwise)"
