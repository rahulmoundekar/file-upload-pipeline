package com.rahul.thumbnail;

public final class ThumbnailObjectKey {

    private ThumbnailObjectKey() {
    }

    public static String from(String originalObjectKey, String format) {

        int dotIndex = originalObjectKey.lastIndexOf('.');

        String base = dotIndex >= 0 ? originalObjectKey.substring(0, dotIndex) : originalObjectKey;

        return base + "-thumb." + format;
    }
}