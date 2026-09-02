package com.rahul.service;

import com.rahul.entity.FileStatus;
import com.rahul.exception.InvalidFileStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class FileStateMachine {

    private final Map<FileStatus, Set<FileStatus>> transitions = new EnumMap<>(FileStatus.class);

    public FileStateMachine() {

        transitions.put(FileStatus.UPLOADING, EnumSet.of(FileStatus.UPLOADED, FileStatus.FAILED));

        transitions.put(FileStatus.UPLOADED, EnumSet.of(FileStatus.PROCESSING, FileStatus.FAILED));

        transitions.put(FileStatus.PROCESSING, EnumSet.of(FileStatus.SCANNING, FileStatus.FAILED));

        transitions.put(FileStatus.SCANNING, EnumSet.of(FileStatus.CLEAN, FileStatus.INFECTED, FileStatus.FAILED));

        transitions.put(FileStatus.CLEAN, EnumSet.of(FileStatus.THUMBNAIL_PROCESSING, FileStatus.COMPLETED));

        transitions.put(FileStatus.THUMBNAIL_PROCESSING, EnumSet.of(FileStatus.COMPLETED, FileStatus.FAILED));

        transitions.put(FileStatus.INFECTED, EnumSet.of(FileStatus.REJECTED));

        transitions.put(FileStatus.REJECTED, EnumSet.noneOf(FileStatus.class));

        transitions.put(FileStatus.COMPLETED, EnumSet.noneOf(FileStatus.class));

        transitions.put(FileStatus.FAILED, EnumSet.noneOf(FileStatus.class));
    }

    public boolean canTransition(FileStatus current, FileStatus target) {

        return transitions.getOrDefault(current, Set.of()).contains(target);
    }

    public void validateTransition(FileStatus current, FileStatus target) {

        if (!canTransition(current, target)) {

            throw new InvalidFileStateTransitionException("Invalid file state transition: " + current + " -> " + target);
        }
    }

    public Set<FileStatus> allowedTransitions(FileStatus current) {

        return transitions.getOrDefault(current, Set.of());
    }
}