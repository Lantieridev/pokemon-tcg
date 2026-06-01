-- V10__add_missing_ids.sql

-- Add missing id column for user_showcase
ALTER TABLE user_showcase DROP CONSTRAINT user_showcase_pkey;
ALTER TABLE user_showcase ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE user_showcase ADD CONSTRAINT user_showcase_user_slot_uk UNIQUE (user_id, slot_position);

-- Add missing id column for user_mutes
ALTER TABLE user_mutes DROP CONSTRAINT user_mutes_pkey;
ALTER TABLE user_mutes ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE user_mutes ADD CONSTRAINT user_mutes_user_muted_uk UNIQUE (user_id, muted_user_id);
