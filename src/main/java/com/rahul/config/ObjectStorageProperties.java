package com.rahul.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage")
public record ObjectStorageProperties(

        @NotBlank
        String endpoint,

        @NotBlank
        String accessKey,

        @NotBlank
        String secretKey,

        @NotBlank
        String bucket,

        boolean secure

) {
}