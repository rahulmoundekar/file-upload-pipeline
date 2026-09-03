package com.rahul.storage;

import com.rahul.config.ObjectStorageProperties;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient minioClient;
    private final ObjectStorageProperties properties;

    @Override
    public void put(String objectKey, MultipartFile file) throws IOException {

        try (InputStream inputStream = file.getInputStream()) {

            put(objectKey, inputStream, file.getSize(), file.getContentType());
        }
    }

    @Override
    public void put(String objectKey, InputStream inputStream, long size, String contentType) throws IOException {

        try {

            PutObjectArgs.Builder builder = PutObjectArgs.builder().bucket(properties.bucket()).object(objectKey).stream(inputStream, size, -1L);

            if (contentType != null && !contentType.isBlank()) {

                builder.contentType(contentType);
            }

            minioClient.putObject(builder.build());

        } catch (Exception e) {

            throw new IOException("Failed to store object", e);
        }
    }

    @Override
    public InputStream get(String objectKey) {

        try {

            return minioClient.getObject(GetObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());

        } catch (Exception e) {

            throw new IllegalStateException("Failed to retrieve object", e);
        }
    }

    @Override
    public void delete(String objectKey) {

        try {

            minioClient.removeObject(RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());

        } catch (Exception e) {

            throw new IllegalStateException("Failed to delete object", e);
        }
    }

    @Override
    public boolean exists(String objectKey) {

        try {

            minioClient.statObject(StatObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getObjectUrl(String objectKey) {

        return properties.endpoint() + "/" + properties.bucket() + "/" + objectKey;
    }

    @Override
    public InputStream getObject(String objectKey) {
        try {

            return minioClient.getObject(GetObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());

        } catch (Exception e) {

            throw new IllegalStateException("Failed to retrieve object", e);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {

            minioClient.removeObject(RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());

        } catch (Exception e) {

            throw new IllegalStateException("Failed to delete object", e);
        }
    }
}