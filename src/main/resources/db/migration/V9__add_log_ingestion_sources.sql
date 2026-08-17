CREATE TABLE log_ingestion_sources (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    source_name         VARCHAR(255) NOT NULL,
    source_type         VARCHAR(30) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    internal_upload_id  VARCHAR(36) NULL,
    last_ingested_at    DATETIME NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_log_ingestion_sources_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_log_ingestion_sources_internal_upload
        FOREIGN KEY (internal_upload_id) REFERENCES uploads(upload_id)
        ON DELETE SET NULL,

    INDEX idx_log_ingestion_sources_user_id (user_id),
    INDEX idx_log_ingestion_sources_internal_upload_id (internal_upload_id),
    INDEX idx_log_ingestion_sources_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
