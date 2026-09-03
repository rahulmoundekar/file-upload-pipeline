package com.rahul.service;

import com.rahul.config.KafkaProperties;
import com.rahul.entity.OutboxEvent;
import com.rahul.entity.OutboxStatus;
import com.rahul.event.EventTypes;
import com.rahul.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = false)
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final OutboxRetryPolicy retryPolicy;

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:1000}")
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxEventRepository.findReadyForPublishing(OutboxStatus.PENDING, Instant.now());

        for (OutboxEvent event : events) {

            publish(event);
        }
    }

    @Transactional
    public void publish(OutboxEvent event) {

        try {

            String topic = resolveTopic(event);

            String key = event.getAggregateId().toString();

            kafkaTemplate.send(topic, key, event.getPayload()).get();

            event.markPublished();

            outboxEventRepository.save(event);

        } catch (Exception exception) {

            handleFailure(event, exception);
        }
    }

    private void handleFailure(OutboxEvent event, Exception exception) {

        String message = exception.getMessage();

        if (retryPolicy.shouldRetry(event.getAttempts() + 1)) {

            Instant nextAttempt = retryPolicy.nextAttemptAt(event.getAttempts() + 1);

            event.scheduleRetry(message, nextAttempt);

        } else {

            event.markFailed(message);
        }

        outboxEventRepository.save(event);
    }

    private String resolveTopic(OutboxEvent event) {

        return switch (event.getEventType()) {

            case EventTypes.FILE_UPLOADED -> kafkaProperties.topics().fileUploaded();

            case EventTypes.FILE_CLEAN -> kafkaProperties.topics().fileClean();

            case EventTypes.FILE_COMPLETED -> kafkaProperties.topics().fileCompleted();

            default -> throw new IllegalArgumentException("Unsupported event type: " + event.getEventType());
        };
    }
}