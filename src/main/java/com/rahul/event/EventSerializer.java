package com.rahul.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.JsonNodeException;

@Component
@RequiredArgsConstructor
public class EventSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(
            Object event
    ) {

        try {

            return objectMapper.writeValueAsString(event);

        } catch (JsonNodeException e) {

            throw new IllegalStateException(
                    "Unable to serialize event",
                    e
            );
        }
    }
}