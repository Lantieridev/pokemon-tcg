CREATE TABLE store_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price INT NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    image_url VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_unlocked_avatars (
    user_id BIGINT NOT NULL,
    avatar_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, avatar_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO store_items (name, description, price, item_type, image_url) VALUES
('Ash Ketchum', 'Avatar clásico del entrenador de Pueblo Paleta.', 100, 'AVATAR', 'ash_avatar'),
('Misty', 'Avatar de la líder de gimnasio de Ciudad Celeste.', 100, 'AVATAR', 'misty_avatar'),
('Brock', 'Avatar del líder de gimnasio de Ciudad Plateada.', 100, 'AVATAR', 'brock_avatar'),
('Maestro Pokémon', 'Título prestigioso para lucir en tu perfil.', 500, 'TITLE', 'null'),
('Rico', 'Título que demuestra tu poder adquisitivo.', 300, 'TITLE', 'null'),
('Sobre Base XY', 'Sobre con 5 cartas aleatorias de la primera expansión.', 250, 'PACK', 'pack_xy_base');
