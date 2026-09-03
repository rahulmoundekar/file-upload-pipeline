package com.rahul.service;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileCompletionService {

    private final FileStateService fileStateService;
    private final FileCompletedEventService fileCompletedEventService;

    @Transactional
    public void complete(FileMetadata file) {

        FileMetadata completed = fileStateService.transition(file.getId(), FileStatus.COMPLETED);

        fileCompletedEventService.createEvent(completed);
    }
}