-- Add category and outcome label columns to markets table.
-- DEFAULT values match the domain defaults so existing rows are valid immediately.

ALTER TABLE market.markets
    ADD COLUMN IF NOT EXISTS category  TEXT NOT NULL DEFAULT 'General',
    ADD COLUMN IF NOT EXISTS yes_label TEXT NOT NULL DEFAULT 'Yes',
    ADD COLUMN IF NOT EXISTS no_label  TEXT NOT NULL DEFAULT 'No';
