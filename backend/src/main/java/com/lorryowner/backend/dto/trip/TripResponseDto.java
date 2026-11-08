package com.lorryowner.backend.dto.trip;

import java.math.BigDecimal;
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
        Instant createdAt,
        BigDecimal tripAmount
) {
    public TripResponseDto(
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
    ) {
        this(id, tripReference, lorryId, lorryRegistration, lorryMakeModel, driverId, driverName,
                origin, destination, plannedDepartureDate, plannedDepartureTime, plannedDeliveryDate,
                plannedDeliveryTime, loadType, remarks, status, createdAt, BigDecimal.ZERO);
    }
}
