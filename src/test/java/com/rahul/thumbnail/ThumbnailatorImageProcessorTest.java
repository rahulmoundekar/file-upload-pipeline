package com.rahul.thumbnail;

import com.rahul.config.ThumbnailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThumbnailatorImageProcessorTest {

    private ThumbnailatorImageProcessor processor;

    @BeforeEach
    void setUp() {

        ThumbnailProperties properties = new ThumbnailProperties(300, 300, "jpg", 0.85);

        processor = new ThumbnailatorImageProcessor(properties);
    }

    @Test
    void shouldGenerateThumbnailFromValidImage() throws Exception {

        byte[] sourceImage = createImage(800, 600, "png");

        ThumbnailResult result = processor.generate(new ByteArrayInputStream(sourceImage));

        assertThat(result).isNotNull();

        assertThat(result.content()).isNotEmpty();

        assertThat(result.contentType()).isEqualTo("image/jpeg");

        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(result.content()));

        assertThat(thumbnail).isNotNull();

        assertThat(thumbnail.getWidth()).isLessThanOrEqualTo(300);

        assertThat(thumbnail.getHeight()).isLessThanOrEqualTo(300);
    }

    @Test
    void shouldPreserveAspectRatio() throws Exception {

        byte[] sourceImage = createImage(800, 400, "png");

        ThumbnailResult result = processor.generate(new ByteArrayInputStream(sourceImage));

        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(result.content()));

        assertThat(thumbnail).isNotNull();

        assertThat(thumbnail.getWidth()).isEqualTo(300);

        assertThat(thumbnail.getHeight()).isEqualTo(150);
    }

    @Test
    void shouldGenerateSquareThumbnailWithoutExceedingBounds() throws Exception {

        byte[] sourceImage = createImage(1000, 1000, "png");

        ThumbnailResult result = processor.generate(new ByteArrayInputStream(sourceImage));

        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(result.content()));

        assertThat(thumbnail).isNotNull();

        assertThat(thumbnail.getWidth()).isEqualTo(300);

        assertThat(thumbnail.getHeight()).isEqualTo(300);
    }

    @Test
    void invalidImageShouldBeRejected() {

        byte[] invalidContent = "this is not an image".getBytes();

        assertThatThrownBy(() -> processor.generate(new ByteArrayInputStream(invalidContent))).isInstanceOf(ImageProcessingException.class).hasMessageContaining("Unable to generate thumbnail");
    }

    @Test
    void outputShouldBeValidJpeg() throws Exception {

        byte[] sourceImage = createImage(640, 480, "png");

        ThumbnailResult result = processor.generate(new ByteArrayInputStream(sourceImage));

        assertThat(result.contentType()).isEqualTo("image/jpeg");

        BufferedImage output = ImageIO.read(new ByteArrayInputStream(result.content()));

        assertThat(output).isNotNull();
    }

    private byte[] createImage(int width, int height, String format) throws Exception {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();

        try {

            graphics.fillRect(0, 0, width, height);

        } finally {

            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        boolean written = ImageIO.write(image, format, output);

        assertThat(written).isTrue();

        return output.toByteArray();
    }
}