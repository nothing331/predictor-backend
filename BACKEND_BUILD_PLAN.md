# Prediction Market Backend – Week by Week Build Plan

This document is the **authoritative guide** for building a Polymarket‑style prediction market backend using **Java**, **free currency**, and **binary (YES/NO) markets**.

The goal is **correctness first**, then persistence, then APIs, then real‑time updates.

---

## Core Philosophy

- Backend = rules of reality, not APIs
- All business logic lives **outside controllers**
- APIs only call core logic and return data
- Same input must always produce the same output

If these rules are followed, the system will scale safely.

---

## Fixed Decisions

- Language: **Java**
- Framework (later): **Spring Boot**
- Currency: **Free, fixed starting balance for all users**
- Markets: **Binary only (YES / NO)**
- Users: **Real users, production‑ready**

---

# WEEK 1 — Market Math & Core Logic (NO SERVER)

**Goal:** Build the mathematical and state foundation of the market.

🚫 No Spring Boot
🚫 No database
🚫 No APIs

Only pure Java logic and unit tests.

---

## Concepts Introduced

- LMSR pricing
- Market state
- Deterministic price changes

---

## Packages to Create (Conceptual)

```
core/
 ├─ lmsr/
 │   └─ PricingEngine
 ├─ market/
 │   ├─ Market
 │   ├─ MarketStatus
 │   └─ Outcome
```

---

## Functions to Implement (START HERE)

### 1. LMSR Pricing Functions

These must be **pure functions**.

- `double getYesPrice(double qYes, double qNo, double b)`
- `double getNoPrice(double qYes, double qNo, double b)`
- `double costToBuyYes(double qYes, double qNo, double b, double shares)`
- `double costToBuyNo(double qYes, double qNo, double b, double shares)`

Rules:
- No side effects
- No state
- Same input → same output

---

### 2. Market State Logic

Market fields:
- `marketId`
- `question`
- `qYes`
- `qNo`
- `b`
- `status (OPEN / RESOLVED)`
- `resolvedOutcome (YES / NO / null)`

Market functions:
- `getCurrentYesPrice()`
- `getCurrentNoPrice()`
- `applyTrade(Outcome outcome, double shares)`

---

## Mandatory Tests (Do Not Skip)

You must write unit tests that verify:

- YES price + NO price ≈ 1
- Buying YES increases YES price
- Buying YES decreases NO price
- Deterministic behavior (same inputs → same outputs)

✅ Do not proceed to Week 2 until all tests pass.

---

# WEEK 2 — Users, Balances & Trade Engine

**Goal:** Introduce money and enforce fairness.

## Concepts

- Users
- Balances
- Positions
- Atomic trades

## Logic Added

- Fixed starting balance for users (e.g. 1000)
- Balance validation before trades
- Position tracking per market

## Trade Flow (Order Is Critical)

1. Calculate cost
2. Validate balance
3. Apply market share update
4. Deduct user balance
5. Update user position

---

# WEEK 3 — Market Resolution & Payouts

**Goal:** Finish the market lifecycle.

## Resolution Rules

- Only OPEN markets can be resolved
- Resolution can happen only once
- Outcome is final and irreversible

## Payout Logic

- 1 winning share = 1 unit currency
- Losing shares = 0
- Positions are locked after settlement

---

# WEEK 4 — Persistence Layer

**Goal:** Survive server restarts.

## Data to Persist

- Users
- Markets
- Positions
- Trades

## Rules

- Load state on startup
- Persist after every trade
- Persist after every resolution

---

# WEEK 5 — API Layer (Spring Boot)

**Goal:** Expose functionality safely.

## Required APIs

### Authentication

```
POST /auth/register
POST /auth/login
```

### Markets

```
GET /markets
GET /markets/{id}
POST /markets            (admin)
POST /markets/{id}/resolve (admin)
```

### Trading

```
POST /trade
```

### User

```
GET /me
GET /me/balance
GET /me/positions
```

🚨 Controllers must not contain business logic.

---

# WEEK 6 — WebSockets & Hardening

**Goal:** Real‑time updates and safety.

## WebSocket Events

- Price updates
- Trades
- Market resolution

## Abuse Testing

- Trade with zero balance
- Trade negative shares
- Resolve twice
- Resolve invalid market

---

## Final Reminder

> Build the **physics engine first**.
> APIs, databases, and frontends are just interfaces to those rules.

If the core logic is correct, everything else becomes easy.
