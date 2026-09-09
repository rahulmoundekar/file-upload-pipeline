package com.rahul.repository;

import com.rahul.entity.OutboxEvent;
import com.rahul.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status = :status
              AND (
                    e.nextAttemptAt IS NULL
                    OR e.nextAttemptAt <= :now
                  )
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findReadyForPublishing(
            OutboxStatus status,
            Instant now
    );

    @Query(value = "SELECT * FROM outbox_events WHERE status = :status AND (next_attempt_at IS NULL OR next_attempt_at <= :now) ORDER BY created_at ASC LIMIT 100 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findReadyForPublishingForUpdate(@Param("status") String status, @Param("now") Instant now);

    boolean existsByAggregateIdAndEventType(
            UUID aggregateId,
            String eventType
    );
}
