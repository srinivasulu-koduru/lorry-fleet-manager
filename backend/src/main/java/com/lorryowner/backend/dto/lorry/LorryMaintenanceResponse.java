package com.lorryowner.backend.dto.lorry;

import java.math.BigDecimal;
import java.util.List;

public record LorryMaintenanceResponse(
        Long lorryId,
        String registrationNumber,
        String makeModel,
        BigDecimal totalMaintenanceCost,
        List<MaintenanceRecordDto> records
) {}
