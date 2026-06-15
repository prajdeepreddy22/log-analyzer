ALTER TABLE uploads
    ADD COLUMN processing_error TEXT NULL AFTER status;
