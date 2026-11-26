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
    autoLoad: Boolean = true,
    private val baseUrlOverride: String? = null,
    private val tokenOverride: String? = null
) : ViewModel() {

    companion object {
        private const val TAG = "OwnerTripHistoryVM"
        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 20000
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

    internal fun fetchTripsBlocking(
        page: Int = 0,
        status: String = uiState.selectedFilter,
        search: String = uiState.searchQuery
    ): Result<Pair<List<OwnerTripItemUiState>, Long>> {
        return fetchTripsFromBackend(page, status, search)
    }

    private fun fetchTripsFromBackend(
        page: Int,
        status: String,
        search: String
    ): Result<Pair<List<OwnerTripItemUiState>, Long>> {
        val token = tokenOverride ?: sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Session token missing"))
        }

        val candidateUrls = if (!baseUrlOverride.isNullOrBlank()) {
            listOf(baseUrlOverride.removeSuffix("/"))
        } else {
            ApiConfig.getCandidateBaseUrls(sessionManager)
        }
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
                        items.add(parseTripItem(obj, i))
                    }

                    return Result.success(Pair(items, totalElements))
                } else if (responseCode in listOf(
                        HttpURLConnection.HTTP_BAD_METHOD,
                        HttpURLConnection.HTTP_NOT_FOUND,
                        HttpURLConnection.HTTP_NOT_IMPLEMENTED
                    )
                ) {
                    // Drain and close the 405/404 connection before opening fallback requests
                    val primaryErr = runCatching {
                        connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    }.getOrNull()
                    connection.disconnect()
                    connection = null

                    Log.i(TAG, "Server returned HTTP $responseCode on /api/owner/trips. Falling back to lorry trip aggregation...")
                    val fallback = fetchTripsFallbackLegacy(baseUrl, token)
                    if (fallback.isSuccess) {
                        return fallback
                    }
                    lastException = (fallback.exceptionOrNull() as? Exception)
                        ?: Exception("HTTP $responseCode fallback failed: $primaryErr")
                } else {
                    val err = runCatching {
                        connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    }.getOrNull()
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

    private fun fetchTripsFallbackLegacy(
        baseUrl: String,
        token: String
    ): Result<Pair<List<OwnerTripItemUiState>, Long>> {
        var lorriesConn: HttpURLConnection? = null
        return try {
            val lorriesUrl = URL("$baseUrl/api/owner/lorries")
            lorriesConn = (lorriesUrl.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }

            val lorriesResponseCode = lorriesConn.responseCode
            if (lorriesResponseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return Result.failure(Exception("Session expired (HTTP 401). Please log in again."))
            }
            if (lorriesResponseCode != HttpURLConnection.HTTP_OK) {
                val errBody = runCatching {
                    lorriesConn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                }.getOrNull()
                return Result.failure(Exception("HTTP $lorriesResponseCode: Failed to fetch fleet lorries ${errBody ?: ""}".trim()))
            }

            val lorriesRaw = lorriesConn.inputStream.bufferedReader().use(BufferedReader::readText)
            lorriesConn.disconnect()
            lorriesConn = null

            val lorriesArray = org.json.JSONArray(lorriesRaw)
            val allTrips = mutableListOf<OwnerTripItemUiState>()

            for (i in 0 until lorriesArray.length()) {
                val lorryObj = lorriesArray.getJSONObject(i)
                val lorryId = lorryObj.optLong("id")
                val regNumber = optSafeString(lorryObj, "registrationNumber", "Fleet Lorry")
                val makeModel = optNullableString(lorryObj, "makeModel")

                var tripsConn: HttpURLConnection? = null
                try {
                    val tripsUrl = URL("$baseUrl/api/owner/lorries/$lorryId/trips?size=50")
                    tripsConn = (tripsUrl.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Authorization", "Bearer $token")
                    }

                    if (tripsConn.responseCode == HttpURLConnection.HTTP_OK) {
                        val rawTrips = tripsConn.inputStream.bufferedReader().use(BufferedReader::readText)
                        val tripsJson = JSONObject(rawTrips)
                        val tripsArr = tripsJson.optJSONArray("trips") ?: org.json.JSONArray()

                        for (j in 0 until tripsArr.length()) {
                            val t = tripsArr.getJSONObject(j)
                            allTrips.add(
                                parseTripItem(
                                    obj = t,
                                    index = j,
                                    fallbackLorryId = lorryId,
                                    fallbackReg = regNumber,
                                    fallbackMakeModel = makeModel
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed fetching trips for lorry $lorryId: ${e.message}")
                } finally {
                    tripsConn?.disconnect()
                }
            }

            val distinctTrips = allTrips.distinctBy { it.id }.sortedWith(
                compareByDescending<OwnerTripItemUiState> { it.tripDate }
                    .thenByDescending { it.id }
            )

            Result.success(Pair(distinctTrips, distinctTrips.size.toLong()))
        } catch (e: Exception) {
            Log.e(TAG, "Legacy fallback failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            lorriesConn?.disconnect()
        }
    }

    private fun parseTripItem(
        obj: JSONObject,
        index: Int,
        fallbackLorryId: Long? = null,
        fallbackReg: String = "Fleet Lorry",
        fallbackMakeModel: String? = null
    ): OwnerTripItemUiState {
        val id = obj.optLong("id")
        return OwnerTripItemUiState(
            id = id,
            tripReference = optSafeString(obj, "tripReference", "TRIP-${if (id > 0) id else index}"),
            driverId = if (obj.has("driverId") && !obj.isNull("driverId")) obj.optLong("driverId") else null,
            driverName = optSafeString(obj, "driverName", "Unassigned Driver"),
            lorryId = if (obj.has("lorryId") && !obj.isNull("lorryId")) obj.optLong("lorryId") else fallbackLorryId,
            lorryRegistration = optSafeString(obj, "lorryRegistration", fallbackReg),
            lorryMakeModel = optNullableString(obj, "lorryMakeModel") ?: fallbackMakeModel,
            originLocation = optSafeString(obj, "originLocation", optSafeString(obj, "origin", "Unknown")),
            destinationLocation = optSafeString(obj, "destinationLocation", optSafeString(obj, "destination", "Unknown")),
            tripStatus = optSafeString(obj, "tripStatus", optSafeString(obj, "status", "COMPLETED")),
            tripDate = optSafeString(obj, "tripDate", optSafeString(obj, "plannedDepartureDate", "")),
            startTime = optNullableString(obj, "startTime") ?: optNullableString(obj, "plannedDepartureTime"),
            endTime = optNullableString(obj, "endTime"),
            distanceKm = obj.optDouble("distanceKm", 0.0),
            deliveryNotes = optNullableString(obj, "deliveryNotes") ?: optNullableString(obj, "remarks"),
            tripAmount = obj.optDouble("tripAmount", 0.0)
        )
    }

    private fun optSafeString(obj: JSONObject, key: String, default: String): String {
        if (!obj.has(key) || obj.isNull(key)) return default
        val value = obj.optString(key, default)
        return if (value.isBlank() || value.equals("null", ignoreCase = true)) default else value
    }

    private fun optNullableString(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val value = obj.optString(key, "")
        return if (value.isBlank() || value.equals("null", ignoreCase = true)) null else value
    }
}
