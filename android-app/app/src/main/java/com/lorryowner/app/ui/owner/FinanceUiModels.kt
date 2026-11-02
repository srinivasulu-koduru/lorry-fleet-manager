package com.lorryowner.app.ui.owner

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MonthlySummaryItem(
    val month: Int,
    val monthName: String,
    val income: Double,
    val expense: Double,
    val profit: Double
)

data class FinanceQuickInsightsItem(
    val profitMargin: Double = 0.0,
    val thisMonthProfit: Double = 0.0,
    val ytdIncome: Double = 0.0,
    val ytdExpense: Double = 0.0,
    val ytdProfit: Double = 0.0,
    val bestPerformingMonth: String = "Jan",
    val bestPerformingMonthProfit: Double = 0.0,
    val highestExpenseMonth: String = "Jan",
    val highestExpenseMonthAmount: Double = 0.0,
    val totalIncomeYtd: Double = ytdIncome
)

data class FinanceDashboardUiState(
    val selectedYear: Int = LocalDate.now().year,
    val availableYears: List<Int> = listOf(LocalDate.now().year),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val profitMargin: Double = 0.0,
    val currentMonth: String = "",
    val currentMonthNumber: Int = LocalDate.now().monthValue,
    val currentMonthIncome: Double = 0.0,
    val currentMonthExpense: Double = 0.0,
    val currentMonthProfit: Double = 0.0,
    val monthlySummaries: List<MonthlySummaryItem> = emptyList(),
    val quickInsights: FinanceQuickInsightsItem = FinanceQuickInsightsItem(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val currentMonthName: String
        get() = if (currentMonth.isNotBlank()) currentMonth else FinanceFormattingHelper.getMonthName(currentMonthNumber)
}

data class FinanceTransactionItem(
    val id: Long = 0L,
    val lorryId: Long? = null,
    val lorryRegistration: String? = null,
    val tripId: Long? = null,
    val tripReference: String? = null,
    val transactionType: String = "EXPENSE", // "INCOME" or "EXPENSE"
    val category: String = "OTHER",         // "FREIGHT", "DIESEL", "MAINTENANCE", "TOLL", etc.
    val description: String = "",
    val amount: Double = 0.0,
    val transactionDate: String = "",
    val paymentStatus: String = "RECEIVED",
    val paymentMode: String? = null,
    val referenceNumber: String? = null,
    val notes: String = description
)

data class MonthlyFinanceDetailsUiState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val monthName: String = "",
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val profitMargin: Double = 0.0,
    val incomeTransactions: List<FinanceTransactionItem> = emptyList(),
    val expenseTransactions: List<FinanceTransactionItem> = emptyList(),
    val expenseCategories: Map<String, Double> = emptyMap(),
    val incomeCategories: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val totalExpense: Double get() = totalExpenses
    val transactions: List<FinanceTransactionItem> get() = incomeTransactions + expenseTransactions
    val displayMonthName: String get() = if (monthName.isNotBlank()) monthName else FinanceFormattingHelper.getMonthName(month)

    constructor(
        year: Int,
        month: Int,
        monthName: String,
        totalIncome: Double,
        totalExpense: Double,
        netProfit: Double,
        transactions: List<FinanceTransactionItem>
    ) : this(
        year = year,
        month = month,
        monthName = monthName,
        totalIncome = totalIncome,
        totalExpenses = totalExpense,
        netProfit = netProfit,
        profitMargin = if (totalIncome > 0) Math.round(((totalIncome - totalExpense) / totalIncome) * 1000.0) / 10.0 else 0.0,
        incomeTransactions = transactions.filter { it.transactionType.equals("INCOME", ignoreCase = true) },
        expenseTransactions = transactions.filter { it.transactionType.equals("EXPENSE", ignoreCase = true) }
    )
}

typealias MonthlySummaryUiItem = MonthlySummaryItem
typealias FinanceQuickInsightsUiState = FinanceQuickInsightsItem
typealias FinanceTransactionUiItem = FinanceTransactionItem

object FinanceFormattingHelper {

    /**
     * Formats amounts with standard Indian Lakhs / Crores comma separation
     * (e.g. ₹1,25,000, ₹75,000, ₹50,000, ₹4,25,000) matching the reference UI mockup.
     */
    fun formatIndianRupee(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        val absAmount = Math.abs(Math.round(amount))
        val str = absAmount.toString()
        if (str.length <= 3) {
            return "${sign}₹$str"
        }
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i > 0) {
                sb.append(',')
                count = 0
            }
        }
        val formattedRemaining = sb.reverse().toString()
        return "${sign}₹$formattedRemaining,$lastThree"
    }

    /**
     * Formats amounts into compact Indian financial labels (e.g. ₹50K, ₹1L, ₹1.5L, ₹2L)
     * suitable for chart Y-axis and graph scales.
     */
    fun formatShortRupee(amount: Double): String {
        val abs = Math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 10_000_000 -> {
                val cr = abs / 10_000_000.0
                if (cr % 1.0 == 0.0) "${sign}₹${cr.toInt()}Cr" else "${sign}₹${String.format(Locale.US, "%.1f", cr)}Cr"
            }
            abs >= 100_000 -> {
                val lakh = abs / 100_000.0
                if (lakh % 1.0 == 0.0) "${sign}₹${lakh.toInt()}L" else "${sign}₹${String.format(Locale.US, "%.1f", lakh)}L"
            }
            abs >= 1_000 -> {
                val k = abs / 1_000.0
                if (k % 1.0 == 0.0) "${sign}₹${k.toInt()}K" else "${sign}₹${String.format(Locale.US, "%.1f", k)}K"
            }
            else -> "${sign}₹${abs.toInt()}"
        }
    }

    fun formatDisplayDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "N/A"
        return try {
            val parsed = LocalDate.parse(rawDate.trim())
            parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        } catch (e: Exception) {
            rawDate
        }
    }

    fun formatDateDisplay(rawDate: String?): String = formatDisplayDate(rawDate)

    fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
            5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
            9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
            else -> "Month $month"
        }
    }

    fun getMonthAbbreviation(month: Int): String {
        return when (month) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
            5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
            9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> "M$month"
        }
    }
}

object FinanceCategoryHelper {

    fun getCategoryIcon(category: String): String {
        return when (category.uppercase()) {
            "FREIGHT" -> "🚚"
            "MAINTENANCE" -> "🔧"
            "DIESEL", "FUEL" -> "⛽"
            "TOLL" -> "🛣️"
            "DRIVER_SALARY", "SALARY" -> "👨‍✈️"
            else -> "💼"
        }
    }

    fun getCategoryDisplayName(category: String): String {
        return when (category.uppercase()) {
            "FREIGHT" -> "Freight"
            "MAINTENANCE" -> "Maintenance"
            "DIESEL" -> "Diesel"
            "FUEL" -> "Fuel"
            "TOLL" -> "Toll"
            "DRIVER_SALARY" -> "Driver Salary"
            else -> category.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
