package com.lorryowner.app.ui.owner

import org.junit.Assert.*
import org.junit.Test

class OwnerTripHistoryModelTest {

    private fun sampleTrips(): List<OwnerTripItemUiState> {
        return listOf(
            OwnerTripItemUiState(
                id = 101L,
                tripReference = "TRIP-2026-001",
                driverId = 11L,
                driverName = "Ramesh Kumar",
                lorryId = 21L,
                lorryRegistration = "AP 21 TX 1234",
                lorryMakeModel = "Tata Prima 4028",
                originLocation = "Hyderabad",
                destinationLocation = "Bangalore",
                tripStatus = "IN_PROGRESS",
                tripDate = "2026-11-20",
                startTime = "08:30",
                endTime = null,
                distanceKm = 570.0,
                deliveryNotes = "Heavy Machinery",
                tripAmount = 45000.0
            ),
            OwnerTripItemUiState(
                id = 102L,
                tripReference = "TRIP-2026-002",
                driverId = 12L,
                driverName = "Suresh Patel",
                lorryId = 22L,
                lorryRegistration = "KA 04 MM 5678",
                lorryMakeModel = "Ashok Leyland 2820",
                originLocation = "Chennai",
                destinationLocation = "Hyderabad",
                tripStatus = "COMPLETED",
                tripDate = "2026-11-18",
                startTime = "06:00",
                endTime = "21:00",
                distanceKm = 630.0,
                deliveryNotes = "Auto Parts",
                tripAmount = 52000.0
            ),
            OwnerTripItemUiState(
                id = 103L,
                tripReference = "TRIP-2026-003",
                driverId = 13L,
                driverName = "Vijay Singh",
                lorryId = 23L,
                lorryRegistration = "TS 09 XY 9012",
                lorryMakeModel = "BharatBenz 3528",
                originLocation = "Mumbai",
                destinationLocation = "Pune",
                tripStatus = "CANCELLED",
                tripDate = "2026-11-15",
                startTime = null,
                endTime = null,
                distanceKm = 150.0,
                deliveryNotes = "Client Cancelled",
                tripAmount = 18000.0
            ),
            OwnerTripItemUiState(
                id = 104L,
                tripReference = "TRIP-2026-004",
                driverId = 11L,
                driverName = "Ramesh Kumar",
                lorryId = 21L,
                lorryRegistration = "AP 21 TX 1234",
                lorryMakeModel = "Tata Prima 4028",
                originLocation = "Vijayawada",
                destinationLocation = "Visakhapatnam",
                tripStatus = "COMPLETED",
                tripDate = "2026-11-10",
                startTime = "07:00",
                endTime = "16:00",
                distanceKm = 350.0,
                deliveryNotes = "Steel Pipes",
                tripAmount = 38000.0
            )
        )
    }

    @Test
    fun testInitialViewModelState() {
        val viewModel = OwnerTripHistoryViewModel(sessionManager = null, autoLoad = false)
        val state = viewModel.uiState

        assertTrue(state.trips.isEmpty())
        assertTrue(state.filteredTrips.isEmpty())
        assertEquals("ALL", state.selectedFilter)
        assertEquals("", state.searchQuery)
        assertEquals(0L, state.totalElements)
        assertEquals(0, state.currentPage)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
    }

    @Test
    fun testStatusFilterLogic_all() {
        val viewModel = OwnerTripHistoryViewModel(sessionManager = null, autoLoad = false)
        val trips = sampleTrips()
        viewModel.setTestData(
            OwnerTripHistoryUiState(
                trips = trips,
                filteredTrips = trips,
                selectedFilter = "ALL"
            )
        )

        assertEquals(4, viewModel.uiState.filteredTrips.size)
    }

    @Test
    fun testStatusFilterLogic_inProgress() {
        val viewModel = OwnerTripHistoryViewModel(sessionManager = null, autoLoad = false)
        val trips = sampleTrips()
        val filtered = trips.filter { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }
        viewModel.setTestData(
            OwnerTripHistoryUiState(
                trips = trips,
                filteredTrips = filtered,
                selectedFilter = "IN_PROGRESS"
            )
        )

        assertEquals(1, viewModel.uiState.filteredTrips.size)
        assertEquals("TRIP-2026-001", viewModel.uiState.filteredTrips[0].tripReference)
        assertEquals("IN_PROGRESS", viewModel.uiState.filteredTrips[0].tripStatus)
    }

    @Test
    fun testStatusFilterLogic_completed() {
        val viewModel = OwnerTripHistoryViewModel(sessionManager = null, autoLoad = false)
        val trips = sampleTrips()
        val filtered = trips.filter { it.tripStatus.equals("COMPLETED", ignoreCase = true) }
        viewModel.setTestData(
            OwnerTripHistoryUiState(
                trips = trips,
                filteredTrips = filtered,
                selectedFilter = "COMPLETED"
            )
        )

        assertEquals(2, viewModel.uiState.filteredTrips.size)
        assertTrue(viewModel.uiState.filteredTrips.all { it.tripStatus == "COMPLETED" })
    }

