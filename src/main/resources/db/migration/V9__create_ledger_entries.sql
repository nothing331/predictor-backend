CREATE TABLE IF NOT EXISTS market.ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    amount_delta NUMERIC(20, 8) NOT NULL CHECK (amount_delta <> 0),
    type VARCHAR(40) NOT NULL,
    reference_id TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_user FOREIGN KEY (user_id) REFERENCES market.users(user_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_idempotency_key
    ON market.ledger_entries (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_ledger_user_created
    ON market.ledger_entries (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ledger_type_reference
    ON market.ledger_entries (type, reference_id);

INSERT INTO market.ledger_entries (
    user_id,
    amount_delta,
    type,
    reference_id,
    idempotency_key,
    created_at
)
SELECT
    u.user_id,
    u.balance,
    'MIGRATION_BALANCE',
    u.user_id,
    'MIGRATION_BALANCE:' || u.user_id,
    CURRENT_TIMESTAMP
FROM market.users u
WHERE u.balance <> 0
ON CONFLICT (idempotency_key) DO NOTHING;
