#!/usr/bin/env bash
set -euo pipefail

# The core concurrency guarantee: with a single unit left, exactly one of N
# parallel checkouts may succeed. Order service must reserve the last unit
# atomically; every other checkout fails cleanly and order goes to FAILED.

GW=http://localhost:9000
PID=1
N=${1:-50}

echo "== setting product $PID stock to exactly 1 unit =="
curl -s -X PUT -H "Content-Type: application/json" -d '{"quantity":1}' "$GW/api/inventory/$PID" >/dev/null
echo "  stock now: $(curl -s "$GW/api/inventory/$PID" | python3 -c "import sys,json;print(json.load(sys.stdin)['quantity'])")"

echo "== pinning payment failure-rate to 0 (deterministic race) =="
PAYPID=$(ss -tlnp 2>/dev/null | grep ":8083 " | grep -oP 'pid=\K[0-9]+' | head -1)
if [ -n "$PAYPID" ]; then
  kill "$PAYPID"
  while ss -tln 2>/dev/null | grep -q ":8083 "; do sleep 1; done
fi
setsid nohup mvn -q -pl payment-service spring-boot:run -Dspring-boot.run.arguments=--payment.failure-rate=0 > /tmp/opencode/payment-service.log 2>&1 < /dev/null & disown
until [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/api/payments/99999999 2>/dev/null)" != "000" ]; do sleep 2; done
echo "  payment failure-rate pinned to 0"

echo "== registering account for auth token =="
TOKEN=$(curl -s -X POST "$GW/api/users" -H "Content-Type: application/json" \
  -d '{"name":"Race Runner","email":"race'$RANDOM'@example.com","password":"secret123"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
echo "  token acquired (${TOKEN:0:8}...)"

echo "== firing $N parallel checkouts, each buying 1 unit =="
OUT=$(mktemp)
for i in $(seq 1 "$N"); do
  curl -s -X POST "$GW/api/orders" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "{
    \"customerName\":\"Race $i\",\"customerEmail\":\"race$i@example.com\",\"address\":\"x\",
    \"items\":[{\"productId\":$PID,\"quantity\":1,\"productName\":\"last unit\",\"unitPrice\":549.00}]}" \
    > "$OUT.$i" &
done
wait

CONFIRMED=$(grep -l '"CONFIRMED"' "$OUT".* | wc -l)
FAILED=$(grep -l '"FAILED"' "$OUT".* | wc -l)
REMAINING=$(curl -s "$GW/api/inventory/$PID" | python3 -c "import sys,json;print(json.load(sys.stdin)['quantity'])")

echo "  CONFIRMED: $CONFIRMED"
echo "  FAILED:    $FAILED"
echo "  stock left: $REMAINING"

ok=1
[ "$CONFIRMED" -eq 1 ] || { echo "FAIL: expected exactly 1 CONFIRMED, got $CONFIRMED"; ok=0; }
[ "$FAILED"  -eq $((N-1)) ] || { echo "FAIL: expected $((N-1)) FAILED, got $FAILED"; ok=0; }
[ "$REMAINING" -eq 0 ] || { echo "FAIL: expected 0 stock left, got $REMAINING"; ok=0; }

# reset product 1 quantity so later runs are unaffected
curl -s -X PUT -H "Content-Type: application/json" -d '{"quantity":99}' "$GW/api/inventory/$PID" >/dev/null

[ $ok -eq 1 ] && echo "LAST-UNIT TEST PASSED" || { echo "LAST-UNIT TEST FAILED"; exit 1; }