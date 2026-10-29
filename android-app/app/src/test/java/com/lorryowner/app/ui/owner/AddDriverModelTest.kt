package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddDriverModelTest {

    @Test
    fun testDefaultUiState_initializesWithEmptyValues() {
        val state = AddDriverUiState()
        assertEquals("", state.fullName)
        assertEquals("", state.username)
        assertEquals("", state.tempPassword)
        assertEquals("", state.confirmPassword)
        assertEquals("", state.phoneNumber)
        assertEquals("", state.licenseNumber)
        assertEquals("", state.assignedLorry)
        assertFalse(state.isPasswordVisible)
        assertFalse(state.isConfirmPasswordVisible)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertNull(state.errorMessage)
        assertNull(state.successDriverName)
    }

    @Test
    fun testInputModifications_updateStateAndUppercaseFields() {
        val viewModel = AddDriverViewModel()

        viewModel.onFullNameChange("Venkatesh Rao")
        assertEquals("Venkatesh Rao", viewModel.uiState.fullName)

        viewModel.onUsernameChange("  venkat_r  ")
        assertEquals("venkat_r", viewModel.uiState.username)

        viewModel.onPhoneNumberChange("+91 9988776655")
        assertEquals("+91 9988776655", viewModel.uiState.phoneNumber)

        // License number should be converted to uppercase
        viewModel.onLicenseNumberChange("ap04-2023-0001234")
        assertEquals("AP04-2023-0001234", viewModel.uiState.licenseNumber)

        // Assigned lorry should be converted to uppercase
        viewModel.onAssignedLorryChange("ap 26 te 1234")
        assertEquals("AP 26 TE 1234", viewModel.uiState.assignedLorry)
    }

    @Test
    fun testPasswordVisibilityToggles() {
        val viewModel = AddDriverViewModel()
        assertFalse(viewModel.uiState.isPasswordVisible)
        assertFalse(viewModel.uiState.isConfirmPasswordVisible)

        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.isPasswordVisible)

        viewModel.toggleConfirmPasswordVisibility()
        assertTrue(viewModel.uiState.isConfirmPasswordVisible)

        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.uiState.isPasswordVisible)
    }

    @Test
    fun testValidation_emptyFullName_rejected() {
        val viewModel = AddDriverViewModel()
        var success = false
        viewModel.submitDriver { success = true }

        assertFalse(success)
        assertEquals("Please enter the driver's full name.", viewModel.uiState.errorMessage)
    }

    @Test
    fun testValidation_emptyUsername_rejected() {
        val viewModel = AddDriverViewModel()
        viewModel.onFullNameChange("Venkat")
        var success = false
        viewModel.submitDriver { success = true }

        assertFalse(success)
        assertEquals("Please choose a unique username for the driver.", viewModel.uiState.errorMessage)
    }

    @Test
    fun testValidation_emptyPassword_rejected() {
        val viewModel = AddDriverViewModel()
        viewModel.onFullNameChange("Venkat")
        viewModel.onUsernameChange("venkat")
        var success = false
        viewModel.submitDriver { success = true }

        assertFalse(success)
        assertEquals("Please provide a temporary password for the driver.", viewModel.uiState.errorMessage)
    }

    @Test
    fun testValidation_shortPassword_rejected() {
        val viewModel = AddDriverViewModel()
        viewModel.onFullNameChange("Venkat")
        viewModel.onUsernameChange("venkat")
        viewModel.onPasswordChange("Short1!")
        viewModel.onConfirmPasswordChange("Short1!")
        var success = false
        viewModel.submitDriver { success = true }

        assertFalse(success)
        assertEquals("Temporary password must be at least 8 characters long.", viewModel.uiState.errorMessage)
    }

    @Test
    fun testValidation_passwordPolicyRules_enforced() {
        val viewModel = AddDriverViewModel()
        viewModel.onFullNameChange("Venkat")
        viewModel.onUsernameChange("venkat")

        // No uppercase
        viewModel.onPasswordChange("alllowercase123!")
        viewModel.onConfirmPasswordChange("alllowercase123!")
        viewModel.submitDriver {}
        assertEquals("Password must include at least one uppercase letter (A-Z).", viewModel.uiState.errorMessage)

        // No lowercase
        viewModel.onPasswordChange("ALLUPPERCASE123!")
        viewModel.onConfirmPasswordChange("ALLUPPERCASE123!")
        viewModel.submitDriver {}
        assertEquals("Password must include at least one lowercase letter (a-z).", viewModel.uiState.errorMessage)

        // No digit
        viewModel.onPasswordChange("NoDigitsHereTest!")
        viewModel.onConfirmPasswordChange("NoDigitsHereTest!")
        viewModel.submitDriver {}
        assertEquals("Password must include at least one numeric digit (0-9).", viewModel.uiState.errorMessage)
    }

    @Test
    fun testValidation_passwordMismatch_rejected() {
        val viewModel = AddDriverViewModel()
        viewModel.onFullNameChange("Venkat")
        viewModel.onUsernameChange("venkat")
        viewModel.onPasswordChange("SecurePassword123!")
        viewModel.onConfirmPasswordChange("DifferentPassword123!")
        var success = false
        viewModel.submitDriver { success = true }

        assertFalse(success)
        assertEquals("Passwords do not match. Please re-check.", viewModel.uiState.errorMessage)
    }
}
