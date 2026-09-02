package com.rahul.repository;

import com.rahul.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository
        extends JpaRepository<FileMetadata, UUID> {

    Optional<FileMetadata> findByChecksumSha256(
            String checksumSha256
    );

    boolean existsByObjectKey(
            String objectKey
    );
}