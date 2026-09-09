package com.rahul.event;

import java.time.Instant;
import java.util.UUID;

public record FileDeletedEvent(
        UUID eventId,
        UUID fileId,
        Instant occurredAt
) {
}