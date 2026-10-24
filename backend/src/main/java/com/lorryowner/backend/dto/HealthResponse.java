package com.lorryowner.backend.dto;

/**
 * Health check response DTO.
 *
 * @param status       Application health status (e.g., "UP")
 * @param application  Name of the application
 */
public record HealthResponse(
        String status,
        String application
) {
}
