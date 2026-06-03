-- Persist daily AI budget usage so it survives backend restarts.
CREATE TABLE rate_limit_usage (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    usage_date   DATE NOT NULL,
    usage_count  INT NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_rate_limit_user_date
        UNIQUE (user_id, usage_date),

    INDEX idx_rate_limit_user_date (user_id, usage_date),

    CONSTRAINT fk_rate_limit_usage_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
