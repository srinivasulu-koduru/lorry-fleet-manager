package com.lorryowner.app.service

import com.google.android.gms.maps.model.LatLng
import com.lorryowner.app.network.OsrmRoutingService
import com.lorryowner.app.network.OsrmRoutingService.RouteOption
import java.util.ArrayDeque
import kotlin.math.*

/**
 * Intelligent Dynamic Road-Route Matching & Selection Engine for BHP Logistics Driver App.
 *
 * Capabilities:
 * 1. Multi-Candidate Route Comparison: Evaluates multiple alternative road-following routes
 *    fetched from OSRM against live GPS telemetry.
 * 2. Sliding Window Trajectory Analysis: Analyzes successive GPS fixes over time (distance proximity
 *    + heading/bearing alignment) rather than deciding on a single isolated GPS fix.
 * 3. Noise & Accuracy Rejection: Automatically ignores poor accuracy fixes (> 70m) or GPS jumps.
 * 4. Hysteresis & Stability: Requires at least 3 consecutive agreeing reliable fixes before locking
 *    or switching the active route, preventing flickering at intersections or highway bifurcations.
 * 5. Route Deviation Detection: Detects when the vehicle departs from all candidate routes
 *    (> 600m for 3 consecutive fixes) and triggers rate-limited route recalculation (min 30s interval, min 200m moved).
 */
