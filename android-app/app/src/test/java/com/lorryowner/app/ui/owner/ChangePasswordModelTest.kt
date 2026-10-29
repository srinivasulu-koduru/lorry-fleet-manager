package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangePasswordModelTest {

    @Test
    fun testDefaultUiState_initializesWithEmptyValues() {
        val state = ChangePasswordUiState()
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
        assertEquals("", state.currentPassword)
        assertEquals("", state.newPassword)
        assertEquals("", state.confirmPassword)
        assertFalse(state.hasMinLength)
        assertFalse(state.hasUppercase)
        assertFalse(state.hasLowercase)
        assertFalse(state.hasDigit)
        assertFalse(state.passwordsMatch)
        assertFalse(state.isFormComplete)
    }

    @Test
    fun testPasswordRequirements_evaluatedCorrectly() {
        // Incomplete password
        val weakState = ChangePasswordUiState(
            newPassword = "weak",
            confirmPassword = "weak"
        )
        assertFalse(weakState.hasMinLength)
        assertFalse(weakState.hasUppercase)
        assertTrue(weakState.hasLowercase)
        assertFalse(weakState.hasDigit)
        assertTrue(weakState.passwordsMatch)

        // Password satisfying all conditions
        val strongState = ChangePasswordUiState(
            currentPassword = "OldPassword123!",
            newPassword = "NewPassword456!",
            confirmPassword = "NewPassword456!"
        )
        assertTrue(strongState.hasMinLength)
        assertTrue(strongState.hasUppercase)
        assertTrue(strongState.hasLowercase)
        assertTrue(strongState.hasDigit)
        assertTrue(strongState.passwordsMatch)
        assertTrue(strongState.isFormComplete)
    }

    @Test
    fun testPasswordMismatch_detected() {
        val mismatchState = ChangePasswordUiState(
            currentPassword = "OldPassword123!",
            newPassword = "NewPassword123!",
            confirmPassword = "DifferentPassword123!"
        )
        assertTrue(mismatchState.hasMinLength)
        assertFalse(mismatchState.passwordsMatch)
    }

    @Test
    fun testChangePasswordValidation_enforcesBackendPolicy() {
        val viewModel = ChangePasswordViewModel()

        // 1. Empty current password
        var successCalled = false
        viewModel.submitChangePassword { successCalled = true }
        assertFalse(successCalled)
        assertEquals("Please enter your current password.", viewModel.uiState.errorMessage)

        // 2. Empty new password
        viewModel.onCurrentPasswordChange("CurrentPass123!")
        viewModel.submitChangePassword { successCalled = true }
        assertFalse(successCalled)
        assertEquals("Please enter your new password.", viewModel.uiState.errorMessage)

        // 3. Empty confirm password
        viewModel.onNewPasswordChange("NewPass123!")
        viewModel.submitChangePassword { successCalled = true }
        assertFalse(successCalled)
        assertEquals("Please confirm your new password.", viewModel.uiState.errorMessage)

        // 4. Mismatch
        viewModel.onConfirmPasswordChange("MismatchPass123!")
        viewModel.submitChangePassword { successCalled = true }
        assertFalse(successCalled)
        assertEquals("New password and confirmation do not match.", viewModel.uiState.errorMessage)

        // 5. Short password (< 8 chars)
        viewModel.onNewPasswordChange("Sh1!")
        viewModel.onConfirmPasswordChange("Sh1!")
        viewModel.submitChangePassword { successCalled = true }
        assertFalse(successCalled)
        assertEquals("New password must be at least 8 characters long.", viewModel.uiState.errorMessage)

        // 6. Missing number or uppercase
        viewModel.onNewPasswordChange("alllowercasehere!")
        viewModel.onConfirmPasswordChange("alllowercasehere!")
        viewModel.submitChangePassword { successCalled = true }
        assertFalse(successCalled)
        assertTrue(viewModel.uiState.errorMessage!!.contains("uppercase"))

        // 7. Same as current password
        viewModel.onNewPasswordChange("CurrentPass123!")
        viewModel.onConfirmPasswordChange("CurrentPass123!")
        viewModel.submitChangePassword { successCalled = true }
        assertFalse(successCalled)
        assertEquals("New password cannot be the same as your current password.", viewModel.uiState.errorMessage)
    }
}
