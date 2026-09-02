package com.rahul.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FileContentDetectionService {

    private static final String RESOURCE_NAME = "resourceName";

    private final Tika tika = new Tika();

    public String detect(
            InputStream inputStream,
            String filename
    ) throws IOException {

        Metadata metadata = new Metadata();

        metadata.set(
                RESOURCE_NAME,
                filename
        );

        return tika.detect(
                inputStream,
                metadata
        );
    }
}