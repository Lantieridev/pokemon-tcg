-- V5__user_profile_customizations.sql

-- Agregar columnas de perfil estético y de nivel a la tabla users
ALTER TABLE users ADD COLUMN avatar_icon VARCHAR(100) DEFAULT 'default_trainer';
ALTER TABLE users ADD COLUMN description VARCHAR(250) DEFAULT '';
ALTER TABLE users ADD COLUMN active_title VARCHAR(100) DEFAULT 'Novato';
ALTER TABLE users ADD COLUMN level INT DEFAULT 1;
ALTER TABLE users ADD COLUMN xp INT DEFAULT 0;
ALTER TABLE users ADD COLUMN show_recidivism_warning BOOLEAN DEFAULT FALSE;

-- Tabla para almacenar los títulos desbloqueados por cada usuario
CREATE TABLE user_unlocked_titles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title_name VARCHAR(100) NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, title_name)
);

-- Tabla para la vitrina de cartas destacadas (Showcase de hasta 3 slots)
CREATE TABLE user_showcase (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_id VARCHAR(50) NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    slot_position INT NOT NULL CHECK (slot_position BETWEEN 1 AND 3),
    PRIMARY KEY (user_id, slot_position)
);

-- Tabla para la persistencia real de los honores otorgados
CREATE TABLE user_honors (
    id BIGSERIAL PRIMARY KEY,
    giver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    honor_type VARCHAR(50) NOT NULL,
    given_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla para la persistencia real del sistema de silenciado local
CREATE TABLE user_mutes (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    muted_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, muted_user_id)
);

-- Tabla para persistencia de notificaciones pendientes
CREATE TABLE user_pending_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla para persistencia de penalizaciones
CREATE TABLE user_penalties (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    penalty_type VARCHAR(50) NOT NULL,
    matches_remaining INT,
    expiration TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_pending BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
