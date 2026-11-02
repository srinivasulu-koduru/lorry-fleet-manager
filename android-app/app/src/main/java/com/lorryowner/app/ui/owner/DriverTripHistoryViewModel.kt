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
import java.util.Locale

data class TripItemUiState(
    val id: Long,
    val tripReference: String,
    val driverId: Long = 0L,
    val driverName: String,
    val lorryId: Long?,
    val lorryRegistration: String,
    val lorryMakeModel: String?,
    val originLocation: String,
    val destinationLocation: String,
    val tripStatus: String,
    val tripDate: String,
    val startTime: String?,
    val endTime: String?,
    val distanceKm: String?,
    val deliveryNotes: String?,
    val createdAt: String?,
    val plannedDeliveryTime: String? = null
)

data class DriverPerformanceUiState(
    val totalTrips: Long = 0,
    val completedTrips: Long = 0,
    val inProgressTrips: Long = 0,
    val cancelledTrips: Long = 0,
    val totalRecordedDistanceKm: String = "0 km",
    val distinctLorriesUsed: Long = 0,
    val mostRecentTripDate: String? = null,
    val mostRecentTripReference: String? = null
)

enum class TripStatusFilter {
    ALL,
    COMPLETED,
    IN_PROGRESS,
    CANCELLED
}

data class DriverTripHistoryUiState(
    val driverId: Long = 0,
    val driverName: String = "",
    val driverUsername: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val performance: DriverPerformanceUiState = DriverPerformanceUiState(),
    val allTrips: List<TripItemUiState> = emptyList(),
    val trips: List<TripItemUiState> = emptyList(), // Displayed filtered trips
    val searchQuery: String = "",
    val selectedStatusFilter: TripStatusFilter = TripStatusFilter.ALL,
    val selectedTripForDetails: TripItemUiState? = null
)

