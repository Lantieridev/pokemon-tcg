-- Add stats JSON columns to matches table
ALTER TABLE matches ADD COLUMN player1_stats_json VARCHAR(20000);
ALTER TABLE matches ADD COLUMN player2_stats_json VARCHAR(20000);

-- Create user_card_stats table
CREATE TABLE user_card_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    card_id VARCHAR(255) NOT NULL,
    times_played INTEGER NOT NULL DEFAULT 0,
    damage_dealt INTEGER NOT NULL DEFAULT 0,
    damage_received INTEGER NOT NULL DEFAULT 0,
    kos_made INTEGER NOT NULL DEFAULT 0,
    kos_suffered INTEGER NOT NULL DEFAULT 0
);

-- Create user_energy_stats table
CREATE TABLE user_energy_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    energy_type VARCHAR(255) NOT NULL,
    times_played INTEGER NOT NULL DEFAULT 0
);
