# Butex — FirstClub Membership Backend

A Spring Boot backend for FirstClub’s membership program. Users can pick a plan, choose a tier, subscribe, change tier, cancel, and track their membership.

Built as part of a backend task — repo name **butex** is intentional.

---

## What we built

### Membership plans
- **Monthly**, **Quarterly**, and **Yearly** plans
- Each plan has its own price, duration, and benefits (discount %, free delivery, exclusive deals, etc.)
- Plans are versioned — so pricing can change over time without breaking old subscriptions

### Membership tiers
- **Silver**, **Gold**, and **Platinum** tiers
- Each tier has rank, perks, and eligibility rules (min orders, monthly spend, cohort)
- Users can **upgrade or downgrade** tier on an active subscription
- **Automated tier promotion** — a daily cron checks active subscriptions and upgrades users who qualify for a higher tier based on:
  - Total order count (≥ tier’s `minOrders`)
  - Monthly order spend (≥ tier’s `minMonthlyOrderValue`)
  - Cohort match (user’s `cohortCode` matches tier’s `cohortCode`, when set)

### Subscriptions
- Subscribe to a **plan + tier** together
- View **current** active membership (with start & expiry dates)
- View full **subscription history**
- **Cancel** an active subscription
- **Change tier** (upgrade / downgrade)

### Users
- Create a user (name, email, phone)
- Fetch user by ID

### Benefits & discounts (data model)
- Plan and tier benefits are stored on each plan/tier version
- Separate **discount rule** tables for item/category-level discounts
- Good foundation for checkout integration later

### Audit trail
- **Plan details history** — when plan pricing/versions were created or changed
- **User subscription history** — subscribe, cancel, tier change, renew, expire events

### Other good stuff
- Clean layered structure: `controller → service → repository → entity`
- Consistent API wrapper (`status`, `message`, `data`)
- Global exception handling + request validation
- Basic concurrency guard on subscribe / cancel / tier change (`synchronized`)
- PostgreSQL on **Neon** for cloud DB (local profile for dev, prod profile for hosting)

---

## Tech stack

- Java 17
- Spring Boot 4
- Spring Data JPA
- PostgreSQL (Neon)
- Lombok
- Maven

---

## API endpoints

| Method | Endpoint | What it does |
|--------|----------|--------------|
| `GET` | `/api/v1/membership/plans` | List active plans with pricing & benefits |
| `GET` | `/api/v1/membership/tiers` | List active tiers with perks & criteria |
| `POST` | `/api/v1/users` | Create a user |
| `GET` | `/api/v1/users/{userId}` | Get user details |
| `POST` | `/api/v1/users/{userId}/subscriptions` | Subscribe to a plan + tier |
| `GET` | `/api/v1/users/{userId}/subscriptions/current` | Get active subscription |
| `GET` | `/api/v1/users/{userId}/subscriptions` | Get subscription history |
| `POST` | `/api/v1/users/{userId}/subscriptions/cancel` | Cancel active subscription |
| `PUT` | `/api/v1/users/{userId}/subscriptions/tier` | Upgrade / downgrade tier |

### Example: subscribe

```bash
curl -X POST http://localhost:8080/api/v1/users/1/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"planDetailsId": 1, "tierDetailsId": 1}'
```

Get `planDetailsId` and `tierDetailsId` from the `/plans` and `/tiers` responses first.

---

## Run locally

**Prerequisites:** Java 17+, Maven (or use `./mvnw`)

1. Create `src/main/resources/application-local.properties` (this file is gitignored):

```properties
spring.datasource.url=jdbc:postgresql://<your-neon-host>/neondb?sslmode=require
spring.datasource.username=<username>
spring.datasource.password=<password>
spring.datasource.driver-class-name=org.postgresql.Driver
```

2. Start the app:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

App runs at **http://localhost:8080**

---

## Deploy (Render / Railway / similar)

Set these environment variables:

| Variable | Example |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | your Neon user |
| `SPRING_DATASOURCE_PASSWORD` | your Neon password |

**Build:** `./mvnw clean package -DskipTests`  
**Start:** `java -jar target/butex-0.0.1-SNAPSHOT.jar`

Tables are auto-created/updated on startup (`ddl-auto=update`).

---

## Database

11 tables, including:

- `users`, `user_addresses`
- `plans`, `plan_details`, `plan_discount_rules`, `plan_details_history`
- `tiers`, `tier_details`, `tier_discount_rules`
- `subscriptions`, `user_subscription_history`
- `user_orders` (drives tier promotion checks)

Demo seed data (users, plans, tiers, subscriptions, etc.) lives in the Neon DB — not in the repo.

---

## Tier promotion cron

Runs on a schedule (default: **every day at 2:00 AM**). Configure in `application.properties`:

```properties
membership.tier-promotion.enabled=true
membership.tier-promotion.cron=0 0 2 * * *
```

Order data is read from the `user_orders` table. Users also need an optional `cohort_code` on the `users` table for Platinum-style cohort tiers.

---

## Not in scope yet

- Renew subscription API
- Checkout / benefit application at order time
- API to record orders (orders are expected in `user_orders` for the cron to use)

---

## Project structure

```
src/main/java/com/example/butex/
├── controller/     # REST APIs
├── service/        # Business logic
├── repository/     # DB access
├── entity/         # JPA models
├── dto/            # Request & response objects
├── exception/      # Error handling
└── enums/          # Status types, actions, etc.
```
