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
import java.text.NumberFormat
import java.time.LocalTime
import java.util.Locale

data class FleetOverviewState(
    val runningNow: Long = 3,
    val readyToStart: Long = 1,
    val notReady: Long = 1,
    val totalLorries: Long = 5
)

data class FinancialSummaryState(
    val monthLabel: String = "This month",
    val isSample: Boolean = true,
    val income: String = "₹3,89,500",
    val expenses: String = "₹2,58,000",
    val profit: String = "₹1,31,500"
)

data class ActivityItemState(
    val id: Long,
    val registrationNumber: String,
    val statusDescription: String,
    val timeAgo: String,
    val colorIndicator: String // "GREEN", "ORANGE", "BLUE"
)

data class LorryItemState(
    val id: Long,
    val registrationNumber: String,
    val makeModel: String,
    val capacityTons: Double,
    val operationalStatus: String,
    val notes: String
)

data class OwnerDashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isSessionExpired: Boolean = false,
    val ownerName: String = "Owner",
    val greeting: String = GreetingHelper.getGreeting(),
    val unreadNotificationsCount: Int = 1,
    val fleetOverview: FleetOverviewState = FleetOverviewState(),
    val financialSummary: FinancialSummaryState = FinancialSummaryState(),
    val recentActivities: List<ActivityItemState> = emptyList(),
    val lorriesList: List<LorryItemState> = emptyList(),
    val isLorriesDialogVisible: Boolean = false,
    val isProfileDialogVisible: Boolean = false,
    val selectedNavIndex: Int = 0
)

