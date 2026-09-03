package com.rahul.worker;

import com.rahul.config.WebhookProperties;
import com.rahul.event.EventDeserializer;
import com.rahul.event.FileCompletedEvent;
import com.rahul.webhook.WebhookClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebhookWorkerTest {

    private EventDeserializer eventDeserializer;
    private WebhookClient webhookClient;
    private WebhookProperties webhookProperties;

    private WebhookWorker worker;

    @BeforeEach
    void setUp() {

        eventDeserializer = mock(EventDeserializer.class);

        webhookClient = mock(WebhookClient.class);

        webhookProperties = new WebhookProperties(true, "http://localhost:8089/webhook", "test-secret", 5000, 5000);

        worker = new WebhookWorker(eventDeserializer, webhookClient, webhookProperties);
    }

    @Test
    void shouldSendCompletedEventToWebhook() {

        UUID eventId = UUID.randomUUID();

        UUID fileId = UUID.randomUUID();

        FileCompletedEvent event = new FileCompletedEvent(eventId, fileId, "COMPLETED", "uploads/test.jpg", "test.jpg", "image/jpeg", 1000L, "a".repeat(64), Instant.now());

        String payload = "{\"status\":\"COMPLETED\"}";

        when(eventDeserializer.deserializeFileCompleted(payload)).thenReturn(event);

        worker.handle(payload);

        verify(eventDeserializer).deserializeFileCompleted(payload);

        verify(webhookClient).send(eq(payload), startsWith("sha256="));
    }
}