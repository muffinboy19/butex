#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
PASS=0
FAIL=0

check() {
  local name="$1"
  local expect_code="$2"
  local actual_code="$3"
  local body="$4"
  if [[ "$actual_code" == "$expect_code" ]] && echo "$body" | grep -q '"status":true'; then
    echo "✓ $name (HTTP $actual_code)"
    PASS=$((PASS + 1))
  else
    echo "✗ $name (HTTP $actual_code)"
    echo "  $body" | head -c 300
    echo ""
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Butex local API test ==="
echo "Base: $BASE"
echo ""

# Health
BODY=$(curl -sS -w "\n%{http_code}" "$BASE/api/v1/dummy")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
[[ "$CODE" == "200" ]] && echo "✓ GET /dummy (HTTP 200)" && PASS=$((PASS+1)) || { echo "✗ GET /dummy"; FAIL=$((FAIL+1)); }

# Catalog
for path in membership/plans membership/tiers; do
  BODY=$(curl -sS -w "\n%{http_code}" "$BASE/api/v1/$path")
  CODE=$(echo "$BODY" | tail -1)
  RESP=$(echo "$BODY" | sed '$d')
  check "GET /api/v1/$path" "200" "$CODE" "$RESP"
done

PLAN_DETAILS_ID=$(curl -sS "$BASE/api/v1/membership/plans" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][0]['activeDetails'][0]['id'])")
TIER_DETAILS_ID=$(curl -sS "$BASE/api/v1/membership/tiers" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][0]['activeDetails'][0]['id'])")
PLAN_ID=$(curl -sS "$BASE/api/v1/membership/plans" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][0]['id'])")
TIER_ID=$(curl -sS "$BASE/api/v1/membership/tiers" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][0]['id'])")
ALT_PLAN_DETAILS=$(curl -sS "$BASE/api/v1/membership/plans" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][1]['activeDetails'][0]['id'] if len(d['data'])>1 else d['data'][0]['activeDetails'][0]['id'])")
ALT_TIER_DETAILS=$(curl -sS "$BASE/api/v1/membership/tiers" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][1]['activeDetails'][0]['id'] if len(d['data'])>1 else d['data'][0]['activeDetails'][0]['id'])")

echo "Using planDetailsId=$PLAN_DETAILS_ID tierDetailsId=$TIER_DETAILS_ID"

# User
TS=$(date +%s)
BODY=$(curl -sS -w "\n%{http_code}" -X POST "$BASE/api/v1/users" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"API Test\",\"email\":\"apitest${TS}@example.com\",\"phone\":\"9${TS: -9}\"}")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "POST /users" "200" "$CODE" "$RESP"
USER_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

BODY=$(curl -sS -w "\n%{http_code}" "$BASE/api/v1/users/$USER_ID")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "GET /users/{id}" "200" "$CODE" "$RESP"

# Subscribe
BODY=$(curl -sS -w "\n%{http_code}" -X POST "$BASE/api/v1/users/$USER_ID/subscriptions" \
  -H "Content-Type: application/json" \
  -d "{\"planDetailsId\":$PLAN_DETAILS_ID,\"tierDetailsId\":$TIER_DETAILS_ID}")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "POST /subscriptions" "200" "$CODE" "$RESP"

BODY=$(curl -sS -w "\n%{http_code}" "$BASE/api/v1/users/$USER_ID/subscriptions/current")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "GET /subscriptions/current" "200" "$CODE" "$RESP"

BODY=$(curl -sS -w "\n%{http_code}" "$BASE/api/v1/users/$USER_ID/subscriptions")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "GET /subscriptions" "200" "$CODE" "$RESP"

BODY=$(curl -sS -w "\n%{http_code}" "$BASE/api/v1/users/$USER_ID/subscriptions/events")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "GET /subscriptions/events" "200" "$CODE" "$RESP"

