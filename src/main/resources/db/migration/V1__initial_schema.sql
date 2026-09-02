CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE files (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       original_filename VARCHAR(255) NOT NULL,
                       stored_filename VARCHAR(255) NOT NULL,
                       object_key VARCHAR(1024) NOT NULL,

                       content_type VARCHAR(255) NOT NULL,
                       size_bytes BIGINT NOT NULL,

                       checksum_sha256 CHAR(64) NOT NULL,

                       status VARCHAR(50) NOT NULL,

                       scan_status VARCHAR(50) NOT NULL,
                       thumbnail_status VARCHAR(50) NOT NULL,

                       failure_reason TEXT,

                       version BIGINT NOT NULL DEFAULT 0,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       completed_at TIMESTAMPTZ,

                       CONSTRAINT uk_files_object_key
                           UNIQUE (object_key),

                       CONSTRAINT chk_files_size_positive
                           CHECK (size_bytes >= 0),

                       CONSTRAINT chk_files_status
                           CHECK (
                               status IN (
                                          'UPLOADING',
                                          'UPLOADED',
                                          'PROCESSING',
                                          'COMPLETED',
                                          'FAILED',
                                          'INFECTED'
                                   )
                               ),

                       CONSTRAINT chk_files_scan_status
                           CHECK (
                               scan_status IN (
                                               'PENDING',
                                               'SCANNING',
                                               'CLEAN',
                                               'INFECTED',
                                               'FAILED',
                                               'NOT_REQUIRED'
                                   )
                               ),

                       CONSTRAINT chk_files_thumbnail_status
                           CHECK (
                               thumbnail_status IN (
                                                    'PENDING',
                                                    'PROCESSING',
                                                    'READY',
                                                    'FAILED',
                                                    'NOT_REQUIRED'
                                   )
                               )
);

CREATE INDEX idx_files_status
    ON files(status);

CREATE INDEX idx_files_checksum
    ON files(checksum_sha256);

CREATE INDEX idx_files_created_at
    ON files(created_at);

CREATE TABLE file_processing_jobs (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      file_id UUID NOT NULL,

                                      job_type VARCHAR(50) NOT NULL,
                                      status VARCHAR(50) NOT NULL,

                                      attempt_count INTEGER NOT NULL DEFAULT 0,
                                      max_attempts INTEGER NOT NULL DEFAULT 3,

                                      last_error TEXT,

                                      available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      started_at TIMESTAMPTZ,
                                      completed_at TIMESTAMPTZ,

                                      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_processing_jobs_file
                                          FOREIGN KEY (file_id)
                                              REFERENCES files(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT chk_processing_job_type
                                          CHECK (
                                              job_type IN (
                                                           'VIRUS_SCAN',
                                                           'THUMBNAIL'
                                                  )
                                              ),

                                      CONSTRAINT chk_processing_job_status
                                          CHECK (
                                              status IN (
                                                         'PENDING',
                                                         'PROCESSING',
                                                         'COMPLETED',
                                                         'FAILED',
                                                         'RETRYING'
                                                  )
                                              ),

                                      CONSTRAINT chk_processing_attempt_count
                                          CHECK (attempt_count >= 0),

                                      CONSTRAINT chk_processing_max_attempts
                                          CHECK (max_attempts > 0)
);

CREATE INDEX idx_processing_jobs_file
    ON file_processing_jobs(file_id);

CREATE INDEX idx_processing_jobs_status
    ON file_processing_jobs(status);

CREATE INDEX idx_processing_jobs_available
    ON file_processing_jobs(available_at);

CREATE UNIQUE INDEX uk_processing_job_active
    ON file_processing_jobs(file_id, job_type)
    WHERE status IN (
                     'PENDING',
                     'PROCESSING',
                     'RETRYING'
        );

CREATE TABLE file_events (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                             file_id UUID NOT NULL,

                             event_id UUID NOT NULL,
                             event_type VARCHAR(100) NOT NULL,

                             payload JSONB,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_file_events_file
                                 FOREIGN KEY (file_id)
                                     REFERENCES files(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT uk_file_events_event_id
                                 UNIQUE (event_id)
);

CREATE INDEX idx_file_events_file
    ON file_events(file_id);

CREATE INDEX idx_file_events_type
    ON file_events(event_type);

CREATE INDEX idx_file_events_created_at
    ON file_events(created_at);

CREATE TABLE webhook_deliveries (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    file_id UUID NOT NULL,
                                    event_id UUID NOT NULL,

                                    event_type VARCHAR(100) NOT NULL,

                                    destination_url VARCHAR(2048) NOT NULL,

                                    status VARCHAR(50) NOT NULL,

                                    attempt_count INTEGER NOT NULL DEFAULT 0,

                                    response_status INTEGER,
                                    last_error TEXT,

                                    next_attempt_at TIMESTAMPTZ,

                                    delivered_at TIMESTAMPTZ,

                                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_webhook_file
                                        FOREIGN KEY (file_id)
                                            REFERENCES files(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT chk_webhook_status
                                        CHECK (
                                            status IN (
                                                       'PENDING',
                                                       'DELIVERED',
                                                       'RETRYING',
                                                       'FAILED'
                                                )
                                            )
);

CREATE INDEX idx_webhook_status
    ON webhook_deliveries(status);

CREATE INDEX idx_webhook_next_attempt
    ON webhook_deliveries(next_attempt_at);

CREATE INDEX idx_webhook_file
    ON webhook_deliveries(file_id);

CREATE TABLE idempotency_records (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     idempotency_key VARCHAR(255) NOT NULL,
                                     request_hash CHAR(64) NOT NULL,

                                     file_id UUID,

                                     status VARCHAR(50) NOT NULL,

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     expires_at TIMESTAMPTZ,

                                     CONSTRAINT uk_idempotency_key
                                         UNIQUE (idempotency_key),

                                     CONSTRAINT fk_idempotency_file
                                         FOREIGN KEY (file_id)
                                             REFERENCES files(id)
                                             ON DELETE SET NULL,

                                     CONSTRAINT chk_idempotency_status
                                         CHECK (
                                             status IN (
                                                        'IN_PROGRESS',
                                                        'COMPLETED',
                                                        'FAILED'
                                                 )
                                             )
);

CREATE INDEX idx_idempotency_expires_at
    ON idempotency_records(expires_at);

CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id UUID NOT NULL,

                               event_type VARCHAR(150) NOT NULL,

                               payload TEXT NOT NULL,

                               status VARCHAR(30) NOT NULL,

                               attempts INT NOT NULL DEFAULT 0,

                               published_at TIMESTAMPTZ,

                               last_error TEXT,

                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_status_created
    ON outbox_events(status, created_at);