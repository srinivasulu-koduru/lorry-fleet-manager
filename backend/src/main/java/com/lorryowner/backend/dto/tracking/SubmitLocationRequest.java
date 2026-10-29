package com.lorryowner.backend.dto.tracking;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request payload submitted by driver phone for live GPS trip tracking.
 */
public record SubmitLocationRequest(
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
        Double longitude,

        @DecimalMin(value = "0.0", message = "Speed cannot be negative")
        Double speedKmh,

        @DecimalMin(value = "0.0", message = "Heading must be >= 0.0")
        @DecimalMax(value = "360.0", message = "Heading must be <= 360.0")
        Double heading,

        @DecimalMin(value = "0.0", message = "Accuracy cannot be negative")
        Double accuracyMeters,

        Instant recordedAt
) {
}
