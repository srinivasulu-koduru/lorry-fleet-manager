package com.lorryowner.backend.dto.maintenance;

import java.math.BigDecimal;
import java.util.List;

public record MaintenanceSummaryResponse(
        long totalVehicles,
        long dueSoonCount,
        long overdueCount,
        BigDecimal totalExpenditure,
        BigDecimal currentMonthExpense,
        String currentMonth,
        int currentYear,
        List<VehicleMaintenanceSummaryDto> vehicles
) {
    public MaintenanceSummaryResponse(
            long totalVehicles,
            long dueSoonCount,
            long overdueCount,
            BigDecimal totalExpenditure,
            List<VehicleMaintenanceSummaryDto> vehicles
    ) {
        this(totalVehicles, dueSoonCount, overdueCount, totalExpenditure, BigDecimal.ZERO, "", 0, vehicles);
    }
}
