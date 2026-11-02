package com.lorryowner.backend.dto.finance;

import java.math.BigDecimal;

public record FinanceQuickInsightsDto(
        double profitMargin,
        BigDecimal thisMonthProfit,
        BigDecimal ytdIncome,
        BigDecimal ytdExpense,
        BigDecimal ytdProfit,
        String bestPerformingMonth,
        BigDecimal bestPerformingMonthProfit,
        String highestExpenseMonth,
        BigDecimal highestExpenseMonthAmount
) {}
