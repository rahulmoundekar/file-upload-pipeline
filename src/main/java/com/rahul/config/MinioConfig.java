package com.rahul.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final ObjectStorageProperties properties;

    @Bean
    public MinioClient minioClient() {

        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(
                        properties.accessKey(),
                        properties.secretKey()
                )
                .build();
    }
}