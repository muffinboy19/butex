# Butex API Reference

**Base URL (local):** `http://localhost:8080`  
**Base URL (hosted):** `https://butex-8fr2.onrender.com`  
**Swagger UI:** `/swagger-ui/index.html`  
*(Hosted app on Render free tier may take ~50s to wake on first request.)*

### Hosted curl (copy-paste)

```bash
# Plans & tiers
curl https://butex-8fr2.onrender.com/api/v1/membership/plans
curl https://butex-8fr2.onrender.com/api/v1/membership/tiers

# Create user
curl -X POST https://butex-8fr2.onrender.com/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo","email":"demo@example.com","phone":"9000000001"}'

# Subscribe (use planDetailsId / tierDetailsId from plans & tiers responses)
curl -X POST https://butex-8fr2.onrender.com/api/v1/users/1/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"planDetailsId":1,"tierDetailsId":1}'

# Current subscription
curl https://butex-8fr2.onrender.com/api/v1/users/1/subscriptions/current

# Subscription history
curl https://butex-8fr2.onrender.com/api/v1/users/1/subscriptions

# Cancel
curl -X POST https://butex-8fr2.onrender.com/api/v1/users/1/subscriptions/cancel

# Renew
curl -X POST https://butex-8fr2.onrender.com/api/v1/users/1/subscriptions/renew

# Change tier
curl -X PUT https://butex-8fr2.onrender.com/api/v1/users/1/subscriptions/tier \
  -H "Content-Type: application/json" \
  -d '{"tierDetailsId":2}'

# Change plan
curl -X PUT https://butex-8fr2.onrender.com/api/v1/users/1/subscriptions/plan \
  -H "Content-Type: application/json" \
  -d '{"planDetailsId":2}'

# Checkout benefits
curl -X POST https://butex-8fr2.onrender.com/api/v1/users/1/checkout/benefits \
  -H "Content-Type: application/json" \
  -d '{"items":[{"itemId":"SKU-1","categoryId":"GROCERIES","lineTotal":1000}]}'

# Get user
curl https://butex-8fr2.onrender.com/api/v1/users/1
```

All endpoints return a standard wrapper:

```json
{
  "status": true,
  "message": "Success",
  "data": { }
}
```

On error:

```json
{
  "status": false,
  "message": "Error description",
  "data": null
}
```

Validation errors (`400`) put field errors in `data`:

```json
{
  "status": false,
  "message": "Invalid request",
  "data": {
    "email": "email must be valid"
  }
}
```

---

## 1. Membership catalog

### GET `/api/v1/membership/plans`

List all active membership plans with pricing and benefits.

**Request:** none

**Response `200`:**

```json
{
  "status": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "code": "MONTHLY",
      "name": "Monthly Membership",
      "description": "Flexible monthly plan for FirstClub members",
      "activeDetails": [
        {
          "id": 1,
          "planId": 1,
          "version": 1,
          "durationDays": 30,
          "price": 299.00,
          "currency": "INR",
          "freeDeliveryEnabled": false,
          "extraDiscountPercent": 5.00,
          "exclusiveDealsAccess": false,
          "earlySaleAccess": false,
          "prioritySupport": false,
          "default": true
        }
      ]
    }
  ]
}
```

---

### GET `/api/v1/membership/tiers`

List all active membership tiers with eligibility rules and perks.

**Request:** none

**Response `200`:**

```json
{
  "status": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "code": "SILVER",
      "name": "Silver",
      "description": "Entry tier for new members",
      "rank": 1,
      "activeDetails": [
        {
          "id": 1,
          "tierId": 1,
          "version": 1,
          "minOrders": 0,
          "minMonthlyOrderValue": 0.00,
          "cohortCode": null,
          "freeDeliveryEnabled": false,
          "extraDiscountPercent": 5.00,
          "exclusiveDealsAccess": false,
          "earlySaleAccess": false,
          "prioritySupport": false,
          "default": true
        }
      ]
    }
  ]
}
```

---

## 2. Users

### POST `/api/v1/users`

Create a new user.

**Request body:**

