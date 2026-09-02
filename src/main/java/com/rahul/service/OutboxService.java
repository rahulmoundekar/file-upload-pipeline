package com.rahul.service;

import com.rahul.entity.OutboxEvent;
import com.rahul.event.EventSerializer;
import com.rahul.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;
    private final EventSerializer serializer;

    @Transactional
    public OutboxEvent create(String aggregateType, UUID aggregateId, String eventType, Object payload) {

        String json = serializer.serialize(payload);

        OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, json);

        return repository.save(event);
    }
}