class DriverTripHistoryViewModel(
    val driverId: Long,
    val driverName: String,
    val driverUsername: String,
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "DriverTripHistoryVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(
        DriverTripHistoryUiState(
            driverId = driverId,
            driverName = driverName,
            driverUsername = driverUsername
        )
    )
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchPerformanceAndTrips()
            }
            if (result.isFailure) {
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            } else {
                uiState = uiState.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true)
            withContext(Dispatchers.IO) {
                fetchPerformanceAndTrips()
            }
            uiState = uiState.copy(isRefreshing = false)
        }
    }

    fun onSearchQueryChange(query: String) {
        val filtered = applyFilters(uiState.allTrips, query, uiState.selectedStatusFilter)
        uiState = uiState.copy(searchQuery = query, trips = filtered)
    }

    fun onStatusFilterSelected(filter: TripStatusFilter) {
        val filtered = applyFilters(uiState.allTrips, uiState.searchQuery, filter)
        uiState = uiState.copy(selectedStatusFilter = filter, trips = filtered)
    }

    fun clearFilters() {
        val filtered = applyFilters(uiState.allTrips, "", TripStatusFilter.ALL)
        uiState = uiState.copy(searchQuery = "", selectedStatusFilter = TripStatusFilter.ALL, trips = filtered)
    }

    fun showTripDetails(trip: TripItemUiState) {
        uiState = uiState.copy(selectedTripForDetails = trip)
    }

    fun dismissTripDetails() {
        uiState = uiState.copy(selectedTripForDetails = null)
    }

    private fun applyFilters(
        all: List<TripItemUiState>,
        query: String,
        statusFilter: TripStatusFilter
    ): List<TripItemUiState> {
        val q = query.trim().lowercase(Locale.ROOT)
        return all.filter { trip ->
            val matchesStatus = when (statusFilter) {
                TripStatusFilter.ALL -> true
                TripStatusFilter.COMPLETED -> trip.tripStatus.equals("COMPLETED", ignoreCase = true)
                TripStatusFilter.IN_PROGRESS -> trip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)
                TripStatusFilter.CANCELLED -> trip.tripStatus.equals("CANCELLED", ignoreCase = true)
            }
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                trip.tripReference.lowercase(Locale.ROOT).contains(q) ||
                trip.originLocation.lowercase(Locale.ROOT).contains(q) ||
                trip.destinationLocation.lowercase(Locale.ROOT).contains(q) ||
                trip.lorryRegistration.lowercase(Locale.ROOT).contains(q) ||
                (trip.deliveryNotes?.lowercase(Locale.ROOT)?.contains(q) == true)
            }
            matchesStatus && matchesQuery
        }
    }

    private fun fetchPerformanceAndTrips(): Result<Unit> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(Exception("Session expired. Please log in."))
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            try {
                // 1. Fetch Performance
                val perfEndpoint = "$baseUrl/api/owner/drivers/$driverId/performance"
                val perfConn = (URL(perfEndpoint).openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                var performanceUi = DriverPerformanceUiState()
                if (perfConn.responseCode in 200..299) {
                    val rawJson = perfConn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val obj = JSONObject(rawJson)
                    val distVal = obj.optDouble("totalRecordedDistanceKm", 0.0)
                    val distStr = if (distVal > 0) String.format(Locale.US, "%,.1f km", distVal) else "0 km"

                    performanceUi = DriverPerformanceUiState(
                        totalTrips = obj.optLong("totalTrips", 0),
                        completedTrips = obj.optLong("completedTrips", 0),
                        inProgressTrips = obj.optLong("inProgressTrips", 0),
                        cancelledTrips = obj.optLong("cancelledTrips", 0),
                        totalRecordedDistanceKm = distStr,
                        distinctLorriesUsed = obj.optLong("distinctLorriesUsed", 0),
                        mostRecentTripDate = obj.optString("mostRecentTripDate").takeIf { it.isNotBlank() },
                        mostRecentTripReference = obj.optString("mostRecentTripReference").takeIf { it.isNotBlank() }
                    )
                }
                perfConn.disconnect()

                // 2. Fetch Trips
                val tripsEndpoint = "$baseUrl/api/owner/drivers/$driverId/trips?size=50"
                val tripsConn = (URL(tripsEndpoint).openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (tripsConn.responseCode in 200..299) {
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    val rawJson = tripsConn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val obj = JSONObject(rawJson)
                    val arr = obj.optJSONArray("trips") ?: org.json.JSONArray()
                    val tripList = mutableListOf<TripItemUiState>()

                    for (i in 0 until arr.length()) {
                        val t = arr.getJSONObject(i)
                        val id = t.optLong("id")
                        val ref = t.optString("tripReference", "TRIP-$id")
                        val lorryReg = t.optString("lorryRegistration", "Unrecorded")
                        val lorryModel = t.optString("lorryMakeModel").takeIf { it.isNotBlank() }
                        val origin = t.optString("originLocation", "Origin")
                        val dest = t.optString("destinationLocation", "Destination")
                        val status = t.optString("tripStatus", "COMPLETED")
                        val date = t.optString("tripDate", "")
                        val start = t.optString("startTime").takeIf { it.isNotBlank() }
                        val end = t.optString("endTime").takeIf { it.isNotBlank() }
                        val dist = if (t.has("distanceKm") && !t.isNull("distanceKm")) {
                            val km = t.optDouble("distanceKm", 0.0)
                            String.format(Locale.US, "%,.1f km", km)
                        } else null
                        val notes = t.optString("deliveryNotes").takeIf { it.isNotBlank() }

                        tripList.add(
                            TripItemUiState(
                                id = id,
                                tripReference = ref,
                                driverId = driverId,
                                driverName = driverName,
                                lorryId = t.optLong("lorryId").takeIf { it > 0 },
                                lorryRegistration = lorryReg,
                                lorryMakeModel = lorryModel,
                                originLocation = origin,
                                destinationLocation = dest,
                                tripStatus = status,
                                tripDate = date,
                                startTime = start,
                                endTime = end,
                                distanceKm = dist,
                                deliveryNotes = notes,
                                createdAt = t.optString("createdAt").takeIf { it.isNotBlank() }
                            )
                        )
                    }

                    tripsConn.disconnect()
                    val filtered = applyFilters(tripList, uiState.searchQuery, uiState.selectedStatusFilter)

                    uiState = uiState.copy(
                        performance = performanceUi,
                        allTrips = tripList,
                        trips = filtered,
                        errorMessage = null
                    )
                    return Result.success(Unit)
                } else if (tripsConn.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    sessionManager?.clearSession()
                    return Result.failure(Exception("Session expired. Please sign in again."))
                } else {
                    lastException = Exception("HTTP ${tripsConn.responseCode} loading trips from $baseUrl")
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        return Result.failure(lastException ?: Exception("Could not connect to fleet server."))
    }

    fun cancelTrip(tripId: Long, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val token = sessionManager?.getAccessToken()
            if (token == null) {
                onResult(false, "Authentication token missing")
                return@launch
            }
            val endpointUrl = ApiConfig.buildUrl("/api/owner/trips/$tripId/cancel", sessionManager)
            val result = withContext(Dispatchers.IO) {
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(endpointUrl)
                    connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (connection.responseCode in 200..299) {
                        Result.success(Unit)
                    } else {
                        val err = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Cancellation failed"
                        Result.failure(Exception("Failed to cancel trip: HTTP ${connection.responseCode} $err"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                } finally {
                    connection?.disconnect()
                }
            }

            if (result.isSuccess) {
                loadData()
                onResult(true, "Trip cancelled successfully")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Cancellation failed"
                onResult(false, errorMsg)
            }
        }
    }
}
