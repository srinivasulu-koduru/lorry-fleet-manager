package com.lorryowner.backend.dto.driver;

import java.util.List;

/**
 * High-level dashboard summary response for an authenticated Driver.
 * Strictly scoped to the authenticated driver's assigned lorry and trips.
 */
public record DriverDashboardResponse(
        String driverName,
        String driverUsername,
        String assignedLorryRegistration,
        String assignedLorryMakeModel,
        TripDto todayTrip,
        long currentTripsCount,
        long upcomingTripsCount,
        long completedTripsCount
) {}
