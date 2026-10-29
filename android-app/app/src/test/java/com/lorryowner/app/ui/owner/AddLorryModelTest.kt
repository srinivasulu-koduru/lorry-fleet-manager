package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddLorryModelTest {

    private val sampleDriver = DriverSelectionOption(
        id = 101L,
        fullName = "Ramesh Kumar",
        username = "ramesh_k",
        phoneNumber = "+91 9876543210",
        assignedLorryRegistration = null
    )

    @Test
    fun testDefaultUiState_initializesWithSensibleDefaults() {
        val state = AddLorryUiState()
        assertEquals("", state.registrationNumber)
        assertEquals("", state.makeModel)
        assertEquals("Heavy Commercial Vehicle (HCV)", state.vehicleType)
        assertEquals("", state.capacityTons)
        assertEquals("", state.manufacturingYear)
        assertEquals("", state.ialertUnitId)
        assertEquals("", state.notes)
        assertNull(state.selectedDriver)
        assertTrue(state.availableDrivers.isEmpty())
        assertFalse(state.isLoadingDrivers)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
        assertNull(state.registrationNumberError)
        assertNull(state.makeModelError)
        assertNull(state.capacityTonsError)
        assertNull(state.manufacturingYearError)
        assertFalse(state.isSuccess)
        assertNull(state.createdLorryId)
        assertNull(state.createdRegistrationNumber)
    }

    @Test
    fun testRegistrationNumberChange_automaticallyUppercases() {
        val viewModel = AddLorryViewModel(null)
        viewModel.onRegistrationNumberChange("ap 31 tc 1234")
        assertEquals("AP 31 TC 1234", viewModel.uiState.registrationNumber)
    }

    @Test
    fun testCapacityTonsChange_filtersNonNumericCharacters() {
        val viewModel = AddLorryViewModel(null)
        viewModel.onCapacityTonsChange("16.5abc")
        assertEquals("16.5", viewModel.uiState.capacityTons)

        // Multiple decimal points rejected
        viewModel.onCapacityTonsChange("16.5.9")
        assertEquals("16.5", viewModel.uiState.capacityTons)
    }

    @Test
    fun testManufacturingYearChange_limitsToFourDigits() {
        val viewModel = AddLorryViewModel(null)
        viewModel.onManufacturingYearChange("202456")
        assertEquals("2024", viewModel.uiState.manufacturingYear)
    }

    @Test
    fun testValidation_failsWhenRequiredFieldsMissing() {
        val viewModel = AddLorryViewModel(null)
        val isValid = viewModel.validateForm()

        assertFalse(isValid)
        assertEquals("Registration number is required", viewModel.uiState.registrationNumberError)
        assertEquals("Vehicle make and model is required", viewModel.uiState.makeModelError)
        assertEquals("Payload capacity is required", viewModel.uiState.capacityTonsError)
    }

    @Test
    fun testValidation_failsWhenRegistrationTooShortOrTooLong() {
        val viewModel = AddLorryViewModel(null)
        viewModel.onRegistrationNumberChange("AB")
        viewModel.onMakeModelChange("Tata Prima")
        viewModel.onCapacityTonsChange("16.0")

        var isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertEquals("Registration number must be at least 3 characters", viewModel.uiState.registrationNumberError)

        viewModel.onRegistrationNumberChange("A".repeat(25))
        isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertEquals("Registration number cannot exceed 20 characters", viewModel.uiState.registrationNumberError)
    }

    @Test
    fun testValidation_failsWhenCapacityZeroOrNegative() {
        val viewModel = AddLorryViewModel(null)
        viewModel.onRegistrationNumberChange("AP 31 TC 1234")
        viewModel.onMakeModelChange("Tata Prima")
        viewModel.onCapacityTonsChange("0")

        var isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertEquals("Capacity must be greater than 0 tons", viewModel.uiState.capacityTonsError)

        viewModel.onCapacityTonsChange("250.0")
        isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertEquals("Capacity cannot exceed 200 tons", viewModel.uiState.capacityTonsError)
    }

    @Test
    fun testValidation_failsWhenManufacturingYearInvalid() {
        val viewModel = AddLorryViewModel(null)
        viewModel.onRegistrationNumberChange("AP 31 TC 1234")
        viewModel.onMakeModelChange("Tata Prima")
        viewModel.onCapacityTonsChange("16.0")
        viewModel.onManufacturingYearChange("1980") // Too old

        val isValid = viewModel.validateForm()
        assertFalse(isValid)
        assertNotNull(viewModel.uiState.manufacturingYearError)
    }

    @Test
    fun testValidation_succeedsWithValidData() {
        val viewModel = AddLorryViewModel(null)
        viewModel.onRegistrationNumberChange("AP 09 CD 5555")
        viewModel.onMakeModelChange("BharatBenz 3528C")
        viewModel.onCapacityTonsChange("28.0")
        viewModel.onVehicleTypeChange("Tipper / Dump Truck")
        viewModel.onManufacturingYearChange("2023")
        viewModel.onIalertUnitIdChange("IALERT-5555")
        viewModel.onNotesChange("Heavy industrial tipper.")
        viewModel.onDriverSelected(sampleDriver)

        val isValid = viewModel.validateForm()
        assertTrue(isValid)
        assertNull(viewModel.uiState.registrationNumberError)
        assertNull(viewModel.uiState.makeModelError)
        assertNull(viewModel.uiState.capacityTonsError)
        assertNull(viewModel.uiState.manufacturingYearError)
        assertEquals(sampleDriver, viewModel.uiState.selectedDriver)
    }

    @Test
    fun testDriverSelection_canBeCleared() {
        val viewModel = AddLorryViewModel(null)
        viewModel.onDriverSelected(sampleDriver)
        assertEquals(sampleDriver, viewModel.uiState.selectedDriver)

        viewModel.onDriverSelected(null)
        assertNull(viewModel.uiState.selectedDriver)
    }
}
