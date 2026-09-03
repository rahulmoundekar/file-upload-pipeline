package com.rahul.integration;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.event.FileUploadedEvent;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.repository.ProcessedEventRepository;
import com.rahul.storage.ObjectStorage;
import com.rahul.worker.VirusScanWorker;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class VirusScanWorkerIntegrationTest {

    private static final String FILE_UPLOADED_TOPIC = "file.uploaded";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17").withDatabaseName("file_upload").withUsername("file_app").withPassword("root");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Container
    static GenericContainer<?> clamav = new GenericContainer<>("clamav/clamav:stable").withExposedPorts(3310);

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        // ---------------------------------------------------------
        // PostgreSQL
        // ---------------------------------------------------------

        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("spring.datasource.username", postgres::getUsername);

        registry.add("spring.datasource.password", postgres::getPassword);

        // ---------------------------------------------------------
        // Flyway / JPA
        // ---------------------------------------------------------

        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        registry.add("spring.flyway.url", postgres::getJdbcUrl);

        registry.add("spring.flyway.user", postgres::getUsername);

        registry.add("spring.flyway.password", postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // ---------------------------------------------------------
        // Kafka
        // ---------------------------------------------------------

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.producer.enabled", () -> true);

        registry.add("kafka.consumer.enabled", () -> true);

        registry.add("kafka.consumer.virus-scan-group", () -> "virus-scan-worker-" + UUID.randomUUID());

        // The scheduler is not needed for this test.
        registry.add("outbox.publisher.enabled", () -> false);

        // ---------------------------------------------------------
        // MinIO
        // ---------------------------------------------------------

        registry.add("storage.endpoint", minio::getS3URL);

        registry.add("storage.access-key", minio::getUserName);

        registry.add("storage.secret-key", minio::getPassword);

        registry.add("storage.bucket", () -> "virus-scan-test");

        registry.add("storage.secure", () -> false);

        // ---------------------------------------------------------
        // ClamAV
        // ---------------------------------------------------------

        registry.add("clamav.host", clamav::getHost);

        registry.add("clamav.port", () -> clamav.getMappedPort(3310));

        registry.add("clamav.connection-timeout-ms", () -> 5000);

        registry.add("clamav.read-timeout-ms", () -> 60000);

        registry.add("clamav.chunk-size-bytes", () -> 8192);
    }

    @Autowired
    private VirusScanWorker virusScanWorker;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private ObjectStorage objectStorage;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {

        fileMetadataRepository.deleteAll();

        ensureKafkaTopic();
    }

    // ============================================================
    // Test 1 - Direct worker invocation: clean file
    // ============================================================

    @Test
    void cleanFileShouldBeMarkedClean() throws Exception {
        String objectKey = "uploads/direct-clean.txt";

        byte[] content = "hello clean file".getBytes();

        String checksum = sha256(content);

        storeObject(objectKey, content);

        FileMetadata file = saveFile(objectKey, "direct-clean.txt", "text/plain", content, checksum);

        UUID fileId = file.getId();

        FileUploadedEvent event = createEvent(fileId, objectKey, "direct-clean.txt", "text/plain", content.length, checksum);

        String payload = objectMapper.writeValueAsString(event);

        virusScanWorker.handle(payload);

        FileMetadata updated = findFile(fileId);

        assertThat(updated.getStatus()).isEqualTo(FileStatus.CLEAN);

        assertThat(updated.getScanStatus()).isEqualTo(ScanStatus.CLEAN);

        assertThat(updated.getScanSignature()).isNull();
    }

    // ============================================================
    // Test 2 - Direct worker invocation: EICAR
    // ============================================================

    @Test
    void eicarFileShouldBeRejected() throws Exception {
        String objectKey = "uploads/direct-eicar.txt";

        byte[] content = eicarBytes();

        String checksum = sha256(content);

        storeObject(objectKey, content);

        FileMetadata file = saveFile(objectKey, "direct-eicar.txt", "text/plain", content, checksum);

        UUID fileId = file.getId();

        FileUploadedEvent event = createEvent(fileId, objectKey, "direct-eicar.txt", "text/plain", content.length, checksum);

        String payload = objectMapper.writeValueAsString(event);

        virusScanWorker.handle(payload);

        FileMetadata updated = findFile(fileId);

        assertThat(updated.getStatus()).isEqualTo(FileStatus.REJECTED);

        assertThat(updated.getScanStatus()).isEqualTo(ScanStatus.INFECTED);

        assertThat(updated.getScanSignature()).contains("Eicar");
    }

    // ============================================================
    // Test 3 - Real Kafka listener: clean file
    // ============================================================

    @Test
    void kafkaCleanEventShouldProcessFile() throws Exception {

        String objectKey = "uploads/kafka-clean.txt";

        byte[] content = "hello from kafka worker".getBytes();

        String checksum = sha256(content);

        storeObject(objectKey, content);

        FileMetadata file = saveFile(objectKey, "kafka-clean.txt", "text/plain", content, checksum);

        UUID fileId = file.getId();

        FileUploadedEvent event = createEvent(fileId, objectKey, "kafka-clean.txt", "text/plain", content.length, checksum);

        String payload = objectMapper.writeValueAsString(event);

        kafkaTemplate.send(FILE_UPLOADED_TOPIC, fileId.toString(), payload).get();

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() -> {

            FileMetadata updated = findFile(fileId);

            assertThat(updated.getStatus()).isEqualTo(FileStatus.CLEAN);

            assertThat(updated.getScanStatus()).isEqualTo(ScanStatus.CLEAN);

            assertThat(updated.getScanSignature()).isNull();
        });
    }

    // ============================================================
    // Test 4 - Real Kafka listener: EICAR
    // ============================================================

    @Test
    void kafkaEicarEventShouldRejectFile() throws Exception {

        String objectKey = "uploads/kafka-eicar.txt";

        byte[] content = eicarBytes();

        String checksum = sha256(content);

        storeObject(objectKey, content);

        FileMetadata file = saveFile(objectKey, "kafka-eicar.txt", "text/plain", content, checksum);


        UUID fileId = file.getId();

        FileUploadedEvent event = createEvent(fileId, objectKey, "kafka-eicar.txt", "text/plain", content.length, checksum);

        String payload = objectMapper.writeValueAsString(event);

        kafkaTemplate.send(FILE_UPLOADED_TOPIC, fileId.toString(), payload).get();

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() -> {

            FileMetadata updated = findFile(fileId);

            assertThat(updated.getStatus()).isEqualTo(FileStatus.REJECTED);

            assertThat(updated.getScanStatus()).isEqualTo(ScanStatus.INFECTED);

            assertThat(updated.getScanSignature()).contains("Eicar");
        });
    }

    @Test
    void duplicateKafkaEventShouldBeProcessedOnlyOnce() throws Exception {

        UUID fileId = UUID.randomUUID();

        UUID eventId = UUID.randomUUID();

        String objectKey = "uploads/duplicate-test.txt";

        byte[] content = "duplicate event test".getBytes();

        String checksum = sha256(content);

        storeObject(objectKey, content);

        FileMetadata file = saveFile(objectKey, "duplicate-test.txt", "text/plain", content, checksum);

        fileId = file.getId();

        FileUploadedEvent event = new FileUploadedEvent(eventId, fileId, objectKey, "duplicate-test.txt", "text/plain", content.length, checksum, Instant.now());

        String payload = objectMapper.writeValueAsString(event);

        kafkaTemplate.send(FILE_UPLOADED_TOPIC, fileId.toString(), payload).get();

        kafkaTemplate.send(FILE_UPLOADED_TOPIC, fileId.toString(), payload).get();

        UUID finalFileId = fileId;

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {

            FileMetadata updated = findFile(finalFileId);

            assertThat(updated.getStatus()).isEqualTo(FileStatus.CLEAN);

            assertThat(updated.getScanStatus()).isEqualTo(ScanStatus.CLEAN);
        });

        assertThat(processedEventRepository.findAll().stream().filter(item -> item.getEventId().equals(eventId) && item.getConsumerName().equals(WorkerNames.VIRUS_SCAN)).count()).isEqualTo(1);

        assertThat(processedEventRepository.countByEventIdAndConsumerName(eventId, WorkerNames.VIRUS_SCAN)).isEqualTo(1);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private FileUploadedEvent createEvent(UUID fileId, String objectKey, String filename, String contentType, long size, String checksum) {

        return new FileUploadedEvent(UUID.randomUUID(), fileId, objectKey, filename, contentType, size, checksum, Instant.now());
    }

    private void storeObject(String objectKey, byte[] content) throws IOException {

        objectStorage.put(objectKey, new ByteArrayInputStream(content), content.length, "text/plain");
    }

    private FileMetadata saveFile(String objectKey, String filename, String contentType, byte[] content, String checksum) {

        FileMetadata file = new FileMetadata(filename, filename, objectKey, contentType, content.length, checksum, FileStatus.UPLOADED, ScanStatus.PENDING, ThumbnailStatus.NOT_REQUIRED);

        return fileMetadataRepository.saveAndFlush(file);
    }

    private FileMetadata findFile(UUID fileId) {

        return fileMetadataRepository.findById(fileId).orElseThrow();
    }

    private byte[] eicarBytes() {

        String eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$" + "EICAR-STANDARD-ANTIVIRUS-TEST-FILE!" + "$H+H*";

        return eicar.getBytes();
    }

    private String sha256(byte[] content) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hash = digest.digest(content);

        StringBuilder hex = new StringBuilder(hash.length * 2);

        for (byte b : hash) {

            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }

    private void ensureKafkaTopic() {

        Map<String, Object> properties = new HashMap<>();

        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(properties)) {

            boolean exists = adminClient.listTopics().names().get().contains(FILE_UPLOADED_TOPIC);

            if (!exists) {

                adminClient.createTopics(java.util.List.of(new NewTopic(FILE_UPLOADED_TOPIC, 3, (short) 1))).all().get();
            }

        } catch (Exception e) {

            throw new IllegalStateException("Unable to create Kafka test topic", e);
        }
    }
}