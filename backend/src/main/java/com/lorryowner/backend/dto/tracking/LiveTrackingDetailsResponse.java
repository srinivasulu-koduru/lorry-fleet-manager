package com.lorryowner.backend.dto.tracking;

import java.time.Instant;
import java.util.List;

public record LiveTrackingDetailsResponse(
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
        boolean hasGpsFix,
        Double originLatitude,
        Double originLongitude,
        Double destinationLatitude,
        Double destinationLongitude,
        List<TripLocationBreadcrumbDto> breadcrumbs
) {
}
