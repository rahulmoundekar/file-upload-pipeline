package com.rahul.thumbnail;

public record ThumbnailResult(
        byte[] content,
        String contentType,
        int width,
        int height
) {
}