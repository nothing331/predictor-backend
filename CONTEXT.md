# Predictor Backend

A binary (YES/NO) prediction market backend. Users buy YES or NO shares on **Markets**;
when a market resolves, winning shares pay out 1.00 each via append-only ledger entries.

## Language

**Market**:
A binary YES/NO prediction question that users can trade on. Priced by LMSR.
_Avoid_: Event, question, bet (those are user-facing terms; "Market" is the domain term).

**Outcome**:
The answer to a Market — either `YES` or `NO`. A resolved Market has exactly one.
_Avoid_: Result, side, winner.

**Position**:
A single user's holdings (YES shares and NO shares) in one Market.
_Avoid_: Holding, stake.

**Trade**:
A user-initiated buy that mints shares in a Market and debits the user's balance.
_Avoid_: Order, bet, transaction (overloaded with DB transactions).

**Resolution**:
The act of declaring the winning Outcome on a Market. Transitions the Market from
`OPEN` to `RESOLUTION_PENDING`. Does not pay anyone out.
_Avoid_: Closing, settling (Settlement is a distinct step).

**Settlement**:
The act of paying out winning Positions and clearing them. Runs asynchronously
after Resolution.
_Avoid_: Payout (payout is the amount; settlement is the act), closing.

**Ledger Entry**:
An append-only row in `ledger_entries` recording one money movement. The financial
source of truth; `users.balance` is a projection.
_Avoid_: Transaction, journal entry.

**Idempotency Key**:
Per-operation unique string that prevents duplicate money movements on retry.
Format is type-specific (e.g. `BUY:{userId}:{clientRequestId}`,
`SETTLEMENT_CREDIT:{userId}:{marketId}`).

**Settlement Worker**:
An in-process Spring `@Scheduled` poller that drains `position_settlements`
rows via `FOR UPDATE SKIP LOCKED`, processing each in its own short
transaction. Every API replica runs one; the table is the only coordinator.
_Avoid_: Job runner, queue consumer (those terms imply external infrastructure).

**Settlement Job** *(informal)*:
The set of `position_settlements` rows enqueued by one Resolution for one
Market. There is no `settlement_jobs` table — the job exists implicitly while
any row for that Market is in the table.

## Market lifecycle

A Market is always in exactly one state:

- **`OPEN`** — Trading allowed. No Outcome yet.
- **`RESOLUTION_PENDING`** — Outcome recorded. Trading closed. Settlement in
  progress (workers paying out Positions); per-Position progress lives in
  `position_settlements`, not on the Market.
- **`RESOLVED`** — All Positions settled. Terminal happy state.
- **`SETTLEMENT_FAILED`** — One or more Positions exceeded max retry attempts or
  hit a poison condition. Terminal; requires explicit admin retry to leave.

Allowed transitions:

- `OPEN → RESOLUTION_PENDING` (via Resolution)
- `RESOLUTION_PENDING → RESOLVED` (Settlement Worker, when all Positions settled)
- `RESOLUTION_PENDING → SETTLEMENT_FAILED` (Settlement Worker, on terminal failure)
- `SETTLEMENT_FAILED → RESOLUTION_PENDING` (admin retry only)

**Invariant:** Any non-`OPEN` Market has a non-null `resolvedOutcome`. The DTO
layer keys on `resolvedOutcome != null` (not on the specific state) to decide
whether to emit LMSR prices or settled certainties (1.0 / 0.0).

## Relationships

- A **Market** has many **Positions** (at most one per User).
- A **Trade** mutates one **Position** and writes one `TRADE_DEBIT` **Ledger Entry**.
- A **Resolution** records the winning **Outcome** and enqueues Settlement work.
- **Settlement** of a winning **Position** writes one `SETTLEMENT_CREDIT` **Ledger Entry**.

## Example dialogue

> **Dev:** "When the operator resolves a Market, do users see their balance update right away?"
> **Domain expert:** "No — Resolution just records the Outcome and moves the Market to
> `RESOLUTION_PENDING`. Settlement runs async. Balances update as each Position is settled."
> **Dev:** "So a market in `RESOLUTION_PENDING` could have some users already paid out and others not?"
> **Domain expert:** "Yes. Per-Position state is in `position_settlements`. Once all rows are done,
> the worker flips the Market to `RESOLVED`."

## Flagged ambiguities

- "Settle" was used to mean both Resolution (declaring the outcome) and Settlement
  (paying out positions) — resolved: these are distinct steps. Resolution does not pay.
- "Closing a market" — avoid; use **Resolution** for declaring an Outcome.
- "Held cost" / "release held cost" — there is no escrow / hold model. Trade cost
  is debited at buy time and never refunded. Winning Positions receive a separate
  payout at Settlement; losing Positions receive nothing. Do not describe Settlement
  as "releasing" cost.
