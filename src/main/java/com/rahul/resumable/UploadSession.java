package com.rahul.resumable;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="upload_sessions") @Getter @NoArgsConstructor
public class UploadSession {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="original_filename", nullable=false, length=255) private String originalFilename;
    @Column(name="content_type", nullable=false, length=255) private String contentType;
    @Column(name="expected_size", nullable=false) private long expectedSize;
    @Column(name="chunk_size", nullable=false) private long chunkSize;
    @Column(name="total_parts", nullable=false) private int totalParts;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private UploadSessionStatus status;
    @Column(name="checksum_sha256", length=64) private String checksumSha256;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    public UploadSession(String filename,String contentType,long expected,long chunk,int parts,Instant expires){this.originalFilename=filename;this.contentType=contentType;this.expectedSize=expected;this.chunkSize=chunk;this.totalParts=parts;this.status=UploadSessionStatus.OPEN;this.createdAt=Instant.now();this.updatedAt=this.createdAt;this.expiresAt=expires;}
    public void markCompleting(){status=UploadSessionStatus.COMPLETING;updatedAt=Instant.now();}
    public void markCompleted(String checksum){status=UploadSessionStatus.COMPLETED;checksumSha256=checksum;updatedAt=Instant.now();}
    public void markAborted(){status=UploadSessionStatus.ABORTED;updatedAt=Instant.now();}
    public void markExpired(){status=UploadSessionStatus.EXPIRED;updatedAt=Instant.now();}
}
