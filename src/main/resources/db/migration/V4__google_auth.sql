ALTER TABLE market.users ADD COLUMN google_sub VARCHAR(255) UNIQUE;
ALTER TABLE market.users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE market.users ADD COLUMN picture_url TEXT;
ALTER TABLE market.users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'GOOGLE';
ALTER TABLE market.users ADD COLUMN last_login_at TIMESTAMP;

CREATE TABLE market.refresh_tokens (
    token_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON market.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON market.refresh_tokens(token_hash);