# Checkout
BODY=$(curl -sS -w "\n%{http_code}" -X POST "$BASE/api/v1/users/$USER_ID/checkout/benefits" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"itemId":"SKU-1","categoryId":"GROCERIES","lineTotal":1000}]}')
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "POST /checkout/benefits" "200" "$CODE" "$RESP"

# Renew
BODY=$(curl -sS -w "\n%{http_code}" -X POST "$BASE/api/v1/users/$USER_ID/subscriptions/renew")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "POST /subscriptions/renew" "200" "$CODE" "$RESP"

# Change tier (only if user qualifies — GOLD needs 10 orders)
if [[ "$ALT_TIER_DETAILS" != "$TIER_DETAILS_ID" ]]; then
  BODY=$(curl -sS -w "\n%{http_code}" -X PUT "$BASE/api/v1/users/$USER_ID/subscriptions/tier" \
    -H "Content-Type: application/json" \
    -d "{\"newTierDetailsId\":$ALT_TIER_DETAILS}")
  CODE=$(echo "$BODY" | tail -1)
  RESP=$(echo "$BODY" | sed '$d')
  if [[ "$CODE" == "200" ]] && echo "$RESP" | grep -q '"status":true'; then
    echo "✓ PUT /subscriptions/tier (HTTP $CODE)"
    PASS=$((PASS + 1))
  elif [[ "$CODE" == "400" ]]; then
    echo "✓ PUT /subscriptions/tier (HTTP 400 — expected if user does not qualify)"
    PASS=$((PASS + 1))
  else
    echo "✗ PUT /subscriptions/tier (HTTP $CODE)"
    FAIL=$((FAIL + 1))
  fi
fi

# Change plan
if [[ "$ALT_PLAN_DETAILS" != "$PLAN_DETAILS_ID" ]]; then
  BODY=$(curl -sS -w "\n%{http_code}" -X PUT "$BASE/api/v1/users/$USER_ID/subscriptions/plan" \
    -H "Content-Type: application/json" \
    -d "{\"newPlanDetailsId\":$ALT_PLAN_DETAILS}")
  CODE=$(echo "$BODY" | tail -1)
  RESP=$(echo "$BODY" | sed '$d')
  check "PUT /subscriptions/plan" "200" "$CODE" "$RESP"
fi

# Membership admin: create plan details v2
BODY=$(curl -sS -w "\n%{http_code}" -X POST "$BASE/api/v1/membership/plans/$PLAN_ID/details" \
  -H "Content-Type: application/json" \
  -d '{"durationDays":30,"price":319.00,"changeNotes":"API test v2"}')
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "POST /membership/plans/{id}/details" "200" "$CODE" "$RESP"
NEW_PLAN_DETAILS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

# Update old plan details (non-default v1)
OLD_DETAILS=$(curl -sS "$BASE/api/v1/membership/plans" | python3 -c "
import sys,json
d=json.load(sys.stdin)
for p in d['data']:
  for ad in p['activeDetails']:
    if not ad.get('default', ad.get('isDefault')):
      print(ad['id']); exit()
print(d['data'][0]['activeDetails'][0]['id'])
")
BODY=$(curl -sS -w "\n%{http_code}" -X PUT "$BASE/api/v1/membership/plans/$PLAN_ID/details/$OLD_DETAILS" \
  -H "Content-Type: application/json" \
  -d '{"extraDiscountPercent":6.0,"changeNotes":"hotfix"}')
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "PUT /membership/plans/{id}/details/{id}" "200" "$CODE" "$RESP"

# Cancel
BODY=$(curl -sS -w "\n%{http_code}" -X POST "$BASE/api/v1/users/$USER_ID/subscriptions/cancel")
CODE=$(echo "$BODY" | tail -1)
RESP=$(echo "$BODY" | sed '$d')
check "POST /subscriptions/cancel" "200" "$CODE" "$RESP"

echo ""
echo "=== Summary: $PASS passed, $FAIL failed ==="
[[ "$FAIL" -eq 0 ]]
