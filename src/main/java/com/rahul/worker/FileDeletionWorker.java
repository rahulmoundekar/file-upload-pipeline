package com.rahul.worker;

import com.rahul.entity.FileDerivative;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.event.EventDeserializer;
import com.rahul.event.FileDeletedEvent;
import com.rahul.repository.FileDerivativeRepository;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.service.EventInboxService;
import com.rahul.service.FileStateService;
import com.rahul.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class FileDeletionWorker {

    private final EventDeserializer eventDeserializer;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileDerivativeRepository fileDerivativeRepository;
    private final FileStateService fileStateService;
    private final ObjectStorage objectStorage;
    private final EventInboxService eventInboxService;

    @KafkaListener(topics = "${kafka.topics.file-deleted}", groupId = "${kafka.consumer.deletion-group}")
    public void handle(String payload) {

        FileDeletedEvent event = eventDeserializer.deserializeFileDeleted(payload);

        if (eventInboxService.alreadyProcessed(event.eventId(), WorkerNames.FILE_DELETION)) {
            return;
        }

        process(event);

        eventInboxService.markProcessed(event.eventId(), WorkerNames.FILE_DELETION);
    }

    private void process(FileDeletedEvent event) {

        FileMetadata file = fileMetadataRepository.findById(event.fileId()).orElse(null);

        if (file == null) {
            return;
        }

        /*
         * Idempotency at business level:
         * the cleanup may be retried after a worker crash.
         */
        if (file.getStatus() == FileStatus.DELETED) {
            return;
        }

        if (file.getStatus() != FileStatus.DELETING) {
            throw new IllegalStateException("File is not in DELETING state: " + event.fileId());
        }

        deleteOriginal(file);

        deleteDerivatives(file.getId());

        fileStateService.transition(file.getId(), FileStatus.DELETED);
    }

    private void deleteOriginal(FileMetadata file) {

        if (file.getObjectKey() == null || file.getObjectKey().isBlank()) {
            return;
        }

        objectStorage.deleteObject(file.getObjectKey());
    }

    private void deleteDerivatives(java.util.UUID fileId) {

        List<FileDerivative> derivatives = fileDerivativeRepository.findByFileId(fileId);

        for (FileDerivative derivative : derivatives) {

            objectStorage.deleteObject(derivative.getObjectKey());
        }

        if (!derivatives.isEmpty()) {

            fileDerivativeRepository.deleteAll(derivatives);
        }
    }
}