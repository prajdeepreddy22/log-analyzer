UPDATE analysis
SET confidence_score = 0.000
WHERE confidence_score IS NULL;

ALTER TABLE analysis
    MODIFY confidence_score DECIMAL(4, 3)
        NOT NULL DEFAULT 0.000;
