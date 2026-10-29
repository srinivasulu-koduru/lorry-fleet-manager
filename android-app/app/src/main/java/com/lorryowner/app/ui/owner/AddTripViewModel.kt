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
import java.text.SimpleDateFormat
import java.util.Locale

data class LorryOption(
    val id: Long,
    val registrationNumber: String,
    val makeModel: String,
    val capacityTons: Double,
    val assignedDriverId: Long? = null,
    val assignedDriverName: String? = null
)

data class DriverOption(
    val id: Long,
    val fullName: String,
    val username: String,
    val phoneNumber: String?,
    val assignedLorryRegistration: String?
)

data class CreatedTripUiState(
    val id: Long,
    val tripReference: String,
    val lorryId: Long,
    val lorryRegistration: String,
    val driverId: Long,
    val driverName: String,
    val origin: String,
    val destination: String,
    val plannedDepartureFormatted: String,
    val plannedDeliveryFormatted: String?,
    val loadType: String,
    val remarks: String?,
    val status: String
)

data class AddTripUiState(
    val isLoadingLorriesAndDrivers: Boolean = false,
    val lorries: List<LorryOption> = emptyList(),
    val drivers: List<DriverOption> = emptyList(),
    val selectedLorry: LorryOption? = null,
    val selectedDriver: DriverOption? = null,
    val origin: String = "",
    val destination: String = "",
    val plannedDepartureDate: String = "",
    val plannedDepartureTime: String = "",
    val plannedDeliveryDate: String = "",
    val plannedDeliveryTime: String = "",
    val loadType: String = "General Goods",
    val customLoadType: String = "",
    val remarks: String = "",

    val lorryError: String? = null,
    val driverError: String? = null,
    val originError: String? = null,
    val destinationError: String? = null,
    val departureDateError: String? = null,
    val departureTimeError: String? = null,
    val deliveryDateError: String? = null,
    val deliveryTimeError: String? = null,
    val loadTypeError: String? = null,
    val generalError: String? = null,

    val isSubmitting: Boolean = false,
    val createdTripResult: CreatedTripUiState? = null
)

