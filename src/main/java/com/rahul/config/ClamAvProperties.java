package com.rahul.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clamav")
public record ClamAvProperties(
        String host,
        int port,
        int connectionTimeoutMs,
        int readTimeoutMs,
        int chunkSizeBytes
) {
}