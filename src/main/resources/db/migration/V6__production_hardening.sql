ALTER TABLE files DROP CONSTRAINT IF EXISTS chk_files_status;
ALTER TABLE files ADD CONSTRAINT chk_files_status CHECK (status IN ('UPLOADING','UPLOADED','PROCESSING','SCANNING','CLEAN','THUMBNAIL_PROCESSING','COMPLETED','INFECTED','REJECTED','FAILED','DELETING','DELETED'));

CREATE TABLE IF NOT EXISTS upload_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    expected_size BIGINT NOT NULL,
    chunk_size BIGINT NOT NULL,
    total_parts INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    checksum_sha256 CHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_upload_session_status CHECK (status IN ('OPEN','COMPLETING','COMPLETED','ABORTED','EXPIRED')),
    CONSTRAINT chk_upload_session_sizes CHECK (expected_size >= 0 AND chunk_size > 0 AND total_parts > 0)
);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_expires ON upload_sessions(expires_at);

CREATE TABLE IF NOT EXISTS upload_parts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_session_id UUID NOT NULL REFERENCES upload_sessions(id) ON DELETE CASCADE,
    part_number INTEGER NOT NULL,
    object_key VARCHAR(1024) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_upload_part UNIQUE (upload_session_id, part_number),
    CONSTRAINT chk_upload_part_number CHECK (part_number > 0),
    CONSTRAINT chk_upload_part_size CHECK (size_bytes >= 0)
);
CREATE INDEX IF NOT EXISTS idx_upload_parts_session ON upload_parts(upload_session_id, part_number);

ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS claimed_until TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_outbox_claimed ON outbox_events(status, claimed_until, created_at);
