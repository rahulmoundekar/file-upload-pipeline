package com.rahul.exception;

public class FileNotFoundForEventException extends RuntimeException {

    public FileNotFoundForEventException(String message) {
        super(message);
    }
}