class AddTripViewModel(
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = (sessionManager != null)
) : ViewModel() {

    companion object {
        private const val TAG = "AddTripVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(AddTripUiState())
        private set

    init {
        if (autoLoad && sessionManager != null) {
            loadEligibleLorriesAndDrivers()
        }
    }

    fun loadEligibleLorriesAndDrivers() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingLorriesAndDrivers = true, generalError = null)
            val lorriesResult = withContext(Dispatchers.IO) { fetchLorries() }
            val driversResult = withContext(Dispatchers.IO) { fetchDrivers() }

            uiState = uiState.copy(isLoadingLorriesAndDrivers = false)

            if (lorriesResult.isSuccess && driversResult.isSuccess) {
                val fetchedLorries = lorriesResult.getOrDefault(emptyList())
                val fetchedDrivers = driversResult.getOrDefault(emptyList())

                // Default auto-select first lorry and its assigned driver if present
                val initialLorry = fetchedLorries.firstOrNull()
                val matchedDriver = fetchedDrivers.find { it.id == initialLorry?.assignedDriverId }
                    ?: fetchedDrivers.firstOrNull()

                uiState = uiState.copy(
                    lorries = fetchedLorries,
                    drivers = fetchedDrivers,
                    selectedLorry = uiState.selectedLorry ?: initialLorry,
                    selectedDriver = uiState.selectedDriver ?: matchedDriver
                )
            } else {
                val error = lorriesResult.exceptionOrNull()?.message
                    ?: driversResult.exceptionOrNull()?.message
                    ?: "Failed to load lorries and drivers"
                uiState = uiState.copy(generalError = error)
            }
        }
    }

    fun onLorrySelected(lorry: LorryOption) {
        uiState = uiState.copy(
            selectedLorry = lorry,
            lorryError = null
        )
        // If this lorry has an assigned driver, automatically suggest that driver
        if (lorry.assignedDriverId != null) {
            val assignedDriver = uiState.drivers.find { it.id == lorry.assignedDriverId }
            if (assignedDriver != null) {
                uiState = uiState.copy(selectedDriver = assignedDriver, driverError = null)
            }
        }
    }

    fun onDriverSelected(driver: DriverOption) {
        uiState = uiState.copy(
            selectedDriver = driver,
            driverError = null
        )
    }

    fun onOriginChange(value: String) {
        uiState = uiState.copy(origin = value, originError = null)
    }

    fun onDestinationChange(value: String) {
        uiState = uiState.copy(destination = value, destinationError = null)
    }

    fun onDepartureDateSelected(dateStr: String) {
        uiState = uiState.copy(plannedDepartureDate = dateStr, departureDateError = null)
    }

    fun onDepartureTimeSelected(timeStr: String) {
        uiState = uiState.copy(plannedDepartureTime = timeStr, departureTimeError = null)
    }

    fun onDeliveryDateSelected(dateStr: String) {
        uiState = uiState.copy(plannedDeliveryDate = dateStr, deliveryDateError = null)
    }

    fun onDeliveryTimeSelected(timeStr: String) {
        uiState = uiState.copy(plannedDeliveryTime = timeStr, deliveryTimeError = null)
    }

    fun onClearDeliverySchedule() {
        uiState = uiState.copy(
            plannedDeliveryDate = "",
            plannedDeliveryTime = "",
            deliveryDateError = null,
            deliveryTimeError = null
        )
    }

    fun onLoadTypeSelected(type: String) {
        uiState = uiState.copy(loadType = type, loadTypeError = null)
    }

    fun onCustomLoadTypeChange(value: String) {
        uiState = uiState.copy(customLoadType = value, loadTypeError = null)
    }

    fun onRemarksChange(value: String) {
        uiState = uiState.copy(remarks = value)
    }

    fun clearGeneralError() {
        uiState = uiState.copy(generalError = null)
    }

    fun validateForm(): Boolean {
        var hasError = false
        var lorryErr: String? = null
        var driverErr: String? = null
        var originErr: String? = null
        var destErr: String? = null
        var depDateErr: String? = null
        var depTimeErr: String? = null
        var delDateErr: String? = null
        var delTimeErr: String? = null
        var loadTypeErr: String? = null

        if (uiState.selectedLorry == null) {
            lorryErr = "Please select a commercial lorry"
            hasError = true
        }

        if (uiState.selectedDriver == null) {
            driverErr = "Please select an authorized driver"
            hasError = true
        }

        if (uiState.origin.trim().isEmpty()) {
            originErr = "Origin starting location is required"
            hasError = true
        }

        if (uiState.destination.trim().isEmpty()) {
            destErr = "Destination location is required"
            hasError = true
        } else if (uiState.origin.trim().equals(uiState.destination.trim(), ignoreCase = true)) {
            destErr = "Destination must be different from origin"
            hasError = true
        }

        if (uiState.plannedDepartureDate.isBlank()) {
            depDateErr = "Planned departure date is required"
            hasError = true
        }

        if (uiState.plannedDepartureTime.isBlank()) {
            depTimeErr = "Planned departure time is required"
            hasError = true
        }

        val hasDelDate = uiState.plannedDeliveryDate.isNotBlank()
        val hasDelTime = uiState.plannedDeliveryTime.isNotBlank()

        if (hasDelDate != hasDelTime) {
            if (!hasDelDate) delDateErr = "Delivery date required when time is selected"
            if (!hasDelTime) delTimeErr = "Delivery time required when date is selected"
            hasError = true
        } else if (hasDelDate && hasDelTime && depDateErr == null && depTimeErr == null) {
            try {
                val depStr = "${uiState.plannedDepartureDate} ${uiState.plannedDepartureTime}"
                val delStr = "${uiState.plannedDeliveryDate} ${uiState.plannedDeliveryTime}"
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dep = sdf.parse(depStr)
                val del = sdf.parse(delStr)
                if (dep != null && del != null && !del.after(dep)) {
                    delTimeErr = "Delivery time must be later than departure time"
                    hasError = true
                }
            } catch (e: Exception) {
                delTimeErr = "Invalid timestamp comparison"
                hasError = true
            }
        }

        val effectiveLoadType = if (uiState.loadType == "Other") uiState.customLoadType.trim() else uiState.loadType.trim()
        if (effectiveLoadType.isEmpty()) {
            loadTypeErr = "Please select or enter a cargo load type"
            hasError = true
        }

        uiState = uiState.copy(
            lorryError = lorryErr,
            driverError = driverErr,
            originError = originErr,
            destinationError = destErr,
            departureDateError = depDateErr,
            departureTimeError = depTimeErr,
            deliveryDateError = delDateErr,
            deliveryTimeError = delTimeErr,
            loadTypeError = loadTypeErr
        )

        return !hasError
    }

    fun submitTrip(onSuccess: (CreatedTripUiState) -> Unit) {
        if (!validateForm()) return
        if (uiState.isSubmitting) return

        uiState = uiState.copy(isSubmitting = true, generalError = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                executeCreateTrip()
            }
            uiState = uiState.copy(isSubmitting = false)
            when {
                result.isSuccess -> {
                    val created = result.getOrThrow()
                    uiState = uiState.copy(createdTripResult = created)
                    onSuccess(created)
                }
                else -> {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to create trip. Please try again."
                    uiState = uiState.copy(generalError = errorMsg)
                }
            }
        }
    }

    private fun executeCreateTrip(): Result<CreatedTripUiState> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing. Please log in again."))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        val effectiveLoadType = if (uiState.loadType == "Other") uiState.customLoadType.trim() else uiState.loadType.trim()

        val payload = JSONObject().apply {
            put("lorryId", uiState.selectedLorry!!.id)
            put("driverId", uiState.selectedDriver!!.id)
            put("origin", uiState.origin.trim())
            put("destination", uiState.destination.trim())
            put("plannedDepartureDate", uiState.plannedDepartureDate)
            put("plannedDepartureTime", uiState.plannedDepartureTime)
            if (uiState.plannedDeliveryDate.isNotBlank()) {
                put("plannedDeliveryDate", uiState.plannedDeliveryDate)
                put("plannedDeliveryTime", uiState.plannedDeliveryTime)
            }
            put("loadType", effectiveLoadType)
            if (uiState.remarks.trim().isNotBlank()) {
                put("remarks", uiState.remarks.trim())
            }
        }

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/trips"
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

                    val depFormatted = formatDateTimeDisplay(
                        uiState.plannedDepartureDate,
                        uiState.plannedDepartureTime
                    )
                    val delFormatted = if (uiState.plannedDeliveryDate.isNotBlank()) {
                        formatDateTimeDisplay(uiState.plannedDeliveryDate, uiState.plannedDeliveryTime)
                    } else null

                    val created = CreatedTripUiState(
                        id = json.optLong("id"),
                        tripReference = json.optString("tripReference", "TRIP-CREATED"),
                        lorryId = json.optLong("lorryId", uiState.selectedLorry!!.id),
                        lorryRegistration = json.optString("lorryRegistration", uiState.selectedLorry!!.registrationNumber),
                        driverId = json.optLong("driverId", uiState.selectedDriver!!.id),
                        driverName = json.optString("driverName", uiState.selectedDriver!!.fullName),
                        origin = json.optString("origin", uiState.origin.trim()),
                        destination = json.optString("destination", uiState.destination.trim()),
                        plannedDepartureFormatted = depFormatted,
                        plannedDeliveryFormatted = delFormatted,
                        loadType = effectiveLoadType,
                        remarks = uiState.remarks.trim().ifEmpty { null },
                        status = json.optString("status", "PLANNED")
                    )
                    return Result.success(created)
                } else {
                    val errStream = connection.errorStream
                    val errMsg = if (errStream != null) {
                        try {
                            val errJson = JSONObject(errStream.bufferedReader().use(BufferedReader::readText))
                            errJson.optString("message", "Server error ($responseCode)")
                        } catch (e: Exception) {
                            "Server returned error code $responseCode"
                        }
                    } else {
                        "Server returned error code $responseCode"
                    }
                    return Result.failure(Exception(errMsg))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect to $endpointUrl: ${e.message}")
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        return Result.failure(lastException ?: Exception("Network error: Could not reach backend server"))
    }

    private fun fetchLorries(): Result<List<LorryOption>> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing"))
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastEx: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/lorries?filter=ALL"
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
                    val list = mutableListOf<LorryOption>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            LorryOption(
                                id = obj.optLong("id"),
                                registrationNumber = obj.optString("registrationNumber"),
                                makeModel = obj.optString("makeModel"),
                                capacityTons = obj.optDouble("capacityTons", 0.0),
                                assignedDriverId = if (obj.has("assignedDriverId") && !obj.isNull("assignedDriverId")) obj.optLong("assignedDriverId") else null,
                                assignedDriverName = if (obj.has("assignedDriverName") && !obj.isNull("assignedDriverName")) obj.optString("assignedDriverName") else null
                            )
                        )
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
        return Result.failure(lastEx ?: Exception("Failed to load lorries"))
    }

    private fun fetchDrivers(): Result<List<DriverOption>> {
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
                    val list = mutableListOf<DriverOption>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val isActive = obj.optBoolean("isActive", true)
                        if (isActive) {
                            list.add(
                                DriverOption(
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

    fun formatDateTimeDisplay(dateStr: String, timeStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val date = parser.parse("$dateStr $timeStr")
            if (date != null) formatter.format(date) else "$dateStr $timeStr"
        } catch (e: Exception) {
            "$dateStr $timeStr"
        }
    }
}
