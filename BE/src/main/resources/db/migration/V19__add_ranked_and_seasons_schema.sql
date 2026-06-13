-- Add ranked_matches_played to users table to track placements
ALTER TABLE users ADD COLUMN ranked_matches_played INT DEFAULT 0;

-- Add is_ranked to matches table
ALTER TABLE matches ADD COLUMN is_ranked BOOLEAN DEFAULT FALSE;

-- Create an index on mmr to speed up leaderboard queries
CREATE INDEX idx_users_mmr ON users(mmr DESC);

-- Create seasons table
CREATE TABLE seasons (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
);

-- Create season_records table to store historical MMR after soft-resets
CREATE TABLE season_records (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    final_mmr INT NOT NULL,
    matches_played INT NOT NULL,
    FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(season_id, user_id)
);
