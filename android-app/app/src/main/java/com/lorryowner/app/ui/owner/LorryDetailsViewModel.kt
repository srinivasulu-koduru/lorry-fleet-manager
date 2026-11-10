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
    val selectedMaintenanceForDetails: MaintenanceItemUiState? = null,
    val vehicleDocuments: List<com.lorryowner.app.network.DocumentUiItem> = emptyList(),
    val isUploadingDocument: Boolean = false,
    val documentError: String? = null
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
            val tripsResult = withContext(Dispatchers.IO) {
                fetchLorryDetails()
                val res = fetchTrips()
                fetchMaintenance()
                fetchVehicleDocuments()
                res
            }
            if (tripsResult.isSuccess) {
                val list = tripsResult.getOrDefault(emptyList())
                uiState = uiState.copy(
                    isLoading = false,
                    trips = list,
                    errorMessage = null
                )
                applyTripFilter(uiState.selectedTripFilter)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = tripsResult.exceptionOrNull()?.message ?: "Unable to load trip records"
                )
            }
        }
    }

    fun loadTripsOnly() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val tripsResult = withContext(Dispatchers.IO) {
                fetchTrips()
            }
            if (tripsResult.isSuccess) {
                val list = tripsResult.getOrDefault(emptyList())
                uiState = uiState.copy(
                    isLoading = false,
                    trips = list,
                    errorMessage = null
                )
                applyTripFilter(uiState.selectedTripFilter)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = tripsResult.exceptionOrNull()?.message ?: "Unable to load trip records"
                )
            }
        }
    }

    fun fetchVehicleDocuments() {
        val result = com.lorryowner.app.network.DocumentApiService.fetchDocuments(
            sessionManager = sessionManager,
            endpointPath = "/api/owner/lorries/$lorryId/documents"
        )
        if (result.isSuccess) {
            uiState = uiState.copy(vehicleDocuments = result.getOrDefault(emptyList()))
        }
    }

    fun uploadVehicleDocument(context: android.content.Context, title: String, uri: android.net.Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(isUploadingDocument = true, documentError = null)
            val result = withContext(Dispatchers.IO) {
                com.lorryowner.app.network.DocumentApiService.uploadDocument(
                    context = context,
                    sessionManager = sessionManager,
                    endpointPath = "/api/owner/lorries/$lorryId/documents",
                    documentTitle = title,
                    fileUri = uri
                )
            }
            uiState = uiState.copy(isUploadingDocument = false)
            if (result.isSuccess) {
                fetchVehicleDocuments()
            } else {
                uiState = uiState.copy(documentError = result.exceptionOrNull()?.message ?: "Upload failed")
            }
        }
    }

    fun deleteVehicleDocument(docId: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                com.lorryowner.app.network.DocumentApiService.deleteDocument(
                    sessionManager = sessionManager,
                    endpointPath = "/api/owner/lorries/$lorryId/documents/$docId"
                )
            }
            if (result.isSuccess) {
                fetchVehicleDocuments()
            } else {
                uiState = uiState.copy(documentError = result.exceptionOrNull()?.message ?: "Delete failed")
            }
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

    private fun fetchLorryDetails() {
        val token = sessionManager?.getAccessToken() ?: return
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/lorries/$lorryId"
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
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed connecting to $endpointUrl for lorry details: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun fetchTrips(): Result<List<TripItemUiState>> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(Exception("Session token missing. Please log in again."))
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/lorries/$lorryId/trips?size=50"
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
                if (responseCode in 200..299) {
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
                                lorryId = if (t.has("lorryId") && !t.isNull("lorryId")) t.optLong("lorryId") else lorryId,
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
                                plannedDeliveryTime = if (t.has("plannedDeliveryTime") && !t.isNull("plannedDeliveryTime")) t.optString("plannedDeliveryTime") else null,
                                tripAmount = if (t.has("tripAmount") && !t.isNull("tripAmount")) t.optDouble("tripAmount", 0.0) else 0.0,
                                tripAmountFormatted = if (t.has("tripAmount") && !t.isNull("tripAmount") && t.optDouble("tripAmount", 0.0) > 0)
                                    "₹" + String.format(java.util.Locale.getDefault(), "%,.0f", t.optDouble("tripAmount", 0.0)) else null
                            )
                        )
                    }

                    uiState = uiState.copy(trips = list)
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(list)
                } else {
                    val errMsg = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                        ?: "Server error ($responseCode)"
                    lastException = Exception("HTTP $responseCode: $errMsg")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed connecting to $endpointUrl for trips: ${e.message}")
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }
        return Result.failure(lastException ?: Exception("Failed to connect to backend server"))
    }

    private fun fetchMaintenance() {
        val token = sessionManager?.getAccessToken() ?: return
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/lorries/$lorryId/maintenance"
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
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed connecting to $endpointUrl for maintenance: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }
}
