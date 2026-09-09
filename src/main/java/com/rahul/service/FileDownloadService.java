package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final FileMetadataRepository fileMetadataRepository;
    private final ObjectStorage objectStorage;

    public FileDownload getFile(UUID fileId) {

        FileMetadata metadata = fileMetadataRepository.findById(fileId).orElseThrow(() -> new InvalidFileException("File not found"));

        if (metadata.getStatus() != FileStatus.COMPLETED) {

            throw new InvalidFileException("File is not available for download");
        }

        try {

            InputStream inputStream = objectStorage.get(metadata.getObjectKey());

            return new FileDownload(metadata.getOriginalFilename(), metadata.getContentType(), metadata.getSizeBytes(), inputStream);

        } catch (Exception exception) {

            throw new IllegalStateException("Unable to retrieve file", exception);
        }
    }

    public record FileDownload(String originalFilename, String contentType, long sizeBytes, InputStream inputStream) {
    }
}