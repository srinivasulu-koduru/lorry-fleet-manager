package com.lorryowner.backend.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response DTO.
 * Guarantees consistent error formatting across all endpoints without exposing internal details or stack traces.
 *
 * @param timestamp Time the error occurred (UTC)
 * @param status    HTTP status code
 * @param error     HTTP status error description
 * @param message   User-friendly error message
 * @param path      Request path
 * @param errors    Validation field errors (optional, null for non-validation errors)
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> errors
) {
    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}
