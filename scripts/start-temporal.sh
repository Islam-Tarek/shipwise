#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$REPO_ROOT/data/temporal"

docker rm -f shipwise-temporal 2>/dev/null || true

if ! docker image inspect temporalio/temporal:1.8.3 >/dev/null 2>&1; then
  docker pull temporalio/temporal:1.8.3
fi

docker run -d --name shipwise-temporal \
  -p 7233:7233 \
  -p 8233:8233 \
  -v "$REPO_ROOT/data/temporal:/temporal" \
  temporalio/temporal:1.8.3 \
  server start-dev --ip 0.0.0.0 --db-filename /temporal/temporal.db

echo "waiting for temporal to accept connections on :7233"
for _ in $(seq 1 30); do
  if (echo > /dev/tcp/localhost/7233) >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

sleep 2
echo "temporal dev server is up (gRPC :7233, web UI http://localhost:8233, default namespace created)"