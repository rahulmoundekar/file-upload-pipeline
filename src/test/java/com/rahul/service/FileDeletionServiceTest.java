package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.event.EventTypes;
import com.rahul.event.FileDeletedEvent;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FileDeletionServiceTest {

    private FileMetadataRepository fileMetadataRepository;
    private FileStateService fileStateService;
    private OutboxService outboxService;

    private FileDeletionService service;

    @BeforeEach
    void setUp() {

        fileMetadataRepository = mock(FileMetadataRepository.class);
        fileStateService = mock(FileStateService.class);
        outboxService = mock(OutboxService.class);

        service = new FileDeletionService(fileMetadataRepository, fileStateService, outboxService);
    }

    @Test
    void shouldTransitionFileToDeletingAndCreateDeletionEvent() {

        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);
        FileMetadata deletingFile = mock(FileMetadata.class);

        when(file.getStatus()).thenReturn(FileStatus.COMPLETED);

        when(fileMetadataRepository.findById(fileId)).thenReturn(java.util.Optional.of(file));

        when(deletingFile.getId()).thenReturn(fileId);

        when(fileStateService.transition(fileId, FileStatus.DELETING)).thenReturn(deletingFile);

        service.requestDeletion(fileId);

        verify(fileMetadataRepository).findById(fileId);

        verify(fileStateService).transition(fileId, FileStatus.DELETING);

        verify(outboxService).create(eq("FILE"), eq(fileId), eq(EventTypes.FILE_DELETED), any(FileDeletedEvent.class));
    }

    @Test
    void shouldBeIdempotentWhenFileIsAlreadyDeleting() {

        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);

        when(file.getStatus()).thenReturn(FileStatus.DELETING);

        when(fileMetadataRepository.findById(fileId)).thenReturn(java.util.Optional.of(file));

        service.requestDeletion(fileId);

        verify(fileMetadataRepository).findById(fileId);

        verify(fileStateService, never()).transition(any(UUID.class), any(FileStatus.class));

        verify(outboxService, never()).create(anyString(), any(UUID.class), anyString(), any());
    }

    @Test
    void shouldBeIdempotentWhenFileIsAlreadyDeleted() {

        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);

        when(file.getStatus()).thenReturn(FileStatus.DELETED);

        when(fileMetadataRepository.findById(fileId)).thenReturn(java.util.Optional.of(file));

        service.requestDeletion(fileId);

        verify(fileMetadataRepository).findById(fileId);

        verify(fileStateService, never()).transition(any(UUID.class), any(FileStatus.class));

        verify(outboxService, never()).create(anyString(), any(UUID.class), anyString(), any());
    }

    @Test
    void shouldThrowExceptionWhenFileDoesNotExist() {

        UUID fileId = UUID.randomUUID();

        when(fileMetadataRepository.findById(fileId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.requestDeletion(fileId)).isInstanceOf(InvalidFileException.class).hasMessage("File not found: " + fileId);

        verify(fileStateService, never()).transition(any(UUID.class), any(FileStatus.class));

        verify(outboxService, never()).create(anyString(), any(UUID.class), anyString(), any());
    }
}
