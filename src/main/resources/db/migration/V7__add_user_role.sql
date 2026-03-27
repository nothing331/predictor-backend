ALTER TABLE market.users ADD COLUMN role VARCHAR(20);
UPDATE market.users SET role = 'USER';
ALTER TABLE market.users ALTER COLUMN role SET NOT NULL;
