-- One-time safety net for Markets that were resolved synchronously under the
-- pre-async settlement code path AND somehow have Positions left with
-- settled=false. The old sync resolve held a single transaction across all
-- users, so this should be 0 rows in healthy environments; the migration is
-- shipped unconditionally so the deploy is safe even if a partial-settle
-- did slip through (e.g., a connection drop after row updates committed but
-- before the operator response was acknowledged).
--
-- Idempotent: the ON CONFLICT clause matches the (market_id, user_id) PK
-- on position_settlements, so re-running this migration (or running it on
-- a clean DB) produces zero new rows.
--
-- Markets in OPEN don't get enqueued — no outcome to settle against.
-- Markets in RESOLUTION_PENDING already had their rows inserted by the resolve
-- transaction; ON CONFLICT covers the overlap.
-- Markets in RESOLVED or SETTLEMENT_FAILED that still have unsettled positions
-- gain rows here; the worker drains them. RESOLVED markets stay RESOLVED (the
-- worker's flip-to-RESOLVED check no-ops when status != RESOLUTION_PENDING),
-- which is semantically correct: the market WAS fully resolved as far as the
-- old code knew; we're just retroactively paying out the missed users.
-- SETTLEMENT_FAILED markets stay there until admin explicitly retries.
--
-- See:
--   docs/adr/0002-async-settlement-via-postgres-queue.md
--   Q10 in the design grilling.

INSERT INTO market.position_settlements (market_id, user_id)
SELECT p.market_id, p.user_id
  FROM market.positions p
  JOIN market.markets m ON m.market_id = p.market_id
 WHERE p.settled = false
   AND m.status IN ('RESOLUTION_PENDING', 'RESOLVED', 'SETTLEMENT_FAILED')
ON CONFLICT (market_id, user_id) DO NOTHING;
