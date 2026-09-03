package com.rahul.service;

import com.rahul.config.ThumbnailProperties;
import com.rahul.entity.DerivativeType;
import com.rahul.entity.FileDerivative;
import com.rahul.repository.FileDerivativeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FileDerivativeServiceTest {

    private FileDerivativeRepository repository;

    private FileDerivativeService service;

    private ThumbnailProperties thumbnailProperties;

    @BeforeEach
    void setUp() {

        repository = mock(FileDerivativeRepository.class);

        thumbnailProperties = mock(ThumbnailProperties.class);

        service = new FileDerivativeService(repository, thumbnailProperties);
    }

    @Test
    void shouldCreateThumbnailDerivative() {

        UUID fileId = UUID.randomUUID();

        when(repository.existsByFileIdAndDerivativeType(fileId, DerivativeType.THUMBNAIL)).thenReturn(false);

        when(repository.save(any(FileDerivative.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileDerivative result = service.createThumbnail(fileId, "uploads/test-thumb.jpg", "image/jpeg", 5000, 300, 225);

        assertThat(result.getFileId()).isEqualTo(fileId);

        assertThat(result.getDerivativeType()).isEqualTo(DerivativeType.THUMBNAIL);

        assertThat(result.getWidth()).isEqualTo(300);

        assertThat(result.getHeight()).isEqualTo(225);

        verify(repository).save(any(FileDerivative.class));
    }

    @Test
    void duplicateThumbnailShouldReturnExistingDerivative() {

        UUID fileId = UUID.randomUUID();

        FileDerivative existing = new FileDerivative(fileId, DerivativeType.THUMBNAIL, "uploads/test-thumb.jpg", "image/jpeg", 5000, 300, 225);

        when(repository.existsByFileIdAndDerivativeType(fileId, DerivativeType.THUMBNAIL)).thenReturn(true);

        when(repository.findByFileIdAndDerivativeType(fileId, DerivativeType.THUMBNAIL)).thenReturn(Optional.of(existing));

        FileDerivative result = service.createThumbnail(fileId, "uploads/test-thumb.jpg", "image/jpeg", 5000, 300, 225);

        assertThat(result).isSameAs(existing);

        verify(repository, never()).save(any());
    }
}