package com.rahul.event;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}