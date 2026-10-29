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

class MaintenanceDashboardViewModel(
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = (sessionManager != null)
) : ViewModel() {

    companion object {
        private const val TAG = "MaintenanceDashVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(MaintenanceDashboardUiState())
        private set

    init {
        if (autoLoad) {
            loadSummary()
        }
    }

    fun loadSummary() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchSummaryFromBackend()
            }
            if (result.isSuccess) {
                val state = result.getOrThrow()
                uiState = state.copy(isLoading = false, errorMessage = null)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Unable to load maintenance summary"
                )
            }
        }
    }

    private fun fetchSummaryFromBackend(): Result<MaintenanceDashboardUiState> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("No authenticated session"))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/owner/maintenance/summary")
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

                    val totalVehicles = json.optLong("totalVehicles", 0L)
                    val dueSoonCount = json.optLong("dueSoonCount", 0L)
                    val overdueCount = json.optLong("overdueCount", 0L)
                    val totalExpenditure = json.optDouble("totalExpenditure", 0.0)

                    val vehiclesList = mutableListOf<VehicleMaintenanceSummaryItem>()
                    val vehiclesArray = json.optJSONArray("vehicles")
                    if (vehiclesArray != null) {
                        for (i in 0 until vehiclesArray.length()) {
                            val vObj = vehiclesArray.getJSONObject(i)
                            vehiclesList.add(
                                VehicleMaintenanceSummaryItem(
                                    lorryId = vObj.optLong("lorryId", 0L),
                                    registrationNumber = vObj.optString("registrationNumber", "Vehicle"),
                                    makeModel = vObj.optString("makeModel", "Commercial Vehicle"),
                                    lastServiceDate = vObj.optString("lastServiceDate").takeIf { it.isNotBlank() && it != "null" },
                                    nextServiceDate = vObj.optString("nextServiceDate").takeIf { it.isNotBlank() && it != "null" },
                                    status = vObj.optString("status", "Up to Date"),
                                    totalCost = vObj.optDouble("totalCost", 0.0),
                                    totalRecordsCount = vObj.optInt("totalRecordsCount", 0)
                                )
                            )
                        }
                    }

                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(
                        MaintenanceDashboardUiState(
                            totalVehicles = totalVehicles,
                            dueSoonCount = dueSoonCount,
                            overdueCount = overdueCount,
                            totalExpenditure = totalExpenditure,
                            vehicles = vehiclesList
                        )
                    )
                } else {
                    Log.w(TAG, "Server responded with HTTP ${conn.responseCode} on $baseUrl")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Connection failed on $baseUrl", e)
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(lastException ?: Exception("Network connection failed"))
    }

    fun setTestData(state: MaintenanceDashboardUiState) {
        uiState = state
    }
}
