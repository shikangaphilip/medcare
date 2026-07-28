package com.philmed.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One place for turning exceptions into clean JSON, instead of every
 * controller repeating its own formatting (or letting Spring's default
 * whitelabel error page / raw stack trace leak out). Controller-local
 * try/catch blocks still run first for cases that need a specific status
 * code (e.g. 404 vs 400) — this is the safety net underneath them.
 */
@RestControllerAdvice
public class ErrorHandling {

    public static class ApiError {
        public Instant timestamp = Instant.now();
        public int status;
        public String message;
        public Map<String, String> fieldErrors;

        public ApiError(int status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    /** Triggered by @Valid failing on a request body — returns one message per bad field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation failed");
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        error.fieldErrors = fieldErrors;
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /** Last-resort catch-all so an unexpected bug returns clean JSON, not a stack trace. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong. Please try again."));
    }
}
