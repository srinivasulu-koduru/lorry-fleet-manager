package com.lorryowner.backend.dto.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleMaintenanceSummaryDto(
        Long lorryId,
        String registrationNumber,
        String makeModel,
        LocalDate lastServiceDate,
        LocalDate nextServiceDate,
        String status, // "Up to Date", "Due Soon", "Overdue"
        BigDecimal totalCost,
        int totalRecordsCount
) {}
