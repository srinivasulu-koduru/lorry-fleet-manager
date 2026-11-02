package com.lorryowner.app.service

import android.location.Location
import kotlin.math.*

/**
 * Result representing how an incoming GPS telemetry fix should be processed.
 */
sealed class GpsFilterResult {
    /**
     * Fix was discarded because accuracy is worse than 35m or coordinates are invalid.
     */
    data class DiscardUnreliable(
        val accuracyMeters: Float,
        val reason: String
    ) : GpsFilterResult()

    /**
     * Fix was detected as stationary GPS jitter (speed < 1 km/h and displacement < 15m).
     * Position fluctuations are suppressed; coordinates remain anchored at [anchoredLat], [anchoredLon].
     * @param shouldEmitHeartbeat If true, a rate-limited heartbeat should be transmitted to the server
     *                            using the anchored coordinates so the vehicle does not appear stale.
     * @param anchoredLat Stable anchored latitude.
     * @param anchoredLon Stable anchored longitude.
     * @param displacementFromAnchor Distance in meters from the stationary anchor.
     * @param speedKmh Speed in km/h.
     */
    data class SuppressStationaryJitter(
        val anchoredLat: Double,
        val anchoredLon: Double,
        val shouldEmitHeartbeat: Boolean,
        val displacementFromAnchor: Double,
        val speedKmh: Float
    ) : GpsFilterResult()

    /**
     * Fix represents genuine vehicle movement.
     * Should be recorded in the travelled path and transmitted to the backend.
     * @param latitude Validated latitude.
     * @param longitude Validated longitude.
     * @param displacement Distance in meters from previous transmitted position.
     * @param speedKmh Speed in km/h.
     * @param isFirstFix True if this is the initial GPS fix establishing the trip origin.
     */
    data class AcceptGenuineMovement(
        val latitude: Double,
        val longitude: Double,
        val displacement: Double,
        val speedKmh: Float,
        val isFirstFix: Boolean = false
    ) : GpsFilterResult()
}

/**
 * Intelligent filter for lorry GPS tracking:
 * 1. Accuracy gate: Rejects unreliable fixes worse than 35m.
 * 2. Stationary jitter suppression: When vehicle speed < 1 km/h and movement < 15m,
 *    suppresses coordinate wandering/zigzagging while anchoring to the stable stop location.
 * 3. Multi-factor state machine: Evaluates speed, accuracy, displacement and successive
 *    reliable fixes together to ensure genuine departure is never suppressed and stationary
 *    GPS drift is never mistaken for movement or false route deviation.
 * 4. Heartbeat preservation: While stationary, emits a periodic heartbeat (~25s) using
 *    the anchored coordinates so the backend/owner tracking remains alive.
 */
