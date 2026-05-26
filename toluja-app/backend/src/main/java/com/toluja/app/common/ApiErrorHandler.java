package com.toluja.app.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return errorResponse(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Validation error", request.getRequestURI(), fields));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        return errorResponse(status)
                .body(new ApiError(status, ex.getReason() == null ? "Request error" : ex.getReason(), request.getRequestURI(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, "Access denied", request.getRequestURI(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "Internal server error", request.getRequestURI(), null));
    }

    private ResponseEntity.BodyBuilder errorResponse(HttpStatus status) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private ResponseEntity.BodyBuilder errorResponse(int status) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON);
    }

    public record ApiError(int status, String message, String path, Map<String, String> fields, OffsetDateTime timestamp) {
        public ApiError(int status, String message, String path, Map<String, String> fields) {
            this(status, message, path, fields, OffsetDateTime.now());
        }
    }
}
