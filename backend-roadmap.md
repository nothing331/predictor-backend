# Backend Build Order for a Polymarket‑Style Prediction Market

This document describes the **exact step‑by‑step order** for building the backend of a game‑based prediction market using fake currency and LMSR pricing. The goal is correctness, clarity, and long‑term extensibility — not premature optimization.

---

## Step 0: Define Invariants (Before Writing Code)

Before creating any files, clearly write down the rules that must *never* be violated:

1. A trade always produces a deterministic price change.
2. A user can never spend more currency than they have.
3. Market resolution payouts are correct and irreversible.
4. Restarting the server does not change outcomes.

Every line of backend code exists to protect these invariants.

---

## Step 1: Initialize Backend Repository (No Server Yet)

Create a new Git repository, for example:

```
prediction-market-backend/
```

Initial structure:

```
/core
  lmsr.ts
  market.ts
  trade.ts
/tests
  lmsr.test.ts
```

At this stage:
- No HTTP server
- No database
- No Redis
- No WebSockets

You are building a **pure logic engine**.

---

## Step 2: Implement LMSR as Pure Functions

Create an LMSR module that:
- Takes current market state (`qYes`, `qNo`, `b`)
- Computes YES and NO prices
- Computes the cost to buy Δ shares

Rules:
- No side effects
- No global state
- No I/O

Example conceptual API:
- `getPrices(qYes, qNo, b)`
- `costToBuy(outcome, deltaShares)`

Write tests to verify:
- Buying YES increases YES price
- Buying YES decreases NO price
- Prices always sum to ~1

This is the mathematical core of the system.

---

## Step 3: Define Market State (Still No Server)

Create a `market` module that represents **state only**.

A market includes:
- Market ID
- `qYes`, `qNo`
- Liquidity parameter `b`
- Status: `OPEN | RESOLVED`
- Resolved outcome (nullable)

Add deterministic functions:
- `getPrice(market)`
- `applyTrade(market, trade)`

Given the same inputs, the output must always be identical.

---

## Step 4: Add User Balances and Positions

Introduce minimal user economics:

User data:
- Balance
- Positions per market (YES shares, NO shares)

Trade logic:
- Validate sufficient balance before trade
- Deduct cost after trade
- Update user positions

Still:
- In‑memory only
- Hardcoded users are fine

Goal:
> Simulate multiple trades and verify balances and prices make sense.

---

## Step 5: Implement Market Resolution Logic

Write a dedicated resolution function:
- `resolveMarket(market, outcome)`

Resolution must:
- Lock the market
- Compute payouts
- Credit winning positions
- Mark positions as settled

Add tests:
- YES holders paid correctly
- NO holders paid correctly
- Double resolution throws an error

If this step is wrong, the system is fundamentally broken.

---

## Step 6: Add Persistence Layer

Introduce a database (Postgres or MongoDB).

Persist:
- Markets
- Users
- Positions
- Trades

Implement:
- Load state at startup
- Persist after every trade
- Persist after resolution

Restart the server and verify:
- Prices are unchanged
- Balances are unchanged

This step separates toys from real systems.

---

## Step 7: Add a Thin HTTP API Layer

Expose minimal endpoints:

- `GET /markets/:id`
- `POST /trade`
- `POST /resolve` (admin only)

Rules:
- Controllers contain no business logic
- All math and rules live in core modules

If math appears in controllers, refactor immediately.

---

## Step 8: Add WebSockets (Broadcast Only)

WebSockets must:
- Broadcast price updates
- Broadcast trade events
- Broadcast market resolution

They must **never** change state.

State transitions occur only through core logic.

---

## Step 9: Stress and Abuse Testing

Actively try to break the system:
- Trade with zero balance
- Resolve early
- Resolve twice
- Spam trades rapidly

The backend should fail safely and consistently.

---

## Step 10: Freeze and Document

Before building the frontend:
- Document data models
- Document trade lifecycle
- Document resolution rules

If you cannot explain the system clearly, it is not finished.

---

## Core Mental Model

Backend ≠ server

Backend = **rules of reality**

Servers, APIs, and WebSockets are simply ways for users to interact with those rules.

Build the physics engine first. Everything else is presentation.

