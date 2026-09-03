package com.rahul.integration;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class KafkaDltIntegrationTest {

    private static final String SOURCE_TOPIC = "file.uploaded";

    private static final String DLT_TOPIC = "file.uploaded.DLT";

    private static final int PARTITIONS = 3;

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17").withDatabaseName("file_upload").withUsername("file_app").withPassword("root");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {


        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("spring.datasource.username", postgres::getUsername);

        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");

        registry.add("storage.endpoint", minio::getS3URL);

        registry.add("storage.access-key", minio::getUserName);

        registry.add("storage.secret-key", minio::getPassword);

        registry.add("storage.bucket", () -> "file-integrity-test");

        registry.add("storage.secure", () -> false);


        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);

        /*
         * This test needs the Kafka producer because the test
         * publishes an invalid event.
         */
        registry.add("kafka.producer.enabled", () -> true);

        /*
         * Enable VirusScanWorker so the source message is consumed.
         */
        registry.add("kafka.consumer.enabled", () -> true);

        /*
         * Use a unique consumer group so previous test offsets
         * cannot affect this test.
         */
        registry.add("kafka.consumer.virus-scan-group", () -> "virus-scan-dlt-test-" + UUID.randomUUID());

        /*
         * Outbox is not part of this test.
         */
        registry.add("outbox.publisher.enabled", () -> false);

    }

    @BeforeAll
    static void createTopics() throws Exception {

        Map<String, Object> properties = new HashMap<>();

        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(properties)) {

            adminClient.createTopics(List.of(new NewTopic(SOURCE_TOPIC, PARTITIONS, (short) 1), new NewTopic(DLT_TOPIC, PARTITIONS, (short) 1))).all().get();
        }
    }

    @Test
    void invalidEventShouldBeSentToDlt() throws Exception {

        String eventId = UUID.randomUUID().toString();

        /*
         * Deliberately invalid event:
         * eventId is null.
         */
        String invalidPayload = """
                {
                  "eventId": null,
                  "fileId": "%s",
                  "objectKey": "uploads/test.txt",
                  "originalFilename": "test.txt",
                  "contentType": "text/plain",
                  "sizeBytes": 10,
                  "checksumSha256": "%s",
                  "occurredAt": "2026-09-03T10:00:00Z"
                }
                """.formatted(UUID.randomUUID(), "a".repeat(64));

        kafkaTemplate.send(SOURCE_TOPIC, eventId, invalidPayload).get();

        ConsumerRecordHolder holder = new ConsumerRecordHolder();

        try (Consumer<String, String> consumer = createDltConsumer()) {

            consumer.subscribe(List.of(DLT_TOPIC));

            await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(250)).untilAsserted(() -> {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                records.forEach(holder::set);

                assertThat(holder.record()).isNotNull();
            });

            assertThat(holder.record().value()).isEqualTo(invalidPayload);

            assertThat(holder.record().key()).isEqualTo(eventId);

            assertThat(holder.record().topic()).isEqualTo(DLT_TOPIC);

            assertThat(holder.record().partition()).isBetween(0, PARTITIONS - 1);

            assertThat(holder.record().headers().toArray()).isNotEmpty();
        }
    }

    private Consumer<String, String> createDltConsumer() {

        Map<String, Object> properties = new HashMap<>();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID());

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<String, String>(properties).createConsumer();
    }

    private static final class ConsumerRecordHolder {

        private org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record;

        void set(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record) {

            this.record = record;
        }

        org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record() {

            return record;
        }
    }
}