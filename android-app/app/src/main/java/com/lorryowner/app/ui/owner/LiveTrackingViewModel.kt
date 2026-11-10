package com.lorryowner.app.ui.owner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.lorryowner.app.network.ApiConfig
import com.lorryowner.app.network.CityCoordinateResolver
import com.lorryowner.app.network.OsrmRoutingService
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
            if (result.isSuccess) {
                val enrichedData = attachRoadRouteAndDeviation(result.getOrThrow())
                uiState = uiState.copy(
                    isLoading = false,
                    trackingData = enrichedData,
                    errorMessage = null
                )
            } else {
                uiState = uiState.copy(
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
            val enrichedData = attachRoadRouteAndDeviation(result.getOrThrow())
            uiState = uiState.copy(
                trackingData = enrichedData,
                errorMessage = null
            )
        }
    }

    private var lastRouteQueryLat: Double? = null
    private var lastRouteQueryLon: Double? = null
    private var cachedRoadRoute: List<LatLng> = emptyList()

    private suspend fun attachRoadRouteAndDeviation(parsed: LiveTrackingLorryUiState): LiveTrackingLorryUiState {
        // Resolve destination coordinates safely:
        // If destinationLocation is a known city in CityCoordinateResolver, use its authoritative coordinate;
        // Also if backend sent default Hyderabad (17.3850, 78.4867) while destinationLocation is NOT Hyderabad, override it!
        val resolvedDest = CityCoordinateResolver.resolveCityCoordinates(parsed.destinationLocation)
        val isDestKnown = CityCoordinateResolver.findInLookupTable(parsed.destinationLocation) != null
        val backendDestIsDefaultHyd = parsed.destinationLatitude != null &&
                kotlin.math.abs(parsed.destinationLatitude - 17.3850) < 0.01 &&
                kotlin.math.abs((parsed.destinationLongitude ?: 0.0) - 78.4867) < 0.01 &&
                !parsed.destinationLocation.lowercase().contains("hyderabad")

        val destLat = if (isDestKnown || backendDestIsDefaultHyd) resolvedDest.latitude else (parsed.destinationLatitude ?: resolvedDest.latitude)
        val destLon = if (isDestKnown || backendDestIsDefaultHyd) resolvedDest.longitude else (parsed.destinationLongitude ?: resolvedDest.longitude)

        // Resolve origin coordinates safely
        val resolvedOrigin = CityCoordinateResolver.resolveCityCoordinates(parsed.originLocation)
        val isOriginKnown = CityCoordinateResolver.findInLookupTable(parsed.originLocation) != null
        val backendOriginIsDefaultHyd = parsed.originLatitude != null &&
                kotlin.math.abs(parsed.originLatitude - 17.3850) < 0.01 &&
                kotlin.math.abs((parsed.originLongitude ?: 0.0) - 78.4867) < 0.01 &&
                !parsed.originLocation.lowercase().contains("hyderabad")

        val originLat = if (isOriginKnown || backendOriginIsDefaultHyd) resolvedOrigin.latitude else (parsed.originLatitude ?: resolvedOrigin.latitude)
        val originLon = if (isOriginKnown || backendOriginIsDefaultHyd) resolvedOrigin.longitude else (parsed.originLongitude ?: resolvedOrigin.longitude)

        // Start position: prefer live truck position (or fallback to origin)
        val truckLat = parsed.latitude
        val truckLon = parsed.longitude
        val startLat = truckLat ?: originLat
        val startLon = truckLon ?: originLon

        val finalRoute = if (destLat != null && destLon != null && startLat != null && startLon != null) {
            val lastLat = lastRouteQueryLat
            val lastLon = lastRouteQueryLon
            val isNearby = lastLat != null && lastLon != null &&
                    OsrmRoutingService.haversineDistanceMeters(lastLat, lastLon, startLat, startLon) < 150.0

            if (isNearby && cachedRoadRoute.isNotEmpty()) {
                cachedRoadRoute
            } else {
                val routeRes = withContext(Dispatchers.IO) {
                    OsrmRoutingService.fetchRouteFromOsrm(
                        originLat = startLat,
                        originLon = startLon,
                        destLat = destLat,
                        destLon = destLon
                    )
                }
                if (routeRes.isSuccess) {
                    val pts = routeRes.getOrThrow().points
                    if (pts.isNotEmpty()) {
                        cachedRoadRoute = pts
                        lastRouteQueryLat = startLat
                        lastRouteQueryLon = startLon
                        pts
                    } else cachedRoadRoute
                } else {
                    cachedRoadRoute
                }
            }
        } else {
            emptyList()
        }

        val (isDeviated, devDistKm) = if (finalRoute.isNotEmpty() && truckLat != null && truckLon != null) {
            val accuracy = parsed.accuracyMeters ?: 50.0
            if (accuracy <= 150.0) {
                val truckPoint = LatLng(truckLat, truckLon)
                val distMeters = OsrmRoutingService.calculateMinDistanceToRoute(truckPoint, finalRoute)
                val isDev = distMeters > OsrmRoutingService.DEFAULT_DEVIATION_THRESHOLD_METERS
                Pair(isDev, distMeters / 1000.0)
            } else {
                Pair(false, 0.0)
            }
        } else {
            Pair(false, 0.0)
        }

        val computedRemainingKm = if (finalRoute.isNotEmpty() && truckLat != null && truckLon != null) {
            OsrmRoutingService.calculateRemainingRoadDistanceKm(LatLng(truckLat, truckLon), finalRoute)
        } else {
            parsed.remainingDistanceKm
        }

        return parsed.copy(
            plannedRoadRoute = finalRoute,
            originLatitude = originLat,
            originLongitude = originLon,
            destinationLatitude = destLat,
            destinationLongitude = destLon,
            remainingDistanceKm = if (computedRemainingKm > 0.0) computedRemainingKm else parsed.remainingDistanceKm,
            isRouteDeviated = isDeviated,
            deviationDistanceKm = devDistKm
        )
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
                        tripAmount = if (obj.has("tripAmount") && !obj.isNull("tripAmount")) obj.optDouble("tripAmount", 0.0) else 0.0,
                        tripAmountFormatted = if (obj.has("tripAmount") && !obj.isNull("tripAmount") && obj.optDouble("tripAmount", 0.0) > 0)
                            "₹" + String.format(java.util.Locale.getDefault(), "%,.0f", obj.optDouble("tripAmount", 0.0)) else null,
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
