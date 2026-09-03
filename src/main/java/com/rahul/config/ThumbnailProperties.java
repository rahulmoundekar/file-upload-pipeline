package com.rahul.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "thumbnail")
public record ThumbnailProperties(int width, int height, String format, double quality) {
}