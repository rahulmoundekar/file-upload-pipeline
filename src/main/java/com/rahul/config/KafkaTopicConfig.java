package com.rahul.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic fileUploadedTopic(KafkaProperties properties) {
        return TopicBuilder.name(properties.topics().fileUploaded()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic virusScanTopic(KafkaProperties properties) {
        return TopicBuilder.name(properties.topics().virusScan()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic thumbnailTopic(KafkaProperties properties) {
        return TopicBuilder.name(properties.topics().thumbnail()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic processingTopic(KafkaProperties properties) {
        return TopicBuilder.name(properties.topics().processing()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic webhookTopic(KafkaProperties properties) {
        return TopicBuilder.name(properties.topics().webhook()).partitions(3).replicas(1).build();
    }
}