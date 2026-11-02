package com.lorryowner.app.ui.owner

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MaintenanceModelTest {

    @Test
    fun dashboardViewModel_initialState_defaultsAreCorrect() {
        val viewModel = MaintenanceDashboardViewModel(sessionManager = null, autoLoad = false)
        assertEquals(0L, viewModel.uiState.totalVehicles)
        assertEquals(0L, viewModel.uiState.dueSoonCount)
        assertEquals(0L, viewModel.uiState.overdueCount)
        assertEquals(0L, viewModel.uiState.upToDateCount)
        assertEquals(0.0, viewModel.uiState.totalExpenditure, 0.001)
        assertTrue(viewModel.uiState.vehicles.isEmpty())
        assertFalse(viewModel.uiState.isLoading)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun dashboardViewModel_setTestData_updatesMetricsAndVehicles() {
        val viewModel = MaintenanceDashboardViewModel(sessionManager = null, autoLoad = false)
        val sampleVehicles = listOf(
            VehicleMaintenanceSummaryItem(
                lorryId = 1L,
                registrationNumber = "AP 02 AB 1234",
                makeModel = "Tata Commercial Vehicle",
                lastServiceDate = "2026-10-12",
                nextServiceDate = "2027-01-12",
                status = "Up to Date",
                totalCost = 16500.0,
                totalRecordsCount = 2
            ),
            VehicleMaintenanceSummaryItem(
                lorryId = 2L,
                registrationNumber = "AP 02 CD 5678",
                makeModel = "Ashok Leyland",
                lastServiceDate = "2026-08-05",
                nextServiceDate = "2026-11-05",
                status = "Due Soon",
                totalCost = 9000.0,
                totalRecordsCount = 1
            ),
            VehicleMaintenanceSummaryItem(
                lorryId = 3L,
                registrationNumber = "AP 02 EF 9012",
                makeModel = "Eicher Commercial Vehicle",
                lastServiceDate = "2026-05-15",
                nextServiceDate = "2026-10-15",
                status = "Overdue",
                totalCost = 14000.0,
                totalRecordsCount = 3
            )
        )

        viewModel.setTestData(
            MaintenanceDashboardUiState(
                totalVehicles = 5,
                dueSoonCount = 2,
                overdueCount = 1,
                upToDateCount = 2,
                totalExpenditure = 39500.0,
                vehicles = sampleVehicles
            )
        )

        assertEquals(5L, viewModel.uiState.totalVehicles)
        assertEquals(2L, viewModel.uiState.dueSoonCount)
        assertEquals(1L, viewModel.uiState.overdueCount)
        assertEquals(2L, viewModel.uiState.upToDateCount)
        assertEquals(39500.0, viewModel.uiState.totalExpenditure, 0.001)
        assertEquals(3, viewModel.uiState.vehicles.size)
        assertEquals("Up to Date", viewModel.uiState.vehicles[0].status)
        assertEquals("Due Soon", viewModel.uiState.vehicles[1].status)
        assertEquals("Overdue", viewModel.uiState.vehicles[2].status)
    }

    @Test
    fun vehicleDetailsViewModel_setTestData_populatesHistory() {
        val viewModel = VehicleMaintenanceDetailsViewModel(lorryId = 1L, sessionManager = null, autoLoad = false)
        val records = listOf(
            MaintenanceHistoryRecordItem(
                id = 1L,
                lorryId = 1L,
                serviceType = "Engine Oil Change",
                maintenanceDate = "2026-10-12",
                description = "Routine synthetic oil change and filter replacement",
                cost = 4500.0,
                odometerReadingKm = 45000,
                nextServiceDate = "2027-01-12",
                serviceCenter = "SRS Auto Center"
            ),
            MaintenanceHistoryRecordItem(
                id = 2L,
                lorryId = 1L,
                serviceType = "Tyre Replacement",
                maintenanceDate = "2026-08-10",
                description = "4 rear MRF tyres replaced",
                cost = 12000.0,
                odometerReadingKm = 40500,
                nextServiceDate = null,
                serviceCenter = "MRF Tyre Plaza"
            )
        )

        viewModel.setTestData(
            VehicleMaintenanceDetailsUiState(
                lorryId = 1L,
                registrationNumber = "AP 02 AB 1234",
                makeModel = "Tata Commercial Vehicle",
                lastServiceDate = "2026-10-12",
                nextServiceDate = "2027-01-12",
                status = "Up to Date",
                totalMaintenanceCost = 16500.0,
                records = records
            )
        )

        assertEquals("AP 02 AB 1234", viewModel.uiState.registrationNumber)
        assertEquals(16500.0, viewModel.uiState.totalMaintenanceCost, 0.001)
        assertEquals(2, viewModel.uiState.records.size)
        assertEquals("Engine Oil Change", viewModel.uiState.records[0].serviceType)
        assertEquals(4500.0, viewModel.uiState.records[0].cost, 0.001)
        assertEquals(45000, viewModel.uiState.records[0].odometerReadingKm)
    }

    @Test
    fun addMaintenanceViewModel_validate_rejectsMissingLorry() {
        val vm = AddMaintenanceViewModel(sessionManager = null, initialLorryId = null, autoLoad = false)
        vm.onServiceTypeChanged("Regular Service")
        vm.onServiceDateChanged("2026-10-28")
        vm.onDescriptionChanged("Test description")
        vm.onCostChanged("5000")
        vm.onOdometerChanged("40000")

        val error = vm.validate()
        assertNotNull(error)
        assertTrue(error!!.contains("select a lorry", ignoreCase = true))
    }

    @Test
    fun addMaintenanceViewModel_validate_rejectsBlankDescription() {
        val vm = AddMaintenanceViewModel(sessionManager = null, initialLorryId = 1L, autoLoad = false)
        vm.onServiceTypeChanged("Regular Service")
        vm.onServiceDateChanged("2026-10-28")
        vm.onDescriptionChanged("   ")
        vm.onCostChanged("5000")
        vm.onOdometerChanged("40000")

        val error = vm.validate()
        assertNotNull(error)
        assertTrue(error!!.contains("description", ignoreCase = true))
    }

    @Test
    fun addMaintenanceViewModel_validate_rejectsNegativeCost() {
        val vm = AddMaintenanceViewModel(sessionManager = null, initialLorryId = 1L, autoLoad = false)
        vm.onServiceTypeChanged("Regular Service")
        vm.onServiceDateChanged("2026-10-28")
        vm.onDescriptionChanged("Regular inspection")
        vm.onCostChanged("-150.0")
        vm.onOdometerChanged("40000")

        val error = vm.validate()
        assertNotNull(error)
        assertTrue(error!!.contains("cost", ignoreCase = true))
    }

    @Test
    fun addMaintenanceViewModel_validate_rejectsNegativeOdometer() {
        val vm = AddMaintenanceViewModel(sessionManager = null, initialLorryId = 1L, autoLoad = false)
        vm.onServiceTypeChanged("Regular Service")
        vm.onServiceDateChanged("2026-10-28")
        vm.onDescriptionChanged("Regular inspection")
        vm.onCostChanged("3500.0")
        vm.onOdometerChanged("-50")

        val error = vm.validate()
        assertNotNull(error)
        assertTrue(error!!.contains("odometer", ignoreCase = true))
    }

    @Test
    fun addMaintenanceViewModel_validate_rejectsNextServiceDateBeforeServiceDate() {
        val vm = AddMaintenanceViewModel(sessionManager = null, initialLorryId = 1L, autoLoad = false)
        vm.onServiceTypeChanged("Regular Service")
        vm.onServiceDateChanged("2026-10-28")
        vm.onDescriptionChanged("Regular inspection")
        vm.onCostChanged("3500.0")
        vm.onOdometerChanged("40000")
        vm.onNextServiceDateChanged("2026-10-20") // Prior to 2026-10-28

        val error = vm.validate()
        assertNotNull(error)
        assertTrue(error!!.contains("earlier than service date", ignoreCase = true))
    }

    @Test
    fun addMaintenanceViewModel_validate_passesForValidData() {
        val vm = AddMaintenanceViewModel(sessionManager = null, initialLorryId = 1L, autoLoad = false)
        vm.onServiceTypeChanged("Engine Repair")
        vm.onServiceDateChanged("2026-10-28")
        vm.onDescriptionChanged("Oil pump overhaul and testing")
        vm.onCostChanged("8500")
        vm.onOdometerChanged("45200")
        vm.onNextServiceDateChanged("2027-01-28")
        vm.onGarageChanged("SRS Auto Workshop")

        val error = vm.validate()
        assertNull(error)
    }

    @Test
    fun formattingHelper_formatDisplayDate_worksCorrectly() {
        assertEquals("12 Oct 2026", MaintenanceFormattingHelper.formatDisplayDate("2026-10-12"))
        assertEquals("05 Nov 2026", MaintenanceFormattingHelper.formatDisplayDate("2026-11-05"))
        assertEquals("15 Jan 2027", MaintenanceFormattingHelper.formatDisplayDate("2027-01-15"))
        assertEquals("N/A", MaintenanceFormattingHelper.formatDisplayDate(null))
        assertEquals("N/A", MaintenanceFormattingHelper.formatDisplayDate(""))
    }

    @Test
    fun formattingHelper_formatIndianRupee_worksCorrectly() {
        val formatted = MaintenanceFormattingHelper.formatIndianRupee(16500.0)
        assertTrue(formatted.contains("16,500") || formatted.contains("16500"))
        assertTrue(formatted.contains("₹"))
    }

    @Test
    fun dashboardViewModel_currentMonthExpense_populatesCorrectly() {
        val viewModel = MaintenanceDashboardViewModel(sessionManager = null, autoLoad = false)
        viewModel.setTestData(
            MaintenanceDashboardUiState(
                currentMonthExpense = 24500.0,
                currentMonth = "October",
                currentYear = 2026,
                vehicles = listOf(
                    VehicleMaintenanceSummaryItem(
                        lorryId = 1L,
                        registrationNumber = "AP 02 AB 1234",
                        makeModel = "Tata • 12 Wheeler",
                        lastServiceDate = "2026-10-18",
                        nextServiceDate = "2027-01-18",
                        status = "Up to Date",
                        totalCost = 24500.0,
                        totalRecordsCount = 8
                    )
                )
            )
        )

        assertEquals(24500.0, viewModel.uiState.currentMonthExpense, 0.001)
        assertEquals("October", viewModel.uiState.currentMonth)
        assertEquals(2026, viewModel.uiState.currentYear)
        assertEquals(1, viewModel.uiState.vehicles.size)
        assertEquals(8, viewModel.uiState.vehicles[0].totalRecordsCount)
    }

    @Test
    fun formattingHelper_parseDateBadge_parsesIsoDateCorrectly() {
        val badge = MaintenanceFormattingHelper.parseDateBadge("2026-10-18")
        assertEquals("18", badge.day)
        assertEquals("Oct", badge.month)
        assertEquals("2026", badge.year)

        val badgeSingleDigit = MaintenanceFormattingHelper.parseDateBadge("2026-09-05")
        assertEquals("05", badgeSingleDigit.day)
        assertEquals("Sep", badgeSingleDigit.month)
        assertEquals("2026", badgeSingleDigit.year)
    }

    @Test
    fun formattingHelper_parseDateBadge_handlesFallbackForInvalidDate() {
        val badgeNull = MaintenanceFormattingHelper.parseDateBadge(null)
        assertEquals("--", badgeNull.day)
        assertEquals("---", badgeNull.month)

        val badgeEmpty = MaintenanceFormattingHelper.parseDateBadge("")
        assertEquals("--", badgeEmpty.day)
        assertEquals("---", badgeEmpty.month)
    }
}
