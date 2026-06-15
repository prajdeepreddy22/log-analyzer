UPDATE analysis
SET root_cause = CASE
    WHEN root_cause IS NULL OR TRIM(root_cause) = ''
        THEN NULL
    WHEN UPPER(root_cause) IN (
        'NETWORK_TIMEOUT',
        'DATABASE_CONNECTIVITY_FAILURE',
        'MEMORY_EXHAUSTION',
        'NULL_REFERENCE_ERROR',
        'INTERNAL_SERVER_FAILURE',
        'UNKNOWN_ERROR'
    )
        THEN UPPER(root_cause)
    WHEN UPPER(root_cause) LIKE '%NULL%POINTER%'
        OR UPPER(root_cause) LIKE '%NULL%REFERENCE%'
        THEN 'NULL_REFERENCE_ERROR'
    WHEN UPPER(root_cause) LIKE '%OUTOFMEMORY%'
        OR UPPER(root_cause) LIKE '%MEMORY%'
        OR UPPER(root_cause) LIKE '%HEAP%'
        THEN 'MEMORY_EXHAUSTION'
    WHEN UPPER(root_cause) LIKE '%SQL%'
        OR UPPER(root_cause) LIKE '%DATABASE%'
        OR UPPER(root_cause) LIKE '%JDBC%'
        THEN 'DATABASE_CONNECTIVITY_FAILURE'
    WHEN UPPER(root_cause) LIKE '%TIMEOUT%'
        OR UPPER(root_cause) LIKE '%CONNECTION REFUSED%'
        OR UPPER(root_cause) LIKE '%CONNECTEXCEPTION%'
        THEN 'NETWORK_TIMEOUT'
    WHEN UPPER(root_cause) LIKE '%INTERNAL%'
        OR UPPER(root_cause) LIKE '%APPLICATION%'
        OR UPPER(root_cause) LIKE '%HTTP 5%'
        THEN 'INTERNAL_SERVER_FAILURE'
    ELSE 'UNKNOWN_ERROR'
END
WHERE root_cause IS NOT NULL;

INSERT INTO incidents (
    incident_id,
    upload_id,
    user_id,
    root_cause,
    severity_score,
    confidence_score,
    occurrence_count,
    first_seen,
    last_seen,
    created_at,
    updated_at
)
SELECT
    UUID(),
    analysis.upload_id,
    analysis.user_id,
    analysis.root_cause,
    COALESCE(analysis.severity_score, 1),
    analysis.confidence_score,
    1,
    analysis.created_at,
    analysis.updated_at,
    analysis.created_at,
    analysis.updated_at
FROM analysis
WHERE analysis.analysis_status = 'COMPLETED'
  AND analysis.incident_id IS NULL
  AND analysis.root_cause IS NOT NULL
ON DUPLICATE KEY UPDATE
    severity_score = GREATEST(
        incidents.severity_score,
        VALUES(severity_score)
    ),
    confidence_score = (
        (incidents.confidence_score * incidents.occurrence_count)
        + VALUES(confidence_score)
    ) / (incidents.occurrence_count + 1),
    occurrence_count = incidents.occurrence_count + 1,
    first_seen = LEAST(incidents.first_seen, VALUES(first_seen)),
    last_seen = GREATEST(incidents.last_seen, VALUES(last_seen));

UPDATE analysis
JOIN incidents
  ON incidents.upload_id = analysis.upload_id
 AND incidents.user_id = analysis.user_id
 AND incidents.root_cause = analysis.root_cause
SET analysis.incident_id = incidents.incident_id
WHERE analysis.analysis_status = 'COMPLETED'
  AND analysis.incident_id IS NULL;

ALTER TABLE analysis
    ADD CONSTRAINT chk_analysis_root_cause
        CHECK (
            root_cause IS NULL
            OR root_cause IN (
                'NETWORK_TIMEOUT',
                'DATABASE_CONNECTIVITY_FAILURE',
                'MEMORY_EXHAUSTION',
                'NULL_REFERENCE_ERROR',
                'INTERNAL_SERVER_FAILURE',
                'UNKNOWN_ERROR'
            )
        );

ALTER TABLE incidents
    ADD CONSTRAINT chk_incidents_root_cause
        CHECK (
            root_cause IN (
                'NETWORK_TIMEOUT',
                'DATABASE_CONNECTIVITY_FAILURE',
                'MEMORY_EXHAUSTION',
                'NULL_REFERENCE_ERROR',
                'INTERNAL_SERVER_FAILURE',
                'UNKNOWN_ERROR'
            )
        );
