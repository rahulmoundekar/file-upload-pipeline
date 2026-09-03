package com.rahul.thumbnail;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageFileDetector {

    private ImageFileDetector() {
    }

    public static boolean isImage(InputStream inputStream) {

        try {

            BufferedImage image = ImageIO.read(inputStream);

            return image != null;

        } catch (IOException e) {

            return false;
        }
    }
}