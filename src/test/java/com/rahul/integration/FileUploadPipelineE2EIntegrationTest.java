package com.rahul.integration;

import com.rahul.entity.DerivativeType;
import com.rahul.entity.FileDerivative;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.repository.FileDerivativeRepository;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.repository.OutboxEventRepository;
import com.rahul.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class FileUploadPipelineE2EIntegrationTest {

    private static final String BUCKET = "file-upload-e2e";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17").withDatabaseName("file_upload").withUsername("file_app").withPassword("root");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Container
    static GenericContainer<?> clamav = new GenericContainer<>("clamav/clamav:stable").withExposedPorts(3310);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("spring.datasource.username", postgres::getUsername);

        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.producer.enabled", () -> true);

        registry.add("kafka.consumer.enabled", () -> true);

        registry.add("kafka.consumer.virus-scan-group", () -> "e2e-virus-scan-" + UUID.randomUUID());

        registry.add("kafka.consumer.thumbnail-group", () -> "e2e-thumbnail-" + UUID.randomUUID());

        // Enable outbox publisher
        registry.add("outbox.publisher.enabled", () -> true);

        registry.add("outbox.publisher.scheduler-enabled", () -> true);

        registry.add("outbox.publisher.delay-ms", () -> 250);

        // MinIO
        registry.add("storage.endpoint", minio::getS3URL);

        registry.add("storage.access-key", minio::getUserName);

        registry.add("storage.secret-key", minio::getPassword);

        registry.add("storage.bucket", () -> BUCKET);

        registry.add("storage.secure", () -> false);

        // ClamAV
        registry.add("clamav.host", clamav::getHost);

        registry.add("clamav.port", () -> clamav.getMappedPort(3310));

        registry.add("clamav.connection-timeout-ms", () -> 5000);

        registry.add("clamav.read-timeout-ms", () -> 60000);

        registry.add("clamav.chunk-size-bytes", () -> 8192);

        // Thumbnail
        registry.add("thumbnail.width", () -> 300);

        registry.add("thumbnail.height", () -> 300);

        registry.add("thumbnail.format", () -> "jpg");

        registry.add("thumbnail.quality", () -> 0.85);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileDerivativeRepository fileDerivativeRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectStorage objectStorage;

    @Autowired
    private ObjectMapper objectMapper;

    // ============================================================
    // TEST 1
    // Image upload → virus scan → thumbnail → COMPLETED
    // ============================================================

    @Test
    void imageUploadShouldReachCompletedWithThumbnail() throws Exception {

        byte[] image = createImage(800, 600);

        String filename = "e2e-image.png";

        String contentType = "image/png";

        UUID fileId = uploadFile(filename, contentType, image);

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {

            FileMetadata file = findFile(fileId);

            assertThat(file.getStatus()).isEqualTo(FileStatus.COMPLETED);

            assertThat(file.getScanStatus()).isEqualTo(ScanStatus.CLEAN);
        });

        FileDerivative derivative = fileDerivativeRepository.findByFileIdAndDerivativeType(fileId, DerivativeType.THUMBNAIL).orElseThrow();

        assertThat(derivative.getContentType()).isEqualTo("image/jpeg");

        assertThat(derivative.getWidth()).isLessThanOrEqualTo(300);

        assertThat(derivative.getHeight()).isLessThanOrEqualTo(300);

        try (InputStream inputStream = objectStorage.getObject(derivative.getObjectKey())) {

            BufferedImage thumbnail = ImageIO.read(inputStream);

            assertThat(thumbnail).isNotNull();

            assertThat(thumbnail.getWidth()).isLessThanOrEqualTo(300);

            assertThat(thumbnail.getHeight()).isLessThanOrEqualTo(300);
        }

        FileMetadata file = findFile(fileId);

        assertThat(file.getStatus()).isEqualTo(FileStatus.COMPLETED);
    }

    // ============================================================
    // TEST 2
    // Non-image upload → virus scan → COMPLETED
    // No thumbnail
    // ============================================================

    @Test
    void nonImageUploadShouldCompleteWithoutThumbnail() throws Exception {

        byte[] content = "hello end-to-end pipeline".getBytes();

        UUID fileId = uploadFile("e2e-test.txt", "text/plain", content);

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {

            FileMetadata file = findFile(fileId);

            assertThat(file.getStatus()).isEqualTo(FileStatus.COMPLETED);

            assertThat(file.getScanStatus()).isEqualTo(ScanStatus.CLEAN);
        });

        Optional<FileDerivative> derivative = fileDerivativeRepository.findByFileIdAndDerivativeType(fileId, DerivativeType.THUMBNAIL);

        assertThat(derivative).isEmpty();
    }

    // ============================================================
    // TEST 3
    // EICAR → INFECTED → REJECTED
    // ============================================================

    @Test
    void infectedUploadShouldBeRejected() throws Exception {

        String eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$" + "EICAR-STANDARD-ANTIVIRUS-TEST-FILE!" + "$H+H*";

        UUID fileId = uploadFile("e2e-eicar.txt", "text/plain", eicar.getBytes());

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {

            FileMetadata file = findFile(fileId);

            assertThat(file.getScanStatus()).isEqualTo(ScanStatus.INFECTED);

            assertThat(file.getStatus()).isEqualTo(FileStatus.REJECTED);

            assertThat(file.getScanSignature()).contains("Eicar");
        });

        Optional<FileDerivative> derivative = fileDerivativeRepository.findByFileIdAndDerivativeType(fileId, DerivativeType.THUMBNAIL);

        assertThat(derivative).isEmpty();
    }

    // ============================================================
    // TEST 4
    // Verify outbox was created for successful image
    // ============================================================

    @Test
    void successfulImageUploadShouldCreateOutboxEvent() throws Exception {

        byte[] image = createImage(400, 300);

        UUID fileId = uploadFile("outbox-e2e.png", "image/png", image);

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {

            FileMetadata file = findFile(fileId);

            assertThat(file.getStatus()).isEqualTo(FileStatus.COMPLETED);
        });

        long eventCount = outboxEventRepository.findAll().stream().filter(event -> fileId.equals(event.getAggregateId())).count();

        assertThat(eventCount).isGreaterThanOrEqualTo(2);
    }

    // ============================================================
    // HTTP upload helper
    // ============================================================

    private UUID uploadFile(String filename, String contentType, byte[] content) throws Exception {

        ByteArrayResource resource = new ByteArrayResource(content) {

            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", resource);

        ResponseEntity<String> response = restClient().post().uri("/api/files").contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(response.getBody()).isNotBlank();

        JsonNode json = objectMapper.readTree(response.getBody());

        return UUID.fromString(json.get("id").asText());
    }

    // ============================================================
    // Database helper
    // ============================================================

    private FileMetadata findFile(UUID fileId) {

        return fileMetadataRepository.findById(fileId).orElseThrow();
    }

    // ============================================================
    // Image generator
    // ============================================================

    private byte[] createImage(int width, int height) throws Exception {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();

        try {

            graphics.setColor(Color.WHITE);

            graphics.fillRect(0, 0, width, height);

            graphics.setColor(Color.BLACK);

            graphics.fillRect(50, 50, Math.min(200, width - 100), Math.min(100, height - 100));

        } finally {

            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        boolean written = ImageIO.write(image, "png", output);

        assertThat(written).isTrue();

        return output.toByteArray();
    }

    private RestClient restClient() {

        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }
}