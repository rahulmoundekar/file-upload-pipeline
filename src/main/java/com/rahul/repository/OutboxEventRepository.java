package com.rahul.repository;

import com.rahul.entity.OutboxEvent;
import com.rahul.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
