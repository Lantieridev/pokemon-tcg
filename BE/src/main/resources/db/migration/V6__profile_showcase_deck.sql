-- V6__profile_showcase_deck.sql
-- Agregar columna para exponer un mazo destacado en el perfil del usuario
ALTER TABLE users ADD COLUMN showcased_deck_id BIGINT REFERENCES decks(id) ON DELETE SET NULL;
