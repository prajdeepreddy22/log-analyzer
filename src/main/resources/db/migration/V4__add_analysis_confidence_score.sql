ALTER TABLE analysis
    ADD COLUMN confidence_score DECIMAL(4, 3) NULL AFTER severity_score,
    ADD CONSTRAINT chk_analysis_confidence_score
        CHECK (confidence_score BETWEEN 0.000 AND 1.000);
