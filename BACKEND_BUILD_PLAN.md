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

## Fixed Decisions (LOCKED)

- Language: **Java**
- Pricing Model: **Canonical LMSR (log‑sum‑exp cost function)**
- Currency: **Free, fixed starting balance for all users**
- Markets: **Binary only (YES / NO)**
- Market Rules:
  - No trades after resolution
  - Markets never reopen
  - Liquidity parameter `b` fixed per market
- Numeric Precision:
  - Shares: `double`
  - Balances & payouts: `BigDecimal`

---

# ✅ WEEK 1 — Market Math & Core Logic (COMPLETED)

**Goal:** Build and lock the mathematical and state foundation of the market.

🚫 No Spring Boot  
🚫 No database  
🚫 No APIs  

Only pure Java logic and unit tests.

---

## ✅ What Was Implemented

### Core Packages

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

### LMSR Pricing Engine (`core.lmsr.PricingEngine`)

- Canonical LMSR cost function:
  - `C(qYes, qNo) = b × ln(e^(qYes/b) + e^(qNo/b))`
- Log‑sum‑exp stabilization for numerical safety
- Derived price functions (no stored prices)
- Cost‑to‑buy calculations based on cost deltas

**Key Properties Guaranteed:**
- Prices always sum to 1
- Prices are always strictly between 0 and 1
- Cost is monotonic and non‑negative
- Deterministic behavior (same inputs → same outputs)

---

### Market State (`core.market.Market`)

- Stores only **minimal state**:
  - `qYes`, `qNo`, `b`, `status`, `resolvedOutcome`
- Prices are derived dynamically via `PricingEngine`
- Buy‑only trades (Week 1 scope)
- Market lifecycle enforced:
  - OPEN → RESOLVED

---

## ✅ Week 1 Test Coverage (LOCKED)

### PricingEngine Tests

Located at:
```
src/test/java/core/lmsr/PricingEngineTest.java
```

**Invariants Covered:**
- YES price + NO price ≈ 1
- Prices ∈ (0, 1)
- Symmetry when swapping YES/NO shares
- Liquidity extremes (low and high `b`)
- Cost function correctness:
  - `C(0,0,b) = b × ln(2)`
  - Cost increases when shares are added
  - Symmetry of cost function
  - Direct vs overflow‑protected consistency

✅ LMSR math is now considered **final and locked**.

---

## ✅ Week 1 Exit Criteria (Met)

- Canonical LMSR implemented correctly
- Market state separated from pricing math
- No stored prices or side effects
- All critical invariants enforced by tests

✅ **Week 1 is officially complete.**

---

# ✅ WEEK 2 — Users, Balances & Trade Engine (COMPLETED)

**Goal:** Introduce money and enforce fairness while preserving Week‑1 invariants.

✅ Week 2 exit criteria met: users, balances, atomic trades, and mandatory tests are locked. Proceed to Week 3.

---

## Concepts Introduced

- Users
- Balances (free currency)
- Positions (per market, per outcome)
- Atomic trades

---

## New Core Packages (Planned)

```
core/
 ├─ user/
 │   ├─ User
 │   └─ Position
 ├─ trade/
 │   ├─ Trade
 │   └─ TradeEngine
```

---

## User Model

- Fields:
  - `userId`
  - `balance` (`BigDecimal`)
  - `positions` (marketId → YES/NO shares)
- All users start with a **fixed initial balance** (e.g. 1000)

---

## Trade Engine (Critical Logic)

Trades must be **atomic**.

### Trade Flow (Order Is Non‑Negotiable)

1. Calculate trade cost using `PricingEngine`
2. Validate sufficient user balance
3. Apply market share update
4. Deduct user balance
5. Update user position

If any step fails → **entire trade fails**.

---

## Mandatory Week 2 Tests

- Cannot trade with insufficient balance
- Balance decreases exactly by trade cost
- Shares update correctly per user and market
- Failed trades do not mutate any state

🚫 Do NOT proceed to Week 3 until all Week‑2 tests pass.

---

# WEEK 3 — Market Resolution & Payouts

**Goal:** Finish the market lifecycle.

- Resolve only OPEN markets
- Resolution is final and irreversible
- Winning shares pay out 1 unit currency
- Losing shares pay out 0

---

# WEEK 4 — Persistence Layer

**Goal:** Survive server restarts.

- Persist users, markets, positions, trades
- Load state on startup
- Persist after every trade and resolution

---

# WEEK 5 — API Layer (Spring Boot)

**Goal:** Expose functionality safely.

🚨 Controllers must not contain business logic.

---

# WEEK 6 — WebSockets & Hardening

**Goal:** Real‑time updates and abuse prevention.

---

## Final Reminder

> Build the **physics engine first**.
> APIs, databases, and frontends are just interfaces to those rules.

If the core logic is correct, everything else becomes easy.
