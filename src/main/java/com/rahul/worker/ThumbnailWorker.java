package com.rahul.worker;

import com.rahul.config.ThumbnailProperties;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.event.FileCleanEvent;
import com.rahul.event.EventDeserializer;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.service.FileDerivativeService;
import com.rahul.service.FileStateService;
import com.rahul.storage.ObjectStorage;
import com.rahul.thumbnail.ThumbnailObjectKey;
import com.rahul.thumbnail.ThumbnailResult;
import com.rahul.thumbnail.ThumbnailService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class ThumbnailWorker {

    private final EventDeserializer eventDeserializer;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStateService fileStateService;
    private final ObjectStorage objectStorage;
    private final ThumbnailService thumbnailService;
    private final FileDerivativeService fileDerivativeService;
    private final ThumbnailProperties thumbnailProperties;

    @KafkaListener(topics = "${kafka.topics.file-clean}", groupId = "${kafka.consumer.thumbnail-group}")
    public void handle(String payload) {

        FileCleanEvent event = eventDeserializer.deserializeFileClean(payload);

        process(event);
    }

    private void process(FileCleanEvent event) {

        FileMetadata file = fileMetadataRepository.findById(event.fileId()).orElseThrow(() -> new IllegalArgumentException("File not found: " + event.fileId()));

        if (file.getStatus() != FileStatus.CLEAN) {
            return;
        }

        /*
         * Non-image files don't require a thumbnail.
         */
        if (event.contentType() == null || !event.contentType().startsWith("image/")) {

            fileStateService.transition(file.getId(), FileStatus.COMPLETED);

            return;
        }

        fileStateService.transition(file.getId(), FileStatus.THUMBNAIL_PROCESSING);

        try (InputStream inputStream = objectStorage.getObject(event.objectKey())) {

            ThumbnailResult result = thumbnailService.generate(inputStream);

            String thumbnailKey = ThumbnailObjectKey.from(event.objectKey(), thumbnailProperties.format());

            objectStorage.put(thumbnailKey, new ByteArrayInputStream(result.content()), result.content().length, result.contentType());

            fileDerivativeService.createThumbnail(file.getId(), thumbnailKey, result.contentType(), result.content().length, result.width(), result.height());

            fileStateService.transition(file.getId(), FileStatus.COMPLETED);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}