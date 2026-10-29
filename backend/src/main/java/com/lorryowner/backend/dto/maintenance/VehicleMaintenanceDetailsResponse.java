package com.lorryowner.backend.dto.maintenance;

import com.lorryowner.backend.dto.lorry.MaintenanceRecordDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VehicleMaintenanceDetailsResponse(
        Long lorryId,
        String registrationNumber,
        String makeModel,
        LocalDate lastServiceDate,
        LocalDate nextServiceDate,
        String status,
        BigDecimal totalMaintenanceCost,
        List<MaintenanceRecordDto> records
) {}
