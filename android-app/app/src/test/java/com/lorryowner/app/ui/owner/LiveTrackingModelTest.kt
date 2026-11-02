package com.lorryowner.app.ui.owner

import com.google.android.gms.maps.model.LatLng
import com.lorryowner.app.network.OsrmRoutingService
import org.junit.Assert.*
import org.junit.Test

class LiveTrackingModelTest {

    @Test
    fun testSpeedFormatting() {
        assertEquals("58 km/h", TrackingFormattingHelper.formatSpeed(58.0))
        assertEquals("58", TrackingFormattingHelper.formatSpeedNumber(58.0))
        assertEquals("0 km/h", TrackingFormattingHelper.formatSpeed(null))
        assertEquals("0", TrackingFormattingHelper.formatSpeedNumber(null))
        assertEquals("65 km/h", TrackingFormattingHelper.formatSpeed(65.4))
    }

    @Test
    fun testDistanceFormatting() {
        assertEquals("145 km", TrackingFormattingHelper.formatDistance(145.0))
        assertEquals("285.5 km", TrackingFormattingHelper.formatDistance(285.5))
        assertEquals("0 km", TrackingFormattingHelper.formatDistance(null))
        assertEquals("0 km", TrackingFormattingHelper.formatDistance(-10.0))
    }

    @Test
    fun testGpsStatusParsing() {
        assertEquals(GpsStatus.LIVE, TrackingFormattingHelper.parseGpsStatus("LIVE"))
        assertEquals(GpsStatus.LIVE, TrackingFormattingHelper.parseGpsStatus("live"))
        assertEquals(GpsStatus.STALE, TrackingFormattingHelper.parseGpsStatus("STALE"))
        assertEquals(GpsStatus.STALE, TrackingFormattingHelper.parseGpsStatus("stale"))
        assertEquals(GpsStatus.WAITING_FOR_LOCATION, TrackingFormattingHelper.parseGpsStatus("WAITING_FOR_LOCATION"))
        assertEquals(GpsStatus.WAITING_FOR_LOCATION, TrackingFormattingHelper.parseGpsStatus(null))
        assertEquals(GpsStatus.WAITING_FOR_LOCATION, TrackingFormattingHelper.parseGpsStatus("UNKNOWN"))
    }

    @Test
    fun testLiveTrackingViewModelMapModeToggle() {
        val vm = LiveTrackingViewModel(null, lorryId = 1L, autoLoad = false)
        assertTrue("Default map mode should be SATELLITE", vm.uiState.isSatelliteMode)

        vm.toggleMapMode()
        assertFalse("Toggled map mode should be NORMAL", vm.uiState.isSatelliteMode)

        vm.toggleMapMode()
        assertTrue("Toggled back should be SATELLITE", vm.uiState.isSatelliteMode)
    }

    @Test
    fun testRunningLorriesViewModelTestData() {
        val vm = RunningLorriesViewModel(null, autoLoad = false)
        val testLorries = listOf(
            LiveTrackingLorryUiState(
                lorryId = 1L,
                registrationNumber = "AP 31 TC 1234",
                makeModel = "Ashok Leyland 2820 HG",
                tripId = 10L,
                tripReference = "TRIP-2026-1003",
                driverName = "Ramesh Kumar",
                originLocation = "Hyderabad Logistics Park, TS",
                destinationLocation = "Nellore Distribution Center, AP",
                currentLocationDescription = "Near Kurnool (NH 44)",
                latitude = 15.8281,
                longitude = 78.0373,
                currentSpeedKmh = 58.0,
                distanceTravelledKm = 145.0,
                remainingDistanceKm = 285.0,
                estimatedDrivingTime = "4h 55m",
                gpsStatus = GpsStatus.LIVE,
                isLive = true,
                hasGpsFix = true
            )
        )

        vm.setTestData(testLorries)
        assertEquals(1, vm.uiState.lorries.size)
        assertEquals("AP 31 TC 1234", vm.uiState.lorries[0].registrationNumber)
        assertEquals("Ramesh Kumar", vm.uiState.lorries[0].driverName)
        assertEquals(GpsStatus.LIVE, vm.uiState.lorries[0].gpsStatus)
        assertTrue(vm.uiState.lorries[0].isLive)
        assertTrue(vm.uiState.lorries[0].hasGpsFix)
        assertEquals("58 km/h", TrackingFormattingHelper.formatSpeed(vm.uiState.lorries[0].currentSpeedKmh))
    }

