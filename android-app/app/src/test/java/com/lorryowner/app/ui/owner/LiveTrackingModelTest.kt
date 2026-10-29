package com.lorryowner.app.ui.owner

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
}
