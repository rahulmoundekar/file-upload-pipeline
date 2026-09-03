package com.rahul.service;

import com.rahul.config.ThumbnailProperties;
import com.rahul.entity.DerivativeType;
import com.rahul.entity.FileDerivative;
import com.rahul.repository.FileDerivativeRepository;
import com.rahul.thumbnail.ThumbnailObjectKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FileDerivativeService {

    private final FileDerivativeRepository repository;
    private final ThumbnailProperties thumbnailProperties;

    public FileDerivativeService(FileDerivativeRepository repository, ThumbnailProperties thumbnailProperties) {
        this.repository = repository;
        this.thumbnailProperties = thumbnailProperties;
    }

    @Transactional
    public FileDerivative createThumbnail(UUID fileId, String objectKey, String contentType, long sizeBytes, int width, int height) {

        if (repository.existsByFileIdAndDerivativeType(fileId, DerivativeType.THUMBNAIL)) {

            return repository.findByFileIdAndDerivativeType(fileId, DerivativeType.THUMBNAIL).orElseThrow();
        }

        FileDerivative derivative = new FileDerivative(fileId, DerivativeType.THUMBNAIL, objectKey, contentType, sizeBytes, width, height);

        return repository.save(derivative);
    }
}