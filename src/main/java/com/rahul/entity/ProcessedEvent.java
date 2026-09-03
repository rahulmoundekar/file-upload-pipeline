package com.rahul.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events", uniqueConstraints = {@UniqueConstraint(name = "uk_processed_event_consumer", columnNames = {"event_id", "consumer_name"})}, indexes = {@Index(name = "idx_processed_events_event_id", columnList = "event_id")})
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(UUID eventId, String consumerName) {

        this.eventId = eventId;
        this.consumerName = consumerName;

        Instant now = Instant.now();

        this.processedAt = now;
        this.createdAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}