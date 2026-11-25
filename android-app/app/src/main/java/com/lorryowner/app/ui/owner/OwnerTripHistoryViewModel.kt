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
import java.net.URLEncoder

data class OwnerTripItemUiState(
    val id: Long,
    val tripReference: String,
    val driverId: Long?,
    val driverName: String,
    val lorryId: Long?,
    val lorryRegistration: String,
    val lorryMakeModel: String?,
    val originLocation: String,
    val destinationLocation: String,
    val tripStatus: String, // "PLANNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    val tripDate: String,
    val startTime: String?,
    val endTime: String?,
    val distanceKm: Double,
    val deliveryNotes: String?,
    val tripAmount: Double = 0.0
)

data class OwnerTripHistoryUiState(
    val trips: List<OwnerTripItemUiState> = emptyList(),
    val filteredTrips: List<OwnerTripItemUiState> = emptyList(),
    val selectedFilter: String = "ALL", // "ALL", "IN_PROGRESS", "COMPLETED", "CANCELLED", "PLANNED"
    val searchQuery: String = "",
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class OwnerTripHistoryViewModel(
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = true
) : ViewModel() {

    companion object {
        private const val TAG = "OwnerTripHistoryVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(OwnerTripHistoryUiState())
        private set

    init {
        if (autoLoad) {
            loadTrips()
        }
    }

    fun setTestData(state: OwnerTripHistoryUiState) {
        uiState = state
    }

    fun loadTrips(page: Int = 0) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchTripsFromBackend(page = page, status = uiState.selectedFilter, search = uiState.searchQuery)
            }
            if (result.isSuccess) {
                val pair = result.getOrThrow()
                uiState = uiState.copy(
                    trips = pair.first,
                    filteredTrips = filterLocally(pair.first, uiState.selectedFilter, uiState.searchQuery),
                    totalElements = pair.second,
                    currentPage = page,
                    isLoading = false
                )
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load trip history"
                )
            }
        }
    }

    fun refreshTrips() {
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchTripsFromBackend(page = 0, status = uiState.selectedFilter, search = uiState.searchQuery)
            }
            if (result.isSuccess) {
                val pair = result.getOrThrow()
                uiState = uiState.copy(
                    trips = pair.first,
                    filteredTrips = filterLocally(pair.first, uiState.selectedFilter, uiState.searchQuery),
                    totalElements = pair.second,
                    currentPage = 0,
                    isRefreshing = false
                )
            } else {
                uiState = uiState.copy(
                    isRefreshing = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to refresh trip history"
                )
            }
        }
    }

    fun setFilter(filter: String) {
        if (uiState.selectedFilter == filter) return
        uiState = uiState.copy(
            selectedFilter = filter,
            filteredTrips = filterLocally(uiState.trips, filter, uiState.searchQuery)
        )
        loadTrips(page = 0)
    }

    fun onSearchQueryChanged(query: String) {
        uiState = uiState.copy(
            searchQuery = query,
            filteredTrips = filterLocally(uiState.trips, uiState.selectedFilter, query)
        )
    }

    private fun filterLocally(
        list: List<OwnerTripItemUiState>,
        filter: String,
        query: String
    ): List<OwnerTripItemUiState> {
        val filteredByStatus = if (filter == "ALL") {
            list
        } else {
            list.filter { it.tripStatus.equals(filter, ignoreCase = true) }
        }

        if (query.isBlank()) return filteredByStatus

        val q = query.trim().lowercase()
        return filteredByStatus.filter {
            it.tripReference.lowercase().contains(q) ||
            it.lorryRegistration.lowercase().contains(q) ||
            it.driverName.lowercase().contains(q) ||
            it.originLocation.lowercase().contains(q) ||
            it.destinationLocation.lowercase().contains(q)
        }
    }

    private fun fetchTripsFromBackend(
        page: Int,
        status: String,
        search: String
    ): Result<Pair<List<OwnerTripItemUiState>, Long>> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Session token missing"))
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        val queryParams = StringBuilder("?page=$page&size=50")
        if (status.isNotBlank() && status != "ALL") {
            queryParams.append("&status=").append(URLEncoder.encode(status, "UTF-8"))
        }
        if (search.isNotBlank()) {
            queryParams.append("&search=").append(URLEncoder.encode(search.trim(), "UTF-8"))
        }

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/trips$queryParams"
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

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(rawJson)

                    val totalElements = json.optLong("totalElements", 0L)
                    val tripsArray = json.optJSONArray("trips") ?: org.json.JSONArray()
                    val items = mutableListOf<OwnerTripItemUiState>()

                    for (i in 0 until tripsArray.length()) {
                        val obj = tripsArray.getJSONObject(i)
                        items.add(
                            OwnerTripItemUiState(
                                id = obj.optLong("id"),
                                tripReference = obj.optString("tripReference", "TRIP-$i"),
                                driverId = if (obj.has("driverId") && !obj.isNull("driverId")) obj.optLong("driverId") else null,
                                driverName = obj.optString("driverName", "Unassigned"),
                                lorryId = if (obj.has("lorryId") && !obj.isNull("lorryId")) obj.optLong("lorryId") else null,
                                lorryRegistration = obj.optString("lorryRegistration", "Fleet Lorry"),
                                lorryMakeModel = if (obj.has("lorryMakeModel") && !obj.isNull("lorryMakeModel")) obj.optString("lorryMakeModel") else null,
                                originLocation = obj.optString("originLocation", "Unknown"),
                                destinationLocation = obj.optString("destinationLocation", "Unknown"),
                                tripStatus = obj.optString("tripStatus", "COMPLETED"),
                                tripDate = obj.optString("tripDate", ""),
                                startTime = if (obj.has("startTime") && !obj.isNull("startTime")) obj.optString("startTime") else null,
                                endTime = if (obj.has("endTime") && !obj.isNull("endTime")) obj.optString("endTime") else null,
                                distanceKm = obj.optDouble("distanceKm", 0.0),
                                deliveryNotes = if (obj.has("deliveryNotes") && !obj.isNull("deliveryNotes")) obj.optString("deliveryNotes") else null,
                                tripAmount = obj.optDouble("tripAmount", 0.0)
                            )
                        )
                    }

                    return Result.success(Pair(items, totalElements))
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    lastException = Exception("HTTP $responseCode: $err")
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Failed to connect to $baseUrl: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }

        return Result.failure(lastException ?: Exception("Network request failed"))
    }
}
