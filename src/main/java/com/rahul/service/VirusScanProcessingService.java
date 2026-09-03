package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.event.FileUploadedEvent;
import com.rahul.exception.FileNotFoundForEventException;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.storage.ObjectStorage;
import com.rahul.virus.ScanResult;
import com.rahul.virus.VirusScanService;
import com.rahul.worker.WorkerNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class VirusScanProcessingService {

    private final EventInboxService eventInboxService;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStateService fileStateService;
    private final ObjectStorage objectStorage;
    private final VirusScanService virusScanService;

    @Transactional
    public void process(FileUploadedEvent event) {

        boolean claimed = eventInboxService.markProcessed(event.eventId(), WorkerNames.VIRUS_SCAN);

        if (!claimed) {
            return;
        }

        FileMetadata file = fileMetadataRepository.findById(event.fileId()).orElseThrow(() -> new FileNotFoundForEventException("File not found: " + event.fileId()));

        if (!file.getObjectKey().equals(event.objectKey())) {

            throw new IllegalStateException("Object key mismatch for file: " + event.fileId());
        }

        fileStateService.transition(file.getId(), FileStatus.PROCESSING);

        fileStateService.transition(file.getId(), FileStatus.SCANNING);

        try (InputStream inputStream = objectStorage.getObject(event.objectKey())) {

            ScanResult result = virusScanService.scan(inputStream);

            if (result.status() == ScanResult.Status.CLEAN) {

                fileStateService.transition(file.getId(), FileStatus.CLEAN);

            } else {

                fileStateService.markInfected(file.getId(), result.signature());

                fileStateService.transition(file.getId(), FileStatus.REJECTED);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}