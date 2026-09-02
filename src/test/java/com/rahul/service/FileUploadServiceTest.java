package com.rahul.service;

import com.rahul.config.UploadProperties;
import com.rahul.dto.FileUploadResponse;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.storage.ObjectKeyGenerator;
import com.rahul.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private ObjectStorage objectStorage;

    @Mock
    private ObjectKeyGenerator objectKeyGenerator;

    @Mock
    private FileChecksumService checksumService;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private UploadProperties uploadProperties;

    @InjectMocks
    private FileUploadService fileUploadService;

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private ThumbnailPolicy thumbnailPolicy;

    @Mock
    private OutboxService outboxService;

    @Test
    void uploadShouldStoreObjectAndPersistMetadata() throws IOException {

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());

        when(uploadProperties.maxFileSizeBytes()).thenReturn(50_000_000L);

        when(fileValidationService.validate(file)).thenReturn(new FileValidationResult("hello.txt", "txt", "text/plain", "text/plain"));

        when(thumbnailPolicy.initialStatus("text/plain"))
                .thenReturn(ThumbnailStatus.NOT_REQUIRED);

        when(checksumService.sha256(file)).thenReturn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        when(objectKeyGenerator.generate("hello.txt")).thenReturn("uploads/2026/09/02/test-hello.txt");

        when(fileMetadataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FileUploadResponse response = fileUploadService.upload(file);

        assertThat(response).isNotNull();

        verify(fileValidationService).validate(file);

        verify(objectStorage).put(eq("uploads/2026/09/02/test-hello.txt"), eq(file));

        verify(fileMetadataRepository).save(any());
    }

    @Test
    void uploadShouldRejectFileExceedingLimit() {

        MockMultipartFile file = new MockMultipartFile("file", "large.txt", "text/plain", "hello".getBytes());

        when(uploadProperties.maxFileSizeBytes()).thenReturn(2L);

        assertThatThrownBy(() -> fileUploadService.upload(file)).isInstanceOf(InvalidFileException.class).hasMessage("File exceeds the maximum allowed size");

        verifyNoInteractions(objectStorage, fileMetadataRepository, fileValidationService);
    }

    @Test
    void uploadShouldRejectEmptyFile() {

        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> fileUploadService.upload(file)).isInstanceOf(InvalidFileException.class).hasMessage("File must not be empty");

        verifyNoInteractions(objectStorage, fileMetadataRepository, fileValidationService);
    }

}