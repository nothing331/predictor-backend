ALTER TABLE market.trades
ADD COLUMN IF NOT EXISTS client_request_id TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_trades_user_client_request
ON market.trades (user_id, client_request_id)
WHERE client_request_id IS NOT NULL;
