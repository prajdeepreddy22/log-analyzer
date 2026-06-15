CREATE TABLE incidents (
    incident_id       VARCHAR(36) PRIMARY KEY,
    upload_id         VARCHAR(36) NOT NULL,
    user_id           BIGINT NOT NULL,
    root_cause        VARCHAR(64) NOT NULL,
    severity_score    TINYINT NOT NULL,
    confidence_score  DECIMAL(4, 3) NOT NULL,
    occurrence_count  INT NOT NULL DEFAULT 1,
    first_seen        DATETIME NOT NULL,
    last_seen         DATETIME NOT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_incidents_upload
        FOREIGN KEY (upload_id) REFERENCES uploads(upload_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_incidents_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_incidents_upload_user_root
        UNIQUE (upload_id, user_id, root_cause),

    CONSTRAINT chk_incidents_severity
        CHECK (severity_score BETWEEN 1 AND 5),

    CONSTRAINT chk_incidents_confidence
        CHECK (confidence_score BETWEEN 0.000 AND 1.000),

    CONSTRAINT chk_incidents_occurrence_count
        CHECK (occurrence_count > 0),

    INDEX idx_incidents_user_severity_frequency
        (user_id, severity_score, occurrence_count),

    INDEX idx_incidents_upload_root
        (upload_id, root_cause)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE analysis
    ADD COLUMN incident_id VARCHAR(36) NULL AFTER user_id,
    ADD CONSTRAINT fk_analysis_incident
        FOREIGN KEY (incident_id) REFERENCES incidents(incident_id)
        ON DELETE SET NULL,
    ADD INDEX idx_analysis_incident_id (incident_id);
