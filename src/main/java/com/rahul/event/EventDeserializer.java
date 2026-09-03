package com.rahul.event;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class EventDeserializer {

    private final ObjectMapper objectMapper;

    public EventDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FileUploadedEvent deserializeFileUploaded(String payload) {

        try {

            return objectMapper.readValue(payload, FileUploadedEvent.class);

        } catch (JacksonException e) {

            throw new IllegalArgumentException("Invalid FileUploadedEvent payload", e);
        }
    }
}