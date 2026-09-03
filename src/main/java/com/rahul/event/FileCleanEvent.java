package com.rahul.event;

import java.time.Instant;
import java.util.UUID;

public record FileCleanEvent(UUID eventId, UUID fileId, String objectKey, String originalFilename, String contentType,
                             long sizeBytes, String checksumSha256, Instant occurredAt) {
}