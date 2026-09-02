package com.rahul.dto;

import java.time.Instant;
import java.util.UUID;

public record FileUploadResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        String status,
        Instant createdAt
) {
}