```json
{
  "name": "Aarav Sharma",
  "email": "aarav@example.com",
  "phone": "9876543210",
  "cohortCode": "VIP"
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `name` | yes | |
| `email` | yes | Must be valid email, unique |
| `phone` | yes | Unique |
| `cohortCode` | no | Used for cohort-based tiers (e.g. Platinum VIP) |

**Response `200`:**

```json
{
  "status": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "Aarav Sharma",
    "email": "aarav@example.com",
    "phone": "9876543210",
    "active": true,
    "cohortCode": "VIP"
  }
}
```

**Errors:**
- `400` — validation failed / email already registered

---

### GET `/api/v1/users/{userId}`

Get user by ID.

**Response `200`:**

```json
{
  "status": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "Aarav Sharma",
    "email": "aarav.sharma@example.com",
    "phone": "9876543210",
    "active": true,
    "cohortCode": null
  }
}
```

**Errors:**
- `404` — `User not found: {userId}`

---

## 3. Subscriptions

Base path: `/api/v1/users/{userId}/subscriptions`

> Use `planDetailsId` and `tierDetailsId` from `/membership/plans` and `/membership/tiers` responses.  
> User must **qualify** for the chosen tier (orders, monthly spend, cohort).

---

### POST `/api/v1/users/{userId}/subscriptions`

Subscribe to a plan + tier.

**Request body:**

```json
{
  "planDetailsId": 1,
  "tierDetailsId": 1
}
```

**Response `200`:**

```json
{
  "status": true,
  "message": "Success",
  "data": {
    "id": 1,
    "userId": 1,
    "planDetailsId": 1,
    "tierDetailsId": 1,
    "planCode": "MONTHLY",
    "planName": "Monthly Membership",
    "tierCode": "SILVER",
    "tierName": "Silver",
    "startsAt": "2026-06-09T20:20:05",
    "expiresAt": "2026-07-09T20:20:05",
    "status": "ACTIVE",
    "subStatus": "NEW"
  }
}
```

**Errors:**
- `400` — already has active subscription / does not qualify for tier / invalid plan or tier
- `404` — user not found

---

### GET `/api/v1/users/{userId}/subscriptions/current`

Get the user's **current valid** active subscription (not expired).

**Response `200`:** same shape as subscribe `data` above.

**Errors:**
- `404` — no active subscription

---

### GET `/api/v1/users/{userId}/subscriptions`

Get full subscription history for the user.

**Response `200`:**

```json
{
  "status": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "planDetailsId": 1,
      "tierDetailsId": 1,
      "planCode": "MONTHLY",
      "planName": "Monthly Membership",
      "tierCode": "SILVER",
      "tierName": "Silver",
      "startsAt": "2026-05-31T00:49:32",
      "expiresAt": "2026-07-30T00:49:32",
      "status": "ACTIVE",
      "subStatus": "RENEWED"
    }
  ]
}
```

---

### POST `/api/v1/users/{userId}/subscriptions/renew`

Renew the active subscription. Extends `expiresAt` by the current plan duration.

**Request body:** none

**Response `200`:**

```json
{
  "status": true,
  "message": "Success",
  "data": {
    "id": 1,
    "userId": 1,
    "planCode": "MONTHLY",
    "tierCode": "SILVER",
    "status": "ACTIVE",
    "subStatus": "RENEWED",
    "expiresAt": "2026-07-30T00:49:32"
  }
}
```

**Errors:**
- `404` — no active subscription

---

### PUT `/api/v1/users/{userId}/subscriptions/plan`

Change membership plan (e.g. Monthly → Yearly). Resets start/expiry dates.

**Request body:**

```json
{
  "planDetailsId": 3
}
```

**Response `200`:** subscription object with updated `planCode`, `planName`, `startsAt`, `expiresAt`.

**Errors:**
- `400` — already on this plan / plan not active
- `404` — no active subscription

---

### PUT `/api/v1/users/{userId}/subscriptions/tier`

Upgrade or downgrade tier. User must qualify for the target tier.

**Request body:**

```json
{
  "tierDetailsId": 2
}
```

**Response `200`:** subscription object with updated `tierCode`, `tierName`.

**Errors:**
- `400` — does not qualify for tier / already on this tier
- `404` — no active subscription

---

### POST `/api/v1/users/{userId}/subscriptions/cancel`

Cancel the active subscription.

**Request body:** none

**Response `200`:**

```json
{
  "status": true,
  "message": "Success",
  "data": {
    "id": 1,
    "userId": 1,
    "status": "CANCELLED",
    "planCode": "MONTHLY",
    "tierCode": "SILVER"
  }
}
```

**Errors:**
- `404` — no active subscription

---

## 4. Checkout benefits

### POST `/api/v1/users/{userId}/checkout/benefits`

Calculate membership benefits and discounts for a cart at checkout.

**Request body:**

```json
{
  "items": [
    {
      "itemId": "SKU-DAIRY-001",
      "categoryId": "GROCERIES",
      "lineTotal": 500
    },
    {
      "itemId": "SKU-PHONE-200",
      "categoryId": "ELECTRONICS",
      "lineTotal": 15000
    }
  ]
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `items` | yes | At least one line item |
| `itemId` | yes | Product SKU |
| `categoryId` | yes | Category code |
| `lineTotal` | yes | Positive number |

**Response `200` (with active membership):**

```json
{
  "status": true,
  "message": "Success",
  "data": {
    "membershipActive": true,
    "planCode": "MONTHLY",
    "tierCode": "SILVER",
    "freeDelivery": false,
    "exclusiveDealsAccess": false,
    "earlySaleAccess": false,
    "prioritySupport": false,
    "cartSubtotal": 15500,
    "totalDiscountAmount": 25.00,
    "finalPayableAmount": 15475.00,
    "items": [
      {
        "itemId": "SKU-DAIRY-001",
        "categoryId": "GROCERIES",
        "lineTotal": 500,
        "appliedDiscountPercent": 5.00,
        "discountAmount": 25.00,
        "payableAmount": 475.00
      }
    ]
  }
}
```

**Response `200` (no active membership):**

```json
{
  "status": true,
  "message": "Success",
  "data": {
    "membershipActive": false,
    "planCode": null,
    "tierCode": null,
    "freeDelivery": false,
    "exclusiveDealsAccess": false,
    "earlySaleAccess": false,
    "prioritySupport": false,
    "cartSubtotal": 500,
    "totalDiscountAmount": 0,
    "finalPayableAmount": 500,
    "items": [
      {
        "itemId": "SKU-DAIRY-001",
        "categoryId": "GROCERIES",
        "lineTotal": 500,
        "appliedDiscountPercent": 0,
        "discountAmount": 0,
        "payableAmount": 500
      }
    ]
  }
}
```

---

## 5. HTTP status codes

| Code | When |
|------|------|
| `200` | Success |
| `400` | Business rule violation, validation error, bad request |
| `404` | User or subscription not found |
| `405` | Wrong HTTP method |
| `500` | Unexpected server error |

---

## 6. Quick test flow

**Hosted** (`BASE=https://butex-8fr2.onrender.com`):

```bash
BASE=https://butex-8fr2.onrender.com

# 1. List plans & tiers
curl $BASE/api/v1/membership/plans
curl $BASE/api/v1/membership/tiers

# 2. Create user
curl -X POST $BASE/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo","email":"demo@example.com","phone":"9000000001"}'

# 3. Subscribe (use ids from step 1)
curl -X POST $BASE/api/v1/users/1/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"planDetailsId":1,"tierDetailsId":1}'

# 4. Current subscription
curl $BASE/api/v1/users/1/subscriptions/current

# 5. Checkout benefits
curl -X POST $BASE/api/v1/users/1/checkout/benefits \
  -H "Content-Type: application/json" \
  -d '{"items":[{"itemId":"SKU-1","categoryId":"GROCERIES","lineTotal":1000}]}'

# 6. Renew
curl -X POST $BASE/api/v1/users/1/subscriptions/renew

# 7. Cancel
curl -X POST $BASE/api/v1/users/1/subscriptions/cancel
```

**Local** (`BASE=http://localhost:8080`):

```bash
BASE=http://localhost:8080

curl $BASE/api/v1/membership/plans
curl -X POST $BASE/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo","email":"demo@example.com","phone":"9000000001"}'
curl -X POST $BASE/api/v1/users/1/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"planDetailsId":1,"tierDetailsId":1}'
```

---

## 7. Subscription & tier enums

**`status`:** `ACTIVE` | `CANCELLED` | `EXPIRED`

**`subStatus`:** `NEW` | `RENEWED`

**Tier codes:** `SILVER` | `GOLD` | `PLATINUM`

**Plan codes:** `MONTHLY` | `QUARTERLY` | `YEARLY`
