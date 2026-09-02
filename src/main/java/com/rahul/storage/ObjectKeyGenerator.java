package com.rahul.storage;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class ObjectKeyGenerator {

    public String generate(String originalFilename) {

        String safeFilename = sanitizeFilename(originalFilename);

        LocalDate today = LocalDate.now();

        return String.format("uploads/%d/%02d/%02d/%s-%s", today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID(), safeFilename);
    }

    private String sanitizeFilename(String filename) {

        if (filename == null || filename.isBlank()) {

            return "file";
        }

        String normalized = filename.replace("\\", "_").replace("/", "_");

        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}