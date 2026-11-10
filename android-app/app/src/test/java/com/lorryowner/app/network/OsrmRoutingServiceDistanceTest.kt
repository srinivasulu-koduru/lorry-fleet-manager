package com.lorryowner.app.network

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmRoutingServiceDistanceTest {

    @Test
    fun testEmptyRouteReturnsZero() {
        val dist = OsrmRoutingService.calculateRemainingRoadDistanceKm(
            LatLng(17.3850, 78.4867),
            emptyList()
        )
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun testDestinationReachedThresholdReturnsZero() {
        // Destination point
        val dest = LatLng(17.3850, 78.4867)
        // Vehicle is ~20 meters away
        val nearDest = LatLng(17.3851, 78.4867)
        val route = listOf(
            LatLng(17.3500, 78.4500),
            dest
        )
        val dist = OsrmRoutingService.calculateRemainingRoadDistanceKm(nearDest, route)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun testRemainingDistanceExcludesAlreadyTravelledSegments() {
        // Straight-ish route along latitude (approx 111 km per degree of latitude)
        // P0 (15.0000, 78.0000) -> P1 (15.1000, 78.0000) [~11.1 km]
        // P1 (15.1000, 78.0000) -> P2 (15.2000, 78.0000) [~11.1 km]
        // P2 (15.2000, 78.0000) -> P3 (15.3000, 78.0000) [~11.1 km]
        // Total route length is ~33.3 km
        val p0 = LatLng(15.0000, 78.0000)
        val p1 = LatLng(15.1000, 78.0000)
        val p2 = LatLng(15.2000, 78.0000)
        val p3 = LatLng(15.3000, 78.0000)
        val route = listOf(p0, p1, p2, p3)

        // Vehicle is at P2 (has completed segments P0->P1 and P1->P2)
        // Remaining should only be P2->P3 (~11.1 km)
        val distAtP2 = OsrmRoutingService.calculateRemainingRoadDistanceKm(p2, route)
        // Should be approximately 11.1 km, definitely NOT 33.3 km
        assertTrue("Remaining at P2 should be ~11.1 km, was $distAtP2", distAtP2 in 10.5..11.8)

        // Vehicle halfway between P2 and P3 (15.2500, 78.0000)
        // Remaining should only be ~5.6 km
        val halfway = LatLng(15.2500, 78.0000)
        val distHalfway = OsrmRoutingService.calculateRemainingRoadDistanceKm(halfway, route)
        assertTrue("Remaining halfway should be ~5.6 km, was $distHalfway", distHalfway in 5.0..6.0)
    }

    @Test
    fun testSliceRemainingRouteStartsAtCurrentPosition() {
        val p0 = LatLng(15.0000, 78.0000)
        val p1 = LatLng(15.1000, 78.0000)
        val p2 = LatLng(15.2000, 78.0000)
        val p3 = LatLng(15.3000, 78.0000)
        val route = listOf(p0, p1, p2, p3)

        // Vehicle is at 15.1500, 78.0000 (between P1 and P2)
        val current = LatLng(15.1500, 78.0000)
        val sliced = OsrmRoutingService.sliceRemainingRoute(current, route)

        assertTrue("Sliced route must contain at least 2 points", sliced.size >= 2)
        assertEquals("First point must be current vehicle location", current.latitude, sliced.first().latitude, 0.0001)
        assertEquals("First point must be current vehicle location", current.longitude, sliced.first().longitude, 0.0001)
        assertEquals("Last point must be original destination", p3.latitude, sliced.last().latitude, 0.0001)
        assertEquals("Last point must be original destination", p3.longitude, sliced.last().longitude, 0.0001)
    }
}
