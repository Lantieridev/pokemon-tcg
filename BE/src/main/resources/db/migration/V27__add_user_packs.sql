CREATE TABLE user_packs (
    user_id BIGINT NOT NULL,
    pack_type VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, pack_type),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
INSERT INTO user_packs (user_id, pack_type, quantity) SELECT id, 'pack_base', packs FROM users WHERE packs > 0;