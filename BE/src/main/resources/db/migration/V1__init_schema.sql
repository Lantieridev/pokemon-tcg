-- V1__init_schema.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cards (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    supertype VARCHAR(50) NOT NULL,
    subtype VARCHAR(100),
    hp INTEGER,
    rules JSONB,
    attacks JSONB,
    weaknesses JSONB,
    resistances JSONB,
    retreat_cost JSONB,
    set_id VARCHAR(50) NOT NULL
);

CREATE TABLE decks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE deck_cards (
    deck_id BIGINT NOT NULL REFERENCES decks(id),
    card_id VARCHAR(50) NOT NULL REFERENCES cards(id),
    quantity INTEGER NOT NULL,
    PRIMARY KEY (deck_id, card_id)
);

CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    player1_id BIGINT NOT NULL REFERENCES users(id),
    player2_id BIGINT REFERENCES users(id),
    winner_id BIGINT REFERENCES users(id),
    current_state JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE match_logs (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES matches(id),
    turn_number INTEGER NOT NULL,
    player_id BIGINT REFERENCES users(id),
    action_type VARCHAR(100) NOT NULL,
    result TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
