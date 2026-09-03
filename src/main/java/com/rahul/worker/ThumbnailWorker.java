package com.rahul.worker;

import com.rahul.event.EventDeserializer;
import com.rahul.event.FileCleanEvent;
import com.rahul.service.ThumbnailProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class ThumbnailWorker {

    private final EventDeserializer eventDeserializer;
    private final ThumbnailProcessingService processingService;

    @KafkaListener(
            topics = "${kafka.topics.file-clean}",
            groupId = "${kafka.consumer.thumbnail-group}"
    )
    public void handle(String payload) {

        FileCleanEvent event = eventDeserializer.deserializeFileClean(payload);

        processingService.process(event);
    }
}