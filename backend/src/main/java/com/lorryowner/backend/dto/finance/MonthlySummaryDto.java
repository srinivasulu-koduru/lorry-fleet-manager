package com.lorryowner.backend.dto.finance;

import java.math.BigDecimal;

public record MonthlySummaryDto(
        int month,
        String monthName,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal profit
) {}
