package com.lorryowner.app.ui.owner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorryowner.app.network.ApiConfig
import com.lorryowner.app.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class RunningLorriesViewModel(
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = true
) : ViewModel() {

    var uiState by mutableStateOf(RunningLorriesUiState())
        private set

    private var pollingJob: Job? = null

    init {
        if (autoLoad) {
            loadRunningLorries()
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000L)
                fetchRunningLorriesSilent()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun loadRunningLorries() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchRunningLorriesFromNetwork()
            }
            uiState = if (result.isSuccess) {
                uiState.copy(
                    isLoading = false,
                    lorries = result.getOrThrow(),
                    errorMessage = null
                )
            } else {
                uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load running lorries"
                )
            }
        }
    }

    private suspend fun fetchRunningLorriesSilent() {
        val result = withContext(Dispatchers.IO) {
            fetchRunningLorriesFromNetwork()
        }
        if (result.isSuccess) {
            uiState = uiState.copy(lorries = result.getOrThrow(), errorMessage = null)
        }
    }

    private fun fetchRunningLorriesFromNetwork(): Result<List<LiveTrackingLorryUiState>> {
        val token = sessionManager?.getAccessToken() ?: ""
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)

        for (baseUrl in candidateUrls) {
            try {
                val url = URL("$baseUrl/api/owner/tracking/running-lorries")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    if (token.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }

                val code = conn.responseCode
                if (code == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val array = JSONArray(body)
                    val list = mutableListOf<LiveTrackingLorryUiState>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            LiveTrackingLorryUiState(
                                lorryId = obj.optLong("lorryId"),
                                registrationNumber = obj.optString("registrationNumber"),
                                makeModel = obj.optString("makeModel"),
                                tripId = obj.optLong("tripId"),
                                tripReference = obj.optString("tripReference"),
                                driverName = obj.optString("driverName"),
                                driverPhone = if (obj.has("driverPhone") && !obj.isNull("driverPhone")) obj.getString("driverPhone") else null,
                                originLocation = obj.optString("originLocation"),
                                destinationLocation = obj.optString("destinationLocation"),
                                currentLocationDescription = obj.optString("currentLocationDescription", "Waiting for driver location"),
                                latitude = if (obj.has("latitude") && !obj.isNull("latitude")) obj.getDouble("latitude") else null,
                                longitude = if (obj.has("longitude") && !obj.isNull("longitude")) obj.getDouble("longitude") else null,
                                currentSpeedKmh = if (obj.has("currentSpeedKmh") && !obj.isNull("currentSpeedKmh")) obj.getDouble("currentSpeedKmh") else null,
                                heading = if (obj.has("heading") && !obj.isNull("heading")) obj.getDouble("heading") else null,
                                accuracyMeters = if (obj.has("accuracyMeters") && !obj.isNull("accuracyMeters")) obj.getDouble("accuracyMeters") else null,
                                distanceTravelledKm = obj.optDouble("distanceTravelledKm", 0.0),
                                remainingDistanceKm = obj.optDouble("remainingDistanceKm", 0.0),
                                estimatedDrivingTime = obj.optString("estimatedDrivingTime", "--"),
                                estimatedArrival = obj.optString("estimatedArrival", "--"),
                                gpsStatus = TrackingFormattingHelper.parseGpsStatus(obj.optString("gpsStatus")),
                                lastGpsUpdate = if (obj.has("lastGpsUpdate") && !obj.isNull("lastGpsUpdate")) obj.getString("lastGpsUpdate") else null,
                                isLive = obj.optBoolean("isLive", false),
                                hasGpsFix = obj.optBoolean("hasGpsFix", false)
                            )
                        )
                    }
                    return Result.success(list)
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Try next base URL
            }
        }
        return Result.failure(Exception("Unable to connect to backend server to load running lorries."))
    }

    fun setTestData(lorries: List<LiveTrackingLorryUiState>) {
        uiState = uiState.copy(isLoading = false, lorries = lorries, errorMessage = null)
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}
