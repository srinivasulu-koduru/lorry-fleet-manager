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
import java.net.URLEncoder

enum class LorryFilter {
    ALL,
    RUNNING,
    NOT_RUNNING
}

data class FleetSummaryUiState(
    val totalLorries: Long = 0,
    val runningLorries: Long = 0,
    val notRunningLorries: Long = 0,
    val runningPercentage: Double = 0.0,
    val notRunningPercentage: Double = 0.0,
    val totalTrips: Long = 0,
    val totalDistanceKm: Double = 0.0,
    val totalMaintenanceCost: Double = 0.0
)

data class LorryCardUiState(
    val id: Long,
    val registrationNumber: String,
    val makeModel: String,
    val capacityTons: Double,
    val operationalStatus: String,
    val running: Boolean,
    val assignedDriverId: Long? = null,
    val assignedDriverName: String? = null,
    val assignedDriverPhone: String? = null,
    val activeTripId: Long? = null,
    val activeTripReference: String? = null,
    val activeTripOrigin: String? = null,
    val activeTripDestination: String? = null,
    val activeTripStartTime: String? = null,
    val activeTripEtaFormatted: String = "ETA unavailable",
    val totalTrips: Long = 0,
    val completedTrips: Long = 0,
    val inProgressTrips: Long = 0,
    val cancelledTrips: Long = 0,
    val totalRecordedDistanceKm: Double = 0.0,
    val totalMaintenanceCost: Double = 0.0,
    val notes: String? = null
)

data class MyLorriesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val summary: FleetSummaryUiState = FleetSummaryUiState(),
    val lorries: List<LorryCardUiState> = emptyList(),
    val filteredLorries: List<LorryCardUiState> = emptyList(),
    val filter: LorryFilter = LorryFilter.ALL,
    val searchQuery: String = ""
)

class MyLorriesViewModel(
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "MyLorriesVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(MyLorriesUiState())
        private set

    init {
        loadFleetData()
    }

    fun loadFleetData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val summaryResult = withContext(Dispatchers.IO) { fetchSummary() }
            val lorriesResult = withContext(Dispatchers.IO) { fetchLorries() }

            if (summaryResult.isSuccess && lorriesResult.isSuccess) {
                uiState = uiState.copy(isLoading = false, isRefreshing = false)
                applyFilters()
            } else {
                val error = summaryResult.exceptionOrNull()?.message
                    ?: lorriesResult.exceptionOrNull()?.message
                    ?: "Failed to load fleet data"
                uiState = uiState.copy(isLoading = false, isRefreshing = false, errorMessage = error)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, errorMessage = null)
            withContext(Dispatchers.IO) {
                fetchSummary()
                fetchLorries()
            }
            uiState = uiState.copy(isRefreshing = false)
            applyFilters()
        }
    }

    fun onSearchQueryChanged(query: String) {
        uiState = uiState.copy(searchQuery = query)
        applyFilters()
    }

    fun onFilterSelected(filter: LorryFilter) {
        uiState = uiState.copy(filter = filter)
        applyFilters()
    }

    fun clearFilters() {
        uiState = uiState.copy(searchQuery = "", filter = LorryFilter.ALL)
        applyFilters()
    }

    private fun applyFilters() {
        val query = uiState.searchQuery.trim().lowercase()
        val filter = uiState.filter

        val filtered = uiState.lorries.filter { lorry ->
            val matchesFilter = when (filter) {
                LorryFilter.ALL -> true
                LorryFilter.RUNNING -> lorry.running
                LorryFilter.NOT_RUNNING -> !lorry.running
            }

            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                lorry.registrationNumber.lowercase().contains(query) ||
                        lorry.makeModel.lowercase().contains(query) ||
                        (lorry.assignedDriverName?.lowercase()?.contains(query) == true) ||
                        (lorry.activeTripDestination?.lowercase()?.contains(query) == true)
            }

            matchesFilter && matchesSearch
        }

        uiState = uiState.copy(filteredLorries = filtered)
    }

    private fun fetchSummary(): Result<Unit> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing"))
        val endpointUrl = ApiConfig.buildUrl("/api/owner/lorries/summary", sessionManager)
        var connection: HttpURLConnection? = null

        return try {
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
                val json = JSONObject(rawJson)

                val summary = FleetSummaryUiState(
                    totalLorries = json.optLong("totalLorries", 0L),
                    runningLorries = json.optLong("runningLorries", 0L),
                    notRunningLorries = json.optLong("notRunningLorries", 0L),
                    runningPercentage = json.optDouble("runningPercentage", 0.0),
                    notRunningPercentage = json.optDouble("notRunningPercentage", 0.0),
                    totalTrips = json.optLong("totalTrips", 0L),
                    totalDistanceKm = json.optDouble("totalDistanceKm", 0.0),
                    totalMaintenanceCost = json.optDouble("totalMaintenanceCost", 0.0)
                )

                uiState = uiState.copy(summary = summary)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch fleet summary: HTTP ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching summary: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchLorries(): Result<Unit> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing"))
        val endpointUrl = ApiConfig.buildUrl("/api/owner/lorries", sessionManager)
        var connection: HttpURLConnection? = null

        return try {
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
                val jsonArray = JSONArray(rawJson)
                val list = mutableListOf<LorryCardUiState>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        LorryCardUiState(
                            id = obj.optLong("id"),
                            registrationNumber = obj.optString("registrationNumber"),
                            makeModel = obj.optString("makeModel"),
                            capacityTons = obj.optDouble("capacityTons", 0.0),
                            operationalStatus = obj.optString("operationalStatus"),
                            running = obj.optBoolean("running", false),
                            assignedDriverId = if (obj.has("assignedDriverId") && !obj.isNull("assignedDriverId")) obj.optLong("assignedDriverId") else null,
                            assignedDriverName = if (obj.has("assignedDriverName") && !obj.isNull("assignedDriverName")) obj.optString("assignedDriverName") else null,
                            assignedDriverPhone = if (obj.has("assignedDriverPhone") && !obj.isNull("assignedDriverPhone")) obj.optString("assignedDriverPhone") else null,
                            activeTripId = if (obj.has("activeTripId") && !obj.isNull("activeTripId")) obj.optLong("activeTripId") else null,
                            activeTripReference = if (obj.has("activeTripReference") && !obj.isNull("activeTripReference")) obj.optString("activeTripReference") else null,
                            activeTripOrigin = if (obj.has("activeTripOrigin") && !obj.isNull("activeTripOrigin")) obj.optString("activeTripOrigin") else null,
                            activeTripDestination = if (obj.has("activeTripDestination") && !obj.isNull("activeTripDestination")) obj.optString("activeTripDestination") else null,
                            activeTripStartTime = if (obj.has("activeTripStartTime") && !obj.isNull("activeTripStartTime")) obj.optString("activeTripStartTime") else null,
                            activeTripEtaFormatted = obj.optString("activeTripEtaFormatted", "ETA unavailable"),
                            totalTrips = obj.optLong("totalTrips", 0L),
                            completedTrips = obj.optLong("completedTrips", 0L),
                            inProgressTrips = obj.optLong("inProgressTrips", 0L),
                            cancelledTrips = obj.optLong("cancelledTrips", 0L),
                            totalRecordedDistanceKm = obj.optDouble("totalRecordedDistanceKm", 0.0),
                            totalMaintenanceCost = obj.optDouble("totalMaintenanceCost", 0.0),
                            notes = if (obj.has("notes") && !obj.isNull("notes")) obj.optString("notes") else null
                        )
                    )
                }

                uiState = uiState.copy(lorries = list)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch lorries: HTTP ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lorries: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
