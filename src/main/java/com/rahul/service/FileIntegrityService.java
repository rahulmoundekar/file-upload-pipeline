package com.rahul.service;

import com.rahul.dto.FileIntegrityResponse;
import com.rahul.entity.FileMetadata;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileIntegrityService {

    private final FileMetadataRepository fileMetadataRepository;
    private final ObjectStorage objectStorage;
    private final FileChecksumService checksumService;

    public FileIntegrityResponse verify(UUID fileId) {

        FileMetadata metadata = fileMetadataRepository.findById(fileId).orElseThrow(() -> new IllegalArgumentException("File not found"));

        try (InputStream inputStream = objectStorage.get(metadata.getObjectKey())) {

            String actualChecksum = checksumService.sha256(inputStream);

            return new FileIntegrityResponse(actualChecksum.equals(metadata.getChecksumSha256()), metadata.getChecksumSha256(), actualChecksum);

        } catch (Exception e) {

            throw new IllegalStateException("Unable to verify file integrity", e);
        }
    }
}