    @Test
    fun testStatusFilterLogic_cancelled() {
        val viewModel = OwnerTripHistoryViewModel(sessionManager = null, autoLoad = false)
        val trips = sampleTrips()
        val filtered = trips.filter { it.tripStatus.equals("CANCELLED", ignoreCase = true) }
        viewModel.setTestData(
            OwnerTripHistoryUiState(
                trips = trips,
                filteredTrips = filtered,
                selectedFilter = "CANCELLED"
            )
        )

        assertEquals(1, viewModel.uiState.filteredTrips.size)
        assertEquals("TRIP-2026-003", viewModel.uiState.filteredTrips[0].tripReference)
    }

    @Test
    fun testSearchQueryFilter_byLorryRegistration() {
        val viewModel = OwnerTripHistoryViewModel(sessionManager = null, autoLoad = false)
        val trips = sampleTrips()
        viewModel.setTestData(
            OwnerTripHistoryUiState(
                trips = trips,
                filteredTrips = trips
            )
        )

        viewModel.onSearchQueryChanged("AP 21")
        assertEquals(2, viewModel.uiState.filteredTrips.size)
        assertTrue(viewModel.uiState.filteredTrips.all { it.lorryRegistration.contains("AP 21") })
    }

    @Test
    fun testSearchQueryFilter_byDriverName() {
        val viewModel = OwnerTripHistoryViewModel(sessionManager = null, autoLoad = false)
        val trips = sampleTrips()
        viewModel.setTestData(
            OwnerTripHistoryUiState(
                trips = trips,
                filteredTrips = trips
            )
        )

        viewModel.onSearchQueryChanged("suresh")
        assertEquals(1, viewModel.uiState.filteredTrips.size)
        assertEquals("Suresh Patel", viewModel.uiState.filteredTrips[0].driverName)
    }

    @Test
    fun testSearchQueryFilter_byRoute() {
        val viewModel = OwnerTripHistoryViewModel(sessionManager = null, autoLoad = false)
        val trips = sampleTrips()
        viewModel.setTestData(
            OwnerTripHistoryUiState(
                trips = trips,
                filteredTrips = trips
            )
        )

        viewModel.onSearchQueryChanged("Bangalore")
        assertEquals(1, viewModel.uiState.filteredTrips.size)
        assertEquals("Bangalore", viewModel.uiState.filteredTrips[0].destinationLocation)
    }

    @Test
    fun testTripDetailsFinancialCalculations() {
        val revenue = 50000.0
        val maintenance = 4500.0
        val fuel = 12000.0
        val driver = 3000.0
        val other = 1500.0

        val totalExpenses = maintenance + fuel + driver + other
        val netProfit = revenue - totalExpenses

        val state = OwnerTripDetailsUiState(
            id = 101L,
            tripReference = "TRIP-2026-001",
            lorryRegistration = "AP 21 TX 1234",
            driverName = "Ramesh Kumar",
            origin = "Hyderabad",
            destination = "Bangalore",
            status = "IN_PROGRESS",
            tripRevenue = revenue,
            maintenanceExpenses = maintenance,
            fuelExpenses = fuel,
            driverExpenses = driver,
            otherExpenses = other,
            totalExpenses = totalExpenses,
            netProfit = netProfit
        )

        assertEquals(21000.0, state.totalExpenses, 0.001)
        assertEquals(29000.0, state.netProfit, 0.001)
        assertTrue(state.netProfit > 0)
    }

    @Test
    fun testMaintenanceRecordItemWithTripReference() {
        val itemWithTrip = MaintenanceHistoryRecordItem(
            id = 501L,
            lorryId = 21L,
            serviceType = "Oil Change",
            maintenanceDate = "2026-11-20",
            description = "Routine maintenance during transit",
            cost = 3500.0,
            odometerReadingKm = 85200,
            nextServiceDate = "2027-02-20",
            serviceCenter = "Highway Garage",
            status = "Completed",
            tripId = 101L,
            tripReference = "TRIP-2026-001"
        )

        assertNotNull(itemWithTrip.tripId)
        assertEquals(101L, itemWithTrip.tripId)
        assertEquals("TRIP-2026-001", itemWithTrip.tripReference)

        val itemWithoutTrip = MaintenanceHistoryRecordItem(
            id = 502L,
            lorryId = 21L,
            serviceType = "Tyre Rotation",
            maintenanceDate = "2026-11-01",
            description = "Depot inspection",
            cost = 2000.0,
            odometerReadingKm = 80000,
            nextServiceDate = "2027-01-01",
            serviceCenter = "BHP Depot",
            status = "Completed",
            tripId = null,
            tripReference = null
        )

        assertNull(itemWithoutTrip.tripId)
        assertNull(itemWithoutTrip.tripReference)
    }
}
