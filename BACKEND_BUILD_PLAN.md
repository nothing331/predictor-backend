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

# ✅ WEEK 3 — Market Resolution & Payouts (COMPLETED)

**Goal:** Finish the market lifecycle.

✅ Completed outcomes:
- Resolve only OPEN markets
- Resolution is final and irreversible
- Winning shares pay out 1 unit currency
- Losing shares pay out 0

**Exit Criteria Met:**
- Settlement engine implemented and tested
- Deterministic payouts with full edge‑case coverage
- No trades or state mutation after resolution
- Positions cleared and protected against double settlement

✅ **Week 3 is officially complete. Proceed to Week 4.**

---

# ✅ WEEK 4 — Persistence Layer (COMPLETED)

**Goal:** Survive server restarts.

This week is about making the Week 1–3 engine restart‑safe. The rules do not
change; you are only adding storage for the existing state.

**What to achieve by the end of Week 4:**
- Stop losing state when the JVM exits
- Load the exact same markets, users, balances, and positions on startup
- Persist every successful trade and resolution immediately
- Fail fast if persisted data is missing or corrupt

---

## Step 1 — Define the persisted state (explicitly)

**Purpose:** Make sure you save only the minimal state required to fully restore
the system. Persist state, not derived values.

**How to do it:**
- List the exact fields per domain object and lock them.
- Avoid storing anything that can be derived (like prices).

**Files to update or add:**
- `BACKEND_BUILD_PLAN.md` (this section)
- `core/user/User.java`
- `core/user/Position.java`
- `core/market/Market.java`
- `core/trade/Trade.java`

**Persisted fields (minimum required):**
- User: `userId`, `balance`, positions per market (YES/NO shares)
- Market: `marketId`, `qYes`, `qNo`, `b`, `status`, `resolvedOutcome`
- Trade: `tradeId`, `userId`, `marketId`, `outcome`, `shares`, `cost`, `timestamp`

---

## Step 2 — Create persistence interfaces (ports)

**Purpose:** Keep persistence separate from business logic so Week 5 APIs can
re-use the same core logic without changes.

**How to do it:**
- Define simple repository interfaces for users, markets, and trades.
- Use “save all” and “load all” methods first (simple and reliable).

**Files to add:**
- `persistence/UserRepository.java`
- `persistence/MarketRepository.java`
- `persistence/TradeRepository.java`

**Methods to include:**
- `saveAll(Collection<T> items)`
- `loadAll()`

---

## Step 3 — Implement file-based repositories

**Purpose:** Provide a working persistence layer without introducing a database
yet. This is the simplest reliable storage for Week 4.

**How to do it:**
- Store each domain collection in a JSON file.
- Write atomically: write to temp file, then rename.
- On load, fail fast if a file is missing or invalid.

**Files to add:**
- `persistence/file/FileUserRepository.java`
- `persistence/file/FileMarketRepository.java`
- `persistence/file/FileTradeRepository.java`

**Data files (runtime):**
- `data/users.json`
- `data/markets.json`
- `data/trades.json`

---

## Step 4 — Add a persistence service (or coordinator)

**Purpose:** Centralize all save/load operations so the core engine calls one
place after every state change.

**How to do it:**
- Create a small coordinator that loads on startup and saves after mutations.
- Keep it thin; no business rules here.

**Files to add:**
- `persistence/PersistenceService.java`

---

## Step 5 — Load state at startup

**Purpose:** Restore the in‑memory engine to its last known state.

**How to do it:**
- On startup, load markets, users, and trades from disk.
- Rebuild in-memory maps from loaded objects.
- Validate references (e.g., positions reference existing markets).

**Files to update or add:**
- `PredictionMarketGame.java` (or your main bootstrap)
- `core/` engine wiring code where maps are built

---

## Step 6 — Persist after every trade and resolution

**Purpose:** Never lose a successful state mutation.

