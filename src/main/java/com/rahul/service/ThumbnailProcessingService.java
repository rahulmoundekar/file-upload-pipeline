package com.rahul.service;

import com.rahul.config.ThumbnailProperties;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.DerivativeType;
import com.rahul.event.FileCleanEvent;
import com.rahul.exception.ObjectKeyMismatchException;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.service.EventInboxService;
import com.rahul.storage.ObjectStorage;
import com.rahul.thumbnail.ThumbnailObjectKey;
import com.rahul.thumbnail.ThumbnailResult;
import com.rahul.thumbnail.ThumbnailService;
import com.rahul.worker.WorkerNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ThumbnailProcessingService {

    private final EventInboxService eventInboxService;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStateService fileStateService;
    private final ObjectStorage objectStorage;
    private final ThumbnailService thumbnailService;
    private final FileDerivativeService fileDerivativeService;
    private final ThumbnailProperties thumbnailProperties;
    private final FileCompletionService fileCompletionService;

    @Transactional
    public void process(FileCleanEvent event) {

        boolean claimed = eventInboxService.markProcessed(event.eventId(), WorkerNames.THUMBNAIL);

        if (!claimed) {
            return;
        }

        FileMetadata file = fileMetadataRepository.findById(event.fileId()).orElseThrow(() -> new IllegalArgumentException("File not found: " + event.fileId()));

        if (!file.getObjectKey().equals(event.objectKey())) {
            throw new ObjectKeyMismatchException("Object key mismatch for file: " + event.fileId());
        }

        /*
         * A clean file is required before thumbnail processing.
         */
        if (file.getStatus() != FileStatus.CLEAN) {
            return;
        }
        fileCompletionService.complete(file);

        /*
         * Non-image files don't need thumbnails.
         */
        if (event.contentType() == null || !event.contentType().startsWith("image/")) {

            fileCompletionService.complete(file);

            return;
        }

        fileStateService.transition(file.getId(), FileStatus.THUMBNAIL_PROCESSING);

        try (InputStream inputStream = objectStorage.getObject(event.objectKey())) {
            ThumbnailResult result = thumbnailService.generate(inputStream);

            String thumbnailKey = ThumbnailObjectKey.from(event.objectKey(), thumbnailProperties.format());

            objectStorage.put(thumbnailKey, new ByteArrayInputStream(result.content()), result.content().length, result.contentType());

            fileDerivativeService.createThumbnail(file.getId(), thumbnailKey, result.contentType(), result.content().length, result.width(), result.height());

            fileCompletionService.complete(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}