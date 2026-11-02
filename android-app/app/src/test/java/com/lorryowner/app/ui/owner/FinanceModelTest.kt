package com.lorryowner.app.ui.owner

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FinanceModelTest {

    @Test
    fun financeDashboardViewModel_initialState_defaultsAreCorrect() {
        val viewModel = FinanceDashboardViewModel(sessionManager = null, autoLoad = false)
        val state = viewModel.uiState

        assertEquals(LocalDate.now().year, state.selectedYear)
        assertEquals(0.0, state.currentMonthIncome, 0.001)
        assertEquals(0.0, state.currentMonthExpense, 0.001)
        assertEquals(0.0, state.currentMonthProfit, 0.001)
        assertEquals(0, state.monthlySummaries.size)
        assertEquals(0.0, state.quickInsights.profitMargin, 0.001)
        assertEquals(0.0, state.quickInsights.thisMonthProfit, 0.001)
        assertEquals(0.0, state.quickInsights.totalIncomeYtd, 0.001)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun financeDashboardViewModel_setTestData_updatesMetricsProperly() {
        val viewModel = FinanceDashboardViewModel(sessionManager = null, autoLoad = false)

        val customSummaries = (1..12).map { m ->
            MonthlySummaryUiItem(
                month = m,
                monthName = FinanceFormattingHelper.getMonthAbbreviation(m),
                income = if (m == 10) 125000.0 else 0.0,
                expense = if (m == 10) 75000.0 else 0.0,
                profit = if (m == 10) 50000.0 else 0.0
            )
        }

        viewModel.setTestData(
            FinanceDashboardUiState(
                selectedYear = 2026,
                currentMonth = "October",
                currentMonthExpense = 75000.0,
                currentMonthIncome = 125000.0,
                currentMonthProfit = 50000.0,
                monthlySummaries = customSummaries,
                quickInsights = FinanceQuickInsightsUiState(
                    profitMargin = 66.7,
                    thisMonthProfit = 50000.0,
                    totalIncomeYtd = 425000.0
                )
            )
        )

        val updated = viewModel.uiState
        assertEquals(2026, updated.selectedYear)
        assertEquals("October", updated.currentMonthName)
        assertEquals(125000.0, updated.currentMonthIncome, 0.001)
        assertEquals(75000.0, updated.currentMonthExpense, 0.001)
        assertEquals(50000.0, updated.currentMonthProfit, 0.001)
        assertEquals(66.7, updated.quickInsights.profitMargin, 0.001)
        assertEquals(50000.0, updated.quickInsights.thisMonthProfit, 0.001)
        assertEquals(425000.0, updated.quickInsights.totalIncomeYtd, 0.001)
        assertEquals(125000.0, updated.monthlySummaries[9].income, 0.001)
    }

    @Test
    fun monthlyFinanceDetailsViewModel_initialState_defaultsAreCorrect() {
        val viewModel = MonthlyFinanceDetailsViewModel(initialYear = 2026, initialMonth = 10, sessionManager = null, autoLoad = false)
        val state = viewModel.uiState

        assertEquals(2026, state.year)
        assertEquals(10, state.month)
        assertEquals("October", state.displayMonthName)
        assertEquals(0.0, state.totalIncome, 0.001)
        assertEquals(0.0, state.totalExpense, 0.001)
        assertEquals(0.0, state.netProfit, 0.001)
        assertTrue(state.transactions.isEmpty())
        assertTrue(state.incomeTransactions.isEmpty())
        assertTrue(state.expenseTransactions.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun monthlyFinanceDetailsViewModel_setTestData_separatesIncomeAndExpense() {
        val viewModel = MonthlyFinanceDetailsViewModel(initialYear = 2026, initialMonth = 10, sessionManager = null, autoLoad = false)

        val testTransactions = listOf(
            FinanceTransactionUiItem(
                id = 101L,
                transactionType = "INCOME",
                category = "FREIGHT",
                amount = 125000.0,
                transactionDate = "2026-10-15",
                lorryRegistration = "AP 02 AB 1234",
                paymentStatus = "RECEIVED",
                notes = "Freight payment from Hyderabad to Bangalore"
            ),
            FinanceTransactionUiItem(
                id = 102L,
                transactionType = "EXPENSE",
                category = "MAINTENANCE",
                amount = 25000.0,
                transactionDate = "2026-10-18",
                lorryRegistration = "AP 02 AB 1234",
                paymentStatus = "PAID",
                notes = "Engine maintenance"
            ),
            FinanceTransactionUiItem(
                id = 103L,
                transactionType = "EXPENSE",
                category = "DIESEL",
                amount = 50000.0,
                transactionDate = "2026-10-20",
                lorryRegistration = "AP 02 CD 5678",
                paymentStatus = "PAID",
                notes = "Diesel fuel recharge"
            )
        )

        viewModel.setTestData(
            MonthlyFinanceDetailsUiState(
                year = 2026,
                month = 10,
                monthName = "October",
                totalIncome = 125000.0,
                totalExpense = 75000.0,
                netProfit = 50000.0,
                transactions = testTransactions
            )
        )

        val state = viewModel.uiState
        assertEquals(125000.0, state.totalIncome, 0.001)
        assertEquals(75000.0, state.totalExpense, 0.001)
        assertEquals(50000.0, state.netProfit, 0.001)
        assertEquals(3, state.transactions.size)
        assertEquals(1, state.incomeTransactions.size)
        assertEquals(2, state.expenseTransactions.size)
        assertEquals("FREIGHT", state.incomeTransactions[0].category)
        assertEquals(125000.0, state.incomeTransactions[0].amount, 0.001)
    }

    @Test
    fun monthlyFinanceDetailsViewModel_selectMonthAndYear_updatesActiveSelection() {
        val viewModel = MonthlyFinanceDetailsViewModel(initialYear = 2026, initialMonth = 10, sessionManager = null, autoLoad = false)

        viewModel.selectMonth(5)
        assertEquals(5, viewModel.uiState.month)
        assertEquals("May", viewModel.uiState.monthName)

        viewModel.selectYear(2025)
        assertEquals(2025, viewModel.uiState.year)
    }

    @Test
    fun financeFormattingHelper_formatIndianRupee_correctIndianCommaSeparation() {
        assertEquals("₹0", FinanceFormattingHelper.formatIndianRupee(0.0))
        assertEquals("₹500", FinanceFormattingHelper.formatIndianRupee(500.0))
        assertEquals("₹1,000", FinanceFormattingHelper.formatIndianRupee(1000.0))
        assertEquals("₹10,000", FinanceFormattingHelper.formatIndianRupee(10000.0))
        assertEquals("₹50,000", FinanceFormattingHelper.formatIndianRupee(50000.0))
        assertEquals("₹75,000", FinanceFormattingHelper.formatIndianRupee(75000.0))
        assertEquals("₹1,25,000", FinanceFormattingHelper.formatIndianRupee(125000.0))
        assertEquals("₹4,25,000", FinanceFormattingHelper.formatIndianRupee(425000.0))
        assertEquals("₹10,50,000", FinanceFormattingHelper.formatIndianRupee(1050000.0))
        assertEquals("-₹15,000", FinanceFormattingHelper.formatIndianRupee(-15000.0))
    }

    @Test
    fun financeFormattingHelper_formatShortRupee_scaleSuffixes() {
        assertEquals("₹0", FinanceFormattingHelper.formatShortRupee(0.0))
        assertEquals("₹500", FinanceFormattingHelper.formatShortRupee(500.0))
        assertEquals("₹1K", FinanceFormattingHelper.formatShortRupee(1000.0))
        assertEquals("₹50K", FinanceFormattingHelper.formatShortRupee(50000.0))
        assertEquals("₹1L", FinanceFormattingHelper.formatShortRupee(100000.0))
        assertEquals("₹1.5L", FinanceFormattingHelper.formatShortRupee(150000.0))
        assertEquals("₹2L", FinanceFormattingHelper.formatShortRupee(200000.0))
        assertEquals("₹10L", FinanceFormattingHelper.formatShortRupee(1000000.0))
        assertEquals("₹1Cr", FinanceFormattingHelper.formatShortRupee(10000000.0))
    }

    @Test
    fun financeFormattingHelper_getMonthNameAndAbbreviation_allMonthsMatch() {
        val expectedFull = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val expectedAbbr = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        for (m in 1..12) {
            assertEquals(expectedFull[m - 1], FinanceFormattingHelper.getMonthName(m))
            assertEquals(expectedAbbr[m - 1], FinanceFormattingHelper.getMonthAbbreviation(m))
        }
    }

    @Test
    fun financeFormattingHelper_formatDateDisplay_convertsIsoToFriendlyDate() {
        assertEquals("15 Oct 2026", FinanceFormattingHelper.formatDateDisplay("2026-10-15"))
        assertEquals("01 Jan 2026", FinanceFormattingHelper.formatDateDisplay("2026-01-01"))
        assertEquals("2026-invalid", FinanceFormattingHelper.formatDateDisplay("2026-invalid"))
    }

    @Test
    fun financeCategoryHelper_categoryMetadata_resolvesAccurately() {
        assertEquals("🚚", FinanceCategoryHelper.getCategoryIcon("FREIGHT"))
        assertEquals("🔧", FinanceCategoryHelper.getCategoryIcon("MAINTENANCE"))
        assertEquals("⛽", FinanceCategoryHelper.getCategoryIcon("DIESEL"))
        assertEquals("🛣️", FinanceCategoryHelper.getCategoryIcon("TOLL"))
        assertEquals("👨‍✈️", FinanceCategoryHelper.getCategoryIcon("DRIVER_SALARY"))
        assertEquals("💼", FinanceCategoryHelper.getCategoryIcon("OTHER"))

        assertEquals("Freight", FinanceCategoryHelper.getCategoryDisplayName("FREIGHT"))
        assertEquals("Maintenance", FinanceCategoryHelper.getCategoryDisplayName("MAINTENANCE"))
        assertEquals("Diesel", FinanceCategoryHelper.getCategoryDisplayName("DIESEL"))
        assertEquals("Toll", FinanceCategoryHelper.getCategoryDisplayName("TOLL"))
        assertEquals("Driver Salary", FinanceCategoryHelper.getCategoryDisplayName("DRIVER_SALARY"))
    }
}
