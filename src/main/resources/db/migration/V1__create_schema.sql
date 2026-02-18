-- This runs automatically on first container start

-- Create schema
CREATE SCHEMA IF NOT EXISTS market;

-- Set search path
ALTER DATABASE prediction_market SET search_path TO market, public;

-- Create users table
CREATE TABLE IF NOT EXISTS market.users (
    user_id VARCHAR(36) PRIMARY KEY,
    user_name TEXT,
    email TEXT NOT NULL,
    balance NUMERIC(20, 8) NOT NULL CHECK (balance >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create markets table
CREATE TABLE IF NOT EXISTS market.markets (
    market_id VARCHAR(36) PRIMARY KEY,
    market_name TEXT NOT NULL,
    market_description TEXT,
    q_yes NUMERIC NOT NULL,
    q_no NUMERIC NOT NULL,
    liquidity_param NUMERIC NOT NULL,
    status VARCHAR(20) NOT NULL,
    resolved_outcome VARCHAR(3),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- Create positions table
CREATE TABLE IF NOT EXISTS market.positions (
    position_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    market_id VARCHAR(36) NOT NULL,
    yes_shares NUMERIC DEFAULT 0,
    no_shares NUMERIC DEFAULT 0,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES market.users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_market FOREIGN KEY (market_id) REFERENCES market.markets(market_id) ON DELETE CASCADE,
    CONSTRAINT unique_user_market UNIQUE (user_id, market_id)
);

-- Create trades table
CREATE TABLE IF NOT EXISTS market.trades (
    trade_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    market_id VARCHAR(36) NOT NULL,
    outcome VARCHAR(3) NOT NULL,
    shares_bought NUMERIC NOT NULL,
    cost NUMERIC(20, 8) NOT NULL,
    traded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trade_user FOREIGN KEY (user_id) REFERENCES market.users(user_id),
    CONSTRAINT fk_trade_market FOREIGN KEY (market_id) REFERENCES market.markets(market_id)
);

