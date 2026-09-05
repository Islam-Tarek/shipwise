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

STOP() { for p in "$@"; do ss -tlnp 2>/dev/null | grep ":$p " | grep -oP 'pid=\K[0-9]+' | xargs -r kill; done; sleep 2; }
WAIT_UP() { until [ "$(curl -s -o /dev/null -w '%{http_code}' "$2" 2>/dev/null)" != "000" ]; do sleep 2; done; }

echo "== baseline inventory stock =="
BEFORE=$(curl -s "$GW/api/inventory/$PID" | python3 -c "import sys,json;print(json.load(sys.stdin)['quantity'])")
echo "  stock before: $BEFORE"

echo "== pinning payment failure-rate to 0 (deterministic refund) =="
STOP 8083
setsid nohup mvn -q -pl payment-service spring-boot:run -Dspring-boot.run.arguments=--payment.failure-rate=0 > /tmp/opencode/payment-service.log 2>&1 < /dev/null & disown
WAIT_UP 8083 http://localhost:8083/api/payments/99999999

echo "== killing shipping-service (8084) mid-saga =="
STOP 8084

echo "== placing order while shipping is down =="
ORDER=$(curl -s -m 30 -X POST "$GW/api/orders" -H "Content-Type: application/json" -d "{
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
setsid nohup mvn -q -pl shipping-service spring-boot:run > /tmp/opencode/shipping-service.log 2>&1 < /dev/null & disown
WAIT_UP 8084 http://localhost:8084/api/shipments/99999999

ok=1
[ "$STATUS" = "FAILED" ] || { echo "FAIL: order not FAILED"; ok=0; }
[ "$PAY" = "REFUNDED" ] || { echo "FAIL: payment not REFUNDED"; ok=0; }
[ "$AFTER" = "$BEFORE" ] || { echo "FAIL: stock not restored"; ok=0; }
[ $ok -eq 1 ] && echo "KILL-TEST PASSED (compensated: refunded + restocked)" || { echo "KILL-TEST FAILED"; exit 1; }