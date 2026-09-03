package com.rahul.event;

import com.rahul.exception.InvalidEventException;
import org.springframework.stereotype.Component;

@Component
public class FileUploadedEventValidator {

    public void validate(FileUploadedEvent event) {

        if (event.eventId() == null) {
            throw new InvalidEventException("eventId must not be null");
        }

        if (event.fileId() == null) {
            throw new IllegalArgumentException("fileId must not be null");
        }

        if (event.objectKey() == null || event.objectKey().isBlank()) {
            throw new IllegalArgumentException("objectKey must not be blank");
        }

        if (event.originalFilename() == null || event.originalFilename().isBlank()) {
            throw new IllegalArgumentException("originalFilename must not be blank");
        }

        if (event.contentType() == null || event.contentType().isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }

        if (event.sizeBytes() < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }

        if (event.checksumSha256() == null || !event.checksumSha256().matches("[a-fA-F0-9]{64}")) {
            throw new IllegalArgumentException("checksumSha256 must contain 64 hexadecimal characters");
        }

        if (event.occurredAt() == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
    }
}