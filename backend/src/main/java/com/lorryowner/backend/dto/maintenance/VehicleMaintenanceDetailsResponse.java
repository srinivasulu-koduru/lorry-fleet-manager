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
        int totalRecordsCount,
        List<MaintenanceRecordDto> records
) {
    public VehicleMaintenanceDetailsResponse(
            Long lorryId,
            String registrationNumber,
            String makeModel,
            LocalDate lastServiceDate,
            LocalDate nextServiceDate,
            String status,
            BigDecimal totalMaintenanceCost,
            List<MaintenanceRecordDto> records
    ) {
        this(lorryId, registrationNumber, makeModel, lastServiceDate, nextServiceDate, status, totalMaintenanceCost, records != null ? records.size() : 0, records);
    }
}
