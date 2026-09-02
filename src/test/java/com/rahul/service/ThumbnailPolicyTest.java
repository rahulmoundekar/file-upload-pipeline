package com.rahul.service;

import com.rahul.entity.ThumbnailStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThumbnailPolicyTest {

    private final ThumbnailPolicy policy = new ThumbnailPolicy();

    @Test
    void jpegShouldRequireThumbnail() {

        assertThat(policy.initialStatus("image/jpeg")).isEqualTo(ThumbnailStatus.PENDING);
    }

    @Test
    void pngShouldRequireThumbnail() {

        assertThat(policy.initialStatus("image/png")).isEqualTo(ThumbnailStatus.PENDING);
    }

    @Test
    void pdfShouldNotRequireThumbnail() {

        assertThat(policy.initialStatus("application/pdf")).isEqualTo(ThumbnailStatus.NOT_REQUIRED);
    }

    @Test
    void textShouldNotRequireThumbnail() {

        assertThat(policy.initialStatus("text/plain")).isEqualTo(ThumbnailStatus.NOT_REQUIRED);
    }
}