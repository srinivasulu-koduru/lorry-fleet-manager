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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AddDriverUiState(
    val fullName: String = "",
    val username: String = "",
    val tempPassword: String = "",
    val confirmPassword: String = "",
    val phoneNumber: String = "",
    val licenseNumber: String = "",
    val assignedLorry: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val successDriverName: String? = null
)

sealed interface CreateDriverResult {
    data class Success(val message: String, val driverName: String) : CreateDriverResult
    data class Failure(val message: String) : CreateDriverResult
}

class AddDriverViewModel(
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "AddDriverVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(AddDriverUiState())
        private set

    fun onFullNameChange(value: String) {
        uiState = uiState.copy(fullName = value, errorMessage = null)
    }

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(username = value.trim(), errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(tempPassword = value, errorMessage = null)
    }

    fun onConfirmPasswordChange(value: String) {
        uiState = uiState.copy(confirmPassword = value, errorMessage = null)
    }

    fun onPhoneNumberChange(value: String) {
        uiState = uiState.copy(phoneNumber = value, errorMessage = null)
    }

    fun onLicenseNumberChange(value: String) {
        uiState = uiState.copy(licenseNumber = value.uppercase(), errorMessage = null)
    }

    fun onAssignedLorryChange(value: String) {
        uiState = uiState.copy(assignedLorry = value.uppercase(), errorMessage = null)
    }

    fun togglePasswordVisibility() {
        uiState = uiState.copy(isPasswordVisible = !uiState.isPasswordVisible)
    }

    fun toggleConfirmPasswordVisibility() {
        uiState = uiState.copy(isConfirmPasswordVisible = !uiState.isConfirmPasswordVisible)
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun submitDriver(onSuccess: () -> Unit) {
        val name = uiState.fullName.trim()
        val uname = uiState.username.trim()
        val pw = uiState.tempPassword
        val confirm = uiState.confirmPassword

        if (name.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Please enter the driver's full name.")
            return
        }
        if (uname.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Please choose a unique username for the driver.")
            return
        }
        if (uname.length < 3) {
            uiState = uiState.copy(errorMessage = "Username must be at least 3 characters long.")
            return
        }
        if (pw.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Please provide a temporary password for the driver.")
            return
        }
        if (pw.length < 8) {
            uiState = uiState.copy(errorMessage = "Temporary password must be at least 8 characters long.")
            return
        }
        if (pw.none { it.isUpperCase() }) {
            uiState = uiState.copy(errorMessage = "Password must include at least one uppercase letter (A-Z).")
            return
        }
        if (pw.none { it.isLowerCase() }) {
            uiState = uiState.copy(errorMessage = "Password must include at least one lowercase letter (a-z).")
            return
        }
        if (pw.none { it.isDigit() }) {
            uiState = uiState.copy(errorMessage = "Password must include at least one numeric digit (0-9).")
            return
        }
        if (pw != confirm) {
            uiState = uiState.copy(errorMessage = "Passwords do not match. Please re-check.")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                executeCreateDriverRequest(
                    username = uname,
                    password = pw,
                    fullName = name,
                    phone = uiState.phoneNumber.trim().ifEmpty { null },
                    license = uiState.licenseNumber.trim().ifEmpty { null },
                    lorryReg = uiState.assignedLorry.trim().ifEmpty { null }
                )
            }

            when (result) {
                is CreateDriverResult.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        isSuccess = true,
                        successDriverName = result.driverName
                    )
                    onSuccess()
                }
                is CreateDriverResult.Failure -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun executeCreateDriverRequest(
        username: String,
        password: String,
        fullName: String,
        phone: String?,
        license: String?,
        lorryReg: String?
    ): CreateDriverResult {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return CreateDriverResult.Failure("Session expired. Please log in again.")
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastErrorResult: CreateDriverResult? = null

        for (baseUrl in candidateUrls) {
            val result = attemptCreateDriverAtUrl(baseUrl, token, username, password, fullName, phone, license, lorryReg)
            if (result is CreateDriverResult.Success) {
                ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                return result
            }
            if (result is CreateDriverResult.Failure &&
                !result.message.contains("Cannot connect") &&
                !result.message.contains("timed out") &&
                !result.message.contains("failed to connect") &&
                !result.message.contains("Connection refused")
            ) {
                return result
            }
            lastErrorResult = result
        }

        return lastErrorResult ?: CreateDriverResult.Failure("Cannot connect to server. Please check your connection.")
    }

    private fun attemptCreateDriverAtUrl(
        baseUrl: String,
        token: String,
        username: String,
        password: String,
        fullName: String,
        phone: String?,
        license: String?,
        lorryReg: String?
    ): CreateDriverResult {
        val fullUrl = "$baseUrl/api/owner/drivers"
        var conn: HttpURLConnection? = null

        return try {
            val url = URL(fullUrl)
            conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }

            val jsonBody = JSONObject().apply {
                put("username", username)
                put("password", password)
                put("fullName", fullName)
                if (phone != null) put("phoneNumber", phone)
                if (license != null) put("licenseNumber", license)
                if (lorryReg != null) put("assignedLorryRegistration", lorryReg)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                val raw = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonRes = JSONObject(raw)
                val createdName = jsonRes.optString("fullName", fullName)
                CreateDriverResult.Success(
                    message = "Driver account created successfully.",
                    driverName = createdName
                )
            } else if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                val errBody = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                val parsedMsg = parseErrorMessage(errBody) ?: "Username '$username' is already in use."
                CreateDriverResult.Failure(parsedMsg)
            } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                val errBody = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                val parsedMsg = parseErrorMessage(errBody) ?: "Invalid driver details. Please check the fields."
                CreateDriverResult.Failure(parsedMsg)
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                sessionManager?.clearSession()
                CreateDriverResult.Failure("Session expired. Please log in again.")
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                CreateDriverResult.Failure("Access denied. Owner permissions required.")
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                val msg = parseErrorMessage(errBody) ?: "Server returned error ($responseCode)."
                CreateDriverResult.Failure(msg)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Request failed at $baseUrl: ${e.message}")
            CreateDriverResult.Failure("Cannot connect to server at $baseUrl: ${e.message}")
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseErrorMessage(rawJson: String): String? {
        if (rawJson.isBlank()) return null
        return try {
            val json = JSONObject(rawJson)
            when {
                json.has("message") -> json.getString("message")
                json.has("error") -> json.getString("error")
                json.has("fieldErrors") -> {
                    val fieldErrors = json.getJSONObject("fieldErrors")
                    val firstKey = fieldErrors.keys().asSequence().firstOrNull()
                    if (firstKey != null) fieldErrors.getString(firstKey) else null
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
