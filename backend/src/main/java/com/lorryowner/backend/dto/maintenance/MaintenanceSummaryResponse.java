package com.lorryowner.backend.dto.maintenance;

import java.math.BigDecimal;
import java.util.List;

public record MaintenanceSummaryResponse(
        long totalVehicles,
        long dueSoonCount,
        long overdueCount,
        BigDecimal totalExpenditure,
        List<VehicleMaintenanceSummaryDto> vehicles
) {}
