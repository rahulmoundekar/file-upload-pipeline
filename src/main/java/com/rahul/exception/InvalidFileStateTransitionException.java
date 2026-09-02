package com.rahul.exception;

public class InvalidFileStateTransitionException
        extends RuntimeException {

    public InvalidFileStateTransitionException(
            String message
    ) {
        super(message);
    }
}