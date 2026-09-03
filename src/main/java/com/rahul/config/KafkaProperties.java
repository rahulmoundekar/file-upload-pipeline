package com.rahul.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
public record KafkaProperties(String bootstrapServers, Topics topics, Consumer consumer) {

    public record Topics(String fileUploaded, String fileClean, String virusScan, String thumbnail, String processing,
                         String webhook) {
    }

    public record Consumer(String virusScanGroup, String thumbnailGroup) {
    }
}