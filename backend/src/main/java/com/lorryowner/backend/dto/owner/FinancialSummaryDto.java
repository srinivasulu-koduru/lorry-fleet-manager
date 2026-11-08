package com.lorryowner.backend.dto.owner;

import java.math.BigDecimal;

public record FinancialSummaryDto(
    String monthLabel,
    boolean isSample,
    BigDecimal income,
    BigDecimal expenses,
    BigDecimal profit,
    String currencySymbol,
    Long totalTrips
) {
    public FinancialSummaryDto(
            String monthLabel,
            boolean isSample,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal profit,
            String currencySymbol
    ) {
        this(monthLabel, isSample, income, expenses, profit, currencySymbol, 0L);
    }
}
