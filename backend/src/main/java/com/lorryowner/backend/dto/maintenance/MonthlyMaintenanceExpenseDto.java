package com.lorryowner.backend.dto.maintenance;

import java.math.BigDecimal;

public record MonthlyMaintenanceExpenseDto(
        int month,
        String monthName,
        int year,
        BigDecimal totalAmount,
        long recordCount
) {}
