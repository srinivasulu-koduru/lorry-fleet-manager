package com.lorryowner.backend.dto.lorry;

import java.math.BigDecimal;

public record FleetSummaryDto(
        long totalLorries,
        long runningLorries,
        long notRunningLorries,
        double runningPercentage,
        double notRunningPercentage,
        long totalTrips,
        BigDecimal totalDistanceKm,
        BigDecimal totalMaintenanceCost
) {}
