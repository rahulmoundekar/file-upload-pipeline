package com.rahul.dto;

public record FileIntegrityResponse(
        boolean valid,
        String expectedChecksum,
        String actualChecksum
) {
}