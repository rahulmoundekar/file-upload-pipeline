package com.rahul.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "files",
        indexes = {
                @Index(
                        name = "idx_files_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_files_checksum",
                        columnList = "checksum_sha256"
                ),
                @Index(
                        name = "idx_files_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@NoArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "original_filename",
            nullable = false,
            length = 255
    )
    private String originalFilename;

    @Column(
            name = "stored_filename",
            nullable = false,
            length = 255
    )
    private String storedFilename;

    @Column(
            name = "object_key",
            nullable = false,
            unique = true,
            length = 1024
    )
    private String objectKey;

    @Column(
            name = "content_type",
            nullable = false,
            length = 255
    )
    private String contentType;

    @Column(
            name = "size_bytes",
            nullable = false
    )
    private long sizeBytes;

    @Column(
            name = "checksum_sha256",
            nullable = false,
            length = 64
    )
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private FileStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "scan_status",
            nullable = false,
            length = 50
    )
    private ScanStatus scanStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "thumbnail_status",
            nullable = false,
            length = 50
    )
    private ThumbnailStatus thumbnailStatus;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public FileMetadata(
            String originalFilename,
            String storedFilename,
            String objectKey,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            FileStatus status,
            ScanStatus scanStatus,
            ThumbnailStatus thumbnailStatus
    ) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.status = status;
        this.scanStatus = scanStatus;
        this.thumbnailStatus = thumbnailStatus;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markUploaded() {
        this.status = FileStatus.UPLOADED;
    }

    public void markFailed(String reason) {
        this.status = FileStatus.FAILED;
        this.failureReason = reason;
    }
}