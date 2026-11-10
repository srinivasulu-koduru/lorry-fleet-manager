package com.lorryowner.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GpsSpeedCalculatorTest {

    private lateinit var calculator: GpsSpeedCalculator

    @Before
    fun setUp() {
        calculator = GpsSpeedCalculator(
            maxRealisticSpeedKmh = 120.0f,
            stationaryThresholdKmh = 1.5f,
            emaAlpha = 0.40f,
            maxSignalGapMs = 25_000L
        )
    }

    @Test
    fun testSensorSpeedPreferredWhenAccurate() {
        val t0 = 100_000L
        // 15.0 m/s = 54.0 km/h, accuracy = 0.5 m/s (very reliable)
        val speed = calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 15.0f,
            speedAccuracyMps = 0.5f,
            currentTimeMs = t0
        )
        // First moving fix picks up directly without prior EMA
        assertEquals(54.0f, speed, 0.01f)
    }

    @Test
    fun testInaccurateSensorSpeedFallsBackToDisplacement() {
        val t0 = 100_000L
        // Fix 1: Sensor speed has very poor accuracy (speed = 2.0 m/s, accuracy = 4.0 m/s > 3.0 and > 50%)
        // Fallback provided: displacement 100m over 5s = 20 m/s = 72 km/h
        val speed = calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 2.0f,
            speedAccuracyMps = 4.0f,
            displacementMeters = 100.0f,
            dtSec = 5.0,
            currentTimeMs = t0
        )
        assertEquals(72.0f, speed, 0.01f)
    }

    @Test
    fun testFallbackSpeedCalculatedWhenSensorSpeedMissing() {
        val t0 = 100_000L
        // No sensor speed, moved 150m in 10s = 15 m/s = 54 km/h
        val speed = calculator.calculateSpeedKmh(
            hasSensorSpeed = false,
            sensorSpeedMps = 0.0f,
            displacementMeters = 150.0f,
            dtSec = 10.0,
            currentTimeMs = t0
        )
        assertEquals(54.0f, speed, 0.01f)
    }

    @Test
    fun testStationaryJitterClampedToZero() {
        val t0 = 100_000L
        // 0.3 m/s = 1.08 km/h (< 1.5 km/h threshold)
        val speed = calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 0.3f,
            speedAccuracyMps = 0.2f,
            currentTimeMs = t0
        )
        assertEquals(0.0f, speed, 0.001f)
    }

    @Test
    fun testSpeedSpikeOver120KmhRejected() {
        val t0 = 100_000L
        // Fix 1: Cruising at 60 km/h (16.6667 m/s)
        val initialSpeed = calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 16.6667f,
            currentTimeMs = t0
        )
        assertEquals(60.0f, initialSpeed, 0.1f)

        // Fix 2: Erroneous GPS glitch reporting 40 m/s = 144 km/h (> 120 km/h)
        val spikeSpeed = calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 40.0f,
            currentTimeMs = t0 + 2000L
        )
        // Outlier rejected; previous speed preserved
        assertEquals(initialSpeed, spikeSpeed, 0.01f)
    }

    @Test
    fun testEmaSmoothingBehavior() {
        val t0 = 100_000L
        // Initial pickup at 40 km/h (11.111 m/s)
        val s1 = calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 11.1111f,
            currentTimeMs = t0
        )
        assertEquals(40.0f, s1, 0.05f)

        // Next fix at 50 km/h (13.8889 m/s)
        // EMA: 0.40 * 50.0 + 0.60 * 40.0 = 20.0 + 24.0 = 44.0 km/h
        val s2 = calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 13.8889f,
            currentTimeMs = t0 + 2000L
        )
        assertEquals(44.0f, s2, 0.1f)
    }

    @Test
    fun testStationaryAnchorImmediatelyDropsSpeedToZero() {
        val t0 = 100_000L
        // Moving at 50 km/h
        calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 13.8889f,
            currentTimeMs = t0
        )

        // Stationary anchor triggered
        val stoppedSpeed = calculator.calculateSpeedKmh(
            hasSensorSpeed = false,
            sensorSpeedMps = 0.0f,
            currentTimeMs = t0 + 5000L,
            isStationaryAnchor = true
        )
        assertEquals(0.0f, stoppedSpeed, 0.001f)
        assertEquals(0.0f, calculator.getCurrentSpeedKmh(), 0.001f)
    }

    @Test
    fun testSignalGapOver25sResetsSpeed() {
        val t0 = 100_000L
        // Moving at 60 km/h
        calculator.calculateSpeedKmh(
            hasSensorSpeed = true,
            sensorSpeedMps = 16.6667f,
            currentTimeMs = t0
        )

        // 30 seconds signal loss (tunnel / poor signal)
        // Next fix is stationary / 0 km/h
        val afterTunnel = calculator.calculateSpeedKmh(
            hasSensorSpeed = false,
            sensorSpeedMps = 0.0f,
            currentTimeMs = t0 + 30_000L
        )
        assertEquals(0.0f, afterTunnel, 0.001f)
    }
}
