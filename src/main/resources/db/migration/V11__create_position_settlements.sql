-- Work queue for async settlement. One row per Position that needs paying out
-- after a Market is resolved. Rows are inserted in the resolve TX, picked up by
-- the SettlementWorker via FOR UPDATE SKIP LOCKED, and DELETED on success.
-- FAILED rows stay for audit. See docs/adr/0002-async-settlement-via-postgres-queue.md.

CREATE TABLE IF NOT EXISTS market.position_settlements (
    market_id    VARCHAR(36) NOT NULL,
    user_id      VARCHAR(36) NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts     INT         NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    last_error   TEXT,
    next_run_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_position_settlements PRIMARY KEY (market_id, user_id),
    CONSTRAINT fk_ps_market FOREIGN KEY (market_id) REFERENCES market.markets(market_id),
    CONSTRAINT fk_ps_user   FOREIGN KEY (user_id)   REFERENCES market.users(user_id),
    CONSTRAINT chk_ps_status CHECK (status IN ('PENDING', 'FAILED'))
);

-- Worker poll query:
--   SELECT market_id, user_id FROM market.position_settlements
--    WHERE status = 'PENDING' AND next_run_at <= now()
--    ORDER BY next_run_at
--    FOR UPDATE SKIP LOCKED LIMIT 50;
-- Composite (status, next_run_at) supports the WHERE + ORDER BY directly and
-- keeps the migration portable to H2 (used in tests), which lacks partial
-- index support. Table is bounded (rows deleted on success), so index size
-- is dominated by in-flight + FAILED entries.
CREATE INDEX IF NOT EXISTS idx_ps_status_run
    ON market.position_settlements (status, next_run_at);
