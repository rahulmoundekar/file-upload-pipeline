package com.rahul.service;

import com.rahul.entity.ProcessedEvent;
import com.rahul.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventInboxService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean alreadyProcessed(UUID eventId, String consumerName) {

        return processedEventRepository.existsByEventIdAndConsumerName(eventId, consumerName);
    }

    @Transactional
    public boolean markProcessed(UUID eventId, String consumerName) {

        if (processedEventRepository.existsByEventIdAndConsumerName(eventId, consumerName)) {

            return false;
        }

        try {

            processedEventRepository.saveAndFlush(new ProcessedEvent(eventId, consumerName));

            return true;

        } catch (DataIntegrityViolationException e) {

            /*
             * Another worker instance may have inserted
             * the same (event_id, consumer_name) concurrently.
             *
             * The unique constraint makes the operation safe.
             */
            return false;
        }
    }
}