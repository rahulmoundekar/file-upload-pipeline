CREATE TABLE processed_events
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),

    event_id      UUID         NOT NULL,

    consumer_name VARCHAR(100) NOT NULL,

    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_processed_event_consumer
        UNIQUE (event_id, consumer_name)
);

CREATE INDEX idx_processed_events_event_id
    ON processed_events (event_id);