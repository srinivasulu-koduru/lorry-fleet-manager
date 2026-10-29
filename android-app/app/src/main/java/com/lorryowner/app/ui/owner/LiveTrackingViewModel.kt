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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LiveTrackingViewModel(
    private val sessionManager: SessionManager? = null,
    private val lorryId: Long? = null,
    private val tripId: Long? = null,
    autoLoad: Boolean = true
) : ViewModel() {

    var uiState by mutableStateOf(LiveTrackingScreenUiState())
        private set

    private var pollingJob: Job? = null

    init {
        if (autoLoad) {
            loadTrackingDetails()
        }
    }

    fun toggleMapMode() {
        uiState = uiState.copy(isSatelliteMode = !uiState.isSatelliteMode)
    }

    fun setSatelliteMode(enabled: Boolean) {
        uiState = uiState.copy(isSatelliteMode = enabled)
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(6_000L) // Refresh live tracking coordinates every 6 seconds
                fetchTrackingDetailsSilent()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun loadTrackingDetails() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchTrackingFromNetwork()
            }
            uiState = if (result.isSuccess) {
                uiState.copy(
                    isLoading = false,
                    trackingData = result.getOrThrow(),
                    errorMessage = null
                )
            } else {
                uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load live tracking details"
                )
            }
        }
    }

    private suspend fun fetchTrackingDetailsSilent() {
        val result = withContext(Dispatchers.IO) {
            fetchTrackingFromNetwork()
        }
        if (result.isSuccess) {
            uiState = uiState.copy(
                trackingData = result.getOrThrow(),
                errorMessage = null
            )
        }
    }

    private fun fetchTrackingFromNetwork(): Result<LiveTrackingLorryUiState> {
        val token = sessionManager?.getAccessToken() ?: ""
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)

        val endpoint = when {
            tripId != null && tripId > 0 -> "/api/owner/tracking/trips/$tripId"
            lorryId != null && lorryId > 0 -> "/api/owner/tracking/lorries/$lorryId"
            else -> "/api/owner/tracking/running-lorries"
        }

        for (baseUrl in candidateUrls) {
            try {
                val url = URL("$baseUrl$endpoint")
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

                    val obj = if (body.trim().startsWith("[")) {
                        val arr = org.json.JSONArray(body)
                        if (arr.length() > 0) arr.getJSONObject(0) else return Result.failure(Exception("No running lorries found"))
                    } else {
                        JSONObject(body)
                    }

                    val breadcrumbs = mutableListOf<BreadcrumbPoint>()
                    if (obj.has("breadcrumbs") && !obj.isNull("breadcrumbs")) {
                        val bArray = obj.getJSONArray("breadcrumbs")
                        for (i in 0 until bArray.length()) {
                            val bObj = bArray.getJSONObject(i)
                            breadcrumbs.add(
                                BreadcrumbPoint(
                                    latitude = bObj.getDouble("latitude"),
                                    longitude = bObj.getDouble("longitude"),
                                    speedKmh = if (bObj.has("speedKmh") && !bObj.isNull("speedKmh")) bObj.getDouble("speedKmh") else null,
                                    timestamp = if (bObj.has("timestamp") && !bObj.isNull("timestamp")) bObj.getString("timestamp") else null
                                )
                            )
                        }
                    }

                    val parsed = LiveTrackingLorryUiState(
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
                        hasGpsFix = obj.optBoolean("hasGpsFix", false),
                        originLatitude = if (obj.has("originLatitude") && !obj.isNull("originLatitude")) obj.getDouble("originLatitude") else null,
                        originLongitude = if (obj.has("originLongitude") && !obj.isNull("originLongitude")) obj.getDouble("originLongitude") else null,
                        destinationLatitude = if (obj.has("destinationLatitude") && !obj.isNull("destinationLatitude")) obj.getDouble("destinationLatitude") else null,
                        destinationLongitude = if (obj.has("destinationLongitude") && !obj.isNull("destinationLongitude")) obj.getDouble("destinationLongitude") else null,
                        breadcrumbs = breadcrumbs
                    )
                    return Result.success(parsed)
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Try next
            }
        }
        return Result.failure(Exception("Unable to connect to live tracking service."))
    }

    fun setTestData(data: LiveTrackingLorryUiState) {
        uiState = uiState.copy(isLoading = false, trackingData = data, errorMessage = null)
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}
