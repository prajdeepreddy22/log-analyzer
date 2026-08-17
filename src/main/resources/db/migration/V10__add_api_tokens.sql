CREATE TABLE api_tokens (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    token_hash    VARCHAR(64) NOT NULL,
    scope         VARCHAR(50) NOT NULL DEFAULT 'INGEST',
    name          VARCHAR(255) NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at  DATETIME NULL,
    revoked       BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_api_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_api_tokens_token_hash UNIQUE (token_hash),

    INDEX idx_api_tokens_user_id (user_id),
    INDEX idx_api_tokens_revoked (revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
