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

class MonthlyFinanceDetailsViewModel(
    initialYear: Int = LocalDate.now().year,
    initialMonth: Int = LocalDate.now().monthValue,
    private val sessionManager: SessionManager? = null,
    autoLoad: Boolean = (sessionManager != null)
) : ViewModel() {

    companion object {
        private const val TAG = "MonthlyFinanceVM"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }

    var uiState by mutableStateOf(
        MonthlyFinanceDetailsUiState(
            year = initialYear,
            month = initialMonth
        )
    )
        private set

    init {
        if (autoLoad) {
            loadDetails(initialYear, initialMonth)
        }
    }

    fun selectMonth(month: Int) {
        if (month in 1..12 && month != uiState.month) {
            uiState = uiState.copy(
                month = month,
                monthName = FinanceFormattingHelper.getMonthName(month)
            )
            if (sessionManager != null) {
                loadDetails(uiState.year, month)
            }
        }
    }

    fun selectYear(year: Int) {
        if (year != uiState.year) {
            uiState = uiState.copy(year = year)
            if (sessionManager != null) {
                loadDetails(year, uiState.month)
            }
        }
    }

    fun loadDetails(year: Int = uiState.year, month: Int = uiState.month) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null, year = year, month = month)
            val result = withContext(Dispatchers.IO) {
                fetchDetailsFromBackend(year, month)
            }
            if (result.isSuccess) {
                uiState = result.getOrThrow().copy(isLoading = false, errorMessage = null)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Unable to load monthly finance details"
                )
            }
        }
    }

    private fun fetchDetailsFromBackend(year: Int, month: Int): Result<MonthlyFinanceDetailsUiState> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("No authenticated session"))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastException: Exception? = null

        for (baseUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl/api/owner/finance/monthly?year=$year&month=$month")
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
                    val respMonth = json.optInt("month", month)
                    val monthName = json.optString("monthName", "")
                    val totalIncome = json.optDouble("totalIncome", 0.0)
                    val totalExpenses = json.optDouble("totalExpenses", 0.0)
                    val netProfit = json.optDouble("netProfit", 0.0)
                    val profitMargin = json.optDouble("profitMargin", 0.0)

                    // Parse income transactions
                    val incomeList = mutableListOf<FinanceTransactionItem>()
                    val incArr = json.optJSONArray("incomeTransactions")
                    if (incArr != null) {
                        for (i in 0 until incArr.length()) {
                            val o = incArr.getJSONObject(i)
                            incomeList.add(parseTransaction(o))
                        }
                    }

                    // Parse expense transactions
                    val expenseList = mutableListOf<FinanceTransactionItem>()
                    val expArr = json.optJSONArray("expenseTransactions")
                    if (expArr != null) {
                        for (i in 0 until expArr.length()) {
                            val o = expArr.getJSONObject(i)
                            expenseList.add(parseTransaction(o))
                        }
                    }

                    // Expense breakdown map
                    val expCategories = mutableMapOf<String, Double>()
                    val expCatObj = json.optJSONObject("expenseCategoryBreakdown")
                    if (expCatObj != null) {
                        val keys = expCatObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            expCategories[k] = expCatObj.optDouble(k, 0.0)
                        }
                    }

                    // Income breakdown map
                    val incCategories = mutableMapOf<String, Double>()
                    val incCatObj = json.optJSONObject("incomeCategoryBreakdown")
                    if (incCatObj != null) {
                        val keys = incCatObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            incCategories[k] = incCatObj.optDouble(k, 0.0)
                        }
                    }

                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(
                        MonthlyFinanceDetailsUiState(
                            year = respYear,
                            month = respMonth,
                            monthName = monthName,
                            totalIncome = totalIncome,
                            totalExpenses = totalExpenses,
                            netProfit = netProfit,
                            profitMargin = profitMargin,
                            incomeTransactions = incomeList,
                            expenseTransactions = expenseList,
                            expenseCategories = expCategories,
                            incomeCategories = incCategories
                        )
                    )
                } else {
                    Log.w(TAG, "Server responded with HTTP ${conn.responseCode} on $baseUrl")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading monthly details from $baseUrl", e)
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }
        return Result.failure(lastException ?: Exception("Network connection failed"))
    }

    private fun parseTransaction(o: JSONObject): FinanceTransactionItem {
        return FinanceTransactionItem(
            id = o.optLong("id", 0L),
            lorryId = if (o.has("lorryId") && !o.isNull("lorryId")) o.optLong("lorryId") else null,
            lorryRegistration = o.optString("lorryRegistration").takeIf { it.isNotBlank() && it != "null" },
            tripId = if (o.has("tripId") && !o.isNull("tripId")) o.optLong("tripId") else null,
            tripReference = o.optString("tripReference").takeIf { it.isNotBlank() && it != "null" },
            transactionType = o.optString("transactionType", "EXPENSE"),
            category = o.optString("category", "OTHER"),
            description = o.optString("description", ""),
            amount = o.optDouble("amount", 0.0),
            transactionDate = o.optString("transactionDate", ""),
            paymentStatus = o.optString("paymentStatus", "RECEIVED"),
            paymentMode = o.optString("paymentMode").takeIf { it.isNotBlank() && it != "null" },
            referenceNumber = o.optString("referenceNumber").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    fun setTestData(state: MonthlyFinanceDetailsUiState) {
        uiState = state
    }
}
