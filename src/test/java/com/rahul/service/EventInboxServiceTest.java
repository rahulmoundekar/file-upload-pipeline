package com.rahul.service;

import com.rahul.entity.ProcessedEvent;
import com.rahul.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventInboxServiceTest {

    private ProcessedEventRepository repository;

    private EventInboxService service;

    @BeforeEach
    void setUp() {

        repository = mock(ProcessedEventRepository.class);

        service = new EventInboxService(repository);
    }

    @Test
    void firstEventShouldBeMarkedProcessed() {

        UUID eventId = UUID.randomUUID();

        when(repository.existsByEventIdAndConsumerName(eventId, "virus-scan-worker")).thenReturn(false);

        boolean result = service.markProcessed(eventId, "virus-scan-worker");

        assertThat(result).isTrue();

        verify(repository).saveAndFlush(any(ProcessedEvent.class));
    }

    @Test
    void duplicateEventShouldNotBeProcessedAgain() {

        UUID eventId = UUID.randomUUID();

        when(repository.existsByEventIdAndConsumerName(eventId, "virus-scan-worker")).thenReturn(true);

        boolean result = service.markProcessed(eventId, "virus-scan-worker");

        assertThat(result).isFalse();

        verify(repository, never()).saveAndFlush(any());
    }

}