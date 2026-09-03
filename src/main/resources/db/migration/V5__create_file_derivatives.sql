CREATE TABLE file_derivatives (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  file_id UUID NOT NULL,

                                  derivative_type VARCHAR(50) NOT NULL,

                                  object_key VARCHAR(1024) NOT NULL,

                                  content_type VARCHAR(255) NOT NULL,

                                  size_bytes BIGINT NOT NULL,

                                  width INTEGER,

                                  height INTEGER,

                                  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_file_derivatives_file
                                      FOREIGN KEY (file_id)
                                          REFERENCES files(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT uk_file_derivative_type
                                      UNIQUE (file_id, derivative_type)
);

CREATE INDEX idx_file_derivatives_file_id
    ON file_derivatives(file_id);