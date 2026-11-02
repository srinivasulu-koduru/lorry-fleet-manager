package com.lorryowner.app.ui.owner

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class VehicleMaintenanceSummaryItem(
    val lorryId: Long,
    val registrationNumber: String,
    val makeModel: String,
    val lastServiceDate: String?,
    val nextServiceDate: String?,
    val status: String, // "Up to Date", "Due Soon", "Overdue"
    val totalCost: Double = 0.0,
    val totalRecordsCount: Int = 0
)

data class MaintenanceDashboardUiState(
    val totalVehicles: Long = 0,
    val dueSoonCount: Long = 0,
    val overdueCount: Long = 0,
    val upToDateCount: Long = 0,
    val totalExpenditure: Double = 0.0,
    val currentMonthExpense: Double = 0.0,
    val currentMonth: String = "",
    val currentYear: Int = 0,
    val vehicles: List<VehicleMaintenanceSummaryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class MaintenanceHistoryRecordItem(
    val id: Long,
    val lorryId: Long,
    val serviceType: String,
    val maintenanceDate: String,
    val description: String,
    val cost: Double,
    val odometerReadingKm: Int?,
    val nextServiceDate: String?,
    val serviceCenter: String?,
    val status: String = "Completed"
)

data class VehicleMaintenanceDetailsUiState(
    val lorryId: Long = 0L,
    val registrationNumber: String = "",
    val makeModel: String = "",
    val lastServiceDate: String? = null,
    val nextServiceDate: String? = null,
    val status: String = "Up to Date",
    val totalMaintenanceCost: Double = 0.0,
    val records: List<MaintenanceHistoryRecordItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class LorrySelectItem(
    val id: Long,
    val registrationNumber: String,
    val makeModel: String
)

object MaintenanceFormattingHelper {

    fun formatDisplayDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "N/A"
        return try {
            val parsed = LocalDate.parse(rawDate.trim())
            parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        } catch (e: Exception) {
            rawDate
        }
    }

    fun formatIndianRupee(amount: Double): String {
        return try {
            val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.maximumFractionDigits = 0
            formatter.format(amount)
        } catch (e: Exception) {
            "₹${amount.toLong()}"
        }
    }

    data class DateBadgeParts(
        val day: String,
        val month: String,
        val year: String
    )

    fun parseDateBadge(rawDate: String?): DateBadgeParts {
        if (rawDate.isNullOrBlank()) return DateBadgeParts("--", "---", "----")
        return try {
            val parsed = LocalDate.parse(rawDate.trim())
            DateBadgeParts(
                day = String.format("%02d", parsed.dayOfMonth),
                month = parsed.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                year = parsed.year.toString()
            )
        } catch (e: Exception) {
            val parts = rawDate.trim().split("-")
            if (parts.size == 3) {
                DateBadgeParts(parts[2], parts[1], parts[0])
            } else {
                DateBadgeParts(rawDate.take(2), "--", "")
            }
        }
    }
}
