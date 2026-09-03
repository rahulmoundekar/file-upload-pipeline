package com.rahul.service;

import com.rahul.event.EventSerializer;
import com.rahul.event.FileCleanEvent;
import com.rahul.entity.OutboxEvent;
import com.rahul.entity.FileMetadata;
import com.rahul.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileCleanEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public void createEvent(FileMetadata file) {

        FileCleanEvent event = new FileCleanEvent(java.util.UUID.randomUUID(), file.getId(), file.getObjectKey(), file.getOriginalFilename(), file.getContentType(), file.getSizeBytes(), file.getChecksumSha256(), java.time.Instant.now());

        OutboxEvent outboxEvent = new OutboxEvent("FILE", file.getId(), "FILE_CLEAN", eventSerializer.serialize(event));

        outboxEventRepository.save(outboxEvent);
    }
}