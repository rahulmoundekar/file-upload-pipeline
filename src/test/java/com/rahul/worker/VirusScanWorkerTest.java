package com.rahul.worker;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.event.EventDeserializer;
import com.rahul.event.FileUploadedEvent;
import com.rahul.event.FileUploadedEventValidator;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.service.FileStateService;
import com.rahul.storage.ObjectStorage;
import com.rahul.virus.ScanResult;
import com.rahul.virus.VirusScanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VirusScanWorkerTest {

    private EventDeserializer eventDeserializer;
    private FileUploadedEventValidator eventValidator;
    private FileMetadataRepository fileMetadataRepository;
    private FileStateService fileStateService;
    private ObjectStorage objectStorage;
    private VirusScanService virusScanService;

    private VirusScanWorker worker;

    @BeforeEach
    void setUp() {

        eventDeserializer = mock(EventDeserializer.class);

        eventValidator = mock(FileUploadedEventValidator.class);

        fileMetadataRepository = mock(FileMetadataRepository.class);

        fileStateService = mock(FileStateService.class);

        objectStorage = mock(ObjectStorage.class);

        virusScanService = mock(VirusScanService.class);

        worker = new VirusScanWorker(eventDeserializer, eventValidator, fileMetadataRepository, fileStateService, objectStorage, virusScanService);
    }

    @Test
    void cleanFileShouldMoveToClean() {

        UUID fileId = UUID.randomUUID();

        String objectKey = "uploads/test.txt";

        FileUploadedEvent event = new FileUploadedEvent(UUID.randomUUID(), fileId, objectKey, "test.txt", "text/plain", 10, "a".repeat(64), Instant.now());

        FileMetadata metadata = mock(FileMetadata.class);

        when(metadata.getId()).thenReturn(fileId);

        when(metadata.getObjectKey()).thenReturn(objectKey);

        when(eventDeserializer.deserializeFileUploaded("{}")).thenReturn(event);

        when(fileMetadataRepository.findById(fileId)).thenReturn(java.util.Optional.of(metadata));

        when(objectStorage.getObject(objectKey)).thenReturn(new ByteArrayInputStream("hello".getBytes()));

        when(virusScanService.scan(any())).thenReturn(ScanResult.clean());

        worker.handle("{}");

        verify(fileStateService)
                .transition(
                        fileId,
                        FileStatus.PROCESSING
                );

        verify(fileStateService)
                .transition(
                        fileId,
                        FileStatus.SCANNING
                );

        verify(virusScanService).scan(any());

        verify(fileStateService).transition(fileId, FileStatus.CLEAN);

        verify(fileStateService, never()).markInfected(any(), any());
    }

    @Test
    void infectedFileShouldBeRejected() {

        UUID fileId = UUID.randomUUID();

        String objectKey = "uploads/eicar.txt";

        FileUploadedEvent event = new FileUploadedEvent(UUID.randomUUID(), fileId, objectKey, "eicar.txt", "text/plain", 100, "a".repeat(64), Instant.now());

        FileMetadata metadata = mock(FileMetadata.class);

        when(metadata.getId()).thenReturn(fileId);

        when(metadata.getObjectKey()).thenReturn(objectKey);

        when(eventDeserializer.deserializeFileUploaded("{}")).thenReturn(event);

        when(fileMetadataRepository.findById(fileId)).thenReturn(java.util.Optional.of(metadata));

        when(objectStorage.getObject(objectKey)).thenReturn(new ByteArrayInputStream("eicar".getBytes()));

        when(virusScanService.scan(any())).thenReturn(ScanResult.infected("Eicar-Test-Signature"));

        worker.handle("{}");

        verify(fileStateService)
                .transition(
                        fileId,
                        FileStatus.PROCESSING
                );

        verify(fileStateService)
                .transition(
                        fileId,
                        FileStatus.SCANNING
                );

        verify(fileStateService).markInfected(fileId, "Eicar-Test-Signature");

        verify(fileStateService).transition(fileId, FileStatus.REJECTED);
    }

    @Test
    void missingFileShouldBeRejectedFromProcessing() {

        UUID fileId = UUID.randomUUID();

        FileUploadedEvent event = new FileUploadedEvent(UUID.randomUUID(), fileId, "uploads/missing.txt", "missing.txt", "text/plain", 10, "a".repeat(64), Instant.now());

        when(eventDeserializer.deserializeFileUploaded("{}")).thenReturn(event);

        when(fileMetadataRepository.findById(fileId)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> worker.handle("{}")).isInstanceOf(IllegalArgumentException.class).hasMessage("File not found: " + fileId);

        verifyNoInteractions(objectStorage, virusScanService);
    }

    @Test
    void objectKeyMismatchShouldBeRejected() {

        UUID fileId = UUID.randomUUID();

        FileUploadedEvent event = new FileUploadedEvent(UUID.randomUUID(), fileId, "uploads/wrong.txt", "wrong.txt", "text/plain", 10, "a".repeat(64), Instant.now());

        FileMetadata metadata = mock(FileMetadata.class);

        when(metadata.getId()).thenReturn(fileId);

        when(metadata.getObjectKey()).thenReturn("uploads/real.txt");

        when(eventDeserializer.deserializeFileUploaded("{}")).thenReturn(event);

        when(fileMetadataRepository.findById(fileId)).thenReturn(java.util.Optional.of(metadata));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> worker.handle("{}")).isInstanceOf(IllegalStateException.class).hasMessage("Object key mismatch for file: " + fileId);

        verifyNoInteractions(objectStorage, virusScanService);
    }
}