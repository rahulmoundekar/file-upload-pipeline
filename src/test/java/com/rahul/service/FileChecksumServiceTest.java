package com.rahul.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FileChecksumServiceTest {

    private final FileChecksumService service = new FileChecksumService();

    @Test
    void shouldCalculateKnownSha256Checksum() {

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());

        String checksum = service.sha256(file);

        assertThat(checksum).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e" + "1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void shouldCalculateSameChecksumFromInputStream() {

        byte[] content = "hello".getBytes();

        String checksum = service.sha256(new ByteArrayInputStream(content));

        assertThat(checksum).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e" + "1b161e5c1fa7425e73043362938b9824");
    }
}