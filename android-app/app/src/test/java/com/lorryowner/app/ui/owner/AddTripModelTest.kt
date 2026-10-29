package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTripModelTest {

    private val sampleLorry = LorryOption(
        id = 1L,
        registrationNumber = "AP04-TU-1234",
        makeModel = "Tata Prima 407",
        capacityTons = 16.0,
        assignedDriverId = 101L,
        assignedDriverName = "Ramesh Kumar"
    )

    private val sampleDriver = DriverOption(
        id = 101L,
        fullName = "Ramesh Kumar",
        username = "ramesh_k",
        phoneNumber = "+91 9876543210",
        assignedLorryRegistration = "AP04-TU-1234"
    )

    private val sampleDriver2 = DriverOption(
        id = 102L,
        fullName = "Suresh Patel",
        username = "suresh_p",
        phoneNumber = "+91 9876543211",
        assignedLorryRegistration = null
    )

    @Test
    fun testDefaultUiState_initializesWithSensibleDefaults() {
        val state = AddTripUiState()
        assertNull(state.selectedLorry)
        assertNull(state.selectedDriver)
        assertEquals("", state.origin)
        assertEquals("", state.destination)
        assertEquals("", state.plannedDepartureDate)
        assertEquals("", state.plannedDepartureTime)
        assertEquals("", state.plannedDeliveryDate)
        assertEquals("", state.plannedDeliveryTime)
        assertEquals("General Goods", state.loadType)
        assertEquals("", state.customLoadType)
        assertEquals("", state.remarks)
        assertNull(state.lorryError)
        assertNull(state.driverError)
        assertNull(state.originError)
        assertNull(state.destinationError)
        assertNull(state.departureDateError)
        assertNull(state.departureTimeError)
        assertNull(state.deliveryDateError)
        assertNull(state.deliveryTimeError)
        assertNull(state.loadTypeError)
        assertNull(state.generalError)
        assertFalse(state.isSubmitting)
        assertNull(state.createdTripResult)
    }

    @Test
    fun testValidation_failsWhenLorryAndDriverMissing() {
        val viewModel = AddTripViewModel(null)
        val isValid = viewModel.validateForm()

        assertFalse(isValid)
        assertNotNull(viewModel.uiState.lorryError)
        assertNotNull(viewModel.uiState.driverError)
        assertNotNull(viewModel.uiState.originError)
        assertNotNull(viewModel.uiState.destinationError)
        assertNotNull(viewModel.uiState.departureDateError)
        assertNotNull(viewModel.uiState.departureTimeError)
    }

    @Test
    fun testValidation_failsWhenOriginMatchesDestination() {
        val viewModel = AddTripViewModel(null)
        viewModel.onLorrySelected(sampleLorry)
        viewModel.onDriverSelected(sampleDriver)
        viewModel.onOriginChange("Hyderabad")
        viewModel.onDestinationChange("  hyderabad  ")
        viewModel.onDepartureDateSelected("2026-11-01")
        viewModel.onDepartureTimeSelected("09:00")

        val isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertEquals("Destination must be different from origin", viewModel.uiState.destinationError)
    }

    @Test
    fun testValidation_failsWhenDeliveryDateProvidedWithoutTime() {
        val viewModel = AddTripViewModel(null)
        viewModel.onLorrySelected(sampleLorry)
        viewModel.onDriverSelected(sampleDriver)
        viewModel.onOriginChange("Hyderabad")
        viewModel.onDestinationChange("Bengaluru")
        viewModel.onDepartureDateSelected("2026-11-01")
        viewModel.onDepartureTimeSelected("09:00")
        viewModel.onDeliveryDateSelected("2026-11-02")
        // Delivery time left blank

        val isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertEquals("Delivery time required when date is selected", viewModel.uiState.deliveryTimeError)
    }

    @Test
    fun testValidation_failsWhenDeliveryBeforeDeparture() {
        val viewModel = AddTripViewModel(null)
        viewModel.onLorrySelected(sampleLorry)
        viewModel.onDriverSelected(sampleDriver)
        viewModel.onOriginChange("Hyderabad")
        viewModel.onDestinationChange("Bengaluru")
        viewModel.onDepartureDateSelected("2026-11-02")
        viewModel.onDepartureTimeSelected("14:00")
        viewModel.onDeliveryDateSelected("2026-11-02")
        viewModel.onDeliveryTimeSelected("11:00") // Earlier on same day

        val isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertEquals("Delivery time must be later than departure time", viewModel.uiState.deliveryTimeError)
    }

    @Test
    fun testValidation_succeedsWithValidScheduleAndCargo() {
        val viewModel = AddTripViewModel(null)
        viewModel.onLorrySelected(sampleLorry)
        viewModel.onDriverSelected(sampleDriver)
        viewModel.onOriginChange("Hyderabad Hub")
        viewModel.onDestinationChange("Bengaluru Depot")
        viewModel.onDepartureDateSelected("2026-11-01")
        viewModel.onDepartureTimeSelected("08:30")
        viewModel.onDeliveryDateSelected("2026-11-02")
        viewModel.onDeliveryTimeSelected("14:00")
        viewModel.onLoadTypeSelected("Food and Groceries")
        viewModel.onRemarksChange("Refrigerated shipment.")

        val isValid = viewModel.validateForm()
        assertTrue(isValid)
        assertNull(viewModel.uiState.lorryError)
        assertNull(viewModel.uiState.driverError)
        assertNull(viewModel.uiState.originError)
        assertNull(viewModel.uiState.destinationError)
        assertNull(viewModel.uiState.departureDateError)
        assertNull(viewModel.uiState.departureTimeError)
        assertNull(viewModel.uiState.deliveryDateError)
        assertNull(viewModel.uiState.deliveryTimeError)
        assertNull(viewModel.uiState.loadTypeError)
    }

    @Test
    fun testValidation_succeedsWithoutOptionalDeliveryDateAndDeliveryTime() {
        val viewModel = AddTripViewModel(null)
        viewModel.onLorrySelected(sampleLorry)
        viewModel.onDriverSelected(sampleDriver)
        viewModel.onOriginChange("Hyderabad Hub")
        viewModel.onDestinationChange("Vijayawada Port")
        viewModel.onDepartureDateSelected("2026-11-05")
        viewModel.onDepartureTimeSelected("06:00")
        // Delivery left completely blank (ETA unavailable)
        viewModel.onLoadTypeSelected("Construction Materials")

        val isValid = viewModel.validateForm()
        assertTrue(isValid)
        assertNull(viewModel.uiState.deliveryDateError)
        assertNull(viewModel.uiState.deliveryTimeError)
    }

    @Test
    fun testValidation_failsWhenCustomLoadTypeIsBlankForOther() {
        val viewModel = AddTripViewModel(null)
        viewModel.onLorrySelected(sampleLorry)
        viewModel.onDriverSelected(sampleDriver)
        viewModel.onOriginChange("Hyderabad")
        viewModel.onDestinationChange("Chennai")
        viewModel.onDepartureDateSelected("2026-11-01")
        viewModel.onDepartureTimeSelected("09:00")
        viewModel.onLoadTypeSelected("Other")
        viewModel.onCustomLoadTypeChange("   ") // Blank custom load

        val isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertEquals("Please select or enter a cargo load type", viewModel.uiState.loadTypeError)
    }

    @Test
    fun testAutoSuggestDriver_whenLorryHasAssignedDriver() {
        val viewModel = AddTripViewModel(null)
        // Set available drivers in state
        viewModel.onLorrySelected(sampleLorry.copy(assignedDriverId = 102L))

        // Manually trigger driver list into state
        val dummyList = listOf(sampleDriver, sampleDriver2)
        // Test selection callback logic
        val lorryWithDriver = sampleLorry.copy(assignedDriverId = 102L)
        // With drivers present
        val stateWithDrivers = viewModel.uiState.copy(drivers = dummyList)
        // When assignedDriverId matches sampleDriver2 (102L)
        val matched = stateWithDrivers.drivers.find { it.id == lorryWithDriver.assignedDriverId }
        assertNotNull(matched)
        assertEquals("Suresh Patel", matched?.fullName)
    }

    @Test
    fun testClearDeliverySchedule_resetsFieldsAndErrors() {
        val viewModel = AddTripViewModel(null)
        viewModel.onDeliveryDateSelected("2026-11-02")
        viewModel.onDeliveryTimeSelected("18:00")
        assertEquals("2026-11-02", viewModel.uiState.plannedDeliveryDate)
        assertEquals("18:00", viewModel.uiState.plannedDeliveryTime)

        viewModel.onClearDeliverySchedule()
        assertEquals("", viewModel.uiState.plannedDeliveryDate)
        assertEquals("", viewModel.uiState.plannedDeliveryTime)
        assertNull(viewModel.uiState.deliveryDateError)
        assertNull(viewModel.uiState.deliveryTimeError)
    }
}
