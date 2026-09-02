package com.rahul.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class FileTypePolicy {

    private final Map<String, Set<String>> allowedTypes = Map.of("pdf", Set.of("application/pdf"),

            "png", Set.of("image/png"),

            "jpg", Set.of("image/jpeg"),

            "jpeg", Set.of("image/jpeg"),

            "gif", Set.of("image/gif"),

            "txt", Set.of("text/plain"));

    public boolean isAllowedExtension(String extension) {
        return extension != null && allowedTypes.containsKey(extension.toLowerCase());
    }

    public boolean isAllowedMime(String extension, String detectedMimeType) {

        if (extension == null || detectedMimeType == null) {

            return false;
        }

        Set<String> allowedMimeTypes = allowedTypes.get(extension.toLowerCase());

        return allowedMimeTypes != null && allowedMimeTypes.contains(detectedMimeType);
    }

    public Set<String> allowedMimeTypes(String extension) {

        if (extension == null) {
            return Set.of();
        }

        return allowedTypes.getOrDefault(extension.toLowerCase(), Set.of());
    }
}