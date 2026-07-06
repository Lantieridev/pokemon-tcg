-- Optimistic lock for UserEntity: currency-mutating endpoints (store purchases, pack
-- opening, battle pass) had no protection against two concurrent requests double-spending
-- the same balance. Existing rows start at version 0, same as a freshly inserted row.
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
