-- V3__chat_reports.sql
CREATE TABLE chat_reports (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES matches(id),
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    reported_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(255) NOT NULL,
    chat_history JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
