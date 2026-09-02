package com.rahul.service;

import com.rahul.config.KafkaProperties;
import com.rahul.entity.OutboxEvent;
import com.rahul.entity.OutboxStatus;
import com.rahul.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private OutboxEventRepository outboxEventRepository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaProperties kafkaProperties;
    private OutboxRetryPolicy retryPolicy;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {

        outboxEventRepository = mock(OutboxEventRepository.class);

        kafkaTemplate = mock(KafkaTemplate.class);

        kafkaProperties = new KafkaProperties("localhost:9092", new KafkaProperties.Topics("file.uploaded", "file.virus-scan", "file.thumbnail", "file.processing", "file.webhook"));

        retryPolicy = new OutboxRetryPolicy(new com.rahul.config.OutboxProperties(5, 1000, 60000));

        publisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, kafkaProperties, retryPolicy);
    }

    @Test
    void successfulPublishShouldMarkEventPublished() throws Exception {

        OutboxEvent event = new OutboxEvent("FILE", UUID.randomUUID(), "FILE_UPLOADED", "{}");

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(eq("file.uploaded"), eq(event.getAggregateId().toString()), eq("{}"))).thenReturn(future);

        publisher.publish(event);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);

        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void failedPublishShouldScheduleRetry() throws Exception {

        OutboxEvent event = new OutboxEvent("FILE", UUID.randomUUID(), "FILE_UPLOADED", "{}");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();

        future.completeExceptionally(new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class))).thenReturn(future);

        publisher.publish(event);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getAttempts()).isEqualTo(1);

        assertThat(event.getNextAttemptAt()).isNotNull();

        assertThat(event.getLastError()).contains("Kafka unavailable");
    }

    @Test
    void unsupportedEventTypeShouldScheduleRetryOrFail() throws Exception {

        OutboxEvent event = new OutboxEvent("FILE", UUID.randomUUID(), "UNKNOWN_EVENT", "{}");

        publisher.publish(event);

        assertThat(event.getAttempts()).isEqualTo(1);

        assertThat(event.getLastError()).contains("Unsupported event type");
    }
}