package com.rahul.service;

import com.rahul.entity.OutboxEvent;
import com.rahul.entity.OutboxStatus;
import com.rahul.event.FileUploadedEvent;
import com.rahul.event.EventSerializer;
import com.rahul.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private EventSerializer serializer;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void shouldCreatePendingOutboxEvent() {

        UUID fileId = UUID.randomUUID();

        FileUploadedEvent event = new FileUploadedEvent(UUID.randomUUID(), fileId, "uploads/test.txt", "test.txt", "text/plain", 10, "abc", Instant.now());

        when(serializer.serialize(event)).thenReturn("{\"fileId\":\"" + fileId + "\"}");

        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent saved = outboxService.create("FILE", fileId, "FILE_UPLOADED", event);

        assertThat(saved).isNotNull();

        assertThat(saved.getAggregateId()).isEqualTo(fileId);

        assertThat(saved.getEventType()).isEqualTo("FILE_UPLOADED");

        assertThat(saved.getStatus().name()).isEqualTo("PENDING");

        verify(repository).save(any(OutboxEvent.class));
    }

    @Test
    void markPublishedShouldClearRetryState() {

        OutboxEvent event = new OutboxEvent("FILE", UUID.randomUUID(), "FILE_UPLOADED", "{}");

        event.scheduleRetry("Kafka unavailable", Instant.now().plusSeconds(5));

        event.markPublished();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);

        assertThat(event.getNextAttemptAt()).isNull();

        assertThat(event.getLastError()).isNull();

        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void scheduleRetryShouldKeepEventPending() {

        OutboxEvent event = new OutboxEvent("FILE", UUID.randomUUID(), "FILE_UPLOADED", "{}");

        Instant next = Instant.now().plusSeconds(5);

        event.scheduleRetry("Kafka unavailable", next);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getAttempts()).isEqualTo(1);

        assertThat(event.getLastError()).isEqualTo("Kafka unavailable");

        assertThat(event.getNextAttemptAt()).isEqualTo(next);
    }

    @Test
    void markFailedShouldMakeEventTerminal() {

        OutboxEvent event = new OutboxEvent("FILE", UUID.randomUUID(), "FILE_UPLOADED", "{}");

        event.markFailed("Maximum retry attempts exceeded");

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);

        assertThat(event.getAttempts()).isEqualTo(1);

        assertThat(event.getNextAttemptAt()).isNull();
    }


}