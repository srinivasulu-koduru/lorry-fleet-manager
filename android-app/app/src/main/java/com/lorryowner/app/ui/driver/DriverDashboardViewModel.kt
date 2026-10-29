package com.lorryowner.app.ui.driver

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorryowner.app.network.ApiConfig
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.owner.GreetingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime

class DriverDashboardViewModel(
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = (sessionManager != null)
) : ViewModel() {

    companion object {
        private const val TAG = "DriverDashboardVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var dashboardState by mutableStateOf(DriverDashboardUiState())
        private set

    var allTrips by mutableStateOf<List<DriverTripUiItem>>(emptyList())
        private set

    var selectedTripsTab by mutableStateOf(DriverTripsTab.CURRENT)
        private set

    var selectedTripForDetails by mutableStateOf<DriverTripUiItem?>(null)
        private set

    var profileState by mutableStateOf(DriverProfileUiState())
        private set

    var currentNavDestination by mutableStateOf(DriverNavDestination.HOME)
        private set

    var isMutatingTrip by mutableStateOf(false)
        private set

    var mutationErrorMessage by mutableStateOf<String?>(null)
        private set

    var mutationSuccessMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Initialize with session identity if available
        val savedName = sessionManager?.getFullName().orEmpty()
            .ifEmpty { sessionManager?.getUsername().orEmpty() }
            .ifEmpty { "Ramesh Kumar" }
        val savedUsername = sessionManager?.getUsername().orEmpty().ifEmpty { "driver_ramesh" }

        val greetingText = GreetingHelper.getGreeting()

        dashboardState = dashboardState.copy(
            driverName = savedName,
            driverUsername = savedUsername,
            greeting = greetingText
        )

        profileState = profileState.copy(
            fullName = savedName,
            username = savedUsername,
            email = "$savedUsername@bhp-logistics.com"
        )

        if (autoLoad) {
            refreshAll()
        }
    }

    fun updateGreeting(time: LocalTime = LocalTime.now()) {
        val greetingText = GreetingHelper.getGreeting(time)
        dashboardState = dashboardState.copy(greeting = greetingText)
    }

    fun selectNavDestination(destination: DriverNavDestination) {
        currentNavDestination = destination
        if (destination == DriverNavDestination.STATUS && selectedTripForDetails == null) {
            // Default to today's trip if available
            selectedTripForDetails = dashboardState.todayTrip ?: allTrips.firstOrNull { it.tripStatus == "IN_PROGRESS" } ?: allTrips.firstOrNull()
        }
    }

    fun selectTripsTab(tab: DriverTripsTab) {
        selectedTripsTab = tab
    }

    fun selectTrip(trip: DriverTripUiItem?) {
        selectedTripForDetails = trip
    }

    fun clearMutationMessages() {
        mutationErrorMessage = null
        mutationSuccessMessage = null
    }

    /**
     * Starts an assigned planned trip.
     * Dispatches POST /api/driver/trips/{id}/start to the authenticated backend.
     */
    fun startTrip(
        tripId: Long,
        onSuccess: (DriverTripUiItem) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (isMutatingTrip) return // debounce / prevent duplicate clicks
        viewModelScope.launch {
            isMutatingTrip = true
            mutationErrorMessage = null
            mutationSuccessMessage = null

            val result = withContext(Dispatchers.IO) {
                postTripMutation(tripId, "start", null)
            }

            isMutatingTrip = false
            if (result.isSuccess) {
                val updatedTrip = result.getOrThrow()
                selectedTripForDetails = updatedTrip
                updateTripInState(updatedTrip)
                mutationSuccessMessage = "Trip started successfully! Status updated to In Progress."
                onSuccess(updatedTrip)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        fetchDashboard()
                        fetchTrips()
                    } catch (e: Exception) {
                        Log.w(TAG, "Background dashboard refresh error after starting trip", e)
                    }
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to start trip"
                mutationErrorMessage = err
                onError(err)
            }
        }
    }

    /**
     * Completes an in-progress trip.
     * Dispatches POST /api/driver/trips/{id}/complete with optional notes to backend.
     */
    fun completeTrip(
        tripId: Long,
        notes: String? = null,
        onSuccess: (DriverTripUiItem) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (isMutatingTrip) return // debounce / prevent duplicate clicks
        viewModelScope.launch {
            isMutatingTrip = true
            mutationErrorMessage = null
            mutationSuccessMessage = null

            val jsonBody = if (!notes.isNullOrBlank()) {
                JSONObject().put("notes", notes.trim()).toString()
            } else null

            val result = withContext(Dispatchers.IO) {
                postTripMutation(tripId, "complete", jsonBody)
            }

            isMutatingTrip = false
            if (result.isSuccess) {
                val updatedTrip = result.getOrThrow()
                selectedTripForDetails = updatedTrip
                updateTripInState(updatedTrip)
                mutationSuccessMessage = "Trip completed successfully! Recorded in trip history."
                onSuccess(updatedTrip)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        fetchDashboard()
                        fetchTrips()
                    } catch (e: Exception) {
                        Log.w(TAG, "Background dashboard refresh error after completing trip", e)
                    }
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to complete trip"
                mutationErrorMessage = err
                onError(err)
            }
        }
    }

    fun updateTripInState(updatedTrip: DriverTripUiItem) {
        // 1. Update in allTrips list
        allTrips = if (allTrips.any { it.id == updatedTrip.id }) {
            allTrips.map { if (it.id == updatedTrip.id) updatedTrip else it }
        } else {
            listOf(updatedTrip) + allTrips
        }

        // 2. Update todayTrip in dashboardState
        val isTodayTrip = dashboardState.todayTrip?.id == updatedTrip.id || dashboardState.todayTrip == null
        val newTodayTrip = if (isTodayTrip) {
            if (updatedTrip.tripStatus.equals("COMPLETED", ignoreCase = true)) {
                // If today's trip was completed, look for next in-progress or planned trip
                allTrips.firstOrNull { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }
                    ?: allTrips.firstOrNull { it.tripStatus.equals("PLANNED", ignoreCase = true) }
            } else {
                updatedTrip
            }
        } else {
            dashboardState.todayTrip
        }

        // 3. Recalculate dashboard counts
        val curCount = allTrips.count { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }.toLong()
        val upCount = allTrips.count { it.tripStatus.equals("PLANNED", ignoreCase = true) }.toLong()
        val compCount = allTrips.count { it.tripStatus.equals("COMPLETED", ignoreCase = true) }.toLong()

        dashboardState = dashboardState.copy(
            todayTrip = newTodayTrip,
            currentTripsCount = curCount,
            upcomingTripsCount = upCount,
            completedTripsCount = compCount
        )

        // 4. Automatically switch trips tab to appropriate category
        if (updatedTrip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)) {
            selectedTripsTab = DriverTripsTab.CURRENT
        } else if (updatedTrip.tripStatus.equals("COMPLETED", ignoreCase = true)) {
            selectedTripsTab = DriverTripsTab.COMPLETED
        }
    }

    private fun postTripMutation(
        tripId: Long,
        action: String,
        jsonBody: String?
    ): Result<DriverTripUiItem> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session expired. Please log in again."))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/driver/trips/$tripId/$action")
                conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    if (jsonBody != null) {
                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true
                        outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
                    } else {
                        setFixedLengthStreamingMode(0)
                    }
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(rawJson)
                    val updatedTrip = parseTripJson(json)
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(updatedTrip)
                } else {
                    val errorStream = conn.errorStream
                    val errorMsg = if (errorStream != null) {
                        try {
                            val rawErr = errorStream.bufferedReader().use(BufferedReader::readText)
                            val errJson = JSONObject(rawErr)
                            errJson.optString("message", "Server error ($responseCode)")
                        } catch (e: Exception) {
                            "Error: HTTP $responseCode"
                        }
                    } else {
                        "Error: HTTP $responseCode"
                    }
                    return Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed trip mutation '$action' on $baseUrl", e)
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(lastException ?: Exception("Network connection failed"))
    }

    fun getFilteredTrips(): List<DriverTripUiItem> {
        return when (selectedTripsTab) {
            DriverTripsTab.CURRENT -> allTrips.filter { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }
            DriverTripsTab.UPCOMING -> allTrips.filter { it.tripStatus.equals("PLANNED", ignoreCase = true) }
            DriverTripsTab.COMPLETED -> allTrips.filter { it.tripStatus.equals("COMPLETED", ignoreCase = true) }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            dashboardState = dashboardState.copy(isLoading = true, errorMessage = null)
            withContext(Dispatchers.IO) {
                fetchDashboard()
                fetchTrips()
                fetchProfile()
            }
            dashboardState = dashboardState.copy(isLoading = false, isRefreshing = false)
        }
    }

    fun fetchDashboard(): Result<Unit> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No active driver session"))
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/driver/dashboard")
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

                    val driverName = json.optString("driverName", dashboardState.driverName)
                    val driverUsername = json.optString("driverUsername", dashboardState.driverUsername)
                    val assignedReg = json.optString("assignedLorryRegistration").takeIf { it.isNotBlank() }
                    val assignedModel = json.optString("assignedLorryMakeModel").takeIf { it.isNotBlank() }
                    val curCount = json.optLong("currentTripsCount", 0L)
                    val upCount = json.optLong("upcomingTripsCount", 0L)
                    val compCount = json.optLong("completedTripsCount", 0L)

                    var todayTripItem: DriverTripUiItem? = null
                    val todayObj = json.optJSONObject("todayTrip")
                    if (todayObj != null) {
                        todayTripItem = parseTripJson(todayObj)
                    }

                    dashboardState = dashboardState.copy(
                        driverName = driverName,
                        driverUsername = driverUsername,
                        assignedLorryRegistration = assignedReg,
                        assignedLorryMakeModel = assignedModel,
                        todayTrip = todayTripItem,
                        currentTripsCount = curCount,
                        upcomingTripsCount = upCount,
                        completedTripsCount = compCount
                    )

                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect to $baseUrl for driver dashboard", e)
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(Exception("Unable to connect to server"))
    }

    fun fetchTrips(): Result<Unit> {
        val token = sessionManager?.getAccessToken() ?: return Result.failure(IllegalStateException("No token"))
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/driver/trips")
                conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val array = JSONArray(rawJson)
                    val list = mutableListOf<DriverTripUiItem>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(parseTripJson(obj))
                    }
                    allTrips = list
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch driver trips from $baseUrl", e)
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(Exception("Unable to fetch trips"))
    }

    fun fetchProfile(): Result<Unit> {
        val token = sessionManager?.getAccessToken() ?: return Result.failure(IllegalStateException("No token"))
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/driver/profile")
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

                    profileState = profileState.copy(
                        id = json.optLong("id", 0L),
                        username = json.optString("username", profileState.username),
                        fullName = json.optString("fullName", profileState.fullName),
                        role = json.optString("role", "Driver"),
                        phoneNumber = json.optString("phoneNumber", profileState.phoneNumber),
                        licenseNumber = json.optString("licenseNumber", profileState.licenseNumber),
                        assignedLorryRegistration = json.optString("assignedLorryRegistration", profileState.assignedLorryRegistration),
                        assignedLorryMakeModel = json.optString("assignedLorryMakeModel", profileState.assignedLorryMakeModel),
                        isOnline = json.optBoolean("isActive", true)
                    )
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch driver profile from $baseUrl", e)
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(Exception("Unable to fetch profile"))
    }

    private fun parseTripJson(obj: JSONObject): DriverTripUiItem {
        return DriverTripUiItem(
            id = obj.optLong("id", 0L),
            tripReference = obj.optString("tripReference", "TRIP-000"),
            driverId = obj.optLong("driverId", 0L),
            driverName = obj.optString("driverName", ""),
            lorryId = if (obj.has("lorryId") && !obj.isNull("lorryId")) obj.optLong("lorryId") else null,
            lorryRegistration = obj.optString("lorryRegistration", "AP 31 TC 1234"),
            lorryMakeModel = obj.optString("lorryMakeModel").takeIf { it.isNotBlank() },
            originLocation = obj.optString("originLocation", "Origin"),
            destinationLocation = obj.optString("destinationLocation", "Destination"),
            tripStatus = obj.optString("tripStatus", "PLANNED"),
            tripDate = obj.optString("tripDate", ""),
            startTime = obj.optString("startTime").takeIf { it.isNotBlank() },
            endTime = obj.optString("endTime").takeIf { it.isNotBlank() },
            distanceKm = obj.optString("distanceKm").takeIf { it.isNotBlank() },
            deliveryNotes = obj.optString("deliveryNotes").takeIf { it.isNotBlank() },
            plannedDeliveryTime = obj.optString("plannedDeliveryTime").takeIf { it.isNotBlank() },
            loadType = obj.optString("loadType").takeIf { it.isNotBlank() }
        )
    }

    // Helper for testing
    fun setTestData(
        dashboard: DriverDashboardUiState,
        trips: List<DriverTripUiItem>,
        profile: DriverProfileUiState
    ) {
        dashboardState = dashboard
        allTrips = trips
        profileState = profile
    }
}
