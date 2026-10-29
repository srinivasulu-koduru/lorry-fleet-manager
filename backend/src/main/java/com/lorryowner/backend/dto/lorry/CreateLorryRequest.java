package com.lorryowner.backend.dto.lorry;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for creating and registering a new commercial fleet lorry.
 * Allows optional driver assignment during registration.
 */
public record CreateLorryRequest(
        @NotBlank(message = "Registration number is required")
        @Size(min = 3, max = 20, message = "Registration number must be between 3 and 20 characters")
        String registrationNumber,

        @NotBlank(message = "Vehicle make and model is required")
        @Size(max = 100, message = "Make and model must not exceed 100 characters")
        String makeModel,

        @NotNull(message = "Payload capacity is required")
        @DecimalMin(value = "0.1", message = "Payload capacity must be at least 0.1 tons")
        @DecimalMax(value = "200.0", message = "Payload capacity must not exceed 200 tons")
        BigDecimal capacityTons,

        String operationalStatus,

        @Size(max = 50, message = "iAlert unit ID must not exceed 50 characters")
        String ialertUnitId,

        String notes,

        Long assignedDriverId
) {
    public CreateLorryRequest(String registrationNumber, String makeModel, BigDecimal capacityTons) {
        this(registrationNumber, makeModel, capacityTons, null, null, null, null);
    }
}
