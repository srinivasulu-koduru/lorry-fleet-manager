package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LorryDetailsModelTest {

    @Test
    fun testDefaultUiState_initializesSafely() {
        val state = LorryDetailsUiState()
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.lorry)
        assertEquals(0, state.trips.size)
        assertEquals(0, state.filteredTrips.size)
        assertEquals(0, state.maintenanceRecords.size)
        assertEquals(0.0, state.totalMaintenanceCost, 0.001)
        assertEquals("ALL", state.selectedTripFilter)
        assertNull(state.selectedTripForDetails)
        assertNull(state.selectedMaintenanceForDetails)
    }

    @Test
    fun testMaintenanceItemUiState_propertiesCorrect() {
        val item = MaintenanceItemUiState(
            id = 101L,
            maintenanceDate = "2026-10-15",
            serviceType = "Scheduled Service",
            description = "100,000 km general overhaul and engine oil change",
            cost = 18500.0,
            serviceCenter = "Tata Authorized Service Center, Vijayawada",
            odometerReadingKm = 98500,
            nextServiceDate = "2027-04-15"
        )

        assertEquals(101L, item.id)
        assertEquals("2026-10-15", item.maintenanceDate)
        assertEquals("Scheduled Service", item.serviceType)
        assertEquals(18500.0, item.cost, 0.001)
        assertEquals("Tata Authorized Service Center, Vijayawada", item.serviceCenter)
        assertEquals(98500, item.odometerReadingKm)
        assertEquals("2027-04-15", item.nextServiceDate)
    }

    @Test
    fun testTripFilterLogic_correctlyFiltersStatus() {
        val trips = listOf(
            TripItemUiState(id = 1L, tripReference = "TRIP-101", driverName = "Driver A", lorryId = 1L, lorryRegistration = "AP 31 TC 1234", lorryMakeModel = "Tata", originLocation = "Hyd", destinationLocation = "Vizag", tripStatus = "COMPLETED", tripDate = "2026-10-20", startTime = null, endTime = null, distanceKm = "350 km", deliveryNotes = null, createdAt = null),
            TripItemUiState(id = 2L, tripReference = "TRIP-102", driverName = "Driver B", lorryId = 1L, lorryRegistration = "AP 31 TC 1234", lorryMakeModel = "Tata", originLocation = "Hyd", destinationLocation = "Guntur", tripStatus = "IN_PROGRESS", tripDate = "2026-10-22", startTime = null, endTime = null, distanceKm = null, deliveryNotes = null, createdAt = null),
            TripItemUiState(id = 3L, tripReference = "TRIP-103", driverName = "Driver C", lorryId = 1L, lorryRegistration = "AP 31 TC 1234", lorryMakeModel = "Tata", originLocation = "Hyd", destinationLocation = "Kurnool", tripStatus = "CANCELLED", tripDate = "2026-10-24", startTime = null, endTime = null, distanceKm = null, deliveryNotes = null, createdAt = null)
        )

        val completedTrips = trips.filter { it.tripStatus.equals("COMPLETED", ignoreCase = true) }
        val inProgressTrips = trips.filter { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }
        val cancelledTrips = trips.filter { it.tripStatus.equals("CANCELLED", ignoreCase = true) }

        assertEquals(1, completedTrips.size)
        assertEquals("TRIP-101", completedTrips[0].tripReference)

        assertEquals(1, inProgressTrips.size)
        assertEquals("TRIP-102", inProgressTrips[0].tripReference)

        assertEquals(1, cancelledTrips.size)
        assertEquals("TRIP-103", cancelledTrips[0].tripReference)
    }
}
