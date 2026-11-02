package com.lorryowner.app.service

import com.google.android.gms.maps.model.LatLng
import com.lorryowner.app.network.CityCoordinateResolver
import com.lorryowner.app.network.OsrmRoutingService.RouteOption
import com.lorryowner.app.service.DriverRouteMatchingEngine.GpsObservation
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DriverRouteMatchingTest {

    private lateinit var engine: DriverRouteMatchingEngine

    // Mock candidate Route A: Heading North along longitude 78.0 (lat 15.0 -> 15.1 -> 15.2)
    private val routeA = RouteOption(
        id = "route_north",
        name = "Route A (North Highway)",
        points = listOf(
            LatLng(15.0000, 78.0000),
            LatLng(15.0500, 78.0000),
            LatLng(15.1000, 78.0000),
            LatLng(15.1500, 78.0000),
            LatLng(15.2000, 78.0000)
        ),
        distanceMeters = 22200.0,
        durationSeconds = 1500.0
    )

    // Mock candidate Route B: Heading East along latitude 15.0 (lon 78.0 -> 78.1 -> 78.2)
    private val routeB = RouteOption(
        id = "route_east",
        name = "Route B (East Highway)",
        points = listOf(
            LatLng(15.0000, 78.0000),
            LatLng(15.0000, 78.0500),
            LatLng(15.0000, 78.1000),
            LatLng(15.0000, 78.1500),
            LatLng(15.0000, 78.2000)
        ),
        distanceMeters = 21500.0,
        durationSeconds = 1450.0
    )

    private val candidateRoutes = listOf(routeA, routeB)

    @Before
    fun setUp() {
        engine = DriverRouteMatchingEngine(
            deviationThresholdMeters = 600.0,
            windowSize = 5,
            requiredConsecutiveConfirmations = 3
        )
    }

    @Test
    fun testInitialRouteDefaultsToPrimary() {
        val fix = GpsObservation(
            latitude = 15.0000,
            longitude = 78.0000,
            accuracyMeters = 10f,
            speedKmh = 0.0
        )
        val state = engine.processGpsFix(fix, candidateRoutes)
        assertEquals("route_north", state.selectedRouteId)
        assertFalse("Should not be marked deviated at origin", state.isDeviated)
    }

    @Test
    fun testDynamicSelectionMatchesRouteBAfterThreeConsecutiveObservations() {
        // Vehicle turns onto East corridor (Route B) heading 90 degrees (East)
        val fix1 = GpsObservation(
            latitude = 15.0000,
            longitude = 78.0500, // On Route B, 5.3km from Route A
            bearing = 90f,
            speedKmh = 50.0,
            accuracyMeters = 15f
        )
        val state1 = engine.processGpsFix(fix1, candidateRoutes)
        assertEquals(1, state1.consecutiveFixCount)
        // With only 1 fix, hysteresis keeps the previous route to avoid premature flip
        assertEquals("route_north", state1.selectedRouteId)

        val fix2 = GpsObservation(
            latitude = 15.0000,
            longitude = 78.0700,
            bearing = 90f,
            speedKmh = 55.0,
            accuracyMeters = 12f
        )
        val state2 = engine.processGpsFix(fix2, candidateRoutes)
        assertEquals(2, state2.consecutiveFixCount)

        val fix3 = GpsObservation(
            latitude = 15.0000,
            longitude = 78.1000,
            bearing = 90f,
            speedKmh = 60.0,
            accuracyMeters = 10f
        )
        val state3 = engine.processGpsFix(fix3, candidateRoutes)
        assertEquals(3, state3.consecutiveFixCount)
        assertTrue("Route should be confirmed matched after 3 fixes", state3.isAutoMatched)
        assertEquals("Dynamic selection must lock onto Route B", "route_east", state3.selectedRouteId)
        assertFalse("Vehicle is strictly on Route B", state3.isDeviated)
    }

    @Test
    fun testNoisyGpsFixIsRejectedAndDoesNotFlipRoute() {
        // Establish lock on Route A
        repeat(3) { i ->
            engine.processGpsFix(
                GpsObservation(
                    latitude = 15.0100 + i * 0.01,
                    longitude = 78.0000,
                    bearing = 0f,
                    speedKmh = 40.0,
                    accuracyMeters = 15f
                ),
                candidateRoutes
            )
        }

        // An inaccurate GPS fix arrives (> 70m) near Route B
        val noisyFix = GpsObservation(
            latitude = 15.0000,
            longitude = 78.1000,
            bearing = 90f,
            speedKmh = 50.0,
            accuracyMeters = 120f // Poor accuracy
        )
        val state = engine.processGpsFix(noisyFix, candidateRoutes)

        // Route MUST remain Route A
        assertEquals("Route must remain Route A despite noisy fix", "route_north", state.selectedRouteId)
        assertTrue(state.statusDescription.contains("fluctuating"))
    }

    @Test
    fun testHysteresisPreventsSingleOutlierFromSwitchingRoute() {
        // Confirm Route A initially
        repeat(3) { i ->
            engine.processGpsFix(
                GpsObservation(
                    latitude = 15.0200 + i * 0.01,
                    longitude = 78.0000,
                    bearing = 0f,
                    speedKmh = 45.0,
                    accuracyMeters = 15f
                ),
                candidateRoutes
            )
        }

        // Single GPS fix close to Route B
        val outlierFix = GpsObservation(
            latitude = 15.0000,
            longitude = 78.0800,
            bearing = 90f,
            speedKmh = 40.0,
            accuracyMeters = 15f
        )
        val stateAfterOutlier = engine.processGpsFix(outlierFix, candidateRoutes)
        assertEquals("Single outlier must NOT switch locked route", "route_north", stateAfterOutlier.selectedRouteId)

        // Next fix back on Route A
        val backOnA = GpsObservation(
            latitude = 15.0600,
            longitude = 78.0000,
            bearing = 0f,
            speedKmh = 50.0,
            accuracyMeters = 10f
        )
        val finalState = engine.processGpsFix(backOnA, candidateRoutes)
        assertEquals("route_north", finalState.selectedRouteId)
    }

    @Test
    fun testRouteDeviationDetectionAndThrottle() {
        // Vehicle drifts far away from both routes (> 1500m)
        val dev1 = GpsObservation(15.0500, 78.0300, 30.0, 45f, 15f)
        val dev2 = GpsObservation(15.0600, 78.0350, 30.0, 45f, 15f)
        val dev3 = GpsObservation(15.0700, 78.0400, 30.0, 45f, 15f)

        engine.processGpsFix(dev1, candidateRoutes)
        engine.processGpsFix(dev2, candidateRoutes)
        val state3 = engine.processGpsFix(dev3, candidateRoutes)

        assertTrue("Deviation must be confirmed after 3 consecutive fixes > 600m off route", state3.isDeviated)
        assertTrue("Deviation distance must be > 600m", state3.deviationDistanceMeters > 600.0)

        // First recalculation check should succeed
        val trigger1 = engine.shouldTriggerRecalculation(15.0700, 78.0400, state3.isDeviated, nowMs = 100_000L)
        assertTrue("First recalculation must be allowed", trigger1)

        // Second recalculation 5 seconds later should be throttled (rate-limited < 30s)
        val trigger2 = engine.shouldTriggerRecalculation(15.0750, 78.0450, state3.isDeviated, nowMs = 105_000L)
        assertFalse("Recalculation within 5 seconds must be throttled", trigger2)

        // Recalculation 35 seconds later with > 200m movement should succeed
        val trigger3 = engine.shouldTriggerRecalculation(15.0900, 78.0600, state3.isDeviated, nowMs = 140_000L)
        assertTrue("Recalculation after 35s and movement must be permitted", trigger3)
    }

    @Test
    fun testCityCoordinateResolverResolvesKeyHubs() {
        val tadipatri = CityCoordinateResolver.resolveCityCoordinates("Tadipatri")
        assertEquals(14.9123, tadipatri.latitude, 0.001)
        assertEquals(78.0125, tadipatri.longitude, 0.001)

        val hyderabad = CityCoordinateResolver.resolveCityCoordinates("Hyderabad, Telangana")
        assertEquals(17.3850, hyderabad.latitude, 0.001)
        assertEquals(78.4867, hyderabad.longitude, 0.001)

        val bengaluru = CityCoordinateResolver.resolveCityCoordinates("Bengaluru, Karnataka")
        assertEquals(12.9716, bengaluru.latitude, 0.001)
        assertEquals(77.5946, bengaluru.longitude, 0.001)

        val kurnool = CityCoordinateResolver.resolveCityCoordinates("Kurnool (NH 44)")
        assertEquals(15.8281, kurnool.latitude, 0.001)
        assertEquals(78.0373, kurnool.longitude, 0.001)
    }
}
