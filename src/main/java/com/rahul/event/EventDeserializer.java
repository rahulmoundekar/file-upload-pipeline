package com.rahul.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rahul.exception.InvalidEventException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.JsonNodeException;

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

            throw new InvalidEventException("Invalid FileUploadedEvent payload", e);
        }
    }

    public FileCleanEvent deserializeFileClean(String payload) {

        try {

            return objectMapper.readValue(payload, FileCleanEvent.class);

        } catch (JacksonException e) {

            throw new InvalidEventException("Invalid FileCleanEvent payload", e);
        }
    }

    public FileCompletedEvent deserializeFileCompleted(String payload) {

        try {

            return objectMapper.readValue(payload, FileCompletedEvent.class);

        } catch (JacksonException e) {

            throw new InvalidEventException("Invalid FileCompletedEvent payload", e);
        }
    }

    public FileDeletedEvent deserializeFileDeleted(String payload) {

        try {
            return objectMapper.readValue(payload, FileDeletedEvent.class);
        } catch (JsonNodeException exception) {
            throw new IllegalArgumentException("Unable to deserialize FileDeletedEvent", exception);
        }
    }
}