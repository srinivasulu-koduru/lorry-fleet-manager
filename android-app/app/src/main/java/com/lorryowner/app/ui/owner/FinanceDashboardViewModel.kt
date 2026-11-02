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
import java.time.LocalDate

class FinanceDashboardViewModel(
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = (sessionManager != null)
) : ViewModel() {

    companion object {
        private const val TAG = "FinanceDashVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(FinanceDashboardUiState())
        private set

    init {
        if (autoLoad) {
            loadSummary(uiState.selectedYear)
        }
    }

    fun selectYear(year: Int) {
        if (year != uiState.selectedYear) {
            uiState = uiState.copy(selectedYear = year)
            if (sessionManager != null) {
                loadSummary(year)
            }
        }
    }

    fun loadSummary(year: Int = uiState.selectedYear) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null, selectedYear = year)
            val result = withContext(Dispatchers.IO) {
                fetchSummaryFromBackend(year)
            }
            if (result.isSuccess) {
                uiState = result.getOrThrow().copy(isLoading = false, errorMessage = null)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Unable to load finance summary"
                )
            }
        }
    }

    private fun fetchSummaryFromBackend(year: Int): Result<FinanceDashboardUiState> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("No authenticated session"))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/owner/finance/summary?year=$year")
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

                    val respYear = json.optInt("year", year)
                    val totalIncome = json.optDouble("totalIncome", 0.0)
                    val totalExpenses = json.optDouble("totalExpenses", 0.0)
                    val netProfit = json.optDouble("netProfit", 0.0)
                    val profitMargin = json.optDouble("profitMargin", 0.0)
                    val currentMonth = json.optString("currentMonth", "")
                    val currentMonthIncome = json.optDouble("currentMonthIncome", 0.0)
                    val currentMonthExpense = json.optDouble("currentMonthExpense", 0.0)
                    val currentMonthProfit = json.optDouble("currentMonthProfit", 0.0)

                    // Available years
                    val availYearsList = mutableListOf<Int>()
                    val yearsArr = json.optJSONArray("availableYears")
                    if (yearsArr != null) {
                        for (i in 0 until yearsArr.length()) {
                            availYearsList.add(yearsArr.getInt(i))
                        }
                    }
                    if (availYearsList.isEmpty()) {
                        availYearsList.add(year)
                    }

                    // Monthly summaries
                    val monthlyList = mutableListOf<MonthlySummaryItem>()
                    val monthArr = json.optJSONArray("monthlySummaries")
                    if (monthArr != null) {
                        for (i in 0 until monthArr.length()) {
                            val mObj = monthArr.getJSONObject(i)
                            monthlyList.add(
                                MonthlySummaryItem(
                                    month = mObj.optInt("month", i + 1),
                                    monthName = mObj.optString("monthName", ""),
                                    income = mObj.optDouble("income", 0.0),
                                    expense = mObj.optDouble("expense", 0.0),
                                    profit = mObj.optDouble("profit", 0.0)
                                )
                            )
                        }
                    }

                    // Quick insights
                    val insObj = json.optJSONObject("quickInsights")
                    val insights = if (insObj != null) {
                        FinanceQuickInsightsItem(
                            profitMargin = insObj.optDouble("profitMargin", profitMargin),
                            thisMonthProfit = insObj.optDouble("thisMonthProfit", currentMonthProfit),
                            ytdIncome = insObj.optDouble("ytdIncome", totalIncome),
                            ytdExpense = insObj.optDouble("ytdExpense", totalExpenses),
                            ytdProfit = insObj.optDouble("ytdProfit", netProfit),
                            bestPerformingMonth = insObj.optString("bestPerformingMonth", "Jan"),
                            bestPerformingMonthProfit = insObj.optDouble("bestPerformingMonthProfit", 0.0),
                            highestExpenseMonth = insObj.optString("highestExpenseMonth", "Jan"),
                            highestExpenseMonthAmount = insObj.optDouble("highestExpenseMonthAmount", 0.0)
                        )
                    } else {
                        FinanceQuickInsightsItem()
                    }

                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(
                        FinanceDashboardUiState(
                            selectedYear = respYear,
                            availableYears = availYearsList,
                            totalIncome = totalIncome,
                            totalExpenses = totalExpenses,
                            netProfit = netProfit,
                            profitMargin = profitMargin,
                            currentMonth = currentMonth,
                            currentMonthIncome = currentMonthIncome,
                            currentMonthExpense = currentMonthExpense,
                            currentMonthProfit = currentMonthProfit,
                            monthlySummaries = monthlyList,
                            quickInsights = insights
                        )
                    )
                } else {
                    Log.w(TAG, "Server responded with HTTP ${conn.responseCode} on $baseUrl")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Connection failed on $baseUrl", e)
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(lastException ?: Exception("Network connection failed"))
    }

    fun setTestData(state: FinanceDashboardUiState) {
        uiState = state
    }
}
