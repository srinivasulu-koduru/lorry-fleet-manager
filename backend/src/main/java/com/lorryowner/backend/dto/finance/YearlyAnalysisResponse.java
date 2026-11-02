package com.lorryowner.backend.dto.finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record YearlyAnalysisResponse(
        int year,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netProfit,
        double profitMargin,
        List<MonthlySummaryDto> monthlySummaries,
        Map<String, BigDecimal> expenseCategoryBreakdown,
        Map<String, BigDecimal> incomeCategoryBreakdown,
        FinanceQuickInsightsDto insights
) {}
