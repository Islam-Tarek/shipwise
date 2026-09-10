#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

MODULES=(
  "order-service:8080"
  "product-service:8081"
  "inventory-service:8082"
  "payment-service:8083"
  "shipping-service:8084"
  "notification-service:8085"
  "auth-service:8086"
  "gateway:9000"
)

listener_pid() {
  ss -tlnp 2>/dev/null | grep ":$1 " | grep -oP 'pid=\K[0-9]+' | head -1 || true
}

port_free() {
  ! ss -tln 2>/dev/null | grep -q ":$1 "
}

stopped=0
skipped=0
failed=0

echo "stopping shipwise services"
echo

for entry in "${MODULES[@]}"; do
  name=${entry%%:*}
  port=${entry##*:}
  pid=$(listener_pid "$port")

  if [ -z "$pid" ]; then
    echo "  [already stopped] $name (nothing on :$port)"
    skipped=$((skipped + 1))
    continue
  fi

  kill -TERM "$pid" 2>/dev/null || true
  for i in $(seq 1 10); do
    port_free "$port" && break
    sleep 1
  done

  if port_free "$port"; then
    echo "  [stopped]         $name (pid $pid on :$port)"
    stopped=$((stopped + 1))
    continue
  fi

  pid2=$(listener_pid "$port" || true)
  kill -9 "$pid2" 2>/dev/null || true
  sleep 1

  if port_free "$port"; then
    echo "  [stopped (forced)] $name (pid $pid2 on :$port)"
    stopped=$((stopped + 1))
  else
    echo "  [FAILED]          $name (pid $pid2 still on :$port)"
    failed=$((failed + 1))
  fi
done

echo
echo "summary: $stopped stopped, $skipped already stopped, ${failed} failed"
if [ "$failed" -gt 0 ]; then
  echo "still listening on:"
  for entry in "${MODULES[@]}"; do
    port=${entry##*:}
pid=$(listener_pid "$port" || true)
    [ -n "$pid" ] && echo "  :$port (pid $pid)"
  done
else
  echo "all service ports free"
fi
echo "postgres left running (stop with: ./scripts/stop-postgres.sh)"