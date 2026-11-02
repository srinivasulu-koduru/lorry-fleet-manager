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
        BigDecimal totalMaintenanceCost,
        long loadingLorries,
        double loadingPercentage,
        long availableLorries,
        double availablePercentage,
        long notReadyLorries,
        double notReadyPercentage
) {
    public FleetSummaryDto(
            long totalLorries,
            long runningLorries,
            long notRunningLorries,
            double runningPercentage,
            double notRunningPercentage,
            long totalTrips,
            BigDecimal totalDistanceKm,
            BigDecimal totalMaintenanceCost
    ) {
        this(
                totalLorries,
                runningLorries,
                notRunningLorries,
                runningPercentage,
                notRunningPercentage,
                totalTrips,
                totalDistanceKm,
                totalMaintenanceCost,
                0L,
                0.0,
                notRunningLorries,
                notRunningPercentage,
                0L,
                0.0
        );
    }
}
