package com.lorryowner.backend.dto.tracking;

import java.time.Instant;

public record LiveTrackingLorryDto(
        Long lorryId,
        String registrationNumber,
        String makeModel,
        Long tripId,
        String tripReference,
        String driverName,
        String driverPhone,
        String originLocation,
        String destinationLocation,
        String currentLocationDescription,
        Double latitude,
        Double longitude,
        Double currentSpeedKmh,
        Double heading,
        Double accuracyMeters,
        Double distanceTravelledKm,
        Double remainingDistanceKm,
        String estimatedDrivingTime,
        String estimatedArrival,
        String gpsStatus,
        Instant lastGpsUpdate,
        boolean isLive,
        boolean hasGpsFix
) {
}
