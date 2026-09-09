package com.rahul.dto;

import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;

import java.time.Instant;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        FileStatus status,
        ScanStatus scanStatus,
        ThumbnailStatus thumbnailStatus,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}