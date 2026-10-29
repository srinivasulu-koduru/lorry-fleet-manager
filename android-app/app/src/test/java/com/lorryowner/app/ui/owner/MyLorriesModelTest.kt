package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyLorriesModelTest {

    @Test
    fun testDefaultUiState_initializesWithSafeDefaults() {
        val state = MyLorriesUiState()
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertEquals(0, state.lorries.size)
        assertEquals(0, state.filteredLorries.size)
        assertEquals(LorryFilter.ALL, state.filter)
        assertEquals("", state.searchQuery)
        assertEquals(0L, state.summary.totalLorries)
        assertEquals(0L, state.summary.runningLorries)
        assertEquals(0L, state.summary.notRunningLorries)
        assertEquals(0.0, state.summary.totalDistanceKm, 0.001)
        assertEquals(0.0, state.summary.totalMaintenanceCost, 0.001)
    }

    @Test
    fun testFleetSummaryCalculation_computesCorrectPercentages() {
        val summary = FleetSummaryUiState(
            totalLorries = 5L,
            runningLorries = 3L,
            notRunningLorries = 2L,
            runningPercentage = 60.0,
            notRunningPercentage = 40.0,
            totalDistanceKm = 625.50,
            totalMaintenanceCost = 48750.0
        )

        assertEquals(5L, summary.totalLorries)
        assertEquals(3L, summary.runningLorries)
        assertEquals(2L, summary.notRunningLorries)
        assertEquals(60.0, summary.runningPercentage, 0.001)
        assertEquals(40.0, summary.notRunningPercentage, 0.001)
        assertEquals(625.50, summary.totalDistanceKm, 0.001)
        assertEquals(48750.0, summary.totalMaintenanceCost, 0.001)
    }

    @Test
    fun testLorryCardUiState_creationAndProperties() {
        val lorry = LorryCardUiState(
            id = 1L,
            registrationNumber = "AP 31 TC 1234",
            makeModel = "Tata Prima 4028",
            capacityTons = 28.5,
            operationalStatus = "ON_TRIP",
            running = true,
            assignedDriverId = 2L,
            assignedDriverName = "Ramesh Kumar",
            activeTripOrigin = "Visakhapatnam Port",
            activeTripDestination = "Hyderabad Outer Ring",
            activeTripEtaFormatted = "ETA: 9h 14m",
            totalTrips = 12L,
            totalMaintenanceCost = 48750.0
        )

        assertEquals(1L, lorry.id)
        assertEquals("AP 31 TC 1234", lorry.registrationNumber)
        assertTrue(lorry.running)
        assertEquals("Ramesh Kumar", lorry.assignedDriverName)
        assertEquals("Visakhapatnam Port", lorry.activeTripOrigin)
        assertEquals("Hyderabad Outer Ring", lorry.activeTripDestination)
        assertEquals("ETA: 9h 14m", lorry.activeTripEtaFormatted)
        assertEquals(12L, lorry.totalTrips)
        assertEquals(48750.0, lorry.totalMaintenanceCost, 0.001)
    }

    @Test
    fun testFilterLogic_appliesRunningFilterAccurately() {
        val lorries = listOf(
            LorryCardUiState(id = 1L, registrationNumber = "AP 31 TC 1234", makeModel = "Tata Prima", capacityTons = 28.0, running = true, operationalStatus = "ON_TRIP"),
            LorryCardUiState(id = 2L, registrationNumber = "TS 09 UB 5678", makeModel = "Ashok Leyland", capacityTons = 25.0, running = false, operationalStatus = "AVAILABLE"),
            LorryCardUiState(id = 3L, registrationNumber = "KA 04 E 9012", makeModel = "BharatBenz", capacityTons = 30.0, running = true, operationalStatus = "ON_TRIP")
        )

        val runningOnly = lorries.filter { it.running }
        val notRunningOnly = lorries.filter { !it.running }

        assertEquals(2, runningOnly.size)
        assertEquals("AP 31 TC 1234", runningOnly[0].registrationNumber)
        assertEquals("KA 04 E 9012", runningOnly[1].registrationNumber)

        assertEquals(1, notRunningOnly.size)
        assertEquals("TS 09 UB 5678", notRunningOnly[0].registrationNumber)
    }

    @Test
    fun testSearchLogic_filtersByRegistrationAndMakeModel() {
        val lorries = listOf(
            LorryCardUiState(id = 1L, registrationNumber = "AP 31 TC 1234", makeModel = "Tata Prima 4028", capacityTons = 28.0, running = true, operationalStatus = "ON_TRIP"),
            LorryCardUiState(id = 2L, registrationNumber = "TS 09 UB 5678", makeModel = "Ashok Leyland 2820", capacityTons = 25.0, running = false, operationalStatus = "AVAILABLE")
        )

        val query1 = "tata"
        val matched1 = lorries.filter {
            it.registrationNumber.contains(query1, ignoreCase = true) ||
            it.makeModel.contains(query1, ignoreCase = true)
        }
        assertEquals(1, matched1.size)
        assertEquals(1L, matched1[0].id)

        val query2 = "5678"
        val matched2 = lorries.filter {
            it.registrationNumber.contains(query2, ignoreCase = true) ||
            it.makeModel.contains(query2, ignoreCase = true)
        }
        assertEquals(1, matched2.size)
        assertEquals(2L, matched2[0].id)
    }
}
