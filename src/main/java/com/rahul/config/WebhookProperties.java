package com.rahul.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webhook")
public record WebhookProperties(
        boolean enabled,
        String url,
        String secret,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}