class DriverRouteMatchingEngine(
    val deviationThresholdMeters: Double = 600.0,
    val windowSize: Int = 5,
    val requiredConsecutiveConfirmations: Int = 3
) {

    data class GpsObservation(
        val latitude: Double,
        val longitude: Double,
        val speedKmh: Double? = null,
        val bearing: Float? = null,
        val accuracyMeters: Float? = null,
        val timestampMs: Long = System.currentTimeMillis()
    )

    data class RouteMatchingState(
        val selectedRouteId: String? = null,
        val selectedRoute: RouteOption? = null,
        val candidateScores: Map<String, Double> = emptyMap(),
        val matchConfidence: Float = 0.0f,
        val consecutiveFixCount: Int = 0,
        val isAutoMatched: Boolean = false,
        val isDeviated: Boolean = false,
        val deviationDistanceMeters: Double = 0.0,
        val statusDescription: String = "Observing movement..."
    )

    private val recentObservations = ArrayDeque<GpsObservation>()
    private var currentlySelectedRouteId: String? = null
    private var consecutiveLeaderRouteId: String? = null
    private var consecutiveAgreementCount: Int = 0
    private var consecutiveDeviationCount: Int = 0

    // Rate-limiting state for OSRM recalculation
    private var lastRecalculationTimeMs: Long = 0L
    private var lastRecalculationLat: Double? = null
    private var lastRecalculationLon: Double? = null

    /**
     * Resets internal state (e.g. when trip starts or changes).
     */
    fun reset(initialSelectedRouteId: String? = null) {
        recentObservations.clear()
        currentlySelectedRouteId = initialSelectedRouteId
        consecutiveLeaderRouteId = null
        consecutiveAgreementCount = 0
        consecutiveDeviationCount = 0
        lastRecalculationTimeMs = 0L
        lastRecalculationLat = null
        lastRecalculationLon = null
    }

    /**
     * Manually overrides selected route (e.g. if driver taps a route chip).
     */
    fun manualSelectRoute(routeId: String) {
        currentlySelectedRouteId = routeId
        consecutiveAgreementCount = requiredConsecutiveConfirmations
        consecutiveLeaderRouteId = routeId
    }

    /**
     * Processes a single genuine GPS fix against candidate routes.
     */
    fun processGpsFix(
        observation: GpsObservation,
        candidateRoutes: List<RouteOption>
    ): RouteMatchingState {
        if (candidateRoutes.isEmpty()) {
            return RouteMatchingState(
                selectedRouteId = null,
                selectedRoute = null,
                statusDescription = "No routes available"
            )
        }

        // 1. Accuracy Filter: Reject inaccurate fixes (> 35m)
        if (observation.accuracyMeters != null && observation.accuracyMeters > 35.0f) {
            val currentSelected = candidateRoutes.firstOrNull { it.id == currentlySelectedRouteId }
                ?: candidateRoutes.firstOrNull()
            return RouteMatchingState(
                selectedRouteId = currentSelected?.id,
                selectedRoute = currentSelected,
                isAutoMatched = currentlySelectedRouteId != null,
                isDeviated = consecutiveDeviationCount >= requiredConsecutiveConfirmations,
                statusDescription = "GPS fluctuating (±${observation.accuracyMeters.toInt()}m) — holding route"
            )
        }

        // 2. Add to sliding window
        recentObservations.addLast(observation)
        if (recentObservations.size > windowSize) {
            recentObservations.removeFirst()
        }

        val point = LatLng(observation.latitude, observation.longitude)

        // 3. Score each candidate route across recent sliding window
        val candidateScores = mutableMapOf<String, Double>()
        val currentFixScores = mutableMapOf<String, Double>()
        val distancesToRoutes = mutableMapOf<String, Double>()

        for (route in candidateRoutes) {
            val dist = OsrmRoutingService.calculateMinDistanceToRoute(point, route.points)
            distancesToRoutes[route.id] = dist

            val currentScore = scoreObservationForRoute(observation, route, dist)
            currentFixScores[route.id] = currentScore

            // Average score across window
            var totalScore = 0.0
            for (obs in recentObservations) {
                val obsPt = LatLng(obs.latitude, obs.longitude)
                val obsDist = OsrmRoutingService.calculateMinDistanceToRoute(obsPt, route.points)
                totalScore += scoreObservationForRoute(obs, route, obsDist)
            }
            candidateScores[route.id] = totalScore / recentObservations.size
        }

        // 4. Identify best candidate route for this fix
        val bestRouteForFix = candidateRoutes.maxByOrNull { currentFixScores[it.id] ?: 0.0 }
        val bestRouteId = bestRouteForFix?.id

        if (bestRouteId == consecutiveLeaderRouteId) {
            consecutiveAgreementCount++
        } else {
            consecutiveLeaderRouteId = bestRouteId
            consecutiveAgreementCount = 1
        }

        // 5. Automatic Route Selection & Switching (Hysteresis)
        if (currentlySelectedRouteId == null) {
            // Initial lock: if single route or if candidate reached 2+ confirmations
            if (candidateRoutes.size == 1 || consecutiveAgreementCount >= 2) {
                currentlySelectedRouteId = bestRouteId
            } else {
                // Default to primary highway initially until movement confirms
                currentlySelectedRouteId = candidateRoutes.first().id
            }
        } else if (bestRouteId != null && bestRouteId != currentlySelectedRouteId) {
            // Active route switch: require at least [requiredConsecutiveConfirmations] (3) consecutive fixes
            // AND decisive distance divergence (off current route by > 120m and on new route within < 80m)
            val distCurrent = distancesToRoutes[currentlySelectedRouteId] ?: Double.MAX_VALUE
            val distNew = distancesToRoutes[bestRouteId] ?: Double.MAX_VALUE

            val scoreDiff = (candidateScores[bestRouteId] ?: 0.0) - (candidateScores[currentlySelectedRouteId] ?: 0.0)
            val isDecisivelyOnNewRoute = distCurrent > 120.0 && distNew < 80.0

            if (consecutiveAgreementCount >= requiredConsecutiveConfirmations && (isDecisivelyOnNewRoute || scoreDiff >= 0.25)) {
                currentlySelectedRouteId = bestRouteId
            }
        }

        val selectedRoute = candidateRoutes.firstOrNull { it.id == currentlySelectedRouteId }
            ?: candidateRoutes.first()

        // 6. Route Deviation Detection
        val distanceToSelectedRoute = distancesToRoutes[selectedRoute.id] ?: 0.0
        // Suppress false deviation alerts when vehicle is stationary (speed < 1 km/h)
        val isStationary = observation.speedKmh != null && observation.speedKmh < 1.0
        val isFixDeviated = distanceToSelectedRoute > deviationThresholdMeters && !isStationary

        if (isFixDeviated) {
            consecutiveDeviationCount++
        } else if (!isStationary) {
            consecutiveDeviationCount = 0
        }

        val isConfirmedDeviated = consecutiveDeviationCount >= requiredConsecutiveConfirmations
        val confidence = (candidateScores[selectedRoute.id] ?: 0.5).coerceIn(0.0, 1.0).toFloat()

        val statusDescription = when {
            isConfirmedDeviated -> "⚠️ Route Deviation: ${(distanceToSelectedRoute / 1000.0).roundTo(1)} km off route"
            consecutiveAgreementCount < requiredConsecutiveConfirmations && candidateRoutes.size > 1 ->
                "Observing movement (${consecutiveAgreementCount}/$requiredConsecutiveConfirmations fixes)..."
            candidateRoutes.size > 1 -> "Matched route: ${selectedRoute.name}"
            else -> "On planned route"
        }

        return RouteMatchingState(
            selectedRouteId = selectedRoute.id,
            selectedRoute = selectedRoute,
            candidateScores = candidateScores,
            matchConfidence = confidence,
            consecutiveFixCount = consecutiveAgreementCount,
            isAutoMatched = consecutiveAgreementCount >= requiredConsecutiveConfirmations,
            isDeviated = isConfirmedDeviated,
            deviationDistanceMeters = distanceToSelectedRoute,
            statusDescription = statusDescription
        )
    }

    /**
     * Checks if a route recalculation should be triggered based on rate-limiting.
     * Criteria:
     * - Must be currently deviated
     * - At least 30 seconds since last recalculation
     * - At least 200 meters moved since last recalculation
     */
    fun shouldTriggerRecalculation(
        currentLat: Double,
        currentLon: Double,
        isDeviated: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!isDeviated) return false

        val timeDiff = nowMs - lastRecalculationTimeMs
        if (timeDiff < 30_000L) {
            return false // Throttle: min 30 seconds between recalculation calls
        }

        val lastLat = lastRecalculationLat
        val lastLon = lastRecalculationLon
        if (lastLat != null && lastLon != null) {
            val distMoved = OsrmRoutingService.haversineDistanceMeters(lastLat, lastLon, currentLat, currentLon)
            if (distMoved < 200.0) {
                return false // Throttle: must have moved at least 200m
            }
        }

        // Record recalculation trigger
        lastRecalculationTimeMs = nowMs
        lastRecalculationLat = currentLat
        lastRecalculationLon = currentLon
        return true
    }

    /**
     * Evaluates a single GPS observation against a specific route.
     * Combines distance proximity (65%) and bearing alignment (35%).
     */
    private fun scoreObservationForRoute(
        obs: GpsObservation,
        route: RouteOption,
        distanceMeters: Double
    ): Double {
        // Distance score: 1.0 at 0-50m, decays to 0.0 at 500m
        val sDist = (1.0 - (distanceMeters / 500.0)).coerceIn(0.0, 1.0)

        // Bearing alignment score if vehicle is moving
        val isMoving = (obs.speedKmh != null && obs.speedKmh >= 5.0) || obs.bearing != null
        if (isMoving && obs.bearing != null && route.points.size >= 2) {
            val roadBearing = OsrmRoutingService.findClosestSegmentBearing(
                LatLng(obs.latitude, obs.longitude),
                route.points
            )

            var diff = abs(obs.bearing - roadBearing) % 360f
            if (diff > 180f) diff = 360f - diff

            // If heading within 90 degrees, alignment score is cos(diff), else 0
            val sBearing = if (diff <= 90f) {
                cos(Math.toRadians(diff.toDouble())).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            return 0.65 * sDist + 0.35 * sBearing
        }

        return sDist
    }

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}
