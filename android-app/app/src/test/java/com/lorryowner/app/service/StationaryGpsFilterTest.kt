package com.lorryowner.app.service

import com.lorryowner.app.service.DriverRouteMatchingEngine.GpsObservation
import com.lorryowner.app.network.OsrmRoutingService.RouteOption
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StationaryGpsFilterTest {

    private lateinit var filter: StationaryGpsFilter

    @Before
    fun setUp() {
        filter = StationaryGpsFilter(
            maxAccuracyMeters = 35.0f,
            stationarySpeedThresholdKmh = 1.0f,
            stationaryDisplacementThresholdMeters = 15.0,
            minMovingIntervalMs = 5_000L,
            stationaryHeartbeatIntervalMs = 25_000L,
            consecutiveFixesToConfirmMovement = 2,
            consecutiveFixesToConfirmStop = 2
        )
    }

    @Test
    fun testPoorAccuracyFixesDiscarded() {
        val t0 = 100_000L

        // Fix with poor accuracy (40m > 35m)
        val result1 = filter.filter(
            latitude = 15.0000,
            longitude = 78.0000,
            accuracyMeters = 40.0f,
            speedKmh = 0.0f,
            timestampMs = t0
        )
        assertTrue("Accuracy > 35m must be discarded", result1 is GpsFilterResult.DiscardUnreliable)

        // Fix with severe inaccuracy (100m)
        val result2 = filter.filter(
            latitude = 15.0000,
            longitude = 78.0000,
            accuracyMeters = 100.0f,
            speedKmh = 20.0f,
            timestampMs = t0 + 1000
        )
        assertTrue("Severe inaccuracy must be discarded", result2 is GpsFilterResult.DiscardUnreliable)

        // Valid accuracy (12m <= 35m)
        val result3 = filter.filter(
            latitude = 15.0000,
            longitude = 78.0000,
            accuracyMeters = 12.0f,
            speedKmh = 0.0f,
            timestampMs = t0 + 2000
        )
        assertTrue("Valid accuracy fix must be accepted", result3 is GpsFilterResult.AcceptGenuineMovement)
    }

    @Test
    fun testStationaryGpsJitterSuppression() {
        val t0 = 100_000L

        // First reliable fix: parked at depot (15.00000, 78.00000)
        val fix1 = filter.filter(
            latitude = 15.00000,
            longitude = 78.00000,
            accuracyMeters = 10.0f,
            speedKmh = 0.0f,
            timestampMs = t0
        )
        assertTrue(fix1 is GpsFilterResult.AcceptGenuineMovement)
        assertTrue((fix1 as GpsFilterResult.AcceptGenuineMovement).isFirstFix)
        assertTrue("Initial state must be stationary", filter.isStationary)

        // 6 seconds later: GPS noise drifts 6.2 meters North, speed 0.2 km/h
        val fix2 = filter.filter(
            latitude = 15.000055, // ~6.1m north
            longitude = 78.00000,
            accuracyMeters = 14.0f,
            speedKmh = 0.2f,
            timestampMs = t0 + 6_000L
        )
        assertTrue("Small stationary drift must be suppressed", fix2 is GpsFilterResult.SuppressStationaryJitter)
        val jitter2 = fix2 as GpsFilterResult.SuppressStationaryJitter
        assertEquals("Must remain locked to initial anchor latitude", 15.00000, jitter2.anchoredLat, 0.000001)
        assertEquals("Must remain locked to initial anchor longitude", 78.00000, jitter2.anchoredLon, 0.000001)
        assertFalse("Heartbeat should not be emitted before 25s", jitter2.shouldEmitHeartbeat)

        // 12 seconds later: GPS noise drifts 8.5 meters South-East, speed 0.0 km/h
        val fix3 = filter.filter(
            latitude = 14.99994,
            longitude = 78.00006,
            accuracyMeters = 15.0f,
            speedKmh = 0.0f,
            timestampMs = t0 + 12_000L
        )
        assertTrue("Drift must be suppressed", fix3 is GpsFilterResult.SuppressStationaryJitter)
        val jitter3 = fix3 as GpsFilterResult.SuppressStationaryJitter
        assertEquals(15.00000, jitter3.anchoredLat, 0.000001)
        assertEquals(78.00000, jitter3.anchoredLon, 0.000001)
        assertFalse(jitter3.shouldEmitHeartbeat)
    }

    @Test
    fun testStationaryHeartbeatPreservedAfterInterval() {
        val t0 = 100_000L

        // Initial anchor fix
        filter.filter(15.0000, 78.0000, 10.0f, 0.0f, t0)

        // Jitter fix after 26 seconds (exceeding 25s heartbeat threshold)
        val heartbeatFix = filter.filter(
            latitude = 15.00005,
            longitude = 78.00005,
            accuracyMeters = 12.0f,
            speedKmh = 0.1f,
            timestampMs = t0 + 26_000L
        )
        assertTrue(heartbeatFix is GpsFilterResult.SuppressStationaryJitter)
        val result = heartbeatFix as GpsFilterResult.SuppressStationaryJitter
        assertTrue("Heartbeat must be emitted when elapsed time >= 25s", result.shouldEmitHeartbeat)
        assertEquals("Coordinates must remain locked to anchor", 15.0000, result.anchoredLat, 0.000001)
        assertEquals(0.0f, result.speedKmh, 0.001f)
    }

    @Test
    fun testGenuineVehicleMovementAccepted() {
        val t0 = 100_000L

        // Initial anchor at depot
        filter.filter(15.0000, 78.0000, 10.0f, 0.0f, t0)

        // Vehicle leaves depot onto main road: moves 55 meters, speed 25 km/h
        val moveFix1 = filter.filter(
            latitude = 15.00050, // ~55m north
            longitude = 78.00000,
            accuracyMeters = 8.0f,
            speedKmh = 25.0f,
            timestampMs = t0 + 6_000L
        )
        assertTrue("Genuine departure must be accepted", moveFix1 is GpsFilterResult.AcceptGenuineMovement)
        val move1 = moveFix1 as GpsFilterResult.AcceptGenuineMovement
        assertEquals(15.00050, move1.latitude, 0.000001)
        assertEquals(25.0f, move1.speedKmh, 0.1f)
        assertFalse("Filter must transition out of stationary state", filter.isStationary)

        // Next 6-second update: moves another 65 meters, speed 40 km/h
        val moveFix2 = filter.filter(
            latitude = 15.00110, // ~66m further north
            longitude = 78.00000,
            accuracyMeters = 7.0f,
            speedKmh = 40.0f,
            timestampMs = t0 + 12_000L
        )
        assertTrue("Continuing movement must be accepted", moveFix2 is GpsFilterResult.AcceptGenuineMovement)
        val move2 = moveFix2 as GpsFilterResult.AcceptGenuineMovement
        assertEquals(15.00110, move2.latitude, 0.000001)
        assertEquals(40.0f, move2.speedKmh, 0.1f)
    }

    @Test
    fun testStationaryToMovingTransitionWithBorderlineFix() {
        val t0 = 100_000L

        // Initial anchor at depot
        filter.filter(15.0000, 78.0000, 10.0f, 0.0f, t0)

        // Borderline fix: speed 1.2 km/h, displacement 16m (could be slow start or single multipath jump)
        val fix1 = filter.filter(
            latitude = 15.00015,
            longitude = 78.00000,
            accuracyMeters = 12.0f,
            speedKmh = 1.2f,
            timestampMs = t0 + 6_000L
        )
        // First borderline fix: holds stationary pending confirmation
        assertTrue("Single borderline fix is held pending confirmation", fix1 is GpsFilterResult.SuppressStationaryJitter)
        assertTrue(filter.isStationary)

        // Second successive fix confirms movement: speed 15.0 km/h, displacement 40m
        val fix2 = filter.filter(
            latitude = 15.00040,
            longitude = 78.00000,
            accuracyMeters = 9.0f,
            speedKmh = 15.0f,
            timestampMs = t0 + 12_000L
        )
        assertTrue("Second fix confirms genuine movement", fix2 is GpsFilterResult.AcceptGenuineMovement)
        assertFalse("Vehicle transitioned to moving", filter.isStationary)
    }

    @Test
    fun testMovingToStationaryTransition() {
        val t0 = 100_000L

        // Start driving
        filter.filter(15.0000, 78.0000, 10.0f, 35.0f, t0)
        assertFalse(filter.isStationary)

        // Continue driving
        filter.filter(15.0010, 78.0000, 10.0f, 40.0f, t0 + 6_000L)
        assertFalse(filter.isStationary)

        // Vehicle stops at toll gate (Fix 1: speed 0.0 km/h, 2m moved)
        val stopCandidate1 = filter.filter(
            latitude = 15.00102,
            longitude = 78.00000,
            accuracyMeters = 10.0f,
            speedKmh = 0.0f,
            timestampMs = t0 + 12_000L
        )
        // First stop fix while moving: accepted as latest position before confirming full stop
        assertTrue(stopCandidate1 is GpsFilterResult.AcceptGenuineMovement)

        // Fix 2: still stationary (speed 0.0 km/h, 1m moved)
        val stopCandidate2 = filter.filter(
            latitude = 15.00103,
            longitude = 78.00000,
            accuracyMeters = 10.0f,
            speedKmh = 0.0f,
            timestampMs = t0 + 18_000L
        )
        // Second consecutive stationary fix confirms transition to STATIONARY
        assertTrue("Confirmed stop transitions to stationary", stopCandidate2 is GpsFilterResult.SuppressStationaryJitter)
        assertTrue("Filter state must now be stationary", filter.isStationary)
        assertEquals("Anchor is set at the stopped position", 15.00103, filter.anchorLat!!, 0.000001)
    }

    @Test
    fun testFalseRouteDeviationAvoidedWhenStationary() {
        val engine = DriverRouteMatchingEngine(deviationThresholdMeters = 500.0)

        val highwayRoute = RouteOption(
            id = "highway_nh44",
            name = "NH 44 Highway",
            points = listOf(
                LatLng(15.1000, 78.0000),
                LatLng(15.2000, 78.0000),
                LatLng(15.3000, 78.0000)
            ),
            distanceMeters = 22000.0,
            durationSeconds = 1200.0
        )
        val routes = listOf(highwayRoute)

        // Lorry is stationary at loading dock 9 km off the highway (speed 0.0 km/h)
        repeat(5) { i ->
            val obs = GpsObservation(
                latitude = 15.0000 + (i * 0.00005), // Stationary drift
                longitude = 78.0000,
                speedKmh = 0.0,
                accuracyMeters = 12f
            )
            val state = engine.processGpsFix(obs, routes)
            assertFalse("Stationary vehicle at depot must NOT trigger route deviation alert", state.isDeviated)
        }
    }

    @Test
    fun testBatteryOptimizationStateHandling() {
        // 1. Battery optimized (restricted) and driver has NOT declined prompt -> MUST prompt
        assertTrue(
            "Must prompt when app is battery optimized and prompt was not previously declined",
            com.lorryowner.app.ui.driver.DriverPermissionUtils.shouldPromptBatteryOptimization(
                isBatteryOptimized = true,
                hasDeclinedPrompt = false
            )
        )

        // 2. Battery optimized, but driver already declined -> DO NOT repeatedly prompt
        assertFalse(
            "Must not repeatedly prompt when driver previously declined",
            com.lorryowner.app.ui.driver.DriverPermissionUtils.shouldPromptBatteryOptimization(
                isBatteryOptimized = true,
                hasDeclinedPrompt = true
            )
        )

        // 3. App is already unrestricted (not battery optimized) -> DO NOT prompt
        assertFalse(
            "Must not prompt when app is already unrestricted",
            com.lorryowner.app.ui.driver.DriverPermissionUtils.shouldPromptBatteryOptimization(
                isBatteryOptimized = false,
                hasDeclinedPrompt = false
            )
        )

        // 4. App is unrestricted and previously declined -> DO NOT prompt
        assertFalse(
            "Must not prompt when already unrestricted",
            com.lorryowner.app.ui.driver.DriverPermissionUtils.shouldPromptBatteryOptimization(
                isBatteryOptimized = false,
                hasDeclinedPrompt = true
            )
        )
    }
}
