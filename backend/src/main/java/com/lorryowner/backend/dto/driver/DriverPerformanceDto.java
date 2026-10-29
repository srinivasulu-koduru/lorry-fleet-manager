package com.lorryowner.backend.dto.driver;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DriverPerformanceDto(
        Long driverId,
        String driverUsername,
        String driverFullName,
        long totalTrips,
        long completedTrips,
        long inProgressTrips,
        long cancelledTrips,
        BigDecimal totalRecordedDistanceKm,
        long distinctLorriesUsed,
        LocalDate mostRecentTripDate,
        String mostRecentTripReference
) {}