class OwnerDashboardViewModel(
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "OwnerDashboardVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(OwnerDashboardUiState())
        private set

    init {
        val savedName = sessionManager?.getFullName().orEmpty()
            .ifEmpty { sessionManager?.getUsername().orEmpty() }
            .ifEmpty { "Owner" }
        uiState = uiState.copy(
            ownerName = savedName,
            greeting = GreetingHelper.getGreeting()
        )
        loadDashboard()
    }

    /**
     * Updates the dynamic time-based greeting if the period has changed.
     */
    fun updateGreeting(time: LocalTime = LocalTime.now()) {
        val newGreeting = GreetingHelper.getGreeting(time)
        if (uiState.greeting != newGreeting) {
            uiState = uiState.copy(greeting = newGreeting)
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                fetchDashboardFromBackend()
            }
            if (result.isSuccess) {
                uiState = uiState.copy(isLoading = false, isRefreshing = false)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun refreshDashboard() {
        updateGreeting()
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true)
            withContext(Dispatchers.IO) {
                fetchDashboardFromBackend()
            }
            uiState = uiState.copy(isRefreshing = false)
        }
    }

    fun showLorriesDialog() {
        uiState = uiState.copy(isLorriesDialogVisible = true)
        if (uiState.lorriesList.isEmpty()) {
            loadLorriesList()
        }
    }

    fun dismissLorriesDialog() {
        uiState = uiState.copy(isLorriesDialogVisible = false)
    }

    fun showProfileDialog() {
        uiState = uiState.copy(isProfileDialogVisible = true)
    }

    fun dismissProfileDialog() {
        uiState = uiState.copy(isProfileDialogVisible = false)
    }

    fun onNavItemSelected(index: Int) {
        uiState = uiState.copy(selectedNavIndex = index)
    }

    private fun loadLorriesList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fetchLorriesFromBackend()
            }
        }
    }

    private fun fetchDashboardFromBackend(): Result<Unit> {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Session token missing"))
        }

        val endpointUrl = ApiConfig.buildUrl("/api/owner/dashboard", sessionManager)
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

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(rawJson)

                val ownerName = json.optString("ownerName", uiState.ownerName)
                val notifications = json.optInt("unreadNotificationsCount", 1)

                // Fleet overview counts
                val fleetObj = json.optJSONObject("fleetOverview")
                val runningNow = fleetObj?.optLong("runningNow", 3L) ?: 3L
                val readyToStart = fleetObj?.optLong("readyToStart", 1L) ?: 1L
                val notReady = fleetObj?.optLong("notReady", 1L) ?: 1L
                val totalLorries = fleetObj?.optLong("totalLorries", 5L) ?: 5L

                // Financial summary
                val finObj = json.optJSONObject("financialSummary")
                val monthLabel = finObj?.optString("monthLabel", "This month") ?: "This month"
                val isSample = finObj?.optBoolean("isSample", true) ?: true
                val incomeVal = finObj?.optDouble("income", 389500.0) ?: 389500.0
                val expenseVal = finObj?.optDouble("expenses", 258000.0) ?: 258000.0
                val profitVal = finObj?.optDouble("profit", 131500.0) ?: 131500.0

                // Recent activities
                val activitiesList = mutableListOf<ActivityItemState>()
                val activitiesArr = json.optJSONArray("recentActivities")
                if (activitiesArr != null) {
                    for (i in 0 until activitiesArr.length()) {
                        val act = activitiesArr.getJSONObject(i)
                        activitiesList.add(
                            ActivityItemState(
                                id = act.optLong("id", i.toLong()),
                                registrationNumber = act.optString("registrationNumber", ""),
                                statusDescription = act.optString("statusDescription", ""),
                                timeAgo = act.optString("timeAgo", ""),
                                colorIndicator = act.optString("colorIndicator", "GREEN")
                            )
                        )
                    }
                }

                uiState = uiState.copy(
                    ownerName = ownerName,
                    unreadNotificationsCount = notifications,
                    fleetOverview = FleetOverviewState(
                        runningNow = runningNow,
                        readyToStart = readyToStart,
                        notReady = notReady,
                        totalLorries = totalLorries
                    ),
                    financialSummary = FinancialSummaryState(
                        monthLabel = monthLabel,
                        isSample = isSample,
                        income = formatCurrency(incomeVal),
                        expenses = formatCurrency(expenseVal),
                        profit = formatCurrency(profitVal)
                    ),
                    recentActivities = activitiesList
                )
                Result.success(Unit)
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.w(TAG, "Dashboard endpoint returned HTTP 401 Unauthorized - session expired or invalid")
                sessionManager?.clearSession()
                uiState = uiState.copy(isSessionExpired = true)
                Result.failure(Exception("Session expired. Please sign in again."))
            } else {
                Log.w(TAG, "Dashboard endpoint returned HTTP $responseCode")
                Result.failure(Exception("HTTP $responseCode from backend"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching dashboard from $endpointUrl: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchLorriesFromBackend() {
        val token = sessionManager?.getAccessToken() ?: return
        val endpointUrl = ApiConfig.buildUrl("/api/owner/lorries", sessionManager)
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
                val jsonArr = JSONArray(rawJson)
                val list = mutableListOf<LorryItemState>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    list.add(
                        LorryItemState(
                            id = obj.optLong("id"),
                            registrationNumber = obj.optString("registrationNumber"),
                            makeModel = obj.optString("makeModel"),
                            capacityTons = obj.optDouble("capacityTons", 0.0),
                            operationalStatus = obj.optString("operationalStatus"),
                            notes = obj.optString("notes")
                        )
                    )
                }
                uiState = uiState.copy(lorriesList = list)
            } else if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.w(TAG, "Lorries endpoint returned HTTP 401 Unauthorized")
                sessionManager?.clearSession()
                uiState = uiState.copy(isSessionExpired = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch lorries: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.maximumFractionDigits = 0
            val formatted = formatter.format(amount)
            if (!formatted.startsWith("₹")) {
                "₹" + NumberFormat.getNumberInstance(locale).format(amount.toLong())
            } else {
                formatted
            }
        } catch (e: Exception) {
            "₹" + amount.toLong().toString()
        }
    }
}
