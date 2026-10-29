package com.lorryowner.backend.dto.lorry;

import com.lorryowner.backend.entity.OperationalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OwnerLorryDetailDto(
        Long id,
        String registrationNumber,
        String makeModel,
        BigDecimal capacityTons,
        OperationalStatus operationalStatus,
        boolean running,
        Long assignedDriverId,
        String assignedDriverName,
        String assignedDriverPhone,
        Long activeTripId,
        String activeTripReference,
        String activeTripOrigin,
        String activeTripDestination,
        Instant activeTripStartTime,
        Instant activeTripPlannedDeliveryTime,
        String activeTripEtaFormatted,
        long totalTrips,
        long completedTrips,
        long inProgressTrips,
        long cancelledTrips,
        BigDecimal totalRecordedDistanceKm,
        BigDecimal totalMaintenanceCost,
        Instant lastTripDate,
        String notes,
        Instant updatedAt
) {}
