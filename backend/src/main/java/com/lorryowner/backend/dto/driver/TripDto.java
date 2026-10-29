package com.lorryowner.backend.dto.driver;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TripDto(
        Long id,
        String tripReference,
        Long driverId,
        String driverName,
        Long lorryId,
        String lorryRegistration,
        String lorryMakeModel,
        String originLocation,
        String destinationLocation,
        String tripStatus,
        LocalDate tripDate,
        Instant startTime,
        Instant endTime,
        BigDecimal distanceKm,
        String deliveryNotes,
        Instant createdAt,
        Instant plannedDeliveryTime,
        String loadType
) {
    public TripDto(
            Long id,
            String tripReference,
            Long driverId,
            String driverName,
            Long lorryId,
            String lorryRegistration,
            String lorryMakeModel,
            String originLocation,
            String destinationLocation,
            String tripStatus,
            LocalDate tripDate,
            Instant startTime,
            Instant endTime,
            BigDecimal distanceKm,
            String deliveryNotes,
            Instant createdAt
    ) {
        this(id, tripReference, driverId, driverName, lorryId, lorryRegistration, lorryMakeModel,
                originLocation, destinationLocation, tripStatus, tripDate, startTime, endTime,
                distanceKm, deliveryNotes, createdAt, null, null);
    }

    public TripDto(
            Long id,
            String tripReference,
            Long driverId,
            String driverName,
            Long lorryId,
            String lorryRegistration,
            String lorryMakeModel,
            String originLocation,
            String destinationLocation,
            String tripStatus,
            LocalDate tripDate,
            Instant startTime,
            Instant endTime,
            BigDecimal distanceKm,
            String deliveryNotes,
            Instant createdAt,
            Instant plannedDeliveryTime
    ) {
        this(id, tripReference, driverId, driverName, lorryId, lorryRegistration, lorryMakeModel,
                originLocation, destinationLocation, tripStatus, tripDate, startTime, endTime,
                distanceKm, deliveryNotes, createdAt, plannedDeliveryTime, null);
    }
}
