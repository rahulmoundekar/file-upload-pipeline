package com.rahul.integration;

import com.rahul.dto.FileUploadResponse;
import com.rahul.entity.OutboxEvent;
import com.rahul.entity.OutboxStatus;
import com.rahul.event.FileUploadedEvent;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.repository.OutboxEventRepository;
import com.rahul.service.FileUploadService;
import com.rahul.service.OutboxPublisher;
import com.rahul.storage.ObjectStorage;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OutboxKafkaIntegrationTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17").withDatabaseName("file_upload").withUsername("file_app").withPassword("root");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("spring.datasource.username", postgres::getUsername);

        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.flyway.url", postgres::getJdbcUrl);

        registry.add("spring.flyway.user", postgres::getUsername);

        registry.add("spring.flyway.password", postgres::getPassword);

        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("storage.endpoint", minio::getS3URL);

        registry.add("storage.access-key", minio::getUserName);

        registry.add("storage.secret-key", minio::getPassword);

        registry.add("storage.bucket", () -> "outbox-kafka-test");

        registry.add("storage.secure", () -> false);

        registry.add("outbox.publisher.enabled", () -> true);

        registry.add("outbox.publisher.scheduler-enabled", () -> false);
    }

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private ObjectStorage objectStorage;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {

        outboxEventRepository.deleteAll();
        fileMetadataRepository.deleteAll();
    }

    @Test
    void uploadShouldCreateFileAndOutboxEvent() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello outbox".getBytes());

        FileUploadResponse response = fileUploadService.upload(file);

        assertThat(response).isNotNull();

        assertThat(response.status()).isEqualTo("UPLOADED");

        assertThat(fileMetadataRepository.findById(response.id())).isPresent();

        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);

        assertThat(event.getAggregateId()).isEqualTo(response.id());

        assertThat(event.getEventType()).isEqualTo("FILE_UPLOADED");

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getPayload()).contains(response.id().toString());
    }

    @Test
    void outboxEventShouldBePublishedToKafka() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello kafka".getBytes());

        FileUploadResponse response = fileUploadService.upload(file);

        OutboxEvent event = outboxEventRepository.findAll().stream().filter(item -> item.getAggregateId().equals(response.id()) && item.getEventType().equals("FILE_UPLOADED")).findFirst().orElseThrow();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        outboxPublisher.publish(event);

        OutboxEvent published = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(published.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);

        assertThat(published.getPublishedAt()).isNotNull();

        assertThat(published.getAttempts()).isEqualTo(0);
    }

    @Test
    void publishedEventShouldContainCorrectFileMetadata() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "event payload".getBytes());

        FileUploadResponse response = fileUploadService.upload(file);

        OutboxEvent event = outboxEventRepository.findAll().stream().filter(item -> item.getAggregateId().equals(response.id())).findFirst().orElseThrow();

        FileUploadedEvent payload = objectMapper.readValue(event.getPayload(), FileUploadedEvent.class);

        assertThat(payload.eventId()).isNotNull();

        assertThat(payload.fileId()).isEqualTo(response.id());

        assertThat(payload.objectKey()).isNotBlank();

        assertThat(payload.originalFilename()).isEqualTo("hello.txt");

        assertThat(payload.contentType()).isEqualTo("text/plain");

        assertThat(payload.sizeBytes()).isEqualTo(response.sizeBytes());

        assertThat(payload.checksumSha256()).isEqualTo(response.checksumSha256());
    }

    @Test
    void outboxEventShouldReachKafka() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file", "kafka.txt", "text/plain", "hello kafka".getBytes());

        FileUploadResponse response = fileUploadService.upload(file);

        OutboxEvent event = outboxEventRepository.findAll().stream().filter(item -> item.getAggregateId().equals(response.id())).findFirst().orElseThrow();

        outboxPublisher.publish(event);

        Map<String, Object> properties = new HashMap<>();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-test-" + UUID.randomUUID());

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumer = new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of("file.uploaded"));

            ConsumerRecord<String, String> matchingRecord = null;

            long deadline = System.currentTimeMillis() + 15_000;

            while (matchingRecord == null && System.currentTimeMillis() < deadline) {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {

                    if (response.id().toString().equals(record.key())) {

                        matchingRecord = record;

                        break;
                    }
                }
            }

            assertThat(matchingRecord).isNotNull();

            assertThat(matchingRecord.key()).isEqualTo(response.id().toString());

            FileUploadedEvent eventPayload = objectMapper.readValue(matchingRecord.value(), FileUploadedEvent.class);

            assertThat(eventPayload.fileId()).isEqualTo(response.id());

            assertThat(eventPayload.originalFilename()).isEqualTo("kafka.txt");

            assertThat(eventPayload.checksumSha256()).isEqualTo(response.checksumSha256());
        }
    }
}