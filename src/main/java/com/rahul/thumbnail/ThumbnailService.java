package com.rahul.thumbnail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final ImageProcessor imageProcessor;

    public ThumbnailResult generate(InputStream inputStream) {

        return imageProcessor.generate(inputStream);
    }
}