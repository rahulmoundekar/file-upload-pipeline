package com.rahul.service;

import com.rahul.config.UploadProperties;
import com.rahul.dto.FileUploadResponse;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.storage.ObjectKeyGenerator;
import com.rahul.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final ObjectStorage objectStorage;
    private final ObjectKeyGenerator objectKeyGenerator;
    private final FileChecksumService checksumService;
    private final FileMetadataRepository fileMetadataRepository;
    private final UploadProperties uploadProperties;
    private final FileValidationService fileValidationService;
    private final ThumbnailPolicy thumbnailPolicy;

    @Transactional
    public FileUploadResponse upload(MultipartFile file) {

        validateBasicUpload(file);

        FileValidationResult validation =
                fileValidationService.validate(file);

        String checksum = checksumService.sha256(file);

        String originalFilename = sanitizeOriginalFilename(Objects.requireNonNull(file.getOriginalFilename()));

        String objectKey = objectKeyGenerator.generate(originalFilename);

        boolean stored = false;

        try {

            objectStorage.put(objectKey, file);

            stored = true;

            String contentType =
                    validation.detectedContentType();

            ThumbnailStatus thumbnailStatus =
                    thumbnailPolicy.initialStatus(
                            contentType
                    );

            FileMetadata metadata =
                    new FileMetadata(
                            originalFilename,
                            extractStoredFilename(objectKey),
                            objectKey,
                            contentType,
                            file.getSize(),
                            checksum,
                            FileStatus.UPLOADED,
                            ScanStatus.PENDING,
                            thumbnailStatus
                    );

            FileMetadata saved = fileMetadataRepository.save(metadata);

            return toResponse(saved);

        } catch (Exception exception) {

            if (stored) {
                try {
                    objectStorage.delete(objectKey);
                } catch (Exception cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }

            throw new InvalidFileException("File upload failed", exception);
        }
    }

    private void validateBasicUpload(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new InvalidFileException("File must not be empty");
        }

        if (file.getSize() > uploadProperties.maxFileSizeBytes()) {

            throw new InvalidFileException("File exceeds the maximum allowed size");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {

            throw new InvalidFileException("Original filename is required");
        }
    }

    private String sanitizeOriginalFilename(String filename) {

        String sanitized = filename.replace("\\", "_").replace("/", "_").replaceAll("[^a-zA-Z0-9._-]", "_");

        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(sanitized.length() - 255);
        }

        return sanitized;
    }

    private String resolveContentType(MultipartFile file) {

        String contentType = file.getContentType();

        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

    private String extractStoredFilename(String objectKey) {

        int index = objectKey.lastIndexOf('/');

        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }

    private FileUploadResponse toResponse(FileMetadata metadata) {

        return new FileUploadResponse(metadata.getId(), metadata.getOriginalFilename(), metadata.getContentType(), metadata.getSizeBytes(), metadata.getChecksumSha256(), metadata.getStatus().name(), metadata.getCreatedAt());
    }
}