package com.rahul.worker;

import com.rahul.config.WebhookProperties;
import com.rahul.event.EventDeserializer;
import com.rahul.event.FileCompletedEvent;
import com.rahul.webhook.WebhookClient;
import com.rahul.webhook.WebhookSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "webhook.enabled", havingValue = "true", matchIfMissing = false)
public class WebhookWorker {

    private final EventDeserializer eventDeserializer;
    private final WebhookClient webhookClient;
    private final WebhookProperties webhookProperties;

    @KafkaListener(topics = "${kafka.topics.file-completed}", groupId = "${kafka.consumer.webhook-group}")
    public void handle(String payload) {

        FileCompletedEvent event = eventDeserializer.deserializeFileCompleted(payload);

        String signature = WebhookSigner.sign(payload, webhookProperties.secret());

        webhookClient.send(payload, signature);
    }
}