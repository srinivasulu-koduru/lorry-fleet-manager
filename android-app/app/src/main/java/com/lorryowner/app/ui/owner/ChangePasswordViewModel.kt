package com.lorryowner.app.ui.owner

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorryowner.app.network.ApiConfig
import com.lorryowner.app.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isCurrentPasswordVisible: Boolean = false,
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
) {
    // Password complexity checks for dynamic UI feedback
    val hasMinLength: Boolean get() = newPassword.length >= 8
    val hasUppercase: Boolean get() = newPassword.any { it.isUpperCase() }
    val hasLowercase: Boolean get() = newPassword.any { it.isLowerCase() }
    val hasDigit: Boolean get() = newPassword.any { it.isDigit() }
    val passwordsMatch: Boolean get() = newPassword.isNotEmpty() && newPassword == confirmPassword
    val isFormComplete: Boolean get() = currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank()
}

/**
 * ViewModel managing the secure password change flow for authenticated owners.
 *
 * Requirements enforced:
 * - Real authenticated backend password change via REST.
 * - Strict security: zero plaintext passwords logged or persisted.
 * - Minimum security requirements consistent with backend policy (8+ chars, upper, lower, digit).
 * - Debounces repeated submissions while request is in-flight.
 * - Server-side token invalidation handled on success.
 */
class ChangePasswordViewModel(
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ChangePasswordVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000

        // Strict backend-matching policy regex
        private val PASSWORD_STRENGTH_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")
    }

    var uiState by mutableStateOf(ChangePasswordUiState())
        private set

    fun onCurrentPasswordChange(value: String) {
        uiState = uiState.copy(currentPassword = value, errorMessage = null)
    }

    fun onNewPasswordChange(value: String) {
        uiState = uiState.copy(newPassword = value, errorMessage = null)
    }

    fun onConfirmPasswordChange(value: String) {
        uiState = uiState.copy(confirmPassword = value, errorMessage = null)
    }

    fun toggleCurrentPasswordVisibility() {
        uiState = uiState.copy(isCurrentPasswordVisible = !uiState.isCurrentPasswordVisible)
    }

    fun toggleNewPasswordVisibility() {
        uiState = uiState.copy(isNewPasswordVisible = !uiState.isNewPasswordVisible)
    }

    fun toggleConfirmPasswordVisibility() {
        uiState = uiState.copy(isConfirmPasswordVisible = !uiState.isConfirmPasswordVisible)
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    /**
     * Validates input locally, executes backend request, and triggers callback upon success.
     */
    fun submitChangePassword(onSuccess: () -> Unit) {
        if (uiState.isLoading) return

        val current = uiState.currentPassword
        val newPw = uiState.newPassword
        val confirm = uiState.confirmPassword

        // 1. Check required fields
        if (current.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please enter your current password.")
            return
        }
        if (newPw.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please enter your new password.")
            return
        }
        if (confirm.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please confirm your new password.")
            return
        }

        // 2. Matching confirmation
        if (newPw != confirm) {
            uiState = uiState.copy(errorMessage = "New password and confirmation do not match.")
            return
        }

        // 3. Password length requirement
        if (newPw.length < 8) {
            uiState = uiState.copy(errorMessage = "New password must be at least 8 characters long.")
            return
        }

        // 4. Password complexity requirement (consistent with backend)
        if (!PASSWORD_STRENGTH_PATTERN.matcher(newPw).matches()) {
            uiState = uiState.copy(
                errorMessage = "New password must contain at least one uppercase letter, one lowercase letter, and one number."
            )
            return
        }

        // 5. Must not be same as current
        if (newPw == current) {
            uiState = uiState.copy(errorMessage = "New password cannot be the same as your current password.")
            return
        }

        // Proceed to network submission
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                executeChangePasswordRequest(current, newPw, confirm)
            }

            when (result) {
                is ChangePasswordResult.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = result.message,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = ""
                    )
                    onSuccess()
                }
                is ChangePasswordResult.Failure -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun executeChangePasswordRequest(
        current: String,
        newPw: String,
        confirm: String
    ): ChangePasswordResult {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return ChangePasswordResult.Failure("Session expired. Please log in again.")
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastErrorResult: ChangePasswordResult? = null

        for (baseUrl in candidateUrls) {
            val result = attemptChangePasswordAtUrl(baseUrl, token, current, newPw, confirm)
            if (result is ChangePasswordResult.Success) {
                ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                return result
            }
            if (result is ChangePasswordResult.Failure &&
                !result.message.contains("Cannot connect") &&
                !result.message.contains("timed out") &&
                !result.message.contains("failed to connect") &&
                !result.message.contains("Connection refused")
            ) {
                // Legitimate server response (e.g. 400 Bad Request: "Current password does not match.")
                ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                return result
            }
            lastErrorResult = result
        }

        return lastErrorResult ?: ChangePasswordResult.Failure("Unable to connect to backend server.")
    }

    private fun attemptChangePasswordAtUrl(
        baseUrl: String,
        token: String,
        current: String,
        newPw: String,
        confirm: String
    ): ChangePasswordResult {
        val endpointUrl = "$baseUrl/api/users/change-password"
        var connection: HttpURLConnection? = null

        return try {
            val url = URL(endpointUrl)
            connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 4000
                readTimeout = 8000
                doOutput = true
                doInput = true
            }

            val requestJson = JSONObject().apply {
                put("currentPassword", current)
                put("newPassword", newPw)
                put("confirmPassword", confirm)
            }
            val requestBytes = requestJson.toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(requestBytes.size)

            connection.outputStream.use { os ->
                os.write(requestBytes)
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = try { JSONObject(responseText) } catch (e: Exception) { JSONObject() }
                val message = json.optString("message", "Password changed successfully.")
                Log.d(TAG, "Password changed successfully for authenticated user.")
                ChangePasswordResult.Success(message)
            } else {
                val errorText = try {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                } catch (e: Exception) {
                    ""
                }
                val serverMsg = try {
                    if (errorText.isNotEmpty()) {
                        val json = JSONObject(errorText)
                        json.optString("message", "")
                    } else ""
                } catch (e: Exception) {
                    ""
                }

                val userFacingError = when {
                    serverMsg.isNotBlank() -> serverMsg
                    responseCode == HttpURLConnection.HTTP_BAD_REQUEST -> "Invalid password request. Please check your credentials."
                    responseCode == HttpURLConnection.HTTP_UNAUTHORIZED -> "Session expired or current password incorrect."
                    responseCode == HttpURLConnection.HTTP_FORBIDDEN -> "You do not have permission to perform this action."
                    else -> "Failed to change password (HTTP $responseCode). Please try again."
                }
                ChangePasswordResult.Failure(userFacingError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during change password at $baseUrl: ${e.message}")
            ChangePasswordResult.Failure("Network error: ${e.localizedMessage ?: "Unable to connect to backend"}")
        } finally {
            connection?.disconnect()
        }
    }
}

sealed class ChangePasswordResult {
    data class Success(val message: String) : ChangePasswordResult()
    data class Failure(val message: String) : ChangePasswordResult()
}
