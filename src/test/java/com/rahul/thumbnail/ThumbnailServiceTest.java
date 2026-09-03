package com.rahul.thumbnail;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThumbnailServiceTest {

    @Test
    void shouldGenerateThumbnail() {

        ImageProcessor processor = mock(ImageProcessor.class);

        ThumbnailService service = new ThumbnailService(processor);

        byte[] input = "image".getBytes();

        ThumbnailResult expected = new ThumbnailResult(new byte[]{1, 2, 3}, "image/jpeg", 300, 300);

        ByteArrayInputStream stream = new ByteArrayInputStream(input);

        when(processor.generate(stream)).thenReturn(expected);

        ThumbnailResult result = service.generate(stream);

        assertThat(result.content()).containsExactly(1, 2, 3);

        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }
}