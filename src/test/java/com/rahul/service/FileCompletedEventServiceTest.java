package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.OutboxEvent;
import com.rahul.event.EventSerializer;
import com.rahul.event.EventTypes;
import com.rahul.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileCompletedEventServiceTest {

    private OutboxEventRepository repository;
    private EventSerializer eventSerializer;

    private FileCompletedEventService service;

    @BeforeEach
    void setUp() {

        repository = mock(OutboxEventRepository.class);

        eventSerializer = mock(EventSerializer.class);

        service = new FileCompletedEventService(repository, eventSerializer);
    }

    @Test
    void shouldCreateFileCompletedOutboxEvent() {

        UUID fileId = UUID.randomUUID();

        FileMetadata file = mock(FileMetadata.class);

        when(file.getId()).thenReturn(fileId);

        when(file.getStatus()).thenReturn(FileStatus.COMPLETED);

        when(file.getObjectKey()).thenReturn("uploads/test.jpg");

        when(file.getOriginalFilename()).thenReturn("test.jpg");

        when(file.getContentType()).thenReturn("image/jpeg");

        when(file.getSizeBytes()).thenReturn(1000L);

        when(file.getChecksumSha256()).thenReturn("a".repeat(64));

        when(eventSerializer.serialize(any())).thenReturn("{\"status\":\"COMPLETED\"}");

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent result = service.createEvent(file);

        assertThat(result).isNotNull();

        assertThat(result.getEventType()).isEqualTo(EventTypes.FILE_COMPLETED);

        assertThat(result.getAggregateId()).isEqualTo(fileId);

        verify(repository).save(any(OutboxEvent.class));

        verify(eventSerializer).serialize(any());
    }
}