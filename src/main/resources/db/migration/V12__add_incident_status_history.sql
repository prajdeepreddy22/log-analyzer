CREATE TABLE incident_status_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id  VARCHAR(36) NOT NULL,
    from_status  VARCHAR(20) NULL,
    to_status    VARCHAR(20) NOT NULL,
    changed_by   BIGINT NOT NULL,
    changed_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note         VARCHAR(500) NULL,

    CONSTRAINT fk_incident_status_history_incident
        FOREIGN KEY (incident_id) REFERENCES incidents(incident_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_incident_status_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_incident_status_history_incident_id (incident_id),
    INDEX idx_incident_status_history_changed_by (changed_by),
    INDEX idx_incident_status_history_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