    @Test
    fun testLiveTrackingDetailsDataIntegrity() {
        val vm = LiveTrackingViewModel(null, lorryId = 1L, tripId = 10L, autoLoad = false)
        val data = LiveTrackingLorryUiState(
            lorryId = 1L,
            registrationNumber = "AP 31 TC 1234",
            makeModel = "Ashok Leyland 2820 HG",
            tripId = 10L,
            tripReference = "TRIP-2026-1003",
            driverName = "Ravi Kumar",
            originLocation = "Hyderabad Logistics Park, TS",
            destinationLocation = "Nellore Distribution Center, AP",
            currentLocationDescription = "Near Kurnool (NH 44)",
            latitude = 15.8281,
            longitude = 78.0373,
            currentSpeedKmh = 58.0,
            distanceTravelledKm = 145.0,
            remainingDistanceKm = 285.0,
            estimatedDrivingTime = "4h 55m",
            estimatedArrival = "04:30 AM",
            gpsStatus = GpsStatus.LIVE,
            isLive = true,
            hasGpsFix = true,
            breadcrumbs = listOf(
                BreadcrumbPoint(17.3850, 78.4867, 50.0, "2026-10-28T18:00:00Z"),
                BreadcrumbPoint(15.8281, 78.0373, 58.0, "2026-10-28T21:30:00Z")
            )
        )

        vm.setTestData(data)
        assertNotNull(vm.uiState.trackingData)
        val result = vm.uiState.trackingData!!
        assertEquals("AP 31 TC 1234", result.registrationNumber)
        assertEquals("Near Kurnool (NH 44)", result.currentLocationDescription)
        assertEquals(2, result.breadcrumbs.size)
        assertEquals(15.8281, result.latitude!!, 0.0001)
        assertEquals(78.0373, result.longitude!!, 0.0001)
        assertEquals(58.0, result.currentSpeedKmh!!, 0.0001)
        assertEquals(145.0, result.distanceTravelledKm, 0.01)
        assertEquals(285.0, result.remainingDistanceKm, 0.01)
        assertEquals("4h 55m", result.estimatedDrivingTime)
        assertEquals("04:30 AM", result.estimatedArrival)
    }

    @Test
    fun testHaversineDistanceCalculation() {
        // Hyderabad to Bengaluru approximate straight line distance (~500 km)
        val distMeters = OsrmRoutingService.haversineDistanceMeters(
            17.3850, 78.4867,
            12.9716, 77.5946
        )
        val distKm = distMeters / 1000.0
        assertTrue("Distance should be around 500km, got: $distKm", distKm in 480.0..520.0)
    }

    @Test
    fun testRouteDeviationDetection_onRoute_notDeviated() {
        val plannedRoute = listOf(
            LatLng(17.3850, 78.4867),
            LatLng(16.7488, 77.9942),
            LatLng(15.8281, 78.0373),
            LatLng(12.9716, 77.5946)
        )

        // Point directly along the segment between 17.3850, 78.4867 and 16.7488, 77.9942
        val onRoutePoint = LatLng(17.0669, 78.2404)
        val distanceMeters = OsrmRoutingService.calculateMinDistanceToRoute(onRoutePoint, plannedRoute)

        assertTrue("Distance to route should be minimal (< 200m), got: $distanceMeters", distanceMeters < 200.0)
        assertFalse("Should not be flagged as deviated", distanceMeters > OsrmRoutingService.DEFAULT_DEVIATION_THRESHOLD_METERS)
    }

    @Test
    fun testRouteDeviationDetection_offRoute_isDeviated() {
        val plannedRoute = listOf(
            LatLng(17.3850, 78.4867),
            LatLng(16.7488, 77.9942),
            LatLng(15.8281, 78.0373),
            LatLng(12.9716, 77.5946)
        )

        // Point far off to the east (e.g. Vijayawada ~250km away)
        val farOffPoint = LatLng(16.5062, 80.6480)
        val distanceMeters = OsrmRoutingService.calculateMinDistanceToRoute(farOffPoint, plannedRoute)

        assertTrue("Distance should be > 50km, got: $distanceMeters", distanceMeters > 50000.0)
        assertTrue("Should be flagged as deviated", distanceMeters > OsrmRoutingService.DEFAULT_DEVIATION_THRESHOLD_METERS)
    }

