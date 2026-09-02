package com.rahul.exception;

import com.rahul.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<ApiError.FieldError> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                new ApiError.FieldError(
                                        error.getField(),
                                        error.getDefaultMessage()
                                )
                        )
                        .toList();

        ApiError response =
                new ApiError(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "Request validation failed",
                        request.getRequestURI(),
                        errors
                );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.BAD_REQUEST,
                "FILE_TOO_LARGE",
                "File exceeds the maximum allowed size",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request
        );
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ApiError> handleInvalidFile(
            InvalidFileException ex,
            HttpServletRequest request
    ) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE",
                ex.getMessage(),
                request
        );
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {

        ApiError error =
                new ApiError(
                        Instant.now(),
                        status.value(),
                        code,
                        message,
                        request.getRequestURI(),
                        List.of()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }
}