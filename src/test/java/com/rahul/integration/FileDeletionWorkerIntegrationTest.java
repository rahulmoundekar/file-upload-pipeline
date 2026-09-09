package com.rahul.integration;

import com.rahul.entity.DerivativeType;
import com.rahul.entity.FileDerivative;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.event.FileDeletedEvent;
import com.rahul.repository.FileDerivativeRepository;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.repository.ProcessedEventRepository;
import com.rahul.storage.ObjectStorage;
import com.rahul.worker.WorkerNames;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FileDeletionWorkerIntegrationTest {
    private static final String BUCKET = "file-deletion-test";
    private static final String FILE_DELETED_TOPIC = "file-deleted";
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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.producer.enabled", () -> true);
        registry.add("kafka.consumer.enabled", () -> true);
        registry.add("kafka.consumer.deletion-group", () -> "file-deletion-test-" + UUID.randomUUID());
        registry.add("kafka.topics.file-deleted", () -> FILE_DELETED_TOPIC);
        registry.add("storage.endpoint", minio::getS3URL);
        registry.add("storage.access-key", minio::getUserName);
        registry.add("storage.secret-key", minio::getPassword);
        registry.add("storage.bucket", () -> BUCKET);
        registry.add("storage.secure", () -> false);
        registry.add("storage.bucket-initializer.enabled", () -> true);
        registry.add("outbox.publisher.enabled", () -> false);
        registry.add("webhook.enabled", () -> false);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectStorage objectStorage;
    @Autowired
    private FileMetadataRepository fileMetadataRepository;
    @Autowired
    private FileDerivativeRepository fileDerivativeRepository;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ensureKafkaTopic();
    }

    @Test
    void deletionEventShouldDeleteObjectsAndMarkFileDeleted() throws Exception {
        UUID eventId = UUID.randomUUID();
        String originalObjectKey = "uploads/test-image/original.txt";
        String thumbnailObjectKey = "derivatives/test-image/thumbnail.jpg";
        byte[] originalContent = "delete-me".getBytes();
        byte[] thumbnailContent = "thumbnail".getBytes();
        storeObject(originalObjectKey, originalContent, "text/plain");
        storeObject(thumbnailObjectKey, thumbnailContent, "image/jpeg");
        FileMetadata file = new FileMetadata("original.txt", "original.txt", originalObjectKey, "text/plain", originalContent.length, "a".repeat(64), FileStatus.DELETING, ScanStatus.CLEAN,
                ThumbnailStatus.COMPLETED);
        file = fileMetadataRepository.saveAndFlush(file); /* * IMPORTANT: * FileMetadata uses UUID generation, so the real file id * must be read AFTER persistence. */
        UUID fileId = file.getId();
        FileDerivative derivative = new FileDerivative(fileId, DerivativeType.THUMBNAIL, thumbnailObjectKey, "image/jpeg", thumbnailContent.length, 300, 300);
        fileDerivativeRepository.saveAndFlush(derivative);
        FileDeletedEvent event = new FileDeletedEvent(eventId, fileId, Instant.now());
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(FILE_DELETED_TOPIC, fileId.toString(), payload).get();
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() -> {
            FileMetadata updated = fileMetadataRepository.findById(fileId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(FileStatus.DELETED);
            assertThat(fileDerivativeRepository.findByFileId(fileId)).isEmpty();
            assertThat(objectStorage.exists(originalObjectKey)).isFalse();
            assertThat(objectStorage.exists(thumbnailObjectKey)).isFalse();
        });
        assertThat(processedEventRepository.countByEventIdAndConsumerName(eventId, WorkerNames.FILE_DELETION)).isEqualTo(1);
    }

    @Test
    void duplicateDeletionEventShouldBeProcessedOnlyOnce() throws Exception {
        UUID eventId = UUID.randomUUID();
        String objectKey = "uploads/duplicate/duplicate.txt";
        byte[] content = "duplicate-event".getBytes();
        storeObject(objectKey, content, "text/plain");
        FileMetadata file = new FileMetadata("duplicate.txt", "duplicate.txt", objectKey, "text/plain", content.length, "b".repeat(64), FileStatus.DELETING, ScanStatus.CLEAN,
                ThumbnailStatus.NOT_REQUIRED);
        file = fileMetadataRepository.saveAndFlush(file); /* * Use the persisted entity id in the Kafka event. */
        UUID fileId = file.getId();
        FileDeletedEvent event = new FileDeletedEvent(eventId, fileId, Instant.now());
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(FILE_DELETED_TOPIC, fileId.toString(), payload).get();
        kafkaTemplate.send(FILE_DELETED_TOPIC, fileId.toString(), payload).get();
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() -> {
            FileMetadata updated = fileMetadataRepository.findById(fileId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(FileStatus.DELETED);
            assertThat(objectStorage.exists(objectKey)).isFalse();
        }); /* * Same eventId must only be recorded once for this worker. */
        assertThat(processedEventRepository.countByEventIdAndConsumerName(eventId, WorkerNames.FILE_DELETION)).isEqualTo(1);
    }

    private void storeObject(String objectKey, byte[] content, String contentType) throws Exception {
        objectStorage.put(objectKey, new ByteArrayInputStream(content), content.length, contentType);
    }

    private void ensureKafkaTopic() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(properties)) {
            boolean exists = adminClient.listTopics().names().get().contains(FILE_DELETED_TOPIC);
            if (!exists) {
                adminClient.createTopics(List.of(new NewTopic(FILE_DELETED_TOPIC, 3, (short) 1))).all().get();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Kafka test topic", exception);
        }
    }
}