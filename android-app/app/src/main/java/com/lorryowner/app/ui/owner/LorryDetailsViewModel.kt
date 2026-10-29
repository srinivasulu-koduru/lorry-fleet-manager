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

data class MaintenanceItemUiState(
    val id: Long,
    val maintenanceDate: String,
    val serviceType: String,
    val description: String,
    val cost: Double,
    val serviceCenter: String,
    val odometerReadingKm: Int?,
    val nextServiceDate: String?
)

data class LorryDetailsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lorry: LorryCardUiState? = null,
    val trips: List<TripItemUiState> = emptyList(),
    val filteredTrips: List<TripItemUiState> = emptyList(),
    val maintenanceRecords: List<MaintenanceItemUiState> = emptyList(),
    val totalMaintenanceCost: Double = 0.0,
    val selectedTripFilter: String = "ALL",
    val selectedTripForDetails: TripItemUiState? = null,
    val selectedMaintenanceForDetails: MaintenanceItemUiState? = null
)

class LorryDetailsViewModel(
    private val lorryId: Long,
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "LorryDetailsVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(LorryDetailsUiState())
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            withContext(Dispatchers.IO) {
                fetchLorryDetails()
                fetchTrips()
                fetchMaintenance()
            }
            uiState = uiState.copy(isLoading = false)
            applyTripFilter(uiState.selectedTripFilter)
        }
    }

    fun applyTripFilter(status: String) {
        uiState = uiState.copy(selectedTripFilter = status)
        val filtered = if (status == "ALL") {
            uiState.trips
        } else {
            uiState.trips.filter { it.tripStatus.equals(status, ignoreCase = true) }
        }
        uiState = uiState.copy(filteredTrips = filtered)
    }

    fun selectTripForDetails(trip: TripItemUiState?) {
        uiState = uiState.copy(selectedTripForDetails = trip)
    }

    fun selectMaintenanceForDetails(item: MaintenanceItemUiState?) {
        uiState = uiState.copy(selectedMaintenanceForDetails = item)
    }

    private fun fetchLorryDetails() {
        val token = sessionManager?.getAccessToken() ?: return
        val endpointUrl = ApiConfig.buildUrl("/api/owner/lorries/$lorryId", sessionManager)
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

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val obj = JSONObject(rawJson)

                val card = LorryCardUiState(
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

                uiState = uiState.copy(lorry = card)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lorry details: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchTrips() {
        val token = sessionManager?.getAccessToken() ?: return
        val endpointUrl = ApiConfig.buildUrl("/api/owner/lorries/$lorryId/trips?size=50", sessionManager)
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

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(rawJson)
                val tripsArr = json.optJSONArray("trips") ?: JSONArray()
                val list = mutableListOf<TripItemUiState>()

                for (i in 0 until tripsArr.length()) {
                    val t = tripsArr.getJSONObject(i)
                    list.add(
                        TripItemUiState(
                            id = t.optLong("id"),
                            tripReference = t.optString("tripReference"),
                            driverId = if (t.has("driverId") && !t.isNull("driverId")) t.optLong("driverId") else 0L,
                            driverName = t.optString("driverName", "Unassigned Driver"),
                            lorryId = if (t.has("lorryId") && !t.isNull("lorryId")) t.optLong("lorryId") else null,
                            lorryRegistration = t.optString("lorryRegistration", "Unknown Lorry"),
                            lorryMakeModel = if (t.has("lorryMakeModel") && !t.isNull("lorryMakeModel")) t.optString("lorryMakeModel") else null,
                            originLocation = t.optString("originLocation"),
                            destinationLocation = t.optString("destinationLocation"),
                            tripStatus = t.optString("tripStatus", "COMPLETED"),
                            tripDate = t.optString("tripDate"),
                            startTime = if (t.has("startTime") && !t.isNull("startTime")) t.optString("startTime") else null,
                            endTime = if (t.has("endTime") && !t.isNull("endTime")) t.optString("endTime") else null,
                            distanceKm = if (t.has("distanceKm") && !t.isNull("distanceKm")) "${t.optDouble("distanceKm")} km" else null,
                            deliveryNotes = if (t.has("deliveryNotes") && !t.isNull("deliveryNotes")) t.optString("deliveryNotes") else null,
                            createdAt = t.optString("createdAt"),
                            plannedDeliveryTime = if (t.has("plannedDeliveryTime") && !t.isNull("plannedDeliveryTime")) t.optString("plannedDeliveryTime") else null
                        )
                    )
                }

                uiState = uiState.copy(trips = list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trips: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchMaintenance() {
        val token = sessionManager?.getAccessToken() ?: return
        val endpointUrl = ApiConfig.buildUrl("/api/owner/lorries/$lorryId/maintenance", sessionManager)
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

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(rawJson)
                val totalCost = json.optDouble("totalMaintenanceCost", 0.0)
                val recordsArr = json.optJSONArray("records") ?: JSONArray()
                val list = mutableListOf<MaintenanceItemUiState>()

                for (i in 0 until recordsArr.length()) {
                    val r = recordsArr.getJSONObject(i)
                    list.add(
                        MaintenanceItemUiState(
                            id = r.optLong("id"),
                            maintenanceDate = r.optString("maintenanceDate"),
                            serviceType = r.optString("serviceType"),
                            description = r.optString("description"),
                            cost = r.optDouble("cost", 0.0),
                            serviceCenter = r.optString("serviceCenter", "Service Workshop"),
                            odometerReadingKm = if (r.has("odometerReadingKm") && !r.isNull("odometerReadingKm")) r.optInt("odometerReadingKm") else null,
                            nextServiceDate = if (r.has("nextServiceDate") && !r.isNull("nextServiceDate")) r.optString("nextServiceDate") else null
                        )
                    )
                }

                uiState = uiState.copy(
                    maintenanceRecords = list,
                    totalMaintenanceCost = totalCost
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching maintenance: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }
}
