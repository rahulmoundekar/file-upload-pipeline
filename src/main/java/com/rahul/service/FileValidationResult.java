package com.rahul.service;

public record FileValidationResult(
        String sanitizedFilename,
        String extension,
        String clientContentType,
        String detectedContentType
) {
}