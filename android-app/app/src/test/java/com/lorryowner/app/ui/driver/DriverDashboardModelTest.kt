package com.lorryowner.app.ui.driver

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class DriverDashboardModelTest {

    private lateinit var viewModel: DriverDashboardViewModel

    private val sampleTrips = listOf(
        DriverTripUiItem(
            id = 1L,
            tripReference = "TRIP-2026-1001",
            driverName = "Ramesh Kumar",
            lorryRegistration = "AP 31 TC 1234",
            originLocation = "Visakhapatnam",
            destinationLocation = "Vijayawada",
            tripStatus = "COMPLETED",
            tripDate = "2026-10-20"
        ),
        DriverTripUiItem(
            id = 2L,
            tripReference = "TRIP-2026-1002",
            driverName = "Ramesh Kumar",
            lorryRegistration = "TS 09 UA 9012",
            originLocation = "Vijayawada",
            destinationLocation = "Hyderabad",
            tripStatus = "COMPLETED",
            tripDate = "2026-10-22"
        ),
        DriverTripUiItem(
            id = 3L,
            tripReference = "TRIP-2026-1003",
            driverName = "Ramesh Kumar",
            lorryRegistration = "AP 31 TC 1234",
            originLocation = "Hyderabad",
            destinationLocation = "Nellore",
            tripStatus = "IN_PROGRESS",
            tripDate = "2026-10-27"
        ),
        DriverTripUiItem(
            id = 4L,
            tripReference = "TRIP-2026-1004",
            driverName = "Ramesh Kumar",
            lorryRegistration = "AP 31 TC 1234",
            originLocation = "Hyderabad",
            destinationLocation = "Bengaluru",
            tripStatus = "PLANNED",
            tripDate = "2026-10-28"
        )
    )

    @Before
    fun setUp() {
        // Auto-load disabled (sessionManager = null) for deterministic JVM test isolation
        viewModel = DriverDashboardViewModel(sessionManager = null, autoLoad = false)
        viewModel.setTestData(
            dashboard = DriverDashboardUiState(
                driverName = "Ramesh Kumar",
                driverUsername = "driver_ramesh",
                assignedLorryRegistration = "AP 31 TC 1234",
                currentTripsCount = 1,
                upcomingTripsCount = 1,
                completedTripsCount = 2,
                todayTrip = sampleTrips[2] // IN_PROGRESS trip
            ),
            trips = sampleTrips,
            profile = DriverProfileUiState(
                fullName = "Ramesh Kumar",
                username = "driver_ramesh",
                assignedLorryRegistration = "AP 31 TC 1234",
                licenseNumber = "DL-1420110012345"
            )
        )
    }

    @Test
    fun initialState_hasCorrectDefaults() {
        assertEquals("Ramesh Kumar", viewModel.dashboardState.driverName)
        assertEquals("driver_ramesh", viewModel.dashboardState.driverUsername)
        assertEquals(DriverNavDestination.HOME, viewModel.currentNavDestination)
        assertEquals(DriverTripsTab.CURRENT, viewModel.selectedTripsTab)
        assertNotNull(viewModel.dashboardState.todayTrip)
        assertEquals("TRIP-2026-1003", viewModel.dashboardState.todayTrip?.tripReference)
    }

    @Test
    fun filterTrips_returnsCurrentInProgressTrips() {
        viewModel.selectTripsTab(DriverTripsTab.CURRENT)
        val filtered = viewModel.getFilteredTrips()

        assertEquals(1, filtered.size)
        assertEquals("TRIP-2026-1003", filtered[0].tripReference)
        assertEquals("IN_PROGRESS", filtered[0].tripStatus)
    }

    @Test
    fun filterTrips_returnsUpcomingPlannedTrips() {
        viewModel.selectTripsTab(DriverTripsTab.UPCOMING)
        val filtered = viewModel.getFilteredTrips()

        assertEquals(1, filtered.size)
        assertEquals("TRIP-2026-1004", filtered[0].tripReference)
        assertEquals("PLANNED", filtered[0].tripStatus)
    }

    @Test
    fun filterTrips_returnsCompletedTrips() {
        viewModel.selectTripsTab(DriverTripsTab.COMPLETED)
        val filtered = viewModel.getFilteredTrips()

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.tripStatus == "COMPLETED" })
        assertEquals("TRIP-2026-1001", filtered[0].tripReference)
        assertEquals("TRIP-2026-1002", filtered[1].tripReference)
    }

    @Test
    fun filterTrips_emptyWhenNoMatchingStatus() {
        val noPlannedTrips = sampleTrips.filter { it.tripStatus != "PLANNED" }
        viewModel.setTestData(viewModel.dashboardState, noPlannedTrips, viewModel.profileState)

        viewModel.selectTripsTab(DriverTripsTab.UPCOMING)
        val filtered = viewModel.getFilteredTrips()

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun navigation_destinationSelectionWorks() {
        viewModel.selectNavDestination(DriverNavDestination.TRIPS)
        assertEquals(DriverNavDestination.TRIPS, viewModel.currentNavDestination)

        viewModel.selectNavDestination(DriverNavDestination.PROFILE)
        assertEquals(DriverNavDestination.PROFILE, viewModel.currentNavDestination)

        viewModel.selectNavDestination(DriverNavDestination.STATUS)
        assertEquals(DriverNavDestination.STATUS, viewModel.currentNavDestination)
        // Defaults to today's trip or in-progress trip when opening status
        assertNotNull(viewModel.selectedTripForDetails)
    }

    @Test
    fun selectTrip_updatesSelectedTripForDetails() {
        val tripToView = sampleTrips[0]
        viewModel.selectTrip(tripToView)

        assertEquals("TRIP-2026-1001", viewModel.selectedTripForDetails?.tripReference)
        assertEquals("Visakhapatnam", viewModel.selectedTripForDetails?.originLocation)
    }

    @Test
    fun updateGreeting_updatesDynamicallyBasedOnTime() {
        viewModel.updateGreeting(LocalTime.of(8, 0))
        assertTrue(viewModel.dashboardState.greeting.contains("Morning", ignoreCase = true))

        viewModel.updateGreeting(LocalTime.of(14, 0))
        assertTrue(viewModel.dashboardState.greeting.contains("Afternoon", ignoreCase = true))

        viewModel.updateGreeting(LocalTime.of(19, 0))
        assertTrue(viewModel.dashboardState.greeting.contains("Evening", ignoreCase = true))
    }

    @Test
    fun profileState_holdsCorrectValues() {
        assertEquals("Ramesh Kumar", viewModel.profileState.fullName)
        assertEquals("driver_ramesh", viewModel.profileState.username)
        assertEquals("AP 31 TC 1234", viewModel.profileState.assignedLorryRegistration)
        assertEquals("DL-1420110012345", viewModel.profileState.licenseNumber)
        assertTrue(viewModel.profileState.isOnline)
    }

    @Test
    fun updateTripInState_startTrip_updatesStatusToInProgressAndRecalculatesCounts() {
        // Given planned trip (id=4)
        val plannedTrip = sampleTrips.first { it.id == 4L }
        assertEquals("PLANNED", plannedTrip.tripStatus)
        assertEquals(1L, viewModel.dashboardState.currentTripsCount)
        assertEquals(1L, viewModel.dashboardState.upcomingTripsCount)

        // When starting the trip
        val startedTrip = plannedTrip.copy(
            tripStatus = "IN_PROGRESS",
            startTime = "2026-10-28T09:00:00Z"
        )
        viewModel.updateTripInState(startedTrip)

        // Then status and counts are dynamically recalculated
        val updatedInList = viewModel.allTrips.first { it.id == 4L }
        assertEquals("IN_PROGRESS", updatedInList.tripStatus)
        assertEquals("2026-10-28T09:00:00Z", updatedInList.startTime)
        assertEquals(2L, viewModel.dashboardState.currentTripsCount)
        assertEquals(0L, viewModel.dashboardState.upcomingTripsCount)
        assertEquals(2L, viewModel.dashboardState.completedTripsCount)
        assertEquals(DriverTripsTab.CURRENT, viewModel.selectedTripsTab)
    }

    @Test
    fun updateTripInState_completeTrip_updatesStatusToCompletedAndRecalculatesCounts() {
        // Given in_progress trip (id=3)
        val inProgressTrip = sampleTrips.first { it.id == 3L }
        assertEquals("IN_PROGRESS", inProgressTrip.tripStatus)
        assertEquals(1L, viewModel.dashboardState.currentTripsCount)
        assertEquals(2L, viewModel.dashboardState.completedTripsCount)

        // When completing the trip with delivery notes
        val completedTrip = inProgressTrip.copy(
            tripStatus = "COMPLETED",
            endTime = "2026-10-28T18:00:00Z",
            deliveryNotes = "Consignment delivered in good condition"
        )
        viewModel.updateTripInState(completedTrip)

        // Then status and counts reflect completion
        val updatedInList = viewModel.allTrips.first { it.id == 3L }
        assertEquals("COMPLETED", updatedInList.tripStatus)
        assertEquals("2026-10-28T18:00:00Z", updatedInList.endTime)
        assertEquals("Consignment delivered in good condition", updatedInList.deliveryNotes)
        assertEquals(0L, viewModel.dashboardState.currentTripsCount)
        assertEquals(1L, viewModel.dashboardState.upcomingTripsCount)
        assertEquals(3L, viewModel.dashboardState.completedTripsCount)
        assertEquals(DriverTripsTab.COMPLETED, viewModel.selectedTripsTab)
    }

    @Test
    fun clearMutationMessages_resetsMessagesProperly() {
        assertFalse(viewModel.isMutatingTrip)
        assertNull(viewModel.mutationErrorMessage)
        assertNull(viewModel.mutationSuccessMessage)

        viewModel.clearMutationMessages()
        assertNull(viewModel.mutationErrorMessage)
        assertNull(viewModel.mutationSuccessMessage)
    }
}
