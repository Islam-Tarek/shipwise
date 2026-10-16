#!/usr/bin/env bash
set -euo pipefail

CONTAINER="shipwise-lgtm"

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "lgtm already running: ${CONTAINER}"
  echo "Grafana: http://localhost:3000 (admin/admin)"
  echo "OTLP HTTP: http://localhost:4318 (traces /v1/traces, metrics /v1/metrics, logs /v1/logs)"
  exit 0
fi

if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  docker start "${CONTAINER}" >/dev/null
else
  docker run -d --name "${CONTAINER}" \
    -p 3000:3000 -p 4317:4317 -p 4318:4318 \
    --rm grafana/otel-lgtm >/dev/null
fi

echo "starting grafana/otel-lgtm..."
for i in $(seq 1 60); do
  if curl -s -o /dev/null -m 2 "http://localhost:3000/api/health" 2>/dev/null; then
    break
  fi
  sleep 2
done

echo "LGTM ready"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo "OTLP HTTP: http://localhost:4318 (traces /v1/traces, metrics /v1/metrics, logs /v1/logs)"
