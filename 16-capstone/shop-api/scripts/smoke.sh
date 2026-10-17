set -euo pipefail

BASE=${BASE:-http://localhost:8080}
ADMIN_EMAIL=${ADMIN_EMAIL:-admin@example.com}
ADMIN_PASSWORD=${ADMIN_PASSWORD:?set ADMIN_PASSWORD}
STAMP=$(date +%s)
CUSTOMER="smoke-$STAMP@example.com"

field() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)"; }
status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

echo "health: $(curl -s "$BASE/actuator/health")"
ADMIN_TOKEN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | field "['token']")
echo "admin token received: $([ -n "$ADMIN_TOKEN" ] && echo yes)"

PRODUCT_ID=$(curl -s -X POST "$BASE/api/products" -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"SMOKE-$STAMP\",\"name\":\"Smoke test product\",\"priceCents\":250000,\"stock\":5}" | field "['id']")
echo "created product $PRODUCT_ID with stock 5"

echo "register a customer: $(status -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' -d "{\"email\":\"$CUSTOMER\",\"password\":\"password-1\"}")"
TOKEN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$CUSTOMER\",\"password\":\"password-1\"}" | field "['token']")

ORDER='{"lines":[{"productId":'"$PRODUCT_ID"',"quantity":2}]}'
FIRST=$(curl -s -D /tmp/smoke-headers -X POST "$BASE/api/orders" -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: smoke-1' -H 'Content-Type: application/json' -d "$ORDER")
echo "first order: id $(echo "$FIRST" | field "['id']"), total $(echo "$FIRST" | field "['totalCents']"), replayed $(grep -i '^idempotent-replayed' /tmp/smoke-headers | tr -d '\r' | cut -d' ' -f2)"
SECOND=$(curl -s -D /tmp/smoke-headers -X POST "$BASE/api/orders" -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: smoke-1' -H 'Content-Type: application/json' -d "$ORDER")
echo "same request again: id $(echo "$SECOND" | field "['id']"), replayed $(grep -i '^idempotent-replayed' /tmp/smoke-headers | tr -d '\r' | cut -d' ' -f2)"
echo "stock now: $(curl -s "$BASE/api/products/$PRODUCT_ID" | field "['stock']")"

echo "too many items: $(status -X POST "$BASE/api/orders" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"lines":[{"productId":'"$PRODUCT_ID"',"quantity":9}]}')"
echo "orders without a token: $(status "$BASE/api/orders")"
echo "customer creating a product: $(status -X POST "$BASE/api/products" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"sku":"X","name":"x","priceCents":1,"stock":1}')"
echo "my orders: $(curl -s "$BASE/api/orders" -H "Authorization: Bearer $TOKEN" | field "['totalElements']")"
echo "OpenAPI paths: $(curl -s "$BASE/v3/api-docs" | field "['paths'].keys().__len__()")"
