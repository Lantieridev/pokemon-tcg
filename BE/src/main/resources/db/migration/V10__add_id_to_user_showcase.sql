-- V10__add_id_to_user_showcase.sql
DROP TABLE IF EXISTS user_showcase CASCADE;

CREATE TABLE user_showcase (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_id VARCHAR(50) NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    slot_position INT NOT NULL CHECK (slot_position BETWEEN 1 AND 3),
    CONSTRAINT user_showcase_user_slot_unique UNIQUE (user_id, slot_position)
);
