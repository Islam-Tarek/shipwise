#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "starting postgres"
./scripts/start-postgres.sh

echo "building all modules"
mvn -q clean install -DskipTests -T1C

MODULES=(product-service auth-service inventory-service payment-service shipping-service order-service notification-service gateway)
PIDS=()
trap 'for pid in "${PIDS[@]:-}"; do kill "$pid" 2>/dev/null || true; done' EXIT

for module in "${MODULES[@]}"; do
  echo "starting $module"
  (
    setsid nohup mvn -q -pl "$module" spring-boot:run > "/tmp/opencode/$module.log" 2>&1 < /dev/null &
    echo $! > "/tmp/opencode/$module.pid"
  )
  PIDS+=("$(cat "/tmp/opencode/$module.pid")")
done

echo "waiting for services"
for port in 8081 8086 8082 8083 8084 8080 8085 9000; do
  for i in $(seq 1 60); do
    code=$(curl -s -o /dev/null -m 2 -w "%{http_code}" "http://localhost:$port/" 2>/dev/null || true)
    [ "$code" != "000" ] && echo "  port $port up" && break
    sleep 2
  done
done

echo "all services up - gateway on http://localhost:9000"
echo "stop with: kill \$(cat /tmp/opencode/*.pid)"