    @Test
    fun testBuildCleanTravelledSegments_stationaryJitter_eliminated() {
        // Stationary indoor jitter points (identical to Trip #12 in Anantapur compound)
        val stationaryJitterPoints = listOf(
            OsrmRoutingService.PathPoint(14.7457146, 77.6896903, null),
            OsrmRoutingService.PathPoint(14.7456567, 77.6897235, 4.33),
            OsrmRoutingService.PathPoint(14.7456061, 77.6896708, null),
            OsrmRoutingService.PathPoint(14.7456826, 77.6897041, null),
            OsrmRoutingService.PathPoint(14.7456566, 77.6897127, null),
            OsrmRoutingService.PathPoint(14.7456393, 77.6896961, null),
            OsrmRoutingService.PathPoint(14.7455942, 77.6897667, null),
            OsrmRoutingService.PathPoint(14.7457495, 77.6896596, null),
            OsrmRoutingService.PathPoint(14.7456108, 77.6897447, null),
            OsrmRoutingService.PathPoint(14.7457908, 77.6896602, null),
            OsrmRoutingService.PathPoint(14.7456374, 77.6897225, null),
            OsrmRoutingService.PathPoint(14.7456666, 77.6897055, null),
            OsrmRoutingService.PathPoint(14.7455589, 77.6897166, null),
            OsrmRoutingService.PathPoint(14.7457125, 77.6897003, null),
            OsrmRoutingService.PathPoint(14.7457124, 77.6896503, 3.07),
            OsrmRoutingService.PathPoint(14.7456254, 77.6896718, 0.32),
            OsrmRoutingService.PathPoint(14.7456489, 77.6897286, null),
            OsrmRoutingService.PathPoint(14.7456973, 77.6896665, null),
            OsrmRoutingService.PathPoint(14.7455581, 77.6896620, null),
            OsrmRoutingService.PathPoint(14.7457415, 77.6895522, null),
            OsrmRoutingService.PathPoint(14.7456532, 77.6896739, null),
            OsrmRoutingService.PathPoint(14.7456323, 77.6897162, null),
            OsrmRoutingService.PathPoint(14.7456446, 77.6897182, null),
            OsrmRoutingService.PathPoint(14.7456243, 77.6897148, null),
            OsrmRoutingService.PathPoint(14.7456872, 77.6896747, null),
            OsrmRoutingService.PathPoint(14.7455921, 77.6897750, null),
            OsrmRoutingService.PathPoint(14.7456029, 77.6897373, null),
            OsrmRoutingService.PathPoint(14.7456167, 77.6897572, 0.41)
        )

        val cleanSegments = OsrmRoutingService.buildCleanTravelledSegments(stationaryJitterPoints)
        // All points are within 17.7m of each other: should produce 0 segments (no green zigzags)
        assertTrue("Stationary jitter should produce 0 segments, got: ${cleanSegments.size}", cleanSegments.isEmpty())
    }

    @Test
    fun testBuildCleanTravelledSegments_genuineHighwayDriving_retained() {
        // Genuine highway driving along NH 44 (points ~60-70m apart, moving south)
        val highwayPoints = listOf(
            OsrmRoutingService.PathPoint(14.7455, 77.6898, 45.0),
            OsrmRoutingService.PathPoint(14.7449, 77.6898, 48.0),
            OsrmRoutingService.PathPoint(14.7443, 77.6898, 52.0),
            OsrmRoutingService.PathPoint(14.7437, 77.6898, 50.0),
            OsrmRoutingService.PathPoint(14.7431, 77.6898, 55.0)
        )

        val cleanSegments = OsrmRoutingService.buildCleanTravelledSegments(highwayPoints)
        assertEquals("Should retain 1 continuous highway driving segment", 1, cleanSegments.size)
        assertEquals("Should contain all 5 driving points", 5, cleanSegments[0].size)
    }
}
