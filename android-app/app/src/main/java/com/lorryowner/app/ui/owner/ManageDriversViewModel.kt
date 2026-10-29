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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class DriverItemUiState(
    val id: Long,
    val fullName: String,
    val username: String,
    val phoneNumber: String? = null,
    val licenseNumber: String? = null,
    val assignedLorry: String? = null,
    val isActive: Boolean = true,
    val completedTrips: Int = 0,
    val totalDistanceKm: String = "0 km"
)

data class DriverSummaryUiState(
    val totalDrivers: Int = 0,
    val activeDrivers: Int = 0,
    val completedTrips: Int = 0,
    val totalDistanceKm: String = "0"
)

enum class DriverFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    ASSIGNED,
    UNASSIGNED
}

data class ManageDriversUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val summary: DriverSummaryUiState = DriverSummaryUiState(),
    val allDrivers: List<DriverItemUiState> = emptyList(),
    val drivers: List<DriverItemUiState> = emptyList(), // Displayed filtered drivers
    val searchQuery: String = "",
    val selectedFilter: DriverFilter = DriverFilter.ALL,
    val selectedDriverForDetails: DriverItemUiState? = null
)

class ManageDriversViewModel(
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ManageDriversVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(ManageDriversUiState())
        private set

    init {
        loadDrivers()
    }

    fun onSearchQueryChange(query: String) {
        val filtered = applyFilters(uiState.allDrivers, query, uiState.selectedFilter)
        uiState = uiState.copy(searchQuery = query, drivers = filtered)
    }

    fun onFilterSelected(filter: DriverFilter) {
        val filtered = applyFilters(uiState.allDrivers, uiState.searchQuery, filter)
        uiState = uiState.copy(selectedFilter = filter, drivers = filtered)
    }

    fun clearFilters() {
        val filtered = applyFilters(uiState.allDrivers, "", DriverFilter.ALL)
        uiState = uiState.copy(searchQuery = "", selectedFilter = DriverFilter.ALL, drivers = filtered)
    }

    fun loadDrivers() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchDriversFromBackend()
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

    fun refreshDrivers() {
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true)
            withContext(Dispatchers.IO) {
                fetchDriversFromBackend()
            }
            uiState = uiState.copy(isRefreshing = false)
        }
    }

    fun showDriverDetails(driver: DriverItemUiState) {
        uiState = uiState.copy(selectedDriverForDetails = driver)
    }

    fun dismissDriverDetails() {
        uiState = uiState.copy(selectedDriverForDetails = null)
    }

    fun toggleDriverStatus(driverId: Long, newStatus: Boolean, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            uiState = uiState.copy(isActionLoading = true)
            val result = withContext(Dispatchers.IO) {
                executeStatusUpdate(driverId, newStatus)
            }
            uiState = uiState.copy(isActionLoading = false)
            when {
                result.isSuccess -> {
                    val updatedAll = uiState.allDrivers.map {
                        if (it.id == driverId) it.copy(isActive = newStatus) else it
                    }
                    val activeCount = updatedAll.count { it.isActive }
                    val filtered = applyFilters(updatedAll, uiState.searchQuery, uiState.selectedFilter)
                    uiState = uiState.copy(
                        allDrivers = updatedAll,
                        drivers = filtered,
                        summary = uiState.summary.copy(activeDrivers = activeCount),
                        selectedDriverForDetails = uiState.selectedDriverForDetails?.takeIf { it.id == driverId }?.copy(isActive = newStatus)
                    )
                    onComplete?.invoke(true, null)
                }
                else -> {
                    val msg = result.exceptionOrNull()?.message ?: "Failed to update driver status"
                    onComplete?.invoke(false, msg)
                }
            }
        }
    }

    fun assignLorry(driverId: Long, registrationNumber: String?, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            uiState = uiState.copy(isActionLoading = true)
            val result = withContext(Dispatchers.IO) {
                executeAssignLorry(driverId, registrationNumber)
            }
            uiState = uiState.copy(isActionLoading = false)
            when {
                result.isSuccess -> {
                    val assignedReg = result.getOrNull()
                    val updatedAll = uiState.allDrivers.map {
                        if (it.id == driverId) it.copy(assignedLorry = assignedReg)
                        else if (assignedReg != null && it.assignedLorry == assignedReg) it.copy(assignedLorry = null)
                        else it
                    }
                    val filtered = applyFilters(updatedAll, uiState.searchQuery, uiState.selectedFilter)
                    uiState = uiState.copy(
                        allDrivers = updatedAll,
                        drivers = filtered,
                        selectedDriverForDetails = uiState.selectedDriverForDetails?.takeIf { it.id == driverId }?.copy(assignedLorry = assignedReg)
                    )
                    onComplete?.invoke(true, null)
                }
                else -> {
                    val msg = result.exceptionOrNull()?.message ?: "Failed to assign lorry"
                    onComplete?.invoke(false, msg)
                }
            }
        }
    }

    fun deleteDriver(driverId: Long, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            uiState = uiState.copy(isActionLoading = true)
            val result = withContext(Dispatchers.IO) {
                executeDeleteDriver(driverId)
            }
            uiState = uiState.copy(isActionLoading = false)
            when {
                result.isSuccess -> {
                    val updatedAll = uiState.allDrivers.filter { it.id != driverId }
                    val activeCount = updatedAll.count { it.isActive }
                    val totalTrips = updatedAll.sumOf { it.completedTrips }
                    val filtered = applyFilters(updatedAll, uiState.searchQuery, uiState.selectedFilter)
                    uiState = uiState.copy(
                        allDrivers = updatedAll,
                        drivers = filtered,
                        summary = uiState.summary.copy(
                            totalDrivers = updatedAll.size,
                            activeDrivers = activeCount,
                            completedTrips = totalTrips
                        ),
                        selectedDriverForDetails = null
                    )
                    onComplete?.invoke(true, null)
                }
                else -> {
                    val msg = result.exceptionOrNull()?.message ?: "Failed to delete driver"
                    onComplete?.invoke(false, msg)
                }
            }
        }
    }

    private fun applyFilters(
        all: List<DriverItemUiState>,
        query: String,
        filter: DriverFilter
    ): List<DriverItemUiState> {
        val q = query.trim().lowercase(Locale.ROOT)
        return all.filter { driver ->
            val matchesFilter = when (filter) {
                DriverFilter.ALL -> true
                DriverFilter.ACTIVE -> driver.isActive
                DriverFilter.INACTIVE -> !driver.isActive
                DriverFilter.ASSIGNED -> !driver.assignedLorry.isNullOrBlank()
                DriverFilter.UNASSIGNED -> driver.assignedLorry.isNullOrBlank()
            }
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                driver.fullName.lowercase(Locale.ROOT).contains(q) ||
                driver.username.lowercase(Locale.ROOT).contains(q) ||
                (driver.phoneNumber?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                (driver.assignedLorry?.lowercase(Locale.ROOT)?.contains(q) == true)
            }
            matchesFilter && matchesQuery
        }
    }

    private fun fetchDriversFromBackend(): Result<Unit> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(Exception("No active session found. Please log in."))
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/drivers"
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
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val jsonArr = JSONArray(rawJson)
                    val driverList = mutableListOf<DriverItemUiState>()

                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        val id = obj.optLong("id")
                        val username = obj.optString("username", "driver")
                        val fullName = obj.optString("fullName", username)
                        val phone = obj.optString("phoneNumber").takeIf { it.isNotBlank() }
                        val license = obj.optString("licenseNumber").takeIf { it.isNotBlank() }
                        val active = if (obj.has("active")) obj.optBoolean("active", true) else obj.optBoolean("isActive", true)
                        val assignedLorry = obj.optString("assignedLorryRegistration").takeIf { it.isNotBlank() }
                            ?: obj.optString("assignedLorry").takeIf { it.isNotBlank() }

                        // Parse real trip metrics returned from backend
                        val completedTrips = obj.optInt("completedTrips", 0)
                        val distanceVal = obj.optDouble("totalRecordedDistanceKm", 0.0)
                        val distanceStr = if (distanceVal > 0) String.format(Locale.US, "%,.1f km", distanceVal) else "0 km"

                        driverList.add(
                            DriverItemUiState(
                                id = id,
                                fullName = fullName,
                                username = username,
                                phoneNumber = phone,
                                licenseNumber = license,
                                assignedLorry = assignedLorry,
                                isActive = active,
                                completedTrips = completedTrips,
                                totalDistanceKm = distanceStr
                            )
                        )
                    }

                    val activeCount = driverList.count { it.isActive }
                    val totalTrips = driverList.sumOf { it.completedTrips }
                    val totalDistanceSum = driverList.sumOf {
                        it.totalDistanceKm.replace(" km", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    }
                    val totalDistanceDisplay = if (totalDistanceSum > 0) String.format(Locale.US, "%,.1f", totalDistanceSum) else "0"

                    val filtered = applyFilters(driverList, uiState.searchQuery, uiState.selectedFilter)

                    uiState = uiState.copy(
                        summary = DriverSummaryUiState(
                            totalDrivers = driverList.size,
                            activeDrivers = activeCount,
                            completedTrips = totalTrips,
                            totalDistanceKm = totalDistanceDisplay
                        ),
                        allDrivers = driverList,
                        drivers = filtered,
                        errorMessage = null
                    )
                    return Result.success(Unit)
                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    sessionManager?.clearSession()
                    return Result.failure(Exception("Session expired. Please sign in again."))
                } else {
                    lastException = Exception("HTTP $responseCode from backend at $baseUrl")
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        return Result.failure(lastException ?: Exception("Could not connect to fleet server."))
    }

    private fun executeStatusUpdate(driverId: Long, newStatus: Boolean): Result<Unit> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Session expired."))

        val candidates = ApiConfig.getCandidateBaseUrls(sessionManager)
        for (baseUrl in candidates) {
            val endpoint = "$baseUrl/api/owner/drivers/$driverId/status"
            var conn: HttpURLConnection? = null
            try {
                val url = URL(endpoint)
                conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val body = JSONObject().apply { put("isActive", newStatus) }
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()); it.flush() }

                val code = conn.responseCode
                if (code in 200..299) {
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Status update failed at $baseUrl: ${e.message}")
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(Exception("Could not update driver status."))
    }

    private fun executeAssignLorry(driverId: Long, registrationNumber: String?): Result<String?> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Session expired."))

        val candidates = ApiConfig.getCandidateBaseUrls(sessionManager)
        for (baseUrl in candidates) {
            val endpoint = "$baseUrl/api/owner/drivers/$driverId/assign-lorry"
            var conn: HttpURLConnection? = null
            try {
                val url = URL(endpoint)
                conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val body = JSONObject()
                if (registrationNumber != null && registrationNumber.isNotBlank()) {
                    body.put("registrationNumber", registrationNumber.trim())
                }
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()); it.flush() }

                val code = conn.responseCode
                if (code in 200..299) {
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    val raw = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(raw)
                    val assigned = json.optString("assignedLorryRegistration").takeIf { it.isNotBlank() }
                    return Result.success(assigned)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lorry assign failed at $baseUrl: ${e.message}")
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(Exception("Could not assign lorry."))
    }

    private fun executeDeleteDriver(driverId: Long): Result<Unit> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Session expired."))

        val candidates = ApiConfig.getCandidateBaseUrls(sessionManager)
        for (baseUrl in candidates) {
            val endpoint = "$baseUrl/api/owner/drivers/$driverId"
            var conn: HttpURLConnection? = null
            try {
                val url = URL(endpoint)
                conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Delete driver failed at $baseUrl: ${e.message}")
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(Exception("Could not delete driver."))
    }
}
