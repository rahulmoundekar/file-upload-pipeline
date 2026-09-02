package com.rahul.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MinioBucketInitializer {

    private final MinioClient minioClient;
    private final ObjectStorageProperties properties;

    @Bean
    ApplicationRunner initializeBucket() {

        return args -> {

            boolean exists =
                    minioClient.bucketExists(
                            BucketExistsArgs.builder()
                                    .bucket(properties.bucket())
                                    .build()
                    );

            if (!exists) {

                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(properties.bucket())
                                .build()
                );

                log.info(
                        "Created object storage bucket: {}",
                        properties.bucket()
                );
            }
        };
    }
}