package com.rahul.thumbnail;

import com.rahul.config.ThumbnailProperties;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class ThumbnailatorImageProcessor implements ImageProcessor {

    private final ThumbnailProperties properties;

    @Override
    public ThumbnailResult generate(InputStream inputStream) {

        try {

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            Thumbnails.of(inputStream).size(properties.width(), properties.height()).outputFormat(properties.format()).outputQuality(properties.quality()).toOutputStream(output);

            byte[] content = output.toByteArray();

            BufferedImage generatedImage = ImageIO.read(new ByteArrayInputStream(content));

            if (generatedImage == null) {

                throw new ImageProcessingException("Generated thumbnail is not a valid image");
            }

            return new ThumbnailResult(content, resolveContentType(properties.format()), generatedImage.getWidth(), generatedImage.getHeight());

        } catch (ImageProcessingException e) {

            throw e;

        } catch (Exception e) {

            throw new ImageProcessingException("Unable to generate thumbnail", e);
        }
    }

    private String resolveContentType(String format) {

        return switch (format.toLowerCase()) {

            case "jpg", "jpeg" -> "image/jpeg";

            case "png" -> "image/png";

            case "webp" -> "image/webp";

            default -> throw new ImageProcessingException("Unsupported thumbnail format: " + format);
        };
    }
}