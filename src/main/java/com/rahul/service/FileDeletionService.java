package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.event.EventTypes;
import com.rahul.event.FileDeletedEvent;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileDeletionService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStateService fileStateService;
    private final OutboxService outboxService;

    @Transactional
    public void requestDeletion(UUID fileId) {

        FileMetadata file = fileMetadataRepository.findById(fileId).orElseThrow(() -> new InvalidFileException("File not found: " + fileId));

        FileStatus currentStatus = file.getStatus();

        /*
         * Idempotent delete request:
         * - DELETING means deletion has already been requested.
         * - DELETED means deletion has already completed.
         */
        if (currentStatus == FileStatus.DELETING || currentStatus == FileStatus.DELETED) {
            return;
        }

        /*
         * FileStateService validates the allowed transition and
         * persists the updated FileMetadata.
         */
        FileMetadata deletingFile = fileStateService.transition(fileId, FileStatus.DELETING);

        /*
         * Transactionally create an outbox event.
         * OutboxPublisher will later publish this event to Kafka.
         */
        FileDeletedEvent event = new FileDeletedEvent(UUID.randomUUID(), deletingFile.getId(), Instant.now());

        outboxService.create("FILE", deletingFile.getId(), EventTypes.FILE_DELETED, event);
    }
}
