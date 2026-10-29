package com.lorryowner.backend.dto.lorry;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenanceRecordDto(
        Long id,
        Long lorryId,
        String lorryRegistration,
        LocalDate maintenanceDate,
        String serviceType,
        String description,
        BigDecimal cost,
        String serviceCenter,
        Integer odometerReadingKm,
        LocalDate nextServiceDate
) {}
