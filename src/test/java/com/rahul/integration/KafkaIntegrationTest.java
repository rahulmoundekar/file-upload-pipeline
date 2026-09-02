package com.rahul.integration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("spring.kafka.admin.properties.bootstrap.servers", kafka::getBootstrapServers);

        registry.add("outbox.publisher.enabled", () -> false);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void kafkaContainerShouldBeAvailable() {

        assertThat(kafka.isRunning()).isTrue();

        assertThat(kafka.getBootstrapServers()).isNotBlank();
    }

    @Test
    void shouldPublishAndConsumeKafkaMessage() throws Exception {

        String topic = "file.uploaded.test";

        String key = UUID.randomUUID().toString();

        String payload = "FILE_UPLOADED:" + key;

        kafkaTemplate.send(topic, key, payload).get();

        Map<String, Object> consumerProperties = new HashMap<>();

        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-integration-test-" + UUID.randomUUID());

        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {

            consumer.subscribe(List.of(topic));

            ConsumerRecord<String, String> matchingRecord = null;

            long deadline = System.currentTimeMillis() + 15_000;

            while (matchingRecord == null && System.currentTimeMillis() < deadline) {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {

                    if (key.equals(record.key()) && payload.equals(record.value())) {

                        matchingRecord = record;
                        break;
                    }
                }
            }

            assertThat(matchingRecord).isNotNull();

            assertThat(matchingRecord.key()).isEqualTo(key);

            assertThat(matchingRecord.value()).isEqualTo(payload);
        }
    }
}