#!/usr/bin/env bash
set -euo pipefail

GW=http://localhost:9000
EMAIL="e2e-$(date +%s)@example.com"

echo "== 1. register account via gateway =="
USER=$(curl -s -X POST "$GW/api/users" -H "Content-Type: application/json" \
  -d "{\"name\":\"E2E Shopper\",\"email\":\"$EMAIL\",\"password\":\"secret123\",\"address\":\"21 Example Ave\"}")
echo "$USER" | python3 -c "import sys,json;d=json.load(sys.stdin)['user'];print('  account',d['id'],d['email'])"
TOKEN=$(echo "$USER" | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

echo "== 2. fetch catalog =="
echo "  catalog has $(curl -s "$GW/api/products?size=1" | python3 -c "import sys,json;print(json.load(sys.stdin)['totalElements'])") products"
PRODUCT_JSON=$(curl -s "$GW/api/products?size=1" | python3 -c "import sys,json;c=json.load(sys.stdin)['content'][0];print(c['id'],c['price'])")
PID=$(echo "$PRODUCT_JSON" | cut -d' ' -f1)
PRICE=$(echo "$PRODUCT_JSON" | cut -d' ' -f2)
echo "  first product id=$PID price=$PRICE"

echo "== 3. stock before =="
QTY_BEFORE=$(curl -s "$GW/api/inventory/$PID" | python3 -c "import sys,json;print(json.load(sys.stdin)['quantity'])")
echo "  product $PID stock: $QTY_BEFORE"

echo "== 3.5 pin payment failure-rate to 0 (deterministic happy path) =="
PAYPID=$(ss -tlnp 2>/dev/null | grep ":8083 " | grep -oP 'pid=\K[0-9]+' | head -1)
if [ -n "$PAYPID" ]; then
  kill "$PAYPID"
  while ss -tln 2>/dev/null | grep -q ":8083 "; do sleep 1; done
fi
setsid nohup mvn -q -pl payment-service spring-boot:run -Dspring-boot.run.arguments=--payment.failure-rate=0 > /tmp/opencode/payment-service.log 2>&1 < /dev/null & disown
until [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/api/payments/99999999 2>/dev/null)" != "000" ]; do sleep 2; done
echo "  payment failure-rate pinned to 0"

echo "== 4. place order for 2 units =="
ORDER=$(curl -s -X POST "$GW/api/orders" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "{
  \"customerName\":\"E2E Shopper\",\"customerEmail\":\"$EMAIL\",\"address\":\"21 Example Ave\",
  \"items\":[{\"productId\":$PID,\"quantity\":2,\"productName\":\"e2e product\",\"unitPrice\":$PRICE}]}")
OID=$(echo "$ORDER" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
echo "  order $OID -> $(echo "$ORDER" | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])")"

echo "== 5. verify cross-service state =="
sleep 1
echo "  order status: $(curl -s http://localhost:8080/api/orders/$OID | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])")"
echo "  payment rows: $(PGPASSWORD=shipwise docker exec shipwise-postgres psql -U shipwise -d shipwise_payment -tAc "SELECT count(*) FROM payments WHERE order_id=$OID;")"
echo "  shipment:     $(PGPASSWORD=shipwise docker exec shipwise-postgres psql -U shipwise -d shipwise_shipping -tAc "SELECT status FROM shipments WHERE order_id=$OID;")"
QTY_AFTER=$(curl -s "$GW/api/inventory/$PID" | python3 -c "import sys,json;print(json.load(sys.stdin)['quantity'])")
echo "  stock:  $QTY_BEFORE -> $QTY_AFTER (expected $((QTY_BEFORE-2)))"

STATUS=$(curl -s http://localhost:8080/api/orders/$OID | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])")
[ "$STATUS" = "CONFIRMED" ] && echo "HAPPY PATH OK" || { echo "ORDER NOT CONFIRMED: $STATUS"; exit 1; }