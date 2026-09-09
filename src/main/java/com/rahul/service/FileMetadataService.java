package com.rahul.service;

import com.rahul.dto.FileMetadataResponse;
import com.rahul.entity.FileMetadata;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final FileMetadataRepository fileMetadataRepository;

    public FileMetadataResponse getFile(UUID fileId) {

        FileMetadata metadata =
                fileMetadataRepository.findById(fileId)
                        .orElseThrow(() ->
                                new InvalidFileException("File not found")
                        );

        return new FileMetadataResponse(
                metadata.getId(),
                metadata.getOriginalFilename(),
                metadata.getContentType(),
                metadata.getSizeBytes(),
                metadata.getChecksumSha256(),
                metadata.getStatus(),
                metadata.getScanStatus(),
                metadata.getThumbnailStatus(),
                metadata.getFailureReason(),
                metadata.getCreatedAt(),
                metadata.getUpdatedAt(),
                metadata.getCompletedAt()
        );
    }
}