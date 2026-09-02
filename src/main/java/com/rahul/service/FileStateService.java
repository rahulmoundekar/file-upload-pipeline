package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStateService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStateMachine fileStateMachine;

    @Transactional
    public FileMetadata transition(UUID fileId, FileStatus target) {

        FileMetadata file = fileMetadataRepository.findById(fileId).orElseThrow(() -> new IllegalArgumentException("File not found"));

        FileStatus current = file.getStatus();

        fileStateMachine.validateTransition(current, target);

        file.changeStatus(target);

        switch (target) {

            case SCANNING -> file.setScanStatus(ScanStatus.SCANNING);

            case CLEAN -> file.setScanStatus(ScanStatus.CLEAN);

            case INFECTED -> file.setScanStatus(ScanStatus.INFECTED);

            case THUMBNAIL_PROCESSING -> file.setThumbnailStatus(ThumbnailStatus.PROCESSING);

            case COMPLETED -> file.setCompletedAt(Instant.now());

            default -> {
                // no additional side effects
            }
        }

        return fileMetadataRepository.save(file);
    }
}