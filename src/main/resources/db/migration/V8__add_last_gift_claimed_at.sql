ALTER TABLE market.users
ADD COLUMN IF NOT EXISTS last_gift_claimed_at TIMESTAMP;
