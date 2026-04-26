-- ============================================================
-- GenAI Log Analyzer — Flyway Initial Schema
-- ============================================================

-- ------------------------------------------------------------
-- Table: users
-- ------------------------------------------------------------
CREATE TABLE users (
                       id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username     VARCHAR(100) NOT NULL UNIQUE,
                       email        VARCHAR(150) NOT NULL UNIQUE,
                       password     VARCHAR(255) NOT NULL,
                       role         ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
                       created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table: uploads
-- ------------------------------------------------------------
CREATE TABLE uploads (
                         upload_id    VARCHAR(36) PRIMARY KEY,
                         user_id      BIGINT NOT NULL,
                         file_name    VARCHAR(255) NOT NULL,
                         file_path    VARCHAR(500) NOT NULL,
                         file_size    BIGINT,
                         upload_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         total_logs   INT NOT NULL DEFAULT 0,
                         error_count  INT NOT NULL DEFAULT 0,
                         warn_count   INT NOT NULL DEFAULT 0,
                         status       ENUM('UPLOADED','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'UPLOADED',
                         created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         CONSTRAINT fk_uploads_user
                             FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

                         INDEX idx_uploads_user_status (user_id, status),
                         INDEX idx_uploads_upload_user (upload_id, user_id)   -- 🔥 NEW (safe)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table: logs
-- ------------------------------------------------------------
CREATE TABLE logs (
                      id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                      upload_id      VARCHAR(36) NOT NULL,
                      log_timestamp  DATETIME NULL,
                      log_sequence   BIGINT,
                      level          ENUM('DEBUG','INFO','WARN','ERROR','FATAL','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
                      service_name   VARCHAR(200),
                      environment    VARCHAR(100),
                      host_name      VARCHAR(200),
                      message        TEXT NOT NULL,
                      source         ENUM('UPLOADED','REALTIME') NOT NULL DEFAULT 'UPLOADED',
                      hash_key       VARCHAR(64) NOT NULL,   -- 🔥 UPDATED (safe now)
                      created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      CONSTRAINT fk_logs_upload
                          FOREIGN KEY (upload_id) REFERENCES uploads(upload_id) ON DELETE CASCADE,

    -- Existing indexes (kept for compatibility)
                      INDEX idx_logs_upload_level_ts_hash (upload_id, level, log_timestamp, hash_key),
                      INDEX idx_logs_sequence (upload_id, log_sequence),

    -- 🔥 NEW (optimized for AI queries)
                      INDEX idx_logs_ai_fetch (upload_id, level, log_timestamp),

                      FULLTEXT INDEX idx_logs_message_fulltext (message)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table: analysis
-- ------------------------------------------------------------
CREATE TABLE analysis (
                          id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                          upload_id          VARCHAR(36) NOT NULL,
                          user_id            BIGINT NOT NULL,
                          hash_key           VARCHAR(64) NOT NULL,
                          summary            TEXT,
                          root_cause         TEXT,
                          developer_mistake  TEXT,
                          fix_suggestion     TEXT,
                          code_fix           TEXT,
                          severity_score     TINYINT,
                          retry_count        INT NOT NULL DEFAULT 0,
                          prompt_version     INT NOT NULL DEFAULT 1,
                          analysis_status    ENUM('PENDING','PROCESSING','COMPLETED','FAILED','RETRYING') NOT NULL DEFAULT 'PENDING',
                          error_message      TEXT,
                          created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          CONSTRAINT fk_analysis_upload
                              FOREIGN KEY (upload_id) REFERENCES uploads(upload_id) ON DELETE CASCADE,

                          CONSTRAINT fk_analysis_user
                              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- 🔥 CRITICAL FOR CACHING
                          UNIQUE KEY uk_analysis_hash_user (hash_key, user_id),

    -- Existing indexes (kept)
                          INDEX idx_analysis_hash_key (hash_key),
                          INDEX idx_analysis_upload_id (upload_id),
                          INDEX idx_analysis_user_id (user_id),
                          INDEX idx_analysis_upload_user (upload_id, user_id),
                          INDEX idx_analysis_status (analysis_status),

    -- 🔥 Optional optimization
                          INDEX idx_analysis_hash_status (hash_key, analysis_status),

    -- 🔥 Data validation
                          CONSTRAINT chk_severity_score CHECK (severity_score BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;