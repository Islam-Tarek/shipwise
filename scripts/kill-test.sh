#!/usr/bin/env bash
set -euo pipefail

# Failure-injection: bring a downstream service (shipping) down in the middle
# of the saga. The order must fail fast and compensate: payment refunded and
# the reserved stock returned to inventory. Nothing may hang or leak.
# Payment failure-rate is pinned to 0 so the failure is guaranteed to come
# from shipping (charge succeeds, shipping step fails, refund runs).

cd "$(dirname "$0")/.."

GW=http://localhost:9000
PID=1

STOP() { docker stop "$1" >/dev/null 2>&1; }
WAIT_DOWN() { until ! curl -s -o /dev/null -m 2 "$1" >/dev/null 2>&1; do sleep 1; done; }
WAIT_UP() { until curl -s -o /dev/null -m 2 "$1" >/dev/null 2>&1; do sleep 1; done; }
echo "== baseline inventory stock =="
BEFORE=$(curl -s "$GW/api/inventory/$PID" | python3 -c "import sys,json;print(json.load(sys.stdin)['quantity'])")
echo "  stock before: $BEFORE"

echo "== payment failure-rate already 0 via compose =="

echo "== killing shipping-service mid-saga =="
STOP shipping-service
WAIT_DOWN http://localhost:8084/api/shipments/99999999
echo "  shipping-service stopped"

echo "== register account for auth token =="
TOKEN=$(curl -s -X POST "$GW/api/users" -H "Content-Type: application/json" \
  -d '{"name":"Kill Test","email":"killtest'$RANDOM'@example.com","password":"secret123"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
echo "  token acquired (${TOKEN:0:8}...)"

echo "== placing order while shipping is down =="
ORDER=$(curl -s -m 30 -X POST "$GW/api/orders" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "{
  \"customerName\":\"Kill Test\",\"customerEmail\":\"kill$RANDOM@example.com\",\"address\":\"x\",
  \"items\":[{\"productId\":$PID,\"quantity\":2,\"productName\":\"kill test\",\"unitPrice\":29.99}]}")
OID=$(echo "$ORDER" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
echo "  order $OID -> $(echo "$ORDER" | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])")"

echo "== verifying compensation =="
sleep 1
STATUS=$(curl -s http://localhost:8080/api/orders/$OID | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])")
echo "  order status:   $STATUS (expect FAILED)"
PAY=$(PGPASSWORD=shipwise docker exec shipwise-postgres psql -U shipwise -d shipwise_payment -tAc "SELECT status FROM payments WHERE order_id=$OID;")
echo "  payment status: $PAY (expect REFUNDED)"
AFTER=$(curl -s "$GW/api/inventory/$PID" | python3 -c "import sys,json;print(json.load(sys.stdin)['quantity'])")
echo "  stock after:    $AFTER (expect $BEFORE)"

echo "== restarting shipping-service =="
docker start shipping-service >/dev/null 2>&1
WAIT_UP http://localhost:8084/api/shipments/99999999
echo "  shipping-service restarted"

ok=1
[ "$STATUS" = "FAILED" ] || { echo "FAIL: order not FAILED"; ok=0; }
[ "$PAY" = "REFUNDED" ] || { echo "FAIL: payment not REFUNDED"; ok=0; }
[ "$AFTER" = "$BEFORE" ] || { echo "FAIL: stock not restored"; ok=0; }
[ $ok -eq 1 ] && echo "KILL-TEST PASSED (compensated: refunded + restocked)" || { echo "KILL-TEST FAILED"; exit 1; }
