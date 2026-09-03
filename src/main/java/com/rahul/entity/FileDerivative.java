package com.rahul.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_derivatives", uniqueConstraints = {@UniqueConstraint(name = "uk_file_derivative_type", columnNames = {"file_id", "derivative_type"})}, indexes = {@Index(name = "idx_file_derivatives_file_id", columnList = "file_id")})
public class FileDerivative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "derivative_type", nullable = false, length = 50)
    private DerivativeType derivativeType;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FileDerivative() {
    }

    public FileDerivative(UUID fileId, DerivativeType derivativeType, String objectKey, String contentType, long sizeBytes, Integer width, Integer height) {

        this.fileId = fileId;
        this.derivativeType = derivativeType;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public DerivativeType getDerivativeType() {
        return derivativeType;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}