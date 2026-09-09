package com.rahul.event;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class EventDeserializerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private final EventDeserializer deserializer = new EventDeserializer(objectMapper);

    @Test
    void shouldDeserializeFileUploadedEvent() {

        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        String json = """
                {
                  "eventId": "%s",
                  "fileId": "%s",
                  "objectKey": "uploads/test.txt",
                  "originalFilename": "test.txt",
                  "contentType": "text/plain",
                  "sizeBytes": 100,
                  "checksumSha256": "%s",
                  "occurredAt": "2026-09-02T10:00:00Z"
                }
                """.formatted(eventId, fileId, "a".repeat(64));

        FileUploadedEvent event = deserializer.deserializeFileUploaded(json);

        assertThat(event.eventId()).isEqualTo(eventId);

        assertThat(event.fileId()).isEqualTo(fileId);

        assertThat(event.objectKey()).isEqualTo("uploads/test.txt");

        assertThat(event.originalFilename()).isEqualTo("test.txt");

        assertThat(event.contentType()).isEqualTo("text/plain");

        assertThat(event.sizeBytes()).isEqualTo(100);

        assertThat(event.checksumSha256()).hasSize(64);

        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-02T10:00:00Z"));
    }

    @Test
    void shouldDeserializeFileDeletedEvent() {

        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-09T10:15:30Z");

        String payload = """
                {
                  "eventId": "%s",
                  "fileId": "%s",
                  "occurredAt": "%s"
                }
                """.formatted(eventId, fileId, occurredAt);

        FileDeletedEvent event = deserializer.deserializeFileDeleted(payload);

        assertNotNull(event);
        assertEquals(eventId, event.eventId());
        assertEquals(fileId, event.fileId());
        assertEquals(occurredAt, event.occurredAt());
    }

    @Test
    void shouldRejectInvalidFileDeletedPayload() {

        assertThrows(
                IllegalArgumentException.class,
                () -> deserializer.deserializeFileDeleted(
                        "{ invalid-json }"
                )
        );
    }
}