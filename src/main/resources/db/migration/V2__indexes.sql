-- Create indexes
CREATE INDEX IF NOT EXISTS idx_positions_user ON market.positions(user_id);
CREATE INDEX IF NOT EXISTS idx_positions_market ON market.positions(market_id);
CREATE INDEX IF NOT EXISTS idx_trades_user ON market.trades(user_id);
CREATE INDEX IF NOT EXISTS idx_trades_market ON market.trades(market_id);
CREATE INDEX IF NOT EXISTS idx_trades_timestamp ON market.trades(traded_at DESC);
CREATE INDEX IF NOT EXISTS idx_markets_status ON market.markets(status);