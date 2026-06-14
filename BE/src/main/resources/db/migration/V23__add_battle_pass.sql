CREATE TABLE IF NOT EXISTS battle_pass_levels (
    level INT PRIMARY KEY,
    required_xp INT NOT NULL,
    free_reward_type VARCHAR(50),
    free_reward_amount INT DEFAULT 0,
    free_reward_value VARCHAR(100),
    premium_reward_type VARCHAR(50),
    premium_reward_amount INT DEFAULT 0,
    premium_reward_value VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS user_battle_pass (
    user_id BIGINT PRIMARY KEY,
    is_premium BOOLEAN DEFAULT FALSE,
    claimed_free_level INT DEFAULT 0,
    claimed_premium_level INT DEFAULT 0,
    CONSTRAINT fk_user_battle_pass_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Seeding some levels
INSERT INTO battle_pass_levels (level, required_xp, free_reward_type, free_reward_amount, free_reward_value, premium_reward_type, premium_reward_amount, premium_reward_value) VALUES
(1, 100, 'COINS', 50, null, 'PACK', 1, null),
(2, 250, 'COINS', 50, null, 'COINS', 100, null),
(3, 450, 'PACK', 1, null, 'TITLE', 1, 'Maestro Gacha'),
(4, 700, 'COINS', 100, null, 'PACK', 2, null),
(5, 1000, 'AVATAR', 1, 'pikachu_cute', 'PACK', 3, null),
(6, 1400, 'COINS', 100, null, 'COINS', 300, null),
(7, 1900, 'PACK', 1, null, 'PACK', 2, null),
(8, 2500, 'COINS', 150, null, 'COINS', 500, null),
(9, 3200, 'PACK', 1, null, 'TITLE', 1, 'Leyenda Viva'),
(10, 4000, 'TITLE', 1, 'Conquistador', 'AVATAR', 1, 'charizard_3d');
