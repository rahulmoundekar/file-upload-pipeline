package com.rahul.integration;

import com.rahul.dto.FileIntegrityResponse;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.service.FileChecksumService;
import com.rahul.service.FileIntegrityService;
import com.rahul.storage.ObjectStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FileIntegrityIntegrationTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17").withDatabaseName("file_upload").withUsername("file_app").withPassword("root");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("spring.datasource.username", postgres::getUsername);

        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");

        registry.add("storage.endpoint", minio::getS3URL);

        registry.add("storage.access-key", minio::getUserName);

        registry.add("storage.secret-key", minio::getPassword);

        registry.add("storage.bucket", () -> "file-integrity-test");

        registry.add("storage.secure", () -> false);
    }

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileIntegrityService fileIntegrityService;

    @Autowired
    private FileChecksumService checksumService;

    @Autowired
    private ObjectStorage objectStorage;

    @BeforeEach
    void cleanDatabase() {
        fileMetadataRepository.deleteAll();
    }

    // tests...
}