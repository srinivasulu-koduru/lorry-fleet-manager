package com.lorryowner.app.ui.driver

/**
 * Navigation tabs for the Driver bottom navigation bar.
 */
enum class DriverNavDestination(val label: String) {
    HOME("Home"),
    TRIPS("Trips"),
    STATUS("Status"),
    PROFILE("Profile")
}

/**
 * Filter tabs for the My Trips screen.
 */
enum class DriverTripsTab(val label: String) {
    CURRENT("Active"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

/**
 * UI representation of a commercial trip assigned to the driver.
 */
data class DriverTripUiItem(
    val id: Long,
    val tripReference: String,
    val driverId: Long = 0L,
    val driverName: String = "",
    val lorryId: Long? = null,
    val lorryRegistration: String = "",
    val lorryMakeModel: String? = null,
    val originLocation: String = "",
    val destinationLocation: String = "",
    val tripStatus: String = "PLANNED", // PLANNED, IN_PROGRESS, COMPLETED
    val tripDate: String = "",
    val startTime: String? = null,
    val endTime: String? = null,
    val distanceKm: String? = null,
    val deliveryNotes: String? = null,
    val plannedDeliveryTime: String? = null,
    val loadType: String? = null,
    val tripAmount: Double = 0.0,
    val tripAmountFormatted: String? = null
)

/**
 * UI state for the Driver Home Dashboard.
 */
data class DriverDashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val driverName: String = "Ramesh Kumar",
    val driverUsername: String = "driver_ramesh",
    val assignedLorryRegistration: String? = "AP 31 TC 1234",
    val assignedLorryMakeModel: String? = "Ashok Leyland 2820 HG",
    val isOnline: Boolean = true,
    val todayTrip: DriverTripUiItem? = null,
    val currentTripsCount: Long = 0,
    val upcomingTripsCount: Long = 0,
    val completedTripsCount: Long = 0,
    val greeting: String = "Good Morning,"
)

/**
 * UI state for the Driver Profile screen.
 */
data class DriverProfileUiState(
    val id: Long = 0L,
    val username: String = "driver_ramesh",
    val fullName: String = "Ramesh Kumar",
    val role: String = "Driver",
    val phoneNumber: String = "+91 98765 43210",
    val email: String = "ramesh.kumar@bhp-logistics.com",
    val licenseNumber: String = "DL-1420110012345",
    val assignedLorryRegistration: String = "AP 31 TC 1234",
    val assignedLorryMakeModel: String = "Tata 1618",
    val experience: String = "5+ Years",
    val joinedOn: String = "15 Mar 2023",
    val isOnline: Boolean = true
)
