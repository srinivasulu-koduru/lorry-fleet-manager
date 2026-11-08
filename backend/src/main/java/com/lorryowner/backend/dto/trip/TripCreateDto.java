package com.lorryowner.backend.dto.trip;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TripCreateDto(
        @NotNull(message = "Lorry ID is required")
        Long lorryId,

        @NotNull(message = "Driver ID is required")
        Long driverId,

        @NotBlank(message = "Origin location is required")
        @Size(max = 150, message = "Origin must not exceed 150 characters")
        String origin,

        @NotBlank(message = "Destination location is required")
        @Size(max = 150, message = "Destination must not exceed 150 characters")
        String destination,

        @NotNull(message = "Planned departure date is required")
        LocalDate plannedDepartureDate,

        @NotNull(message = "Planned departure time is required")
        LocalTime plannedDepartureTime,

        LocalDate plannedDeliveryDate,

        LocalTime plannedDeliveryTime,

        @NotBlank(message = "Load type is required")
        @Size(max = 100, message = "Load type must not exceed 100 characters")
        String loadType,

        @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
        String remarks,

        @DecimalMin(value = "0.0", inclusive = true, message = "Trip amount must be non-negative")
        BigDecimal tripAmount
) {
    public TripCreateDto(
            Long lorryId,
            Long driverId,
            String origin,
            String destination,
            LocalDate plannedDepartureDate,
            LocalTime plannedDepartureTime,
            LocalDate plannedDeliveryDate,
            LocalTime plannedDeliveryTime,
            String loadType,
            String remarks
    ) {
        this(lorryId, driverId, origin, destination, plannedDepartureDate, plannedDepartureTime,
                plannedDeliveryDate, plannedDeliveryTime, loadType, remarks, BigDecimal.ZERO);
    }
}
