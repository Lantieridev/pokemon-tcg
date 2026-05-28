-- Agregar columnas en users para persistir estadísticas acumuladas
ALTER TABLE users 
ADD COLUMN perfect_wins INTEGER DEFAULT 0,
ADD COLUMN comeback_wins INTEGER DEFAULT 0,
ADD COLUMN total_kos INTEGER DEFAULT 0,
ADD COLUMN trainer_cards_played INTEGER DEFAULT 0,
ADD COLUMN total_damage_dealt INTEGER DEFAULT 0;
