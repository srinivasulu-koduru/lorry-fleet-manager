package com.lorryowner.app.ui.owner

import com.google.android.gms.maps.model.LatLng
import java.text.DecimalFormat

data class BreadcrumbPoint(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double?,
    val timestamp: String?
)

enum class GpsStatus {
    LIVE,
    STALE,
    WAITING_FOR_LOCATION
}

data class LiveTrackingLorryUiState(
    val lorryId: Long = 0L,
    val registrationNumber: String = "",
    val makeModel: String = "",
    val tripId: Long = 0L,
    val tripReference: String = "",
    val driverName: String = "",
    val driverPhone: String? = null,
    val originLocation: String = "",
    val destinationLocation: String = "",
    val currentLocationDescription: String = "Waiting for driver location",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val currentSpeedKmh: Double? = null,
    val heading: Double? = null,
    val accuracyMeters: Double? = null,
    val distanceTravelledKm: Double = 0.0,
    val remainingDistanceKm: Double = 0.0,
    val estimatedDrivingTime: String = "--",
    val estimatedArrival: String = "--",
    val gpsStatus: GpsStatus = GpsStatus.WAITING_FOR_LOCATION,
    val lastGpsUpdate: String? = null,
    val isLive: Boolean = false,
    val hasGpsFix: Boolean = false,
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val breadcrumbs: List<BreadcrumbPoint> = emptyList(),
    val plannedRoadRoute: List<LatLng> = emptyList(),
    val isRouteDeviated: Boolean = false,
    val deviationDistanceKm: Double = 0.0,
    val isRouteLoading: Boolean = false,
    val routeErrorMessage: String? = null
)

data class RunningLorriesUiState(
    val isLoading: Boolean = false,
    val lorries: List<LiveTrackingLorryUiState> = emptyList(),
    val errorMessage: String? = null
)

data class LiveTrackingScreenUiState(
    val isLoading: Boolean = false,
    val trackingData: LiveTrackingLorryUiState? = null,
    val isSatelliteMode: Boolean = true,
    val errorMessage: String? = null
)

object TrackingFormattingHelper {
    private val speedFormat = DecimalFormat("0")
    private val distanceFormat = DecimalFormat("0.#")

    fun formatSpeed(speedKmh: Double?): String {
        return if (speedKmh != null && speedKmh >= 0.0) {
            "${speedFormat.format(speedKmh)} km/h"
        } else {
            "0 km/h"
        }
    }

    fun formatSpeedNumber(speedKmh: Double?): String {
        return if (speedKmh != null && speedKmh >= 0.0) {
            speedFormat.format(speedKmh)
        } else {
            "0"
        }
    }

    fun formatDistance(distanceKm: Double?): String {
        return if (distanceKm != null && distanceKm >= 0.0) {
            "${distanceFormat.format(distanceKm)} km"
        } else {
            "0 km"
        }
    }

    fun parseGpsStatus(statusStr: String?): GpsStatus {
        return when (statusStr?.uppercase()) {
            "LIVE" -> GpsStatus.LIVE
            "STALE" -> GpsStatus.STALE
            else -> GpsStatus.WAITING_FOR_LOCATION
        }
    }
}
