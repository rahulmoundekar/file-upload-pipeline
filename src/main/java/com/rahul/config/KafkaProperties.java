package com.rahul.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
public record KafkaProperties(
        String bootstrapServers,
        Topics topics
) {

    public record Topics(
            String fileUploaded,
            String virusScan,
            String thumbnail,
            String processing,
            String webhook
    ) {
    }
}