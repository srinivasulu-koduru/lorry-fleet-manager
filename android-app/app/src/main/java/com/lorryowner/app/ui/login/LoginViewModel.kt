package com.lorryowner.app.ui.login

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

enum class UserRole {
    OWNER,
    DRIVER,
    UNSUPPORTED
}

data class LoginUiState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val userRole: UserRole? = null,
    val isServerSettingsDialogVisible: Boolean = false,
    val serverUrlInput: String = ""
)

/**
 * ViewModel managing the single unified login screen for Owners and Drivers.
 *
 * Rules:
 * - Real backend authentication via REST (/api/auth/login).
 * - No fake authentication, mock timers, or hardcoded usernames.
 * - On successful authentication, session is saved securely using SessionManager.
 * - Restores persistent session on initialization if token is valid.
 * - Strict security: zero sensitive data (passwords, lengths, tokens) logged.
 * - Environment-aware connectivity via ApiConfig:
 *   - Real phones prioritize laptop LAN Wi-Fi IP (http://10.231.41.81:8080).
 *   - Emulators prioritize standard http://10.0.2.2:8080 and http://127.0.0.1:8080.
 *   - Working URL is persisted in SessionManager across app launches.
 */
class LoginViewModel(
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 10000
    }

    // Active backend base URL resolved dynamically via ApiConfig
    var backendBaseUrl: String = ApiConfig.getBaseUrl(sessionManager)

    var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        restoreSessionIfPresent()
    }

    /**
     * Checks if a valid, non-expired session is present in secure storage
     * and automatically authenticates if so.
     */
    fun restoreSessionIfPresent() {
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            val role = sessionManager.getUserRole()
            val username = sessionManager.getUsername()
            if (role != null && role != UserRole.UNSUPPORTED) {
                uiState = uiState.copy(
                    isAuthenticated = true,
                    userRole = role,
                    usernameInput = username
                )
            }
        }
    }

    fun onUsernameChanged(newValue: String) {
        uiState = uiState.copy(
            usernameInput = newValue,
            errorMessage = null,
            infoMessage = null
        )
    }

    fun onPasswordChanged(newValue: String) {
        uiState = uiState.copy(
            passwordInput = newValue,
            errorMessage = null,
            infoMessage = null
        )
    }

    fun togglePasswordVisibility() {
        uiState = uiState.copy(
            isPasswordVisible = !uiState.isPasswordVisible
        )
    }

    fun login(onSuccess: (UserRole) -> Unit) {
        val username = uiState.usernameInput.trim()
        val password = uiState.passwordInput
        Log.d(TAG, "login() requested")

        // 1. Client-Side Validation
        if (username.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Please enter your username")
            return
        }

        if (password.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Please enter your password")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            val result = withContext(Dispatchers.IO) {
                performBackendLogin(username, password)
            }

            when (result) {
                is LoginResult.Success -> {
                    // Save to secure Keystore storage
                    sessionManager?.saveSession(
                        token = result.token,
                        role = result.role.name,
                        username = result.username,
                        userId = result.userId,
                        fullName = result.fullName,
                        expiresInMs = result.expiresIn
                    )

                    uiState = uiState.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userRole = result.role,
                        usernameInput = result.username,
                        passwordInput = "", // Clear sensitive plaintext password from memory
                        errorMessage = null
                    )
                    onSuccess(result.role)
                }
                is LoginResult.Failure -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun performBackendLogin(username: String, password: String): LoginResult {
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        val isEmu = ApiConfig.isEmulator()

        var lastErrorResult: LoginResult? = null

        for (baseUrl in candidateUrls) {
            Log.d(TAG, "Attempting authentication against candidate: $baseUrl")
            val result = attemptLoginAtUrl(baseUrl, username, password)
            if (result is LoginResult.Success) {
                backendBaseUrl = baseUrl
                ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                return result
            }
            // If it was an auth rejection (401/400/403), don't try another URL—it's a valid server response
            if (result is LoginResult.Failure &&
                !result.message.contains("Cannot connect") &&
                !result.message.contains("timed out") &&
                !result.message.contains("failed to connect") &&
                !result.message.contains("Connection refused")
            ) {
                backendBaseUrl = baseUrl
                ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                return result
            }
            lastErrorResult = result
        }

        return lastErrorResult ?: LoginResult.Failure(
            "Cannot connect to backend server. Please check your internet connection."
        )
    }

    private fun attemptLoginAtUrl(baseUrl: String, username: String, password: String): LoginResult {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$baseUrl/api/auth/login")
            connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                doInput = true
            }

            val requestJson = JSONObject().apply {
                put("username", username)
                put("password", password)
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
                val json = JSONObject(responseText)
                val roleString = json.optString("role", "").uppercase()
                val role = when {
                    roleString.contains("OWNER") -> UserRole.OWNER
                    roleString.contains("DRIVER") -> UserRole.DRIVER
                    else -> UserRole.UNSUPPORTED
                }

                val token = json.optString("accessToken", "")
                val userId = json.optLong("userId", -1L)
                val retUsername = json.optString("username", username)
                val fullName = json.optString("fullName", "")
                val expiresIn = json.optLong("expiresIn", 86400000L)

                LoginResult.Success(
                    role = role,
                    token = token,
                    username = retUsername,
                    userId = userId,
                    fullName = fullName,
                    expiresIn = expiresIn
                )
            } else {
                val errorText = try {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                } catch (e: Exception) {
                    ""
                }
                val serverMsg = try {
                    if (errorText.isNotEmpty()) JSONObject(errorText).optString("message", "") else ""
                } catch (e: Exception) {
                    ""
                }

                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    LoginResult.Failure(if (serverMsg.isNotEmpty()) serverMsg else "Invalid username or password. Please try again.")
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    LoginResult.Failure("Backend authentication endpoint (/api/auth/login) not found on $baseUrl. Verify server status.")
                } else {
                    LoginResult.Failure(if (serverMsg.isNotEmpty()) serverMsg else "Server returned response code $responseCode. Please verify backend status.")
                }
            }
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Connection refused at $baseUrl: ${e.message}", e)
            LoginResult.Failure("Cannot connect to backend server. Please check your internet connection.")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Socket timeout at $baseUrl: ${e.message}", e)
            LoginResult.Failure("Connection to backend timed out. Please check your network connection and try again.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during login at $baseUrl: ${e.message}", e)
            LoginResult.Failure("Authentication request failed at $baseUrl: ${e.localizedMessage ?: "Unknown network error"}")
        } finally {
            connection?.disconnect()
        }
    }

    fun logout(infoMessage: String? = null) {
        sessionManager?.clearSession()
        uiState = LoginUiState(infoMessage = infoMessage)
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun showServerSettingsDialog() {
        uiState = uiState.copy(
            isServerSettingsDialogVisible = true,
            serverUrlInput = ApiConfig.getBaseUrl(sessionManager)
        )
    }


    fun dismissServerSettingsDialog() {
        uiState = uiState.copy(isServerSettingsDialogVisible = false)
    }

    fun onServerUrlInputChanged(newUrl: String) {
        uiState = uiState.copy(serverUrlInput = newUrl)
    }

    fun saveCustomServerUrl() {
        val formatted = ApiConfig.formatBaseUrl(uiState.serverUrlInput)
        ApiConfig.setCustomDevUrl(sessionManager, formatted)
        backendBaseUrl = formatted
        dismissServerSettingsDialog()
    }

    fun resetServerUrlToDefault() {
        ApiConfig.setCustomDevUrl(sessionManager, null)
        backendBaseUrl = ApiConfig.getBaseUrl(sessionManager)
        uiState = uiState.copy(serverUrlInput = backendBaseUrl)
    }

    sealed class LoginResult {
        data class Success(
            val role: UserRole,
            val token: String,
            val username: String,
            val userId: Long,
            val fullName: String,
            val expiresIn: Long
        ) : LoginResult()
        data class Failure(val message: String) : LoginResult()
    }
}