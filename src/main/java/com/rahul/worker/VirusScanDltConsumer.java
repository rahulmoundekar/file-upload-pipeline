package com.rahul.worker;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "kafka.consumer.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class VirusScanDltConsumer {

    @KafkaListener(
            topics = "${kafka.topics.file-uploaded}.DLT",
            groupId = "${kafka.consumer.virus-scan-group}-dlt"
    )
    public void handle(
            ConsumerRecord<String, String> record
    ) {

        log.error(
                "Virus scan event moved to DLT. "
                        + "topic={}, partition={}, offset={}, key={}, payload={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value()
        );
    }
}