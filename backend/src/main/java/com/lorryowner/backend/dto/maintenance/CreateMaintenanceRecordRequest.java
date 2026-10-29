package com.lorryowner.backend.dto.maintenance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMaintenanceRecordRequest(
        Long lorryId,

        @NotBlank(message = "Maintenance type is required")
        String serviceType,

        @NotNull(message = "Service date is required")
        LocalDate maintenanceDate,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Maintenance cost is required")
        @DecimalMin(value = "0.00", message = "Maintenance cost cannot be negative")
        BigDecimal cost,

        @NotNull(message = "Odometer reading is required")
        @Min(value = 0, message = "Odometer reading cannot be negative")
        Integer odometerReadingKm,

        LocalDate nextServiceDate,

        String serviceCenter,

        String notes
) {}
