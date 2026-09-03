package com.rahul.worker;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.event.EventDeserializer;
import com.rahul.event.FileUploadedEvent;
import com.rahul.event.FileUploadedEventValidator;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.service.FileStateService;
import com.rahul.storage.ObjectStorage;
import com.rahul.virus.ScanResult;
import com.rahul.virus.VirusScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class VirusScanWorker {

    private final EventDeserializer eventDeserializer;
    private final FileUploadedEventValidator eventValidator;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStateService fileStateService;
    private final ObjectStorage objectStorage;
    private final VirusScanService virusScanService;

    @KafkaListener(topics = "${kafka.topics.file-uploaded}", groupId = "${kafka.consumer.virus-scan-group}")
    public void handle(String payload) {

        FileUploadedEvent event = eventDeserializer.deserializeFileUploaded(payload);

        eventValidator.validate(event);

        process(event);
    }

    private void process(FileUploadedEvent event) {

        FileMetadata file = fileMetadataRepository.findById(event.fileId()).orElseThrow(() -> new IllegalArgumentException("File not found: " + event.fileId()));

        if (!file.getObjectKey().equals(event.objectKey())) {
            throw new IllegalStateException("Object key mismatch for file: " + event.fileId());
        }

        fileStateService.transition(file.getId(), FileStatus.PROCESSING);

        fileStateService.transition(file.getId(), FileStatus.SCANNING);

        try (InputStream inputStream = objectStorage.getObject(event.objectKey())) {

            ScanResult result = virusScanService.scan(inputStream);

            handleScanResult(file.getId(), result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleScanResult(UUID fileId, ScanResult result) {

        if (result.status() == ScanResult.Status.CLEAN) {

            fileStateService.transition(fileId, FileStatus.CLEAN);

            return;
        }

        fileStateService.markInfected(fileId, result.signature());

        fileStateService.transition(fileId, FileStatus.REJECTED);
    }
}