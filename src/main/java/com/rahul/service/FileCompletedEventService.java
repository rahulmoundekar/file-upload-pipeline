package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.OutboxEvent;
import com.rahul.event.EventSerializer;
import com.rahul.event.EventTypes;
import com.rahul.event.FileCompletedEvent;
import com.rahul.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileCompletedEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public OutboxEvent createEvent(FileMetadata file) {

        if (outboxEventRepository.existsByAggregateIdAndEventType(file.getId(), EventTypes.FILE_COMPLETED)) {
            return null;
        }

        FileCompletedEvent event = new FileCompletedEvent(UUID.randomUUID(), file.getId(), file.getStatus().name(), file.getObjectKey(), file.getOriginalFilename(), file.getContentType(), file.getSizeBytes(), file.getChecksumSha256(), Instant.now());

        OutboxEvent outboxEvent = new OutboxEvent("FILE", file.getId(), EventTypes.FILE_COMPLETED, eventSerializer.serialize(event));

        return outboxEventRepository.save(outboxEvent);
    }


}