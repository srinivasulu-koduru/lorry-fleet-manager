package com.lorryowner.backend.dto.trip;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record TripResponseDto(
        Long id,
        String tripReference,
        Long lorryId,
        String lorryRegistration,
        String lorryMakeModel,
        Long driverId,
        String driverName,
        String origin,
        String destination,
        LocalDate plannedDepartureDate,
        LocalTime plannedDepartureTime,
        LocalDate plannedDeliveryDate,
        LocalTime plannedDeliveryTime,
        String loadType,
        String remarks,
        String status,
        Instant createdAt
) {}
