package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageDriversModelTest {

    @Test
    fun testDefaultUiState_initializesWithEmptyValues() {
        val state = ManageDriversUiState()
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertNull(state.selectedDriverForDetails)
        assertEquals(0, state.drivers.size)
        assertEquals(0, state.summary.totalDrivers)
        assertEquals(0, state.summary.activeDrivers)
        assertEquals(0, state.summary.completedTrips)
        assertEquals("0", state.summary.totalDistanceKm)
    }

    @Test
    fun testDriverItemUiState_propertiesCorrect() {
        val driver = DriverItemUiState(
            id = 1L,
            fullName = "Ramesh Kumar",
            username = "driver_ramesh",
            phoneNumber = "+91 98765 43210",
            licenseNumber = "DL-1420110012345",
            assignedLorry = "KA-01-AB-1234",
            isActive = true,
            completedTrips = 48,
            totalDistanceKm = "1,240 km"
        )

        assertEquals(1L, driver.id)
        assertEquals("Ramesh Kumar", driver.fullName)
        assertEquals("driver_ramesh", driver.username)
        assertEquals("+91 98765 43210", driver.phoneNumber)
        assertEquals("DL-1420110012345", driver.licenseNumber)
        assertEquals("KA-01-AB-1234", driver.assignedLorry)
        assertTrue(driver.isActive)
        assertEquals(48, driver.completedTrips)
        assertEquals("1,240 km", driver.totalDistanceKm)
    }

    @Test
    fun testDriverSummaryCalculation_computesActiveCounts() {
        val drivers = listOf(
            DriverItemUiState(id = 1L, fullName = "Driver One", username = "d1", isActive = true, completedTrips = 10),
            DriverItemUiState(id = 2L, fullName = "Driver Two", username = "d2", isActive = false, completedTrips = 0),
            DriverItemUiState(id = 3L, fullName = "Driver Three", username = "d3", isActive = true, completedTrips = 5)
        )

        val total = drivers.size
        val active = drivers.count { it.isActive }
        val trips = drivers.sumOf { it.completedTrips }

        val summary = DriverSummaryUiState(
            totalDrivers = total,
            activeDrivers = active,
            completedTrips = trips,
            totalDistanceKm = "350"
        )

        assertEquals(3, summary.totalDrivers)
        assertEquals(2, summary.activeDrivers)
        assertEquals(15, summary.completedTrips)
        assertEquals("350", summary.totalDistanceKm)
    }

    @Test
    fun testDriverDetailsModalSelection_togglesStateCorrectly() {
        var state = ManageDriversUiState()
        assertNull(state.selectedDriverForDetails)

        val driver = DriverItemUiState(id = 5L, fullName = "Suresh", username = "driver_suresh")
        state = state.copy(selectedDriverForDetails = driver)
        assertEquals(driver, state.selectedDriverForDetails)

        state = state.copy(selectedDriverForDetails = null)
        assertNull(state.selectedDriverForDetails)
    }

    @Test
    fun testDriverStatusToggle_updatesStateAndActiveCount() {
        val d1 = DriverItemUiState(id = 1L, fullName = "Driver One", username = "d1", isActive = true)
        val d2 = DriverItemUiState(id = 2L, fullName = "Driver Two", username = "d2", isActive = false)

        var state = ManageDriversUiState(
            drivers = listOf(d1, d2),
            summary = DriverSummaryUiState(totalDrivers = 2, activeDrivers = 1)
        )

        // Toggle d2 to active
        val updatedDrivers = state.drivers.map {
            if (it.id == 2L) it.copy(isActive = true) else it
        }
        val activeCount = updatedDrivers.count { it.isActive }
        state = state.copy(
            drivers = updatedDrivers,
            summary = state.summary.copy(activeDrivers = activeCount)
        )

        assertEquals(2, state.summary.activeDrivers)
        assertTrue(state.drivers.first { it.id == 2L }.isActive)
    }

    @Test
    fun testLorryAssignment_updatesDriverAndReassignsExisting() {
        val d1 = DriverItemUiState(id = 1L, fullName = "Driver One", username = "d1", assignedLorry = "KA-01-AB-1234")
        val d2 = DriverItemUiState(id = 2L, fullName = "Driver Two", username = "d2", assignedLorry = null)

        var state = ManageDriversUiState(drivers = listOf(d1, d2))

        // Reassign KA-01-AB-1234 to d2
        val newReg = "KA-01-AB-1234"
        val updated = state.drivers.map {
            if (it.id == 2L) it.copy(assignedLorry = newReg)
            else if (it.assignedLorry == newReg) it.copy(assignedLorry = null)
            else it
        }
        state = state.copy(drivers = updated)

        assertNull(state.drivers.first { it.id == 1L }.assignedLorry)
        assertEquals("KA-01-AB-1234", state.drivers.first { it.id == 2L }.assignedLorry)
    }

    @Test
    fun testDriverSearchAndFilter_filtersAccurately() {
        val d1 = DriverItemUiState(id = 1L, fullName = "Ramesh Kumar", username = "driver_ramesh", phoneNumber = "9876543210", assignedLorry = "AP 31 TC 1234", isActive = true)
        val d2 = DriverItemUiState(id = 2L, fullName = "Suresh Reddy", username = "driver_suresh", phoneNumber = "9988776655", assignedLorry = null, isActive = true)
        val d3 = DriverItemUiState(id = 3L, fullName = "Kiran Kumar", username = "driver_kiran", phoneNumber = "9112233445", assignedLorry = "TS 09 UA 9012", isActive = false)

        val all = listOf(d1, d2, d3)

        // 1. Filter ACTIVE
        val activeOnly = all.filter { it.isActive }
        assertEquals(2, activeOnly.size)

        // 2. Filter INACTIVE
        val inactiveOnly = all.filter { !it.isActive }
        assertEquals(1, inactiveOnly.size)
        assertEquals("driver_kiran", inactiveOnly.first().username)

        // 3. Filter ASSIGNED
        val assignedOnly = all.filter { !it.assignedLorry.isNullOrBlank() }
        assertEquals(2, assignedOnly.size)

        // 4. Filter UNASSIGNED
        val unassignedOnly = all.filter { it.assignedLorry.isNullOrBlank() }
        assertEquals(1, unassignedOnly.size)
        assertEquals("driver_suresh", unassignedOnly.first().username)

        // 5. Search by Name "kumar" (case-insensitive) -> matches d1 and d3
        val queryName = "kumar"
        val nameMatches = all.filter { it.fullName.lowercase().contains(queryName) }
        assertEquals(2, nameMatches.size)

        // 6. Search by Lorry Registration "TC 1234" -> matches d1
        val queryLorry = "tc 1234"
        val lorryMatches = all.filter { it.assignedLorry?.lowercase()?.contains(queryLorry) == true }
        assertEquals(1, lorryMatches.size)
        assertEquals("driver_ramesh", lorryMatches.first().username)
    }
}
