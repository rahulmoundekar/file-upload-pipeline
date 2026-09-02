package com.rahul.service;

import com.rahul.config.OutboxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OutboxRetryPolicy {

    private final OutboxProperties properties;

    public boolean shouldRetry(int attempts) {

        return attempts < properties.maxAttempts();
    }

    public Instant nextAttemptAt(int attempts) {

        long multiplier = 1L << Math.max(0, attempts - 1);

        long delay = Math.min(properties.initialBackoffMs() * multiplier, properties.maxBackoffMs());

        return Instant.now().plus(Duration.ofMillis(delay));
    }
}