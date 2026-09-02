package com.rahul.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {@Index(name = "idx_outbox_status_created", columnList = "status,created_at")})
@Getter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }


    public void markFailed(String error) {

        this.attempts++;
        this.lastError = error;
        this.nextAttemptAt = null;
        this.status = OutboxStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void incrementAttempt() {
        this.attempts++;
        this.updatedAt = Instant.now();
    }

    public void markPublished() {

        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.nextAttemptAt = null;
        this.lastError = null;
        this.updatedAt = Instant.now();
    }

    public void scheduleRetry(String error, Instant nextAttemptAt) {

        this.attempts++;
        this.lastError = error;
        this.nextAttemptAt = nextAttemptAt;
        this.status = OutboxStatus.PENDING;
        this.updatedAt = Instant.now();
    }
}