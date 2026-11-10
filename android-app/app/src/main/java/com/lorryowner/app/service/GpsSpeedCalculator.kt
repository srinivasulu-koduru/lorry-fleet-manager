package com.lorryowner.app.service

import android.location.Location
import android.os.Build

/**
 * High-accuracy vehicle speed evaluator and smoother for commercial lorry fleet tracking.
 *
 * Requirements:
 * 1. Prefer Android Location.speed when the location reading is valid and sufficiently accurate.
 * 2. Use speedAccuracyMetersPerSecond where supported (Android 8.0+) to evaluate speed reliability.
 * 3. Fallback to distance / dt calculation when sensor speed is missing or unreliable.
 * 4. Reject impossible location jumps and unrealistic speed spikes (> 120 km/h).
 * 5. Reject stationary GPS jitter (< 1.5 km/h) by clamping to 0.0 km/h.
 * 6. Apply sensible Exponential Moving Average (EMA) smoothing for responsive, non-flickering display.
 * 7. Reset to 0 after extended GPS signal gaps (> 25s).
 * 8. Return speed strictly in km/h.
 */
class GpsSpeedCalculator(
    private val maxRealisticSpeedKmh: Float = 120.0f,
    private val stationaryThresholdKmh: Float = 1.5f,
    private val emaAlpha: Float = 0.40f,
    private val maxSignalGapMs: Long = 25_000L
) {
    private var lastValidLocation: Location? = null
    private var lastValidTimeMs: Long = 0L
    private var currentSmoothedSpeedKmh: Float = 0.0f

    /**
     * Evaluates speed in km/h for an incoming Android Location fix.
     */
    fun calculateSpeedKmh(
        location: Location,
        currentTimeMs: Long = System.currentTimeMillis(),
        isStationaryAnchor: Boolean = false
    ): Float {
        if (isStationaryAnchor) {
            currentSmoothedSpeedKmh = 0.0f
            lastValidLocation = location
            lastValidTimeMs = currentTimeMs
            return 0.0f
        }

        var speedAccMps: Float? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
            speedAccMps = location.speedAccuracyMetersPerSecond
        }

        val lastLoc = lastValidLocation
        val (dispMeters, dtSec) = if (lastLoc != null && lastValidTimeMs > 0) {
            val dt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val nanosDiff = location.elapsedRealtimeNanos - lastLoc.elapsedRealtimeNanos
                if (nanosDiff > 0) nanosDiff / 1_000_000_000.0 else (currentTimeMs - lastValidTimeMs) / 1000.0
            } else {
                (currentTimeMs - lastValidTimeMs) / 1000.0
            }
            val dist = location.distanceTo(lastLoc)
            Pair(dist, dt)
        } else {
            Pair(null, null)
        }

        val speed = calculateSpeedKmh(
            hasSensorSpeed = location.hasSpeed(),
            sensorSpeedMps = location.speed,
            speedAccuracyMps = speedAccMps,
            displacementMeters = dispMeters,
            dtSec = dtSec,
            currentTimeMs = currentTimeMs,
            isStationaryAnchor = isStationaryAnchor
        )

        lastValidLocation = location
        return speed
    }

    /**
     * Pure evaluator for speed in km/h. Suitable for direct testing and modular computation.
     */
    fun calculateSpeedKmh(
        hasSensorSpeed: Boolean,
        sensorSpeedMps: Float,
        speedAccuracyMps: Float? = null,
        displacementMeters: Float? = null,
        dtSec: Double? = null,
        currentTimeMs: Long = System.currentTimeMillis(),
        isStationaryAnchor: Boolean = false
    ): Float {
        if (isStationaryAnchor) {
            currentSmoothedSpeedKmh = 0.0f
            lastValidTimeMs = currentTimeMs
            return 0.0f
        }

        // Stale signal gap check: if no fix for > 25 seconds, reset speed to 0
        if (lastValidTimeMs > 0 && (currentTimeMs - lastValidTimeMs) > maxSignalGapMs) {
            currentSmoothedSpeedKmh = 0.0f
        }

        var candidateSpeedKmh: Float? = null

        // 1. Sensor speed evaluation: Use Location.speed if available and reliable
        if (hasSensorSpeed && sensorSpeedMps >= 0.0f) {
            var isSpeedAccurate = true
            if (speedAccuracyMps != null) {
                // If speed accuracy error is worse than 3.0 m/s and exceeds 50% of current speed, consider unreliable
                if (speedAccuracyMps > 3.0f && speedAccuracyMps > (sensorSpeedMps * 0.5f)) {
                    isSpeedAccurate = false
                }
            }

            if (isSpeedAccurate) {
                candidateSpeedKmh = sensorSpeedMps * 3.6f
            }
        }

        // 2. Fallback speed evaluation: Compute distance / elapsed time between successive reliable GPS fixes
        if (candidateSpeedKmh == null && displacementMeters != null && dtSec != null) {
            if (dtSec in 1.0..30.0) {
                val calculatedMps = displacementMeters / dtSec
                candidateSpeedKmh = (calculatedMps * 3.6).toFloat()
            }
        }

        val rawSpeed = candidateSpeedKmh ?: 0.0f

        // 3. Stationary threshold clamping & outlier rejection
        val clampedSpeed = if (rawSpeed < stationaryThresholdKmh) {
            0.0f
        } else if (rawSpeed > maxRealisticSpeedKmh) {
            // Reject unrealistic speed spike; retain previous smoothed speed
            currentSmoothedSpeedKmh
        } else {
            rawSpeed
        }

        // 4. Exponential Moving Average (EMA) smoothing
        currentSmoothedSpeedKmh = if (clampedSpeed == 0.0f) {
            0.0f // Immediate drop to 0 on vehicle stop
        } else if (currentSmoothedSpeedKmh == 0.0f) {
            clampedSpeed // Immediate pickup on vehicle departure
        } else {
            emaAlpha * clampedSpeed + (1.0f - emaAlpha) * currentSmoothedSpeedKmh
        }

        lastValidTimeMs = currentTimeMs
        return currentSmoothedSpeedKmh
    }

    fun getCurrentSpeedKmh(): Float = currentSmoothedSpeedKmh

    fun reset() {
        lastValidLocation = null
        lastValidTimeMs = 0L
        currentSmoothedSpeedKmh = 0.0f
    }
}
