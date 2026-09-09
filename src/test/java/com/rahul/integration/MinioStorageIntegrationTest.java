package com.rahul.integration;

import com.rahul.FileUploadPipelineApplication;
import com.rahul.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = FileUploadPipelineApplication.class)
@ActiveProfiles("test")
class MinioStorageIntegrationTest {

    @Container
    static final MinIOContainer minio =
            new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void registerMinioProperties(DynamicPropertyRegistry registry) {

        registry.add(
                "storage.endpoint",
                minio::getS3URL
        );

        registry.add(
                "storage.access-key",
                minio::getUserName
        );

        registry.add(
                "storage.secret-key",
                minio::getPassword
        );

        registry.add(
                "storage.bucket",
                () -> "file-uploads-test"
        );

        registry.add(
                "storage.bucket-initializer.enabled",
                () -> true
        );
    }

    @Autowired
    private ObjectStorage objectStorage;

    @Test
    void objectShouldBeStoredAndReadBack() throws Exception {

        String objectKey = "test/hello.txt";
        byte[] expectedContent =
                "hello-minio".getBytes(StandardCharsets.UTF_8);

        try (InputStream inputStream =
                     new java.io.ByteArrayInputStream(expectedContent)) {

            objectStorage.put(
                    objectKey,
                    inputStream,
                    expectedContent.length,
                    "text/plain"
            );
        }

        assertThat(objectStorage.exists(objectKey))
                .isTrue();

        try (InputStream inputStream =
                     objectStorage.get(objectKey)) {

            byte[] actualContent = inputStream.readAllBytes();

            assertThat(actualContent)
                    .isEqualTo(expectedContent);
        } finally {
            objectStorage.delete(objectKey);
        }
    }
}