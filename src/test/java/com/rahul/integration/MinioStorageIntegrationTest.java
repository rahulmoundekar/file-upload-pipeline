package com.rahul.integration;

import com.rahul.FileUploadPipelineApplication;
import com.rahul.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FileUploadPipelineApplication.class)
@ActiveProfiles("test")
class MinioStorageIntegrationTest {

    @Autowired
    private ObjectStorage objectStorage;

    @Test
    void objectShouldBeStoredAndReadBack() throws Exception {

        String objectKey = "test/hello.txt";

        byte[] content =
                "hello file upload pipeline"
                        .getBytes(StandardCharsets.UTF_8);

        objectStorage.put(
                objectKey,
                new java.io.ByteArrayInputStream(content),
                content.length,
                "text/plain"
        );

        assertThat(
                objectStorage.exists(objectKey)
        ).isTrue();

        try (InputStream inputStream =
                     objectStorage.get(objectKey)) {

            String actual =
                    new String(
                            inputStream.readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            assertThat(actual)
                    .isEqualTo(
                            "hello file upload pipeline"
                    );
        }

        objectStorage.delete(objectKey);

        assertThat(
                objectStorage.exists(objectKey)
        ).isFalse();
    }
}