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
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class DriverSelectionOption(
    val id: Long,
    val fullName: String,
    val username: String,
    val phoneNumber: String?,
    val assignedLorryRegistration: String?
)

data class AddLorryUiState(
    val registrationNumber: String = "",
    val makeModel: String = "",
    val vehicleType: String = "Heavy Commercial Vehicle (HCV)",
    val capacityTons: String = "",
    val manufacturingYear: String = "",
    val ialertUnitId: String = "",
    val notes: String = "",
    val selectedDriver: DriverSelectionOption? = null,
    val availableDrivers: List<DriverSelectionOption> = emptyList(),
    val isLoadingDrivers: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val registrationNumberError: String? = null,
    val makeModelError: String? = null,
    val capacityTonsError: String? = null,
    val manufacturingYearError: String? = null,
    val isSuccess: Boolean = false,
    val createdLorryId: Long? = null,
    val createdRegistrationNumber: String? = null
)

class AddLorryViewModel(
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = (sessionManager != null)
) : ViewModel() {

    companion object {
        private const val TAG = "AddLorryVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(AddLorryUiState())
        private set

    init {
        if (autoLoad && sessionManager != null) {
            loadEligibleDrivers()
        }
    }

    fun onRegistrationNumberChange(value: String) {
        val uppercase = value.uppercase()
        uiState = uiState.copy(
            registrationNumber = uppercase,
            registrationNumberError = null,
            errorMessage = null
        )
    }

    fun onMakeModelChange(value: String) {
        uiState = uiState.copy(
            makeModel = value,
            makeModelError = null,
            errorMessage = null
        )
    }

    fun onVehicleTypeChange(value: String) {
        uiState = uiState.copy(
            vehicleType = value,
            errorMessage = null
        )
    }

    fun onCapacityTonsChange(value: String) {
        // Allow digits and a single decimal point
        val filtered = value.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            uiState = uiState.copy(
                capacityTons = filtered,
                capacityTonsError = null,
                errorMessage = null
            )
        }
    }

    fun onManufacturingYearChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(4)
        uiState = uiState.copy(
            manufacturingYear = filtered,
            manufacturingYearError = null,
            errorMessage = null
        )
    }

    fun onIalertUnitIdChange(value: String) {
        uiState = uiState.copy(
            ialertUnitId = value,
            errorMessage = null
        )
    }

    fun onNotesChange(value: String) {
        uiState = uiState.copy(
            notes = value,
            errorMessage = null
        )
    }

    fun onDriverSelected(driver: DriverSelectionOption?) {
        uiState = uiState.copy(
            selectedDriver = driver,
            errorMessage = null
        )
    }

    fun clearErrorMessage() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun validateForm(): Boolean {
        var hasError = false
        var regError: String? = null
        var modelError: String? = null
        var capacityError: String? = null
        var yearError: String? = null

        val trimmedReg = uiState.registrationNumber.trim()
        if (trimmedReg.isEmpty()) {
            regError = "Registration number is required"
            hasError = true
        } else if (trimmedReg.length < 3) {
            regError = "Registration number must be at least 3 characters"
            hasError = true
        } else if (trimmedReg.length > 20) {
            regError = "Registration number cannot exceed 20 characters"
            hasError = true
        }

        val trimmedModel = uiState.makeModel.trim()
        if (trimmedModel.isEmpty()) {
            modelError = "Vehicle make and model is required"
            hasError = true
        } else if (trimmedModel.length > 100) {
            modelError = "Make and model cannot exceed 100 characters"
            hasError = true
        }

        val capacityVal = uiState.capacityTons.trim().toDoubleOrNull()
        if (uiState.capacityTons.trim().isEmpty() || capacityVal == null) {
            capacityError = "Payload capacity is required"
            hasError = true
        } else if (capacityVal <= 0.0) {
            capacityError = "Capacity must be greater than 0 tons"
            hasError = true
        } else if (capacityVal > 200.0) {
            capacityError = "Capacity cannot exceed 200 tons"
            hasError = true
        }

        if (uiState.manufacturingYear.isNotBlank()) {
            val yearVal = uiState.manufacturingYear.toIntOrNull()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            if (yearVal == null || yearVal < 1990 || yearVal > currentYear + 1) {
                yearError = "Enter a valid year between 1990 and ${currentYear + 1}"
                hasError = true
            }
        }

        uiState = uiState.copy(
            registrationNumberError = regError,
            makeModelError = modelError,
            capacityTonsError = capacityError,
            manufacturingYearError = yearError
        )

        return !hasError
    }

    fun loadEligibleDrivers() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingDrivers = true)
            val result = withContext(Dispatchers.IO) { fetchDrivers() }
            uiState = uiState.copy(
                isLoadingDrivers = false,
                availableDrivers = result.getOrDefault(emptyList())
            )
        }
    }

    fun submitLorry(onSuccess: (Long) -> Unit) {
        if (!validateForm()) return
        if (uiState.isSubmitting) return

        uiState = uiState.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { executeCreateLorry() }
            uiState = uiState.copy(isSubmitting = false)

            when {
                result.isSuccess -> {
                    val createdId = result.getOrThrow()
                    uiState = uiState.copy(
                        isSuccess = true,
                        createdLorryId = createdId,
                        createdRegistrationNumber = uiState.registrationNumber.trim()
                    )
                    onSuccess(createdId)
                }
                else -> {
                    val rawError = result.exceptionOrNull()?.message ?: "Failed to register lorry. Please try again."
                    if (rawError.contains("already registered", ignoreCase = true) ||
                        rawError.contains("already exists", ignoreCase = true)) {
                        uiState = uiState.copy(
                            errorMessage = rawError,
                            registrationNumberError = "This registration number is already registered"
                        )
                    } else {
                        uiState = uiState.copy(errorMessage = rawError)
                    }
                }
            }
        }
    }

    private fun executeCreateLorry(): Result<Long> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing. Please log in again."))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        // Compose notes with vehicle type & manufacturing year if provided
        val notesParts = mutableListOf<String>()
        if (uiState.vehicleType.isNotBlank()) {
            notesParts.add("Type: ${uiState.vehicleType}")
        }
        if (uiState.manufacturingYear.isNotBlank()) {
            notesParts.add("Year: ${uiState.manufacturingYear}")
        }
        if (uiState.notes.isNotBlank()) {
            notesParts.add(uiState.notes.trim())
        }
        val combinedNotes = notesParts.joinToString(" | ")

        val payload = JSONObject().apply {
            put("registrationNumber", uiState.registrationNumber.trim().uppercase())
            put("makeModel", uiState.makeModel.trim())
            put("capacityTons", uiState.capacityTons.trim().toDouble())
            put("operationalStatus", "AVAILABLE")
            if (uiState.ialertUnitId.isNotBlank()) {
                put("ialertUnitId", uiState.ialertUnitId.trim())
            }
            if (combinedNotes.isNotBlank()) {
                put("notes", combinedNotes)
            }
            if (uiState.selectedDriver != null) {
                put("assignedDriverId", uiState.selectedDriver!!.id)
            }
        }

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/lorries"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpointUrl)
                connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(rawJson)
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    val createdId = json.optLong("id")
                    return Result.success(createdId)
                }

                val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                val parsedMessage = extractErrorMessage(errorBody, responseCode)
                lastException = Exception(parsedMessage)

                // If client validation error or duplicate error, don't try other candidate URLs
                if (responseCode in 400..499) {
                    return Result.failure(lastException)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Request to $endpointUrl failed: ${e.message}")
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        return Result.failure(lastException ?: Exception("Could not connect to backend server. Please verify your connection."))
    }

    private fun fetchDrivers(): Result<List<DriverSelectionOption>> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing"))
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastEx: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/drivers"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpointUrl)
                connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val arr = JSONArray(rawJson)
                    val list = mutableListOf<DriverSelectionOption>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val isActive = obj.optBoolean("isActive", true)
                        if (isActive) {
                            list.add(
                                DriverSelectionOption(
                                    id = obj.optLong("id"),
                                    fullName = obj.optString("fullName"),
                                    username = obj.optString("username"),
                                    phoneNumber = if (obj.has("phoneNumber") && !obj.isNull("phoneNumber")) obj.optString("phoneNumber") else null,
                                    assignedLorryRegistration = if (obj.has("assignedLorryRegistration") && !obj.isNull("assignedLorryRegistration")) obj.optString("assignedLorryRegistration") else null
                                )
                            )
                        }
                    }
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(list)
                }
            } catch (e: Exception) {
                lastEx = e
            } finally {
                connection?.disconnect()
            }
        }
        return Result.failure(lastEx ?: Exception("Failed to load drivers"))
    }

    private fun extractErrorMessage(body: String, code: Int): String {
        if (body.isBlank()) {
            return when (code) {
                HttpURLConnection.HTTP_BAD_REQUEST -> "Invalid lorry information provided."
                HttpURLConnection.HTTP_CONFLICT -> "A lorry with this registration number already exists."
                HttpURLConnection.HTTP_UNAUTHORIZED -> "Session expired. Please log in again."
                HttpURLConnection.HTTP_FORBIDDEN -> "Access denied."
                else -> "Server error ($code). Please try again."
            }
        }
        return try {
            val json = JSONObject(body)
            when {
                json.has("message") && !json.isNull("message") -> json.getString("message")
                json.has("error") && !json.isNull("error") -> json.getString("error")
                else -> body
            }
        } catch (e: Exception) {
            body
        }
    }
}
