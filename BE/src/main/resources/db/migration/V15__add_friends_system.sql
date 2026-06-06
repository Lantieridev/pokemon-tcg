-- V15__add_friends_system.sql
CREATE TABLE friendships (
    id BIGSERIAL PRIMARY KEY,
    user_id_1 BIGINT NOT NULL REFERENCES users(id),
    user_id_2 BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL, -- PENDING, ACCEPTED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_friendship UNIQUE (user_id_1, user_id_2)
);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    receiver_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
