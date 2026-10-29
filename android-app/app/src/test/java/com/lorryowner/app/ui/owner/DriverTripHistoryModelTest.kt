package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverTripHistoryModelTest {

    @Test
    fun testDefaultUiState_initializesWithEmptyValues() {
        val state = DriverTripHistoryUiState(
            driverId = 2L,
            driverName = "Ramesh Kumar",
            driverUsername = "driver_ramesh"
        )

        assertEquals(2L, state.driverId)
        assertEquals("Ramesh Kumar", state.driverName)
        assertEquals("driver_ramesh", state.driverUsername)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertNull(state.selectedTripForDetails)
        assertEquals(0, state.allTrips.size)
        assertEquals(0, state.trips.size)
        assertEquals("", state.searchQuery)
        assertEquals(TripStatusFilter.ALL, state.selectedStatusFilter)
    }

    @Test
    fun testTripItemUiState_propertiesCorrect() {
        val trip = TripItemUiState(
            id = 101L,
            tripReference = "TRIP-2026-1001",
            driverId = 2L,
            driverName = "Ramesh Kumar",
            lorryId = 1L,
            lorryRegistration = "AP 31 TC 1234",
            lorryMakeModel = "Ashok Leyland 2820 HG",
            originLocation = "Visakhapatnam Port, AP",
            destinationLocation = "Vijayawada Commercial Hub, AP",
            tripStatus = "COMPLETED",
            tripDate = "2026-10-20",
            startTime = "2026-10-20 06:30:00",
            endTime = "2026-10-20 14:45:00",
            distanceKm = "350.5 km",
            deliveryNotes = "Steel consignment delivered",
            createdAt = "2026-10-20T14:45:00Z"
        )

        assertEquals(101L, trip.id)
        assertEquals("TRIP-2026-1001", trip.tripReference)
        assertEquals("AP 31 TC 1234", trip.lorryRegistration)
        assertEquals("COMPLETED", trip.tripStatus)
        assertEquals("350.5 km", trip.distanceKm)
        assertEquals("Visakhapatnam Port, AP", trip.originLocation)
    }

    @Test
    fun testDriverPerformanceUiState_calculations() {
        val perf = DriverPerformanceUiState(
            totalTrips = 5,
            completedTrips = 4,
            inProgressTrips = 1,
            cancelledTrips = 0,
            totalRecordedDistanceKm = "1,250.5 km",
            distinctLorriesUsed = 2,
            mostRecentTripDate = "2026-10-22",
            mostRecentTripReference = "TRIP-2026-1002"
        )

        assertEquals(5, perf.totalTrips)
        assertEquals(4, perf.completedTrips)
        assertEquals(1, perf.inProgressTrips)
        assertEquals(0, perf.cancelledTrips)
        assertEquals("1,250.5 km", perf.totalRecordedDistanceKm)
        assertEquals(2, perf.distinctLorriesUsed)
        assertEquals("2026-10-22", perf.mostRecentTripDate)
        assertEquals("TRIP-2026-1002", perf.mostRecentTripReference)
    }

    @Test
    fun testTripFiltering_byStatusAndSearchQuery() {
        val t1 = TripItemUiState(
            id = 1L, tripReference = "TRIP-1", driverId = 2L, driverName = "Ramesh",
            lorryId = 1L, lorryRegistration = "AP 31 TC 1234", lorryMakeModel = "Leyland",
            originLocation = "Visakhapatnam", destinationLocation = "Vijayawada",
            tripStatus = "COMPLETED", tripDate = "2026-10-20", startTime = null, endTime = null,
            distanceKm = "350 km", deliveryNotes = null, createdAt = null
        )
        val t2 = TripItemUiState(
            id = 2L, tripReference = "TRIP-2", driverId = 2L, driverName = "Ramesh",
            lorryId = 3L, lorryRegistration = "TS 09 UA 9012", lorryMakeModel = "Tata",
            originLocation = "Vijayawada", destinationLocation = "Hyderabad",
            tripStatus = "COMPLETED", tripDate = "2026-10-22", startTime = null, endTime = null,
            distanceKm = "275 km", deliveryNotes = null, createdAt = null
        )
        val t3 = TripItemUiState(
            id = 3L, tripReference = "TRIP-3", driverId = 2L, driverName = "Ramesh",
            lorryId = 1L, lorryRegistration = "AP 31 TC 1234", lorryMakeModel = "Leyland",
            originLocation = "Hyderabad", destinationLocation = "Nellore",
            tripStatus = "IN_PROGRESS", tripDate = "2026-10-27", startTime = null, endTime = null,
            distanceKm = null, deliveryNotes = "In transit", createdAt = null
        )

        val allTrips = listOf(t1, t2, t3)

        // Status filter: COMPLETED
        val completedOnly = allTrips.filter { it.tripStatus == "COMPLETED" }
        assertEquals(2, completedOnly.size)

        // Status filter: IN_PROGRESS
        val inProgressOnly = allTrips.filter { it.tripStatus == "IN_PROGRESS" }
        assertEquals(1, inProgressOnly.size)
        assertEquals("TRIP-3", inProgressOnly.first().tripReference)

        // Search by destination: "Hyderabad" -> matches t2 (dest) and t3 (origin)
        val hyderabadTrips = allTrips.filter {
            it.originLocation.contains("Hyderabad", ignoreCase = true) ||
            it.destinationLocation.contains("Hyderabad", ignoreCase = true)
        }
        assertEquals(2, hyderabadTrips.size)

        // Search by lorry: "TS 09" -> matches t2
        val lorryTrips = allTrips.filter { it.lorryRegistration.contains("TS 09", ignoreCase = true) }
        assertEquals(1, lorryTrips.size)
        assertEquals("TRIP-2", lorryTrips.first().tripReference)
    }

    @Test
    fun testTripDetailsDialogSelection_togglesState() {
        var state = DriverTripHistoryUiState(driverId = 2L)
        assertNull(state.selectedTripForDetails)

        val trip = TripItemUiState(
            id = 50L, tripReference = "TRIP-50", driverId = 2L, driverName = "Ramesh",
            lorryId = 1L, lorryRegistration = "AP 31 TC 1234", lorryMakeModel = null,
            originLocation = "A", destinationLocation = "B", tripStatus = "COMPLETED",
            tripDate = "2026-10-20", startTime = null, endTime = null, distanceKm = null,
            deliveryNotes = null, createdAt = null
        )

        state = state.copy(selectedTripForDetails = trip)
        assertEquals(trip, state.selectedTripForDetails)

        state = state.copy(selectedTripForDetails = null)
        assertNull(state.selectedTripForDetails)
    }
}
