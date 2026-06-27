CREATE TABLE user_cleared_story_nodes (
    user_id BIGINT NOT NULL,
    node_id INT NOT NULL,
    PRIMARY KEY (user_id, node_id),
    CONSTRAINT fk_user_cleared_nodes FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
