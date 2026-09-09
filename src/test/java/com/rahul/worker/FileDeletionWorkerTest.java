package com.rahul.worker;

import com.rahul.entity.FileDerivative;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.event.EventDeserializer;
import com.rahul.event.FileDeletedEvent;
import com.rahul.ops.PipelineMetrics;
import com.rahul.repository.FileDerivativeRepository;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.service.EventInboxService;
import com.rahul.service.FileStateService;
import com.rahul.storage.ObjectStorage;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FileDeletionWorkerTest {

    private EventDeserializer eventDeserializer;
    private FileMetadataRepository fileMetadataRepository;
    private FileDerivativeRepository fileDerivativeRepository;
    private FileStateService fileStateService;
    private ObjectStorage objectStorage;
    private EventInboxService eventInboxService;
    private PipelineMetrics metrics;
    private Counter deletionCounter;

    private FileDeletionWorker worker;

    @BeforeEach
    void setUp() {
        eventDeserializer = mock(EventDeserializer.class);
        fileMetadataRepository = mock(FileMetadataRepository.class);
        fileDerivativeRepository = mock(FileDerivativeRepository.class);
        fileStateService = mock(FileStateService.class);
        objectStorage = mock(ObjectStorage.class);
        eventInboxService = mock(EventInboxService.class);
        metrics = mock(PipelineMetrics.class);
        deletionCounter = mock(Counter.class);

        when(metrics.deletions()).thenReturn(deletionCounter);

        worker = new FileDeletionWorker(
                eventDeserializer,
                fileMetadataRepository,
                fileDerivativeRepository,
                fileStateService,
                objectStorage,
                eventInboxService,
                metrics
        );
    }

    @Test
    void shouldDeleteOriginalAndDerivativesAndMarkFileDeleted() {
        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        FileDeletedEvent event =
                new FileDeletedEvent(eventId, fileId, Instant.now());

        FileMetadata file = mock(FileMetadata.class);
        FileDerivative thumbnail = mock(FileDerivative.class);
        FileDerivative preview = mock(FileDerivative.class);

        when(eventDeserializer.deserializeFileDeleted("{}"))
                .thenReturn(event);
        when(eventInboxService.alreadyProcessed(
                eventId,
                WorkerNames.FILE_DELETION
        )).thenReturn(false);
        when(fileMetadataRepository.findById(fileId))
                .thenReturn(Optional.of(file));
        when(file.getId()).thenReturn(fileId);
        when(file.getStatus()).thenReturn(FileStatus.DELETING);
        when(file.getObjectKey())
                .thenReturn("uploads/test-image.png");
        when(fileDerivativeRepository.findByFileId(fileId))
                .thenReturn(List.of(thumbnail, preview));
        when(thumbnail.getObjectKey())
                .thenReturn("derivatives/t.jpg");
        when(preview.getObjectKey())
                .thenReturn("derivatives/p.jpg");

        worker.handle("{}");

        verify(objectStorage)
                .deleteObject("uploads/test-image.png");
        verify(objectStorage)
                .deleteObject("derivatives/t.jpg");
        verify(objectStorage)
                .deleteObject("derivatives/p.jpg");
        verify(fileDerivativeRepository)
                .deleteAll(List.of(thumbnail, preview));
        verify(fileStateService)
                .transition(fileId, FileStatus.DELETED);
        verify(eventInboxService)
                .markProcessed(eventId, WorkerNames.FILE_DELETION);
        verify(deletionCounter).increment();
    }

    @Test
    void shouldSkipAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        when(eventDeserializer.deserializeFileDeleted("{}"))
                .thenReturn(new FileDeletedEvent(
                        eventId,
                        fileId,
                        Instant.now()
                ));
        when(eventInboxService.alreadyProcessed(
                eventId,
                WorkerNames.FILE_DELETION
        )).thenReturn(true);

        worker.handle("{}");

        verifyNoInteractions(
                fileMetadataRepository,
                fileDerivativeRepository,
                fileStateService,
                objectStorage,
                metrics
        );
        verify(eventInboxService, never())
                .markProcessed(any(), anyString());
    }

    @Test
    void shouldSkipWhenFileDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        when(eventDeserializer.deserializeFileDeleted("{}"))
                .thenReturn(new FileDeletedEvent(
                        eventId,
                        fileId,
                        Instant.now()
                ));
        when(eventInboxService.alreadyProcessed(
                eventId,
                WorkerNames.FILE_DELETION
        )).thenReturn(false);
        when(fileMetadataRepository.findById(fileId))
                .thenReturn(Optional.empty());

        worker.handle("{}");

        verifyNoInteractions(
                fileDerivativeRepository,
                fileStateService,
                objectStorage
        );
        verify(eventInboxService)
                .markProcessed(eventId, WorkerNames.FILE_DELETION);
    }

    @Test
    void shouldSkipWhenFileIsAlreadyDeleted() {
        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);

        when(eventDeserializer.deserializeFileDeleted("{}"))
                .thenReturn(new FileDeletedEvent(
                        eventId,
                        fileId,
                        Instant.now()
                ));
        when(eventInboxService.alreadyProcessed(
                eventId,
                WorkerNames.FILE_DELETION
        )).thenReturn(false);
        when(fileMetadataRepository.findById(fileId))
                .thenReturn(Optional.of(file));
        when(file.getStatus()).thenReturn(FileStatus.DELETED);

        worker.handle("{}");

        verifyNoInteractions(
                fileDerivativeRepository,
                objectStorage,
                fileStateService,
                metrics
        );
        verify(eventInboxService)
                .markProcessed(eventId, WorkerNames.FILE_DELETION);
    }

    @Test
    void shouldSkipOriginalObjectWhenObjectKeyIsBlank() {
        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);

        when(eventDeserializer.deserializeFileDeleted("{}"))
                .thenReturn(new FileDeletedEvent(
                        eventId,
                        fileId,
                        Instant.now()
                ));
        when(eventInboxService.alreadyProcessed(
                eventId,
                WorkerNames.FILE_DELETION
        )).thenReturn(false);
        when(fileMetadataRepository.findById(fileId))
                .thenReturn(Optional.of(file));
        when(file.getId()).thenReturn(fileId);
        when(file.getStatus()).thenReturn(FileStatus.DELETING);
        when(file.getObjectKey()).thenReturn(" ");
        when(fileDerivativeRepository.findByFileId(fileId))
                .thenReturn(List.of());

        worker.handle("{}");

        verify(objectStorage, never())
                .deleteObject(anyString());
        verify(fileDerivativeRepository)
                .findByFileId(fileId);
        verify(fileStateService)
                .transition(fileId, FileStatus.DELETED);
        verify(eventInboxService)
                .markProcessed(eventId, WorkerNames.FILE_DELETION);
        verify(deletionCounter).increment();
    }

    @Test
    void shouldRejectFileThatIsNotInDeletingState() {
        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);

        when(eventDeserializer.deserializeFileDeleted("{}"))
                .thenReturn(new FileDeletedEvent(
                        eventId,
                        fileId,
                        Instant.now()
                ));
        when(eventInboxService.alreadyProcessed(
                eventId,
                WorkerNames.FILE_DELETION
        )).thenReturn(false);
        when(fileMetadataRepository.findById(fileId))
                .thenReturn(Optional.of(file));
        when(file.getStatus()).thenReturn(FileStatus.COMPLETED);

        assertThatThrownBy(() -> worker.handle("{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("File is not in DELETING state: " + fileId);

        verifyNoInteractions(
                fileDerivativeRepository,
                objectStorage,
                fileStateService,
                metrics
        );
        verify(eventInboxService, never())
                .markProcessed(any(), anyString());
    }

    @Test
    void shouldNotMarkEventProcessedWhenStorageDeletionFails() {
        UUID eventId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);

        when(eventDeserializer.deserializeFileDeleted("{}"))
                .thenReturn(new FileDeletedEvent(
                        eventId,
                        fileId,
                        Instant.now()
                ));
        when(eventInboxService.alreadyProcessed(
                eventId,
                WorkerNames.FILE_DELETION
        )).thenReturn(false);
        when(fileMetadataRepository.findById(fileId))
                .thenReturn(Optional.of(file));
        when(file.getId()).thenReturn(fileId);
        when(file.getStatus()).thenReturn(FileStatus.DELETING);
        when(file.getObjectKey())
                .thenReturn("uploads/test.txt");

        doThrow(new RuntimeException("MinIO unavailable"))
                .when(objectStorage)
                .deleteObject("uploads/test.txt");

        assertThatThrownBy(() -> worker.handle("{}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("MinIO unavailable");

        verify(objectStorage)
                .deleteObject("uploads/test.txt");
        verifyNoInteractions(
                fileDerivativeRepository,
                fileStateService
        );
        verify(eventInboxService, never())
                .markProcessed(any(), anyString());
    }
}