**How to do it:**
- After a successful trade, persist users, markets, and trades.
- After resolving a market, persist users and markets.
- Never persist on failed trades.

**Files to update:**
- `core/trade/TradeEngine.java`
- `core/market/Market.java` or `core/market/SettlementEngine.java` (if separate)

---

## Step 7 — Add persistence tests

**Purpose:** Prove that state survives a restart and that data is identical
after reload.

**How to do it:**
- Create test data, persist it, clear memory, reload, and compare.
- Verify resolved markets stay resolved and cannot be traded after reload.

**Files to add:**
- `src/test/java/persistence/PersistenceTest.java`

---

## Week 4 Exit Criteria

- State survives JVM restart with no data loss
- Every successful trade and resolution persists immediately
- Reloaded state is identical to pre‑shutdown state
- Corrupt or missing data fails fast and stops startup

✅ **Week 4 is officially complete. Proceed to Week 5.**

---

# ✅ WEEK 5 — API Layer (Spring Boot) (COMPLETED)

**Goal:** Expose functionality safely.

🚨 Controllers must not contain business logic.

✅ **Week 5 is officially complete. All core API endpoints are functional.**

---

## ✅ What Was Implemented

### API Architecture

```
api/
 ├─ controller/
 │   ├─ MarketController      # Market CRUD + resolution
 │   ├─ UserController        # User management
 │   └─ TradeController       # Trade execution
 ├─ dto/
 │   ├─ CreateMarketRequest   # Market creation payload
 │   ├─ GetAllMarket          # Market response
 │   ├─ CreateUserRequest     # User creation payload
 │   ├─ GetUsersRequest       # User response
 │   ├─ BuyRequest            # Trade execution payload
 │   └─ ResolveMarketRequest  # Resolution payload
 └─ exception/
     ├─ GlobalExceptionHandler  # Centralized error handling
     └─ ErrorResponse           # Standard error format
```

### REST API Endpoints

**Users:**
- `POST /v1/users/create` - Create a new user
- `GET /v1/users/` - List all users

**Markets:**
- `POST /v1/markets/create` - Create a new market
- `GET /v1/markets/` - List all markets (with optional status filter)
- `GET /v1/markets/{marketId}` - Get market by ID
- `POST /v1/markets/{marketId}/resolve` - Resolve a market

**Trades:**
- `POST /v1/trade/buy` - Execute a trade (budget-based share buying)

---

## Key Technical Decisions

### 1. In-Memory Stores for Performance
- Added `MarketStore` and `UserStore` with `ConcurrentHashMap`
- Eliminates disk I/O on every API read
- Thread-safe for concurrent requests
- Data loaded once at startup via `@PostConstruct`

### 2. Validation in Domain Models
- Moved validation logic from services to domain models
- `Market.validate()` and `User.validate()` methods
- Follows DDD pattern: entities validate themselves

### 3. Global Exception Handling
- `GlobalExceptionHandler` with `@RestControllerAdvice`
- Consistent error responses across all endpoints
- Proper HTTP status codes (400, 404, 500)

### 4. Thin Controllers
- Controllers contain ZERO business logic
- Only delegate to services and handle HTTP concerns
- Core logic preserved in `TradeEngine`, `SettlementEngine`

---

## Week 5 Exit Criteria

- ✅ Spring Boot application running on port 8080
- ✅ RESTful API exposes all core functionality
- ✅ Controllers contain no business logic
- ✅ Validation on DTOs with proper error messages
- ✅ Thread-safe concurrent access via in-memory stores
- ✅ Exception handling provides clean error responses
- ✅ All existing tests pass with new architecture

✅ **Week 5 is officially complete. Proceed to Week 6.**

---

# WEEK 6 — WebSockets & Hardening

**Goal:** Real‑time updates and abuse prevention.

---

## Final Reminder

> Build the **physics engine first**.
> APIs, databases, and frontends are just interfaces to those rules.

If the core logic is correct, everything else becomes easy.
