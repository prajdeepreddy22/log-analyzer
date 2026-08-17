ALTER TABLE incidents
    ADD COLUMN title VARCHAR(255) NULL AFTER user_id,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN' AFTER title,
    ADD COLUMN root_cause_summary VARCHAR(1000) NULL AFTER root_cause,
    ADD COLUMN log_source_id BIGINT NULL AFTER upload_id,
    ADD CONSTRAINT fk_incidents_log_source
        FOREIGN KEY (log_source_id) REFERENCES log_ingestion_sources(id)
        ON DELETE SET NULL,
    ADD INDEX idx_incidents_user_status_severity
        (user_id, status, severity_score, occurrence_count);

UPDATE incidents
SET title = CONCAT(root_cause, ' incident')
WHERE title IS NULL;

ALTER TABLE incidents
    MODIFY title VARCHAR(255) NOT NULL;
