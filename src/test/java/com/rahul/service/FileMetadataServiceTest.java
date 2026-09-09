package com.rahul.service;

import com.rahul.dto.FileMetadataResponse;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileMetadataServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    private FileMetadataService fileMetadataService;

    @BeforeEach
    void setUp() {
        fileMetadataService = new FileMetadataService(fileMetadataRepository);
    }

    @Test
    void shouldReturnFileMetadata() {

        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = new FileMetadata("invoice.pdf", "stored-invoice.pdf", "uploads/" + fileId + "/stored-invoice.pdf", "application/pdf", 1024L, "abcdef1234567890", FileStatus.COMPLETED, ScanStatus.CLEAN, ThumbnailStatus.COMPLETED);

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

        FileMetadataResponse response = fileMetadataService.getFile(fileId);

        assertNotNull(response);

        assertEquals("invoice.pdf", response.originalFilename());

        assertEquals("application/pdf", response.contentType());

        assertEquals(1024L, response.sizeBytes());

        assertEquals("abcdef1234567890", response.checksumSha256());

        assertEquals(FileStatus.COMPLETED, response.status());

        assertEquals(ScanStatus.CLEAN, response.scanStatus());

        assertEquals(ThumbnailStatus.COMPLETED, response.thumbnailStatus());

        verify(fileMetadataRepository).findById(fileId);
    }

    @Test
    void shouldThrowExceptionWhenFileDoesNotExist() {

        UUID fileId = UUID.randomUUID();

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());

        InvalidFileException exception = assertThrows(InvalidFileException.class, () -> fileMetadataService.getFile(fileId));

        assertEquals("File not found", exception.getMessage());

        verify(fileMetadataRepository).findById(fileId);
    }

    @Test
    void shouldReturnProcessingStatus() {

        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = new FileMetadata("image.png", "stored-image.png", "uploads/" + fileId + "/stored-image.png", "image/png", 2048L, "1234567890abcdef", FileStatus.THUMBNAIL_PROCESSING, ScanStatus.CLEAN, ThumbnailStatus.PROCESSING);

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

        FileMetadataResponse response = fileMetadataService.getFile(fileId);

        assertEquals(FileStatus.THUMBNAIL_PROCESSING, response.status());

        assertEquals(ScanStatus.CLEAN, response.scanStatus());

        assertEquals(ThumbnailStatus.PROCESSING, response.thumbnailStatus());
    }

    @Test
    void shouldNotExposeInternalStorageInformation() {

        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = new FileMetadata("document.pdf", "secret-internal-name.pdf", "internal/storage/path/document.pdf", "application/pdf", 512L, "checksum", FileStatus.COMPLETED, ScanStatus.CLEAN, ThumbnailStatus.NOT_APPLICABLE);

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

        FileMetadataResponse response = fileMetadataService.getFile(fileId);

        assertNotNull(response);

        assertFalse(response.toString().contains("internal/storage/path/document.pdf"));

        assertFalse(response.toString().contains("secret-internal-name.pdf"));
    }
}