package com.rahul.event;

import com.rahul.exception.InvalidEventException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadedEventValidatorTest {

    private final FileUploadedEventValidator validator = new FileUploadedEventValidator();

    @Test
    void shouldRejectMissingEventId() {

        FileUploadedEvent event = new FileUploadedEvent(null, UUID.randomUUID(), "uploads/test.txt", "test.txt", "text/plain", 10, "a".repeat(64), Instant.now());

        assertThatThrownBy(() -> validator.validate(event)).isInstanceOf(InvalidEventException.class).hasMessage("eventId must not be null");
    }

    @Test
    void shouldRejectInvalidChecksum() {

        FileUploadedEvent event = new FileUploadedEvent(UUID.randomUUID(), UUID.randomUUID(), "uploads/test.txt", "test.txt", "text/plain", 10, "invalid", Instant.now());

        assertThatThrownBy(() -> validator.validate(event)).isInstanceOf(IllegalArgumentException.class).hasMessage("checksumSha256 must contain 64 hexadecimal characters");
    }

    @Test
    void shouldRejectNegativeSize() {

        FileUploadedEvent event = new FileUploadedEvent(UUID.randomUUID(), UUID.randomUUID(), "uploads/test.txt", "test.txt", "text/plain", -1, "a".repeat(64), Instant.now());

        assertThatThrownBy(() -> validator.validate(event)).isInstanceOf(IllegalArgumentException.class).hasMessage("sizeBytes must not be negative");
    }

    @Test
    void shouldAcceptValidEvent() {

        FileUploadedEvent event = new FileUploadedEvent(UUID.randomUUID(), UUID.randomUUID(), "uploads/test.txt", "test.txt", "text/plain", 10, "a".repeat(64), Instant.now());

        validator.validate(event);
    }
}