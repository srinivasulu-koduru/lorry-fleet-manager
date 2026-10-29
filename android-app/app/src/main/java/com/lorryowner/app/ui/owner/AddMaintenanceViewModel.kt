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
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class AddMaintenanceFormState(
    val lorries: List<LorrySelectItem> = emptyList(),
    val selectedLorryId: Long? = null,
    val selectedLorryDisplay: String = "",
    val serviceType: String = "Regular Service",
    val serviceDate: String = LocalDate.now().toString(),
    val description: String = "",
    val cost: String = "",
    val odometer: String = "",
    val nextServiceDate: String = "",
    val garage: String = "",
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val isLoadingLorries: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)

class AddMaintenanceViewModel(
    private val sessionManager: SessionManager? = null,
    val initialLorryId: Long? = null,
    autoLoad: Boolean = (sessionManager != null)
) : ViewModel() {

    companion object {
        private const val TAG = "AddMaintenanceVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000

        val MAINTENANCE_TYPES = listOf(
            "Regular Service",
            "Engine Repair",
            "Tyre Replacement",
            "Brake Service",
            "Electrical Repair",
            "Other"
        )
    }

    var formState by mutableStateOf(
        AddMaintenanceFormState(
            selectedLorryId = initialLorryId,
            serviceDate = LocalDate.now().toString()
        )
    )
        private set

    init {
        if (autoLoad) {
            loadLorries()
        }
    }

    fun onLorrySelected(lorry: LorrySelectItem) {
        formState = formState.copy(
            selectedLorryId = lorry.id,
            selectedLorryDisplay = "${lorry.registrationNumber} - ${lorry.makeModel}",
            errorMessage = null
        )
    }

    fun onServiceTypeChanged(type: String) {
        formState = formState.copy(serviceType = type, errorMessage = null)
    }

    fun onServiceDateChanged(date: String) {
        formState = formState.copy(serviceDate = date, errorMessage = null)
    }

    fun onDescriptionChanged(desc: String) {
        formState = formState.copy(description = desc, errorMessage = null)
    }

    fun onCostChanged(cost: String) {
        formState = formState.copy(cost = cost, errorMessage = null)
    }

    fun onOdometerChanged(odo: String) {
        formState = formState.copy(odometer = odo, errorMessage = null)
    }

    fun onNextServiceDateChanged(date: String) {
        formState = formState.copy(nextServiceDate = date, errorMessage = null)
    }

    fun onGarageChanged(garage: String) {
        formState = formState.copy(garage = garage, errorMessage = null)
    }

    fun onNotesChanged(notes: String) {
        formState = formState.copy(notes = notes, errorMessage = null)
    }

    fun clearMessages() {
        formState = formState.copy(errorMessage = null, successMessage = null)
    }

    fun loadLorries() {
        viewModelScope.launch {
            formState = formState.copy(isLoadingLorries = true)
            val result = withContext(Dispatchers.IO) {
                fetchLorriesFromBackend()
            }
            if (result.isSuccess) {
                val list = result.getOrThrow()
                val preselected = list.firstOrNull { it.id == initialLorryId } ?: list.firstOrNull()
                formState = formState.copy(
                    lorries = list,
                    selectedLorryId = preselected?.id,
                    selectedLorryDisplay = preselected?.let { "${it.registrationNumber} - ${it.makeModel}" } ?: "",
                    isLoadingLorries = false
                )
            } else {
                formState = formState.copy(isLoadingLorries = false)
            }
        }
    }

    fun validate(): String? {
        if (formState.selectedLorryId == null) {
            return "Please select a lorry"
        }
        if (formState.serviceType.isBlank()) {
            return "Please select a maintenance type"
        }
        if (formState.serviceDate.isBlank()) {
            return "Please enter a valid service date"
        }
        if (formState.description.trim().isBlank()) {
            return "Please enter a description of the service performed"
        }
        val costVal = formState.cost.toDoubleOrNull()
        if (costVal == null || costVal < 0) {
            return "Please enter a valid non-negative maintenance cost"
        }
        val odoVal = formState.odometer.toIntOrNull()
        if (odoVal == null || odoVal < 0) {
            return "Please enter a valid non-negative odometer reading"
        }
        if (formState.nextServiceDate.isNotBlank()) {
            try {
                val svcDate = LocalDate.parse(formState.serviceDate.trim())
                val nextDate = LocalDate.parse(formState.nextServiceDate.trim())
                if (nextDate.isBefore(svcDate)) {
                    return "Next service date cannot be earlier than service date"
                }
            } catch (e: Exception) {
                // Ignore parse errors if date format is non-ISO
            }
        }
        return null
    }

    fun submitRecord(onSuccess: () -> Unit = {}) {
        if (formState.isSubmitting) return // Prevent repeated taps

        val validationError = validate()
        if (validationError != null) {
            formState = formState.copy(errorMessage = validationError)
            return
        }

        viewModelScope.launch {
            formState = formState.copy(isSubmitting = true, errorMessage = null, successMessage = null)
            val result = withContext(Dispatchers.IO) {
                postMaintenanceRecord()
            }
            formState = formState.copy(isSubmitting = false)
            if (result.isSuccess) {
                formState = formState.copy(
                    isSuccess = true,
                    successMessage = "Maintenance record saved successfully!"
                )
                onSuccess()
            } else {
                formState = formState.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to save maintenance record"
                )
            }
        }
    }

    private fun postMaintenanceRecord(): Result<Unit> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("No authenticated session"))

        val lorryId = formState.selectedLorryId
            ?: return Result.failure(IllegalStateException("No lorry selected"))

        val payload = JSONObject().apply {
            put("lorryId", lorryId)
            put("serviceType", formState.serviceType.trim())
            put("maintenanceDate", formState.serviceDate.trim())
            put("description", formState.description.trim())
            put("cost", formState.cost.toDouble())
            put("odometerReadingKm", formState.odometer.toInt())
            if (formState.nextServiceDate.isNotBlank()) {
                put("nextServiceDate", formState.nextServiceDate.trim())
            }
            if (formState.garage.isNotBlank()) {
                put("serviceCenter", formState.garage.trim())
            }
            if (formState.notes.isNotBlank()) {
                put("notes", formState.notes.trim())
            }
        }

        val jsonString = payload.toString()
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/owner/lorries/$lorryId/maintenance")
                conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    outputStream.use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }
                }

                if (conn.responseCode in 200..299) {
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(Unit)
                } else {
                    val errorStream = conn.errorStream
                    val errorMsg = if (errorStream != null) {
                        try {
                            val rawErr = errorStream.bufferedReader().use(BufferedReader::readText)
                            val errJson = JSONObject(rawErr)
                            errJson.optString("message", "Error: HTTP ${conn.responseCode}")
                        } catch (e: Exception) {
                            "Error: HTTP ${conn.responseCode}"
                        }
                    } else "Error: HTTP ${conn.responseCode}"
                    return Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed posting to $baseUrl", e)
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(lastException ?: Exception("Network connection failed"))
    }

    private fun fetchLorriesFromBackend(): Result<List<LorrySelectItem>> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("No authenticated session"))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/owner/lorries")
                conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val array = JSONArray(rawJson)
                    val list = mutableListOf<LorrySelectItem>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            LorrySelectItem(
                                id = obj.optLong("id"),
                                registrationNumber = obj.optString("registrationNumber"),
                                makeModel = obj.optString("makeModel")
                            )
                        )
                    }
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(list)
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(lastException ?: Exception("Unable to load lorries"))
    }

    fun setTestData(state: AddMaintenanceFormState) {
        formState = state
    }
}
