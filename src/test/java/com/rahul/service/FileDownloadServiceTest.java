package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.storage.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDownloadServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private ObjectStorage objectStorage;

    private FileDownloadService fileDownloadService;

    @BeforeEach
    void setUp() {
        fileDownloadService = new FileDownloadService(fileMetadataRepository, objectStorage);
    }

    @Test
    void shouldReturnCompletedFileForDownload() {

        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = createMetadata(fileId, FileStatus.COMPLETED);

        InputStream inputStream = new ByteArrayInputStream("hello file".getBytes());

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

        when(objectStorage.get(metadata.getObjectKey())).thenReturn(inputStream);

        FileDownloadService.FileDownload result = fileDownloadService.getFile(fileId);

        assertNotNull(result);
        assertEquals(metadata.getOriginalFilename(), result.originalFilename());
        assertEquals(metadata.getContentType(), result.contentType());
        assertEquals(metadata.getSizeBytes(), result.sizeBytes());
        assertSame(inputStream, result.inputStream());

        verify(fileMetadataRepository).findById(fileId);
        verify(objectStorage).get(metadata.getObjectKey());
    }

    @Test
    void shouldThrowExceptionWhenFileDoesNotExist() {

        UUID fileId = UUID.randomUUID();

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());

        InvalidFileException exception = assertThrows(InvalidFileException.class, () -> fileDownloadService.getFile(fileId));

        assertEquals("File not found", exception.getMessage());

        verify(fileMetadataRepository).findById(fileId);
        verifyNoInteractions(objectStorage);
    }

    @Test
    void shouldRejectDownloadWhenFileIsNotCompleted() {

        UUID fileId = UUID.randomUUID();

        FileStatus[] unavailableStatuses = {FileStatus.UPLOADING, FileStatus.UPLOADED, FileStatus.PROCESSING, FileStatus.SCANNING, FileStatus.CLEAN, FileStatus.THUMBNAIL_PROCESSING, FileStatus.INFECTED, FileStatus.REJECTED, FileStatus.FAILED};

        for (FileStatus status : unavailableStatuses) {

            FileMetadata metadata = createMetadata(fileId, status);

            when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

            InvalidFileException exception = assertThrows(InvalidFileException.class, () -> fileDownloadService.getFile(fileId));

            assertEquals("File is not available for download", exception.getMessage());

            verify(fileMetadataRepository, atLeastOnce()).findById(fileId);

            verify(objectStorage, never()).get(anyString());

            clearInvocations(fileMetadataRepository);
        }
    }

    @Test
    void shouldRetrieveObjectUsingInternalObjectKey() {

        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = createMetadata(fileId, FileStatus.COMPLETED);

        InputStream inputStream = new ByteArrayInputStream("secure content".getBytes());

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

        when(objectStorage.get(metadata.getObjectKey())).thenReturn(inputStream);

        FileDownloadService.FileDownload result = fileDownloadService.getFile(fileId);

        assertEquals(metadata.getOriginalFilename(), result.originalFilename());

        verify(objectStorage).get(metadata.getObjectKey());
    }

    @Test
    void shouldWrapStorageFailure() {

        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = createMetadata(fileId, FileStatus.COMPLETED);

        RuntimeException storageException = new RuntimeException("MinIO unavailable");

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

        when(objectStorage.get(metadata.getObjectKey())).thenThrow(storageException);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> fileDownloadService.getFile(fileId));

        assertEquals("Unable to retrieve file", exception.getMessage());

        assertSame(storageException, exception.getCause());

        verify(objectStorage).get(metadata.getObjectKey());
    }

    private FileMetadata createMetadata(UUID fileId, FileStatus status) {

        FileMetadata metadata = new FileMetadata("document.pdf", "stored-document.pdf", "uploads/" + fileId + "/stored-document.pdf", "application/pdf",
                1024L, "0123456789abcdef", status, ScanStatus.CLEAN, ThumbnailStatus.NOT_APPLICABLE);

        // The ID is generated by JPA in production.
        // For this unit test, the actual ID value is not required.
        return metadata;
    }
}