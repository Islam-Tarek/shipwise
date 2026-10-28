#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

GW=http://localhost:9000
ORD=http://localhost:8080

if [ ! -f .env ]; then
  echo "missing .env (MAIL_USERNAME/MAIL_PASSWORD/MAIL_FROM) - see docs/phase-04.md" >&2
  exit 1
fi
set -a
. ./.env
set +a

: "${MAIL_USERNAME:?set MAIL_USERNAME in .env}"
: "${MAIL_PASSWORD:?set MAIL_PASSWORD in .env}"
: "${MAIL_FROM:?set MAIL_FROM in .env}"

echo "== 0. verify gmail imap login =="
python3 - <<'EOF' || { echo "  imap login failed" >&2; exit 1; }
import imaplib, os
try:
    m = imaplib.IMAP4_SSL("imap.gmail.com", 993, timeout=30)
    m.login(os.environ["MAIL_USERNAME"], os.environ["MAIL_PASSWORD"])
    m.select("INBOX")
    m.logout()
    print("  imap login ok")
except Exception as e:
    print("  imap login FAILED:", e)
    raise SystemExit(1)
EOF

echo "== 0.5 payment failure-rate already 0 via compose (deterministic) =="

echo "== 0.75 start rabbitmq if needed =="
./scripts/start-rabbitmq.sh

EMAIL="$MAIL_USERNAME"
NAME="Notify E2E $(date +%s)"

echo "== 1. register/login account (recipient = own gmail) =="
USER=$(curl -s -X POST "$GW/api/users" -H "Content-Type: application/json" \
  -d "{\"name\":\"$NAME\",\"email\":\"$EMAIL\",\"password\":\"secret123\",\"address\":\"21 Example Ave\"}")
TOKEN=$(echo "$USER" | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])" 2>/dev/null || true)
if [ -z "$TOKEN" ]; then
  echo "  account already exists - logging in"
  USER=$(curl -s -X POST "$GW/api/users/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"secret123\"}")
  TOKEN=$(echo "$USER" | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
fi
echo "  authenticated as $EMAIL"

place_order() {
  PID=$(curl -s "$GW/api/products?size=1" | python3 -c "import sys,json;c=json.load(sys.stdin)['content'][0];print(c['id'])")
  PRICE=$(curl -s "$GW/api/products?size=1" | python3 -c "import sys,json;c=json.load(sys.stdin)['content'][0];print(c['price'])")
  curl -s -X POST "$GW/api/orders" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "{
    \"customerName\":\"$NAME\",\"customerEmail\":\"$EMAIL\",\"address\":\"21 Example Ave\",
    \"items\":[{\"productId\":$PID,\"quantity\":1,\"productName\":\"e2e product\",\"unitPrice\":$PRICE}]}"
}

order_status() { # $1 oid
  curl -s "$ORD/api/orders/$1" | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])"
}

order_total() { # $1 oid
  curl -s "$ORD/api/orders/$1" | python3 -c "import sys,json;print(json.load(sys.stdin)['totalAmount'])"
}

outbox_sent() { # $1 oid -> 1 if sent_at set
  PGPASSWORD=shipwise docker exec shipwise-postgres psql -U shipwise -d shipwise_orders -tAc \
    "SELECT (sent_at IS NOT NULL)::int FROM order_outbox WHERE order_id=$1;" | tr -d ' '
}

queue_ready() { # -> 1 when order-notifications queue is empty
  docker exec shipwise-rabbitmq rabbitmqctl list_queues name messages_ready -q 2>/dev/null \
    | awk '$1=="order-notifications" {print ($2==0) ? 1 : 0}'
}

wait_email() { # $1 oid $2 body-token -> prints FOUND/NOT-FOUND
  local oid="$1" token="$2"
  python3 - "$oid" "$token" <<'EOF'
import imaplib, email, os, sys, time
from email.header import decode_header, make_header
oid, body_token = sys.argv[1], sys.argv[2]
subject = "ShipWise order #%s confirmed" % oid
deadline = time.time() + 300
found = False
while time.time() < deadline and not found:
    conn = None
    try:
        conn = imaplib.IMAP4_SSL("imap.gmail.com", 993, timeout=30)
        conn.login(os.environ["MAIL_USERNAME"], os.environ["MAIL_PASSWORD"])
        conn.select("INBOX")
        typ, data = conn.search(None, "ALL")
        ids = (data[0] or b"").split()
        for num in ids[-40:]:
            try:
                typ, msg_data = conn.fetch(num, "(RFC822)")
            except Exception:
                continue
            if typ != "OK" or not msg_data or msg_data[0] is None:
                continue
            msg = email.message_from_bytes(msg_data[0][1])
            raw = msg.get("Subject") or ""
            try:
                subj = str(make_header(decode_header(raw)))
            except Exception:
                subj = str(raw)
            if subject in subj:
                body = ""
                if msg.is_multipart():
                    for part in msg.walk():
                        if part.get_content_type() == "text/plain":
                            body = (part.get_payload(decode=True) or b"").decode(errors="replace")
                            break
                else:
                    body = (msg.get_payload(decode=True) or b"").decode(errors="replace")
                if body_token in body:
                    found = True
                    break
    except Exception:
        pass
    finally:
        if conn is not None:
            try:
                conn.logout()
            except Exception:
                pass
    if not found:
        time.sleep(3)
print("FOUND" if found else "NOT-FOUND")
EOF
}

echo
echo "== PART A: happy path with broker up =="
ORDER1=$(place_order)
OID1=$(echo "$ORDER1" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
echo "  order $OID1 -> $(echo "$ORDER1" | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])")"
[ "$(order_status "$OID1")" = "CONFIRMED" ] || { echo "  order $OID1 not confirmed" >&2; exit 1; }

echo "== 2. outbox row drained (sent_at set) =="
for i in $(seq 1 15); do
  [ "$(outbox_sent "$OID1")" = "1" ] && break
  sleep 1
done
[ "$(outbox_sent "$OID1")" = "1" ] || { echo "  outbox sent_at NOT set for order $OID1" >&2; exit 1; }
echo "  outbox sent_at set for order $OID1"

echo "== 3. rabbit queue drained =="
for i in $(seq 1 15); do
  [ "$(queue_ready)" = "1" ] && break
  sleep 1
done
[ "$(queue_ready)" = "1" ] || { echo "  queue order-notifications not drained" >&2; exit 1; }
echo "  queue order-notifications empty"

echo "== 4. confirmation email arrived in gmail inbox =="
TOTAL1=$(order_total "$OID1")
RESULT=$(wait_email "$OID1" "$TOTAL1")
echo "  email check: $RESULT"
[ "$RESULT" = "FOUND" ] || { echo "  email for order $OID1 not found" >&2; exit 1; }
echo "  PART A OK"

echo
echo "== PART B: outage demo (rabbitmq down, event must not be lost) =="
echo "== 5. stop rabbitmq =="
./scripts/stop-rabbitmq.sh
sleep 2

echo "== 6. place order while broker is down =="
ORDER2=$(place_order)
OID2=$(echo "$ORDER2" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
STATUS2=$(echo "$ORDER2" | python3 -c "import sys,json;print(json.load(sys.stdin)['status'])")
echo "  order $OID2 -> $STATUS2 (must be CONFIRMED without broker)"
[ "$STATUS2" = "CONFIRMED" ] || { echo "  order $OID2 not confirmed" >&2; exit 1; }

echo "== 7. outbox buffers (sent_at stays NULL while broker down) =="
sleep 4
[ "$(outbox_sent "$OID2")" = "0" ] || { echo "  outbox sent_at should NOT be set while broker is down" >&2; exit 1; }
echo "  order $OID2 event buffered in outbox (sent_at NULL)"

echo "== 8. bring rabbitmq back - dispatcher flushes =="
./scripts/start-rabbitmq.sh
for i in $(seq 1 20); do
  [ "$(outbox_sent "$OID2")" = "1" ] && break
  sleep 1
done
[ "$(outbox_sent "$OID2")" = "1" ] || { echo "  buffered event for order $OID2 was NOT flushed" >&2; exit 1; }
echo "  buffered event flushed (sent_at set) after broker recovery"

echo "== 9. second confirmation email arrived =="
TOTAL2=$(order_total "$OID2")
RESULT2=$(wait_email "$OID2" "$TOTAL2")
echo "  email check: $RESULT2"
[ "$RESULT2" = "FOUND" ] || { echo "  email for order $OID2 not found" >&2; exit 1; }
echo "  PART B OK"

echo
echo "NOTIFICATION-TEST PASSED"