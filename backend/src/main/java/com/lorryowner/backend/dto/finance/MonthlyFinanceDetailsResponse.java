package com.lorryowner.backend.dto.finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MonthlyFinanceDetailsResponse(
        int year,
        int month,
        String monthName,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netProfit,
        double profitMargin,
        List<FinanceTransactionDto> incomeTransactions,
        List<FinanceTransactionDto> expenseTransactions,
        Map<String, BigDecimal> expenseCategoryBreakdown,
        Map<String, BigDecimal> incomeCategoryBreakdown
) {}
