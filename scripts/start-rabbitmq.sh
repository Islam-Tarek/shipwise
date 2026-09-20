#!/usr/bin/env bash
set -euo pipefail

NAME=shipwise-rabbitmq
IMAGE=rabbitmq:4-management

if docker ps --format '{{.Names}}' | grep -q "^$NAME$"; then
  echo "rabbitmq already running ($NAME)"
  exit 0
fi

docker rm -f "$NAME" 2>/dev/null || true

echo "starting rabbitmq (amqp :5672, management UI http://localhost:15672 shipwise/shipwise)"
docker run -d --name "$NAME" \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=shipwise \
  -e RABBITMQ_DEFAULT_PASS=shipwise \
  "$IMAGE" >/dev/null

for _ in $(seq 1 60); do
  if curl -s -m 2 -u shipwise:shipwise http://localhost:15672/api/overview >/dev/null 2>&1; then
    echo "rabbitmq ready (amqp :5672, management http://localhost:15672)"
    exit 0
  fi
  sleep 2
done

echo "rabbitmq did not become ready in time - see docker logs shipwise-rabbitmq" >&2
exit 1