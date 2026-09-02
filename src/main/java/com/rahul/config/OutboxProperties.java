package com.rahul.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.publisher")
public record OutboxProperties(
        int maxAttempts,
        long initialBackoffMs,
        long maxBackoffMs
) {
}