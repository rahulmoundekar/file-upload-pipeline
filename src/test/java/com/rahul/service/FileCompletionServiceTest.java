package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class FileCompletionServiceTest {

    private FileStateService fileStateService;
    private FileCompletedEventService fileCompletedEventService;

    private FileCompletionService service;

    @BeforeEach
    void setUp() {

        fileStateService = mock(FileStateService.class);

        fileCompletedEventService = mock(FileCompletedEventService.class);

        service = new FileCompletionService(fileStateService, fileCompletedEventService);
    }

    @Test
    void shouldTransitionToCompletedAndCreateEvent() {

        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);

        FileMetadata completedFile = mock(FileMetadata.class);

        when(file.getId()).thenReturn(fileId);

        when(fileStateService.transition(fileId, FileStatus.COMPLETED)).thenReturn(completedFile);

        service.complete(file);

        verify(fileStateService).transition(fileId, FileStatus.COMPLETED);

        verify(fileCompletedEventService).createEvent(completedFile);
    }
}