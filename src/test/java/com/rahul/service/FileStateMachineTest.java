package com.rahul.service;

import com.rahul.entity.FileStatus;
import com.rahul.exception.InvalidFileStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStateMachineTest {

    private final FileStateMachine stateMachine = new FileStateMachine();

    @Test
    void uploadedCanTransitionToProcessing() {

        assertThat(stateMachine.canTransition(FileStatus.UPLOADED, FileStatus.PROCESSING)).isTrue();
    }

    @Test
    void processingCanTransitionToScanning() {

        assertThat(stateMachine.canTransition(FileStatus.PROCESSING, FileStatus.SCANNING)).isTrue();
    }

    @Test
    void scanningCanTransitionToClean() {

        assertThat(stateMachine.canTransition(FileStatus.SCANNING, FileStatus.CLEAN)).isTrue();
    }

    @Test
    void scanningCanTransitionToInfected() {

        assertThat(stateMachine.canTransition(FileStatus.SCANNING, FileStatus.INFECTED)).isTrue();
    }

    @Test
    void infectedCanTransitionToRejected() {

        assertThat(stateMachine.canTransition(FileStatus.INFECTED, FileStatus.REJECTED)).isTrue();
    }

    @Test
    void cleanCanTransitionToCompleted() {

        assertThat(stateMachine.canTransition(FileStatus.CLEAN, FileStatus.COMPLETED)).isTrue();
    }

    @Test
    void cleanCanTransitionToThumbnailProcessing() {

        assertThat(stateMachine.canTransition(FileStatus.CLEAN, FileStatus.THUMBNAIL_PROCESSING)).isTrue();
    }

    @Test
    void completedCannotTransitionToProcessing() {

        assertThat(stateMachine.canTransition(FileStatus.COMPLETED, FileStatus.PROCESSING)).isFalse();
    }

    @Test
    void rejectedCannotTransitionToScanning() {

        assertThat(stateMachine.canTransition(FileStatus.REJECTED, FileStatus.SCANNING)).isFalse();
    }

    @Test
    void invalidTransitionShouldThrowException() {

        assertThatThrownBy(() -> stateMachine.validateTransition(FileStatus.COMPLETED, FileStatus.SCANNING)).isInstanceOf(InvalidFileStateTransitionException.class).hasMessage("Invalid file state transition: " + "COMPLETED -> SCANNING");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "UPLOADING,UPLOADED",
            "UPLOADING,FAILED",
            "UPLOADED,PROCESSING",
            "UPLOADED,FAILED",
            "PROCESSING,SCANNING",
            "PROCESSING,FAILED",
            "SCANNING,CLEAN",
            "SCANNING,INFECTED",
            "SCANNING,FAILED",
            "CLEAN,THUMBNAIL_PROCESSING",
            "CLEAN,COMPLETED",
            "THUMBNAIL_PROCESSING,COMPLETED",
            "THUMBNAIL_PROCESSING,FAILED",
            "INFECTED,REJECTED"
    })
    void legalTransitionsShouldBeAllowed(
            FileStatus current,
            FileStatus target
    ) {

        assertThat(
                stateMachine.canTransition(
                        current,
                        target
                )
        ).isTrue();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "UPLOADED,SCANNING",
            "UPLOADED,CLEAN",
            "PROCESSING,COMPLETED",
            "SCANNING,COMPLETED",
            "INFECTED,CLEAN",
            "INFECTED,COMPLETED",
            "REJECTED,PROCESSING",
            "COMPLETED,PROCESSING",
            "COMPLETED,SCANNING",
            "FAILED,PROCESSING"
    })
    void illegalTransitionsShouldBeRejected(
            FileStatus current,
            FileStatus target
    ) {

        assertThat(
                stateMachine.canTransition(
                        current,
                        target
                )
        ).isFalse();
    }
}