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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class TripMaintenanceItemUiState(
    val id: Long,
    val lorryId: Long?,
    val lorryRegistration: String,
    val maintenanceDate: String,
    val serviceType: String,
    val description: String,
    val cost: Double,
    val serviceCenter: String?,
    val odometerReadingKm: Int?,
    val nextServiceDate: String?,
    val tripId: Long?,
    val tripReference: String?
)

data class TripExpenseItemUiState(
    val id: Long,
    val lorryId: Long?,
    val lorryRegistration: String,
    val tripId: Long?,
    val tripReference: String?,
    val transactionType: String,
    val category: String,
    val description: String,
    val amount: Double,
    val transactionDate: String,
    val paymentStatus: String,
    val paymentMode: String?,
    val referenceNumber: String?
)

data class OwnerTripDetailsUiState(
    val id: Long = 0L,
    val tripReference: String = "",
    val lorryId: Long? = null,
    val lorryRegistration: String = "",
    val lorryMakeModel: String? = null,
    val driverId: Long? = null,
    val driverName: String = "",
    val driverPhone: String? = null,
    val origin: String = "",
    val destination: String = "",
    val plannedDepartureDate: String? = null,
    val plannedDepartureTime: String? = null,
    val startTime: String? = null,
    val plannedDeliveryDate: String? = null,
    val plannedDeliveryTime: String? = null,
    val endTime: String? = null,
    val status: String = "PLANNED",
    val distanceKm: Double = 0.0,
    val loadType: String? = null,
    val remarks: String? = null,
    // Financial Breakdown
    val tripRevenue: Double = 0.0,
    val maintenanceExpenses: Double = 0.0,
    val fuelExpenses: Double = 0.0,
    val driverExpenses: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    // Itemized Records
    val maintenanceRecords: List<TripMaintenanceItemUiState> = emptyList(),
    val otherExpensesList: List<TripExpenseItemUiState> = emptyList(),
    // UI Loading & Mutation states
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmittingExpense: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

class OwnerTripDetailsViewModel(
    val tripId: Long,
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = true
) : ViewModel() {

    companion object {
        private const val TAG = "OwnerTripDetailsVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(OwnerTripDetailsUiState(id = tripId))
        private set

    init {
        if (autoLoad) {
            loadTripDetails()
        }
    }

    fun setTestData(state: OwnerTripDetailsUiState) {
        uiState = state
    }

    fun loadTripDetails() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchDetailsFromBackend()
            }
            if (result.isSuccess) {
                uiState = result.getOrThrow().copy(isLoading = false)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load trip details"
                )
            }
        }
    }

    fun refreshTripDetails() {
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchDetailsFromBackend()
            }
            if (result.isSuccess) {
                uiState = result.getOrThrow().copy(isRefreshing = false)
            } else {
                uiState = uiState.copy(
                    isRefreshing = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to refresh trip details"
                )
            }
        }
    }

    fun addTripExpense(
        category: String,
        amount: Double,
        description: String,
        date: String = LocalDate.now().toString(),
        notes: String = "",
        workshop: String = "",
        odometerKm: Int? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (amount <= 0.0) {
            onComplete(false, "Amount must be strictly greater than 0")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isSubmittingExpense = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                postExpenseToBackend(category, amount, description, date, notes, workshop, odometerKm)
            }
            uiState = uiState.copy(isSubmittingExpense = false)
            if (result.isSuccess) {
                loadTripDetails()
                onComplete(true, "Entry added successfully")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to add entry"
                uiState = uiState.copy(errorMessage = err)
                onComplete(false, err)
            }
        }
    }

    fun deleteMaintenanceRecord(recordId: Long, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                deleteRecordFromBackend("/api/owner/trips/$tripId/maintenance/$recordId")
            }
            if (result.isSuccess) {
                loadTripDetails()
                onComplete(true, "Maintenance record deleted")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to delete record"
                onComplete(false, err)
            }
        }
    }

    fun deleteExpenseRecord(expenseId: Long, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                deleteRecordFromBackend("/api/owner/trips/$tripId/expenses/$expenseId")
            }
            if (result.isSuccess) {
                loadTripDetails()
                onComplete(true, "Expense deleted")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to delete expense"
                onComplete(false, err)
            }
        }
    }

    private fun fetchDetailsFromBackend(): Result<OwnerTripDetailsUiState> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Session token missing"))
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/trips/$tripId/details"
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

                    // Parse Maintenance Records
                    val mrArray = json.optJSONArray("maintenanceRecords") ?: org.json.JSONArray()
                    val mrList = mutableListOf<TripMaintenanceItemUiState>()
                    for (i in 0 until mrArray.length()) {
                        val obj = mrArray.getJSONObject(i)
                        mrList.add(
                            TripMaintenanceItemUiState(
                                id = obj.optLong("id"),
                                lorryId = if (obj.has("lorryId") && !obj.isNull("lorryId")) obj.optLong("lorryId") else null,
                                lorryRegistration = obj.optString("lorryRegistration", ""),
                                maintenanceDate = obj.optString("maintenanceDate", ""),
                                serviceType = obj.optString("serviceType", "Service"),
                                description = obj.optString("description", ""),
                                cost = obj.optDouble("cost", 0.0),
                                serviceCenter = if (obj.has("serviceCenter") && !obj.isNull("serviceCenter")) obj.optString("serviceCenter") else null,
                                odometerReadingKm = if (obj.has("odometerReadingKm") && !obj.isNull("odometerReadingKm")) obj.optInt("odometerReadingKm") else null,
                                nextServiceDate = if (obj.has("nextServiceDate") && !obj.isNull("nextServiceDate")) obj.optString("nextServiceDate") else null,
                                tripId = if (obj.has("tripId") && !obj.isNull("tripId")) obj.optLong("tripId") else null,
                                tripReference = if (obj.has("tripReference") && !obj.isNull("tripReference")) obj.optString("tripReference") else null
                            )
                        )
                    }

                    // Parse Other Expense Transactions
                    val expArray = json.optJSONArray("otherExpensesList") ?: org.json.JSONArray()
                    val expList = mutableListOf<TripExpenseItemUiState>()
                    for (i in 0 until expArray.length()) {
                        val obj = expArray.getJSONObject(i)
                        expList.add(
                            TripExpenseItemUiState(
                                id = obj.optLong("id"),
                                lorryId = if (obj.has("lorryId") && !obj.isNull("lorryId")) obj.optLong("lorryId") else null,
                                lorryRegistration = obj.optString("lorryRegistration", ""),
                                tripId = if (obj.has("tripId") && !obj.isNull("tripId")) obj.optLong("tripId") else null,
                                tripReference = if (obj.has("tripReference") && !obj.isNull("tripReference")) obj.optString("tripReference") else null,
                                transactionType = obj.optString("transactionType", "EXPENSE"),
                                category = obj.optString("category", "OTHER"),
                                description = obj.optString("description", ""),
                                amount = obj.optDouble("amount", 0.0),
                                transactionDate = obj.optString("transactionDate", ""),
                                paymentStatus = obj.optString("paymentStatus", "PAID"),
                                paymentMode = if (obj.has("paymentMode") && !obj.isNull("paymentMode")) obj.optString("paymentMode") else null,
                                referenceNumber = if (obj.has("referenceNumber") && !obj.isNull("referenceNumber")) obj.optString("referenceNumber") else null
                            )
                        )
                    }

                    val details = OwnerTripDetailsUiState(
                        id = json.optLong("id", tripId),
                        tripReference = json.optString("tripReference", ""),
                        lorryId = if (json.has("lorryId") && !json.isNull("lorryId")) json.optLong("lorryId") else null,
                        lorryRegistration = json.optString("lorryRegistration", "Fleet Lorry"),
                        lorryMakeModel = if (json.has("lorryMakeModel") && !json.isNull("lorryMakeModel")) json.optString("lorryMakeModel") else null,
                        driverId = if (json.has("driverId") && !json.isNull("driverId")) json.optLong("driverId") else null,
                        driverName = json.optString("driverName", "Unassigned"),
                        driverPhone = if (json.has("driverPhone") && !json.isNull("driverPhone")) json.optString("driverPhone") else null,
                        origin = json.optString("origin", "Unknown"),
                        destination = json.optString("destination", "Unknown"),
                        plannedDepartureDate = if (json.has("plannedDepartureDate") && !json.isNull("plannedDepartureDate")) json.optString("plannedDepartureDate") else null,
                        plannedDepartureTime = if (json.has("plannedDepartureTime") && !json.isNull("plannedDepartureTime")) json.optString("plannedDepartureTime") else null,
                        startTime = if (json.has("startTime") && !json.isNull("startTime")) json.optString("startTime") else null,
                        plannedDeliveryDate = if (json.has("plannedDeliveryDate") && !json.isNull("plannedDeliveryDate")) json.optString("plannedDeliveryDate") else null,
                        plannedDeliveryTime = if (json.has("plannedDeliveryTime") && !json.isNull("plannedDeliveryTime")) json.optString("plannedDeliveryTime") else null,
                        endTime = if (json.has("endTime") && !json.isNull("endTime")) json.optString("endTime") else null,
                        status = json.optString("status", "PLANNED"),
                        distanceKm = json.optDouble("distanceKm", 0.0),
                        loadType = if (json.has("loadType") && !json.isNull("loadType")) json.optString("loadType") else null,
                        remarks = if (json.has("remarks") && !json.isNull("remarks")) json.optString("remarks") else null,
                        // Financial breakdown calculated server-side
                        tripRevenue = json.optDouble("tripRevenue", 0.0),
                        maintenanceExpenses = json.optDouble("maintenanceExpenses", 0.0),
                        fuelExpenses = json.optDouble("fuelExpenses", 0.0),
                        driverExpenses = json.optDouble("driverExpenses", 0.0),
                        otherExpenses = json.optDouble("otherExpenses", 0.0),
                        totalExpenses = json.optDouble("totalExpenses", 0.0),
                        netProfit = json.optDouble("netProfit", 0.0),
                        maintenanceRecords = mrList,
                        otherExpensesList = expList
                    )

                    return Result.success(details)
                } else if (connection.responseCode in listOf(HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_BAD_METHOD)) {
                    Log.i(TAG, "Server returned ${connection.responseCode} on /details. Falling back to basic /api/owner/trips/$tripId")
                    val fallback = fetchDetailsFallbackLegacy(baseUrl, token)
                    if (fallback.isSuccess) {
                        return fallback
                    }
                    val err = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    lastException = Exception("HTTP ${connection.responseCode}: $err")
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    lastException = Exception("HTTP ${connection.responseCode}: $err")
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

    private fun fetchDetailsFallbackLegacy(baseUrl: String, token: String): Result<OwnerTripDetailsUiState> {
        return try {
            val url = URL("$baseUrl/api/owner/trips/$tripId")
            val conn = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val raw = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(raw)
                val revenue = json.optDouble("tripAmount", 0.0)
                val state = OwnerTripDetailsUiState(
                    id = json.optLong("id", tripId),
                    tripReference = json.optString("tripReference", "TRIP-$tripId"),
                    lorryId = if (json.has("lorryId") && !json.isNull("lorryId")) json.optLong("lorryId") else null,
                    lorryRegistration = json.optString("lorryRegistration", "Fleet Lorry"),
                    lorryMakeModel = if (json.has("lorryMakeModel") && !json.isNull("lorryMakeModel")) json.optString("lorryMakeModel") else null,
                    driverId = if (json.has("driverId") && !json.isNull("driverId")) json.optLong("driverId") else null,
                    driverName = json.optString("driverName", "Unassigned Driver"),
                    driverPhone = null,
                    origin = json.optString("originLocation", "Unknown"),
                    destination = json.optString("destinationLocation", "Unknown"),
                    plannedDepartureDate = if (json.has("tripDate") && !json.isNull("tripDate")) json.optString("tripDate") else null,
                    plannedDepartureTime = if (json.has("startTime") && !json.isNull("startTime")) json.optString("startTime") else null,
                    startTime = if (json.has("startTime") && !json.isNull("startTime")) json.optString("startTime") else null,
                    plannedDeliveryDate = null,
                    plannedDeliveryTime = null,
                    endTime = if (json.has("endTime") && !json.isNull("endTime")) json.optString("endTime") else null,
                    status = json.optString("tripStatus", "PLANNED"),
                    distanceKm = json.optDouble("distanceKm", 0.0),
                    loadType = null,
                    remarks = if (json.has("deliveryNotes") && !json.isNull("deliveryNotes")) json.optString("deliveryNotes") else null,
                    tripRevenue = revenue,
                    maintenanceExpenses = 0.0,
                    fuelExpenses = 0.0,
                    driverExpenses = 0.0,
                    otherExpenses = 0.0,
                    totalExpenses = 0.0,
                    netProfit = revenue,
                    maintenanceRecords = emptyList(),
                    otherExpensesList = emptyList()
                )
                Result.success(state)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                Result.failure(Exception("HTTP ${conn.responseCode}: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun postExpenseToBackend(
        category: String,
        amount: Double,
        description: String,
        date: String,
        notes: String,
        workshop: String,
        odometerKm: Int?
    ): Result<Unit> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Session token missing"))
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        val payload = JSONObject().apply {
            put("category", category)
            put("amount", amount)
            put("description", description)
            put("expenseDate", date)
            if (notes.isNotBlank()) put("notes", notes)
            if (workshop.isNotBlank()) put("serviceCenter", workshop)
            if (odometerKm != null && odometerKm >= 0) put("odometerReadingKm", odometerKm)
        }

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl/api/owner/trips/$tripId/expenses"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpointUrl)
                connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
                    return Result.success(Unit)
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    lastException = Exception("HTTP $code: $err")
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Expense post error: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }

        return Result.failure(lastException ?: Exception("Failed to post expense"))
    }

    private fun deleteRecordFromBackend(path: String): Result<Unit> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Session token missing"))
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl$path"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpointUrl)
                connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_OK) {
                    return Result.success(Unit)
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    lastException = Exception("HTTP $code: $err")
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        return Result.failure(lastException ?: Exception("Failed to delete record"))
    }
}
