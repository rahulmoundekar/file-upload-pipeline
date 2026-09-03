package com.rahul.worker;

import com.rahul.config.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class ThumbnailDltConsumer {

    @KafkaListener(topics = "${kafka.topics.file-clean}.DLT", groupId = "${kafka.consumer.thumbnail-group}-dlt")
    public void handle(ConsumerRecord<String, String> record) {

        log.error("Thumbnail event moved to DLT. " + "topic={}, partition={}, offset={}, key={}, payload={}", record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    @Bean
    public NewTopic fileCleanDltTopic(KafkaProperties kafkaProperties) {

        return TopicBuilder.name(kafkaProperties.topics().fileClean() + ".DLT").partitions(3).replicas(1).build();
    }
}