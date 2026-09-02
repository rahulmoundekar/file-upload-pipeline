package com.rahul.service;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FilenameSecurityService {

    private static final int MAX_FILENAME_LENGTH = 255;

    public String validateAndSanitize(String originalFilename) {

        if (originalFilename == null || originalFilename.isBlank()) {

            throw new IllegalArgumentException("Filename is required");
        }

        String filename = originalFilename.trim();

        if (filename.length() > MAX_FILENAME_LENGTH) {

            throw new IllegalArgumentException("Filename exceeds maximum length");
        }

        /*
         * Prevent path traversal such as:
         *
         * ../../file.txt
         * ..\\file.txt
         * C:\\temp\\file.txt
         */
        Path path = Paths.get(filename);

        if (path.getNameCount() != 1 || filename.contains("/") || filename.contains("\\")) {

            throw new IllegalArgumentException("Path separators are not allowed");
        }

        if (filename.equals(".") || filename.equals("..")) {

            throw new IllegalArgumentException("Invalid filename");
        }

        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        if (sanitized.isBlank()) {

            throw new IllegalArgumentException("Invalid filename");
        }

        return sanitized;
    }

    public String extension(String filename) {

        int index = filename.lastIndexOf('.');

        if (index <= 0 || index == filename.length() - 1) {

            return "";
        }

        return filename.substring(index + 1).toLowerCase();
    }
}