class StationaryGpsFilter(
    val maxAccuracyMeters: Float = 35.0f,
    val stationarySpeedThresholdKmh: Float = 1.0f,
    val stationaryDisplacementThresholdMeters: Double = 15.0,
    val minMovingIntervalMs: Long = 5_000L,
    val stationaryHeartbeatIntervalMs: Long = 25_000L,
    val consecutiveFixesToConfirmMovement: Int = 2,
    val consecutiveFixesToConfirmStop: Int = 2
) {

    var isStationary: Boolean = true
        private set

    var anchorLat: Double? = null
        private set
    var anchorLon: Double? = null
        private set

    var lastTransmittedLat: Double? = null
        private set
    var lastTransmittedLon: Double? = null
        private set
    var lastTransmittedTimeMs: Long = 0L
        private set

    var consecutiveMovingCount: Int = 0
        private set
    var consecutiveStationaryCount: Int = 0
        private set

    fun reset() {
        isStationary = true
        anchorLat = null
        anchorLon = null
        lastTransmittedLat = null
        lastTransmittedLon = null
        lastTransmittedTimeMs = 0L
        consecutiveMovingCount = 0
        consecutiveStationaryCount = 0
    }

    /**
     * Filters an incoming Android [Location] fix.
     */
    fun filter(location: Location, currentTimeMs: Long = System.currentTimeMillis()): GpsFilterResult {
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6f).coerceAtLeast(0f) else 0f
        val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
        return filter(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = accuracy,
            speedKmh = speedKmh,
            timestampMs = currentTimeMs
        )
    }

    /**
     * Core mathematical filtering logic decoupled from Android platform specifics,
     * allowing comprehensive deterministic JVM unit testing.
     */
    fun filter(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        speedKmh: Float,
        timestampMs: Long
    ): GpsFilterResult {
        // 1. Accuracy Check: Ignore unreliable GPS fixes with accuracy worse than 35 metres
        if (latitude == 0.0 && longitude == 0.0) {
            return GpsFilterResult.DiscardUnreliable(accuracyMeters, "Invalid (0, 0) coordinates")
        }
        if (accuracyMeters > maxAccuracyMeters) {
            return GpsFilterResult.DiscardUnreliable(
                accuracyMeters,
                "Accuracy (${accuracyMeters}m) worse than required threshold (${maxAccuracyMeters}m)"
            )
        }

        // 2. Initial Anchor Setup on First Reliable Fix
        val currentAnchorLat = anchorLat
        val currentAnchorLon = anchorLon
        if (currentAnchorLat == null || currentAnchorLon == null || lastTransmittedLat == null || lastTransmittedLon == null) {
            anchorLat = latitude
            anchorLon = longitude
            lastTransmittedLat = latitude
            lastTransmittedLon = longitude
            lastTransmittedTimeMs = timestampMs
            isStationary = speedKmh < stationarySpeedThresholdKmh
            consecutiveMovingCount = 0
            consecutiveStationaryCount = 0
            return GpsFilterResult.AcceptGenuineMovement(
                latitude = latitude,
                longitude = longitude,
                displacement = 0.0,
                speedKmh = speedKmh,
                isFirstFix = true
            )
        }

        // 3. Compute Displacement using pure Haversine distance
        val dispFromAnchor = haversineDistanceMeters(currentAnchorLat, currentAnchorLon, latitude, longitude)
        val dispFromLast = haversineDistanceMeters(lastTransmittedLat!!, lastTransmittedLon!!, latitude, longitude)
        val timeDiff = (timestampMs - lastTransmittedTimeMs).coerceAtLeast(0L)

        val isSpeedStationary = speedKmh < stationarySpeedThresholdKmh
        val isDisplacementStationary = dispFromAnchor < stationaryDisplacementThresholdMeters

        if (isStationary) {
            // === Vehicle is currently in STATIONARY state ===

            // Decisive movement check: vehicle accelerated to >= 3 km/h AND moved >= 15m,
            // or moved >= 25m with speed >= 1 km/h
            val isDecisiveMovement = (speedKmh >= 3.0f && dispFromAnchor >= stationaryDisplacementThresholdMeters) ||
                    (dispFromAnchor >= 25.0 && speedKmh >= stationarySpeedThresholdKmh)

            // Candidate movement check: either speed or displacement exceeded threshold
            val isCandidateMovement = (!isSpeedStationary) || (!isDisplacementStationary)

            if (isDecisiveMovement) {
                // Immediate transition to MOVING
                isStationary = false
                consecutiveMovingCount = 0
                anchorLat = latitude
                anchorLon = longitude
                lastTransmittedLat = latitude
                lastTransmittedLon = longitude
                lastTransmittedTimeMs = timestampMs
                return GpsFilterResult.AcceptGenuineMovement(
                    latitude = latitude,
                    longitude = longitude,
                    displacement = dispFromLast,
                    speedKmh = speedKmh
                )
            } else if (isCandidateMovement) {
                consecutiveMovingCount++
                if (consecutiveMovingCount >= consecutiveFixesToConfirmMovement) {
                    // Confirmed transition to MOVING across successive reliable fixes
                    isStationary = false
                    consecutiveMovingCount = 0
                    anchorLat = latitude
                    anchorLon = longitude
                    lastTransmittedLat = latitude
                    lastTransmittedLon = longitude
                    lastTransmittedTimeMs = timestampMs
                    return GpsFilterResult.AcceptGenuineMovement(
                        latitude = latitude,
                        longitude = longitude,
                        displacement = dispFromLast,
                        speedKmh = speedKmh
                    )
                } else {
                    // Single borderline fix: hold stationary anchor pending confirmation by next fix
                    val heartbeat = timeDiff >= stationaryHeartbeatIntervalMs
                    if (heartbeat) {
                        lastTransmittedTimeMs = timestampMs
                    }
                    return GpsFilterResult.SuppressStationaryJitter(
                        anchoredLat = currentAnchorLat,
                        anchoredLon = currentAnchorLon,
                        shouldEmitHeartbeat = heartbeat,
                        displacementFromAnchor = dispFromAnchor,
                        speedKmh = speedKmh
                    )
                }
            } else {
                // Clear stationary drift: speed < 1 km/h and displacement < 15m
                consecutiveMovingCount = 0
                val heartbeat = timeDiff >= stationaryHeartbeatIntervalMs
                if (heartbeat) {
                    lastTransmittedTimeMs = timestampMs
                }
                return GpsFilterResult.SuppressStationaryJitter(
                    anchoredLat = currentAnchorLat,
                    anchoredLon = currentAnchorLon,
                    shouldEmitHeartbeat = heartbeat,
                    displacementFromAnchor = dispFromAnchor,
                    speedKmh = 0.0f
                )
            }
        } else {
            // === Vehicle is currently in MOVING state ===

            if (isSpeedStationary && isDisplacementStationary) {
                consecutiveStationaryCount++
                if (consecutiveStationaryCount >= consecutiveFixesToConfirmStop) {
                    // Confirmed transition to STATIONARY
                    isStationary = true
                    consecutiveStationaryCount = 0
                    anchorLat = latitude
                    anchorLon = longitude
                    lastTransmittedLat = latitude
                    lastTransmittedLon = longitude
                    lastTransmittedTimeMs = timestampMs
                    return GpsFilterResult.SuppressStationaryJitter(
                        anchoredLat = latitude,
                        anchoredLon = longitude,
                        shouldEmitHeartbeat = true, // transmit stop fix with speed 0
                        displacementFromAnchor = 0.0,
                        speedKmh = 0.0f
                    )
                } else {
                    // First slow fix while moving: could be brief pause or traffic crawl
                    if (timeDiff >= minMovingIntervalMs || dispFromLast >= 10.0) {
                        lastTransmittedLat = latitude
                        lastTransmittedLon = longitude
                        lastTransmittedTimeMs = timestampMs
                        return GpsFilterResult.AcceptGenuineMovement(
                            latitude = latitude,
                            longitude = longitude,
                            displacement = dispFromLast,
                            speedKmh = speedKmh
                        )
                    } else {
                        return GpsFilterResult.SuppressStationaryJitter(
                            anchoredLat = lastTransmittedLat ?: latitude,
                            anchoredLon = lastTransmittedLon ?: longitude,
                            shouldEmitHeartbeat = false,
                            displacementFromAnchor = dispFromAnchor,
                            speedKmh = speedKmh
                        )
                    }
                }
            } else {
                // Continuing genuine vehicle movement
                consecutiveStationaryCount = 0
                if (timeDiff >= minMovingIntervalMs || dispFromLast >= stationaryDisplacementThresholdMeters) {
                    anchorLat = latitude
                    anchorLon = longitude
                    lastTransmittedLat = latitude
                    lastTransmittedLon = longitude
                    lastTransmittedTimeMs = timestampMs
                    return GpsFilterResult.AcceptGenuineMovement(
                        latitude = latitude,
                        longitude = longitude,
                        displacement = dispFromLast,
                        speedKmh = speedKmh
                    )
                } else {
                    // Too frequent (< minMovingIntervalMs and < 15m), throttle without losing track
                    return GpsFilterResult.SuppressStationaryJitter(
                        anchoredLat = latitude,
                        anchoredLon = longitude,
                        shouldEmitHeartbeat = false,
                        displacementFromAnchor = dispFromLast,
                        speedKmh = speedKmh
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Pure mathematical Haversine distance in meters.
         */
        fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2.0) * sin(dLon / 2.0)
            val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
            return r * c
        }
    }
}
