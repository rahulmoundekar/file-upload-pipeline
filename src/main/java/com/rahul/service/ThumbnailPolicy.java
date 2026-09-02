package com.rahul.service;

import com.rahul.entity.ThumbnailStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ThumbnailPolicy {

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");

    public ThumbnailStatus initialStatus(String contentType) {

        if (contentType == null) {
            return ThumbnailStatus.NOT_REQUIRED;
        }

        return SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase()) ? ThumbnailStatus.PENDING : ThumbnailStatus.NOT_REQUIRED;
    }

    public boolean requiresThumbnail(String contentType) {

        return contentType != null && SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase());
    }
}