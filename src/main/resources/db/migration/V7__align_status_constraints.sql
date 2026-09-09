ALTER TABLE files DROP CONSTRAINT IF EXISTS chk_files_thumbnail_status;
ALTER TABLE files ADD CONSTRAINT chk_files_thumbnail_status CHECK (thumbnail_status IN ('PENDING','PROCESSING','COMPLETED','FAILED','NOT_REQUIRED','NOT_APPLICABLE'));
ALTER TABLE files DROP CONSTRAINT IF EXISTS chk_files_scan_status;
ALTER TABLE files ADD CONSTRAINT chk_files_scan_status CHECK (scan_status IN ('PENDING','SCANNING','CLEAN','INFECTED','FAILED','NOT_REQUIRED'));
