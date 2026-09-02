package com.rahul.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface ObjectStorage {

    void put(String objectKey, MultipartFile file) throws IOException;

    void put(String objectKey, InputStream inputStream, long size, String contentType) throws IOException;

    InputStream get(String objectKey);

    void delete(String objectKey);

    boolean exists(String objectKey);

    String getObjectUrl(String objectKey);
}