package com.rahul.thumbnail;

import java.io.InputStream;

public interface ImageProcessor {

    ThumbnailResult generate(InputStream inputStream);
}