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

class VehicleMaintenanceDetailsViewModel(
    val lorryId: Long,
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = (sessionManager != null)
) : ViewModel() {

    companion object {
        private const val TAG = "VehicleMaintDetailsVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(VehicleMaintenanceDetailsUiState(lorryId = lorryId))
        private set

    init {
        if (autoLoad && lorryId > 0) {
            loadDetails()
        }
    }

    fun loadDetails() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchDetailsFromBackend()
            }
            if (result.isSuccess) {
                uiState = result.getOrThrow().copy(isLoading = false, errorMessage = null)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Unable to load vehicle maintenance details"
                )
            }
        }
    }

    private fun fetchDetailsFromBackend(): Result<VehicleMaintenanceDetailsUiState> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("No authenticated session"))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                // Try primary endpoint /api/owner/maintenance/vehicles/{id}
                val url = URL("$baseUrl/api/owner/maintenance/vehicles/$lorryId")
                conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(rawJson)
                    val state = parseDetailsJson(json)
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(state)
                } else if (conn.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // Try fallback endpoint /api/owner/lorries/{id}/maintenance
                    conn.disconnect()
                    val fallbackUrl = URL("$baseUrl/api/owner/lorries/$lorryId/maintenance")
                    conn = (fallbackUrl.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val rawJson = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                        val json = JSONObject(rawJson)
                        val state = parseDetailsJson(json)
                        ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                        return Result.success(state)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load details on $baseUrl", e)
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(lastException ?: Exception("Network connection failed"))
    }

    private fun parseDetailsJson(json: JSONObject): VehicleMaintenanceDetailsUiState {
        val reg = json.optString("registrationNumber", "Vehicle")
        val make = json.optString("makeModel", "Commercial Vehicle")
        val lastDate = json.optString("lastServiceDate").takeIf { it.isNotBlank() && it != "null" }
        val nextDate = json.optString("nextServiceDate").takeIf { it.isNotBlank() && it != "null" }
        val status = json.optString("status", "Up to Date")
        val totalCost = json.optDouble("totalMaintenanceCost", 0.0)

        val recordsList = mutableListOf<MaintenanceHistoryRecordItem>()
        val recordsArray = json.optJSONArray("records")
        if (recordsArray != null) {
            for (i in 0 until recordsArray.length()) {
                val r = recordsArray.getJSONObject(i)
                recordsList.add(
                    MaintenanceHistoryRecordItem(
                        id = r.optLong("id", i.toLong() + 1),
                        lorryId = r.optLong("lorryId", lorryId),
                        serviceType = r.optString("serviceType", "Service"),
                        maintenanceDate = r.optString("maintenanceDate", ""),
                        description = r.optString("description", ""),
                        cost = r.optDouble("cost", 0.0),
                        odometerReadingKm = if (r.has("odometerReadingKm") && !r.isNull("odometerReadingKm")) r.optInt("odometerReadingKm") else null,
                        nextServiceDate = r.optString("nextServiceDate").takeIf { it.isNotBlank() && it != "null" },
                        serviceCenter = r.optString("serviceCenter").takeIf { it.isNotBlank() && it != "null" },
                        status = "Completed"
                    )
                )
            }
        }

        return VehicleMaintenanceDetailsUiState(
            lorryId = lorryId,
            registrationNumber = reg,
            makeModel = make,
            lastServiceDate = lastDate,
            nextServiceDate = nextDate,
            status = status,
            totalMaintenanceCost = totalCost,
            records = recordsList
        )
    }

    fun setTestData(state: VehicleMaintenanceDetailsUiState) {
        uiState = state
    }
}
