-- Add a user-facing display name for profile and navigation UI.
ALTER TABLE users
    ADD COLUMN display_name VARCHAR(100) NULL AFTER username;

UPDATE users
SET display_name = username
WHERE display_name IS NULL OR display_name = '';

ALTER TABLE users
    MODIFY display_name VARCHAR(100) NOT NULL;
