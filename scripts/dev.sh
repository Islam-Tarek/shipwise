#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

MODULES=(
  "product-service:8081"
  "auth-service:8086"
  "inventory-service:8082"
  "payment-service:8083"
  "shipping-service:8084"
  "order-service:8080"
  "notification-service:8085"
  "gateway:9000"
)

echo "starting postgres"
./scripts/start-postgres.sh

echo "building all modules"
mvn -q clean install -DskipTests -T1C

PIDS=()
trap 'for pid in "${PIDS[@]:-}"; do kill "$pid" 2>/dev/null || true; done' EXIT

started=0
failed=0

echo
echo "starting services"
for entry in "${MODULES[@]}"; do
  name=${entry%%:*}
  port=${entry##*:}

  (
    setsid nohup mvn -q -pl "$name" spring-boot:run > "/tmp/opencode/$name.log" 2>&1 < /dev/null &
    echo $! > "/tmp/opencode/$name.pid"
  )
  PIDS+=("$(cat "/tmp/opencode/$name.pid")")

  for i in $(seq 1 60); do
    code=$(curl -s -o /dev/null -m 2 -w "%{http_code}" "http://localhost:$port/" 2>/dev/null || true)
    [ "$code" != "000" ] && break
    sleep 2
  done

  if [ "$code" != "000" ]; then
    echo "  [started]  $name (on :$port, pid ${PIDS[-1]})"
    started=$((started + 1))
  else
    echo "  [FAILED]   $name (nothing on :$port after 2m - see /tmp/opencode/$name.log)"
    failed=$((failed + 1))
  fi
done

echo
echo "summary: $started started, $failed failed"
trap - EXIT
if [ "$failed" -eq 0 ]; then
  echo "all services up - gateway on http://localhost:9000"
else
  echo "check /tmp/opencode/*.log for the failing services"
fi
echo "stop with: ./scripts/stop.sh"
if command -v docker >/dev/null 2>&1 && ! docker ps --format '{{.Names}}' | grep -q "^shipwise-lgtm$"; then
  if curl -s -o /dev/null -m 1 http://localhost:3000 >/dev/null 2>&1 || curl -s -o /dev/null -m 1 http://localhost:4318 >/dev/null 2>&1; then
    :
  else
    ./scripts/start-lgtm.sh >/tmp/opencode/lgtm-start.log 2>&1 || true
  fi
fi
