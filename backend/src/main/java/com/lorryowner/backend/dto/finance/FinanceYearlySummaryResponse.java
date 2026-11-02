package com.lorryowner.backend.dto.finance;

import java.math.BigDecimal;
import java.util.List;

public record FinanceYearlySummaryResponse(
        int year,
        List<Integer> availableYears,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netProfit,
        double profitMargin,
        String currentMonth,
        BigDecimal currentMonthIncome,
        BigDecimal currentMonthExpense,
        BigDecimal currentMonthProfit,
        List<MonthlySummaryDto> monthlySummaries,
        FinanceQuickInsightsDto quickInsights
) {}
