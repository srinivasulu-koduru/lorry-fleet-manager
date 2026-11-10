package com.lorryowner.app.network

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * High-performance, zero-cost road-following routing engine powered by OSRM (Open Source Routing Machine).
 *
 * Capabilities:
 * 1. Road-Following Route Generation: Fetches genuine highway and street-following coordinates
 *    between origin and destination using the OSRM driving profile.
 * 2. In-Memory Trip Caching: Caches calculated road polylines by tripId and coordinate hash,
 *    guaranteeing that 6-second live GPS polling does NOT flood or re-query the routing server.
 * 3. Deviation Detection: Evaluates cross-track distance between real-time lorry coordinates and the
 *    planned road geometry to detect route non-compliance.
 * 4. Fault Tolerance: Gracefully handles timeouts and dead zones without disrupting map rendering.
 */
object OsrmRoutingService {

    private const val TAG = "OsrmRoutingService"
    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 8000

    // Configurable deviation threshold in meters (default 800m to allow for highway dhabas/fuel stations)
    const val DEFAULT_DEVIATION_THRESHOLD_METERS = 800.0

    // In-memory route cache keyed by tripId
    private val tripRouteCache = ConcurrentHashMap<Long, RoadRouteResult>()
    private val tripAlternativeRoutesCache = ConcurrentHashMap<Long, List<RouteOption>>()

    data class RoadRouteResult(
        val points: List<LatLng>,
        val distanceMeters: Double,
        val durationSeconds: Double
    )

    data class PathPoint(
        val latitude: Double,
        val longitude: Double,
        val speedKmh: Double? = null
    )

    data class RouteOption(
        val id: String,
        val name: String,
        val points: List<LatLng>,
        val distanceMeters: Double,
        val durationSeconds: Double,
        val summary: String = ""
    )

    /**
     * Retrieves alternative road-following routes between origin and destination.
     * Uses memory cache if available for [tripId].
     */
    fun getOrFetchAlternativeRoutes(
        tripId: Long,
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        forceRefresh: Boolean = false
    ): Result<List<RouteOption>> {
        if (!forceRefresh && tripId > 0 && tripAlternativeRoutesCache.containsKey(tripId)) {
            val cached = tripAlternativeRoutesCache[tripId]
            if (!cached.isNullOrEmpty()) {
                return Result.success(cached)
            }
        }

        val networkResult = fetchAlternativeRoutesFromOsrm(originLat, originLon, destLat, destLon)
        if (networkResult.isSuccess) {
            val list = networkResult.getOrThrow()
            if (tripId > 0 && list.isNotEmpty()) {
                tripAlternativeRoutesCache[tripId] = list
            }
        }
        return networkResult
    }

    /**
     * Retrieves the planned road-following route between origin and destination.
     * Uses memory cache if available for [tripId].
     */
    fun getOrFetchRoute(
        tripId: Long,
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double
    ): Result<RoadRouteResult> {
        // 1. Check cache first
        if (tripId > 0 && tripRouteCache.containsKey(tripId)) {
            val cached = tripRouteCache[tripId]
            if (cached != null && cached.points.isNotEmpty()) {
                return Result.success(cached)
            }
        }

        // 2. Fetch from OSRM driving engine
        val networkResult = fetchRouteFromOsrm(originLat, originLon, destLat, destLon)
        if (networkResult.isSuccess) {
            val result = networkResult.getOrThrow()
            if (tripId > 0 && result.points.isNotEmpty()) {
                tripRouteCache[tripId] = result
            }
        }
        return networkResult
    }

    /**
     * Executes direct HTTP GET to OSRM driving route endpoint.
     */
    fun fetchRouteFromOsrm(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double
    ): Result<RoadRouteResult> {
        val urlString = "https://router.project-osrm.org/route/v1/driving/$originLon,$originLat;$destLon,$destLat?overview=full&geometries=geojson"
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "BHPLogisticsFleetManager/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val code = json.optString("code")
                if (code.equals("Ok", ignoreCase = true)) {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val distance = firstRoute.optDouble("distance", 0.0)
                        val duration = firstRoute.optDouble("duration", 0.0)

                        val geometry = firstRoute.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val points = ArrayList<LatLng>(coordinates.length())

                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            points.add(LatLng(lat, lon))
                        }

                        Log.i(TAG, "Successfully fetched OSRM road route: ${points.size} waypoints, ${(distance / 1000).roundToInt()} km")
                        Result.success(RoadRouteResult(points, distance, duration))
                    } else {
                        Result.failure(Exception("OSRM returned no routes for coordinates"))
                    }
                } else {
                    Result.failure(Exception("OSRM routing returned code: $code"))
                }
            } else {
                Result.failure(Exception("OSRM HTTP error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM routing query failed: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Calculates the minimum perpendicular distance (in meters) from a current GPS fix
     * to the planned road polyline using pure mathematical projection.
     */
    fun calculateMinDistanceToRoute(
        point: LatLng,
        route: List<LatLng>
    ): Double {
        if (route.isEmpty()) return 0.0
        if (route.size == 1) {
            return haversineDistanceMeters(point.latitude, point.longitude, route[0].latitude, route[0].longitude)
        }

        var minDistance = Double.MAX_VALUE

        // Iterate through segments
        for (i in 0 until route.size - 1) {
            val p1 = route[i]
            val p2 = route[i + 1]
            val dist = distanceToSegmentMeters(point, p1, p2)
            if (dist < minDistance) {
                minDistance = dist
            }
        }

        return minDistance
    }

    /**
     * Computes distance from a point to a line segment [a, b] in meters.
     */
    private fun distanceToSegmentMeters(p: LatLng, a: LatLng, b: LatLng): Double {
        val midLat = Math.toRadians((a.latitude + b.latitude) / 2.0)
        val cosMidLat = cos(midLat)

        // Equirectangular local projection
        val dx = Math.toRadians(b.longitude - a.longitude) * cosMidLat
        val dy = Math.toRadians(b.latitude - a.latitude)
        val lenSq = dx * dx + dy * dy

        if (lenSq < 1e-12) {
            return haversineDistanceMeters(p.latitude, p.longitude, a.latitude, a.longitude)
        }

        val px = Math.toRadians(p.longitude - a.longitude) * cosMidLat
        val py = Math.toRadians(p.latitude - a.latitude)
        val t = max(0.0, min(1.0, (px * dx + py * dy) / lenSq))

        val projLat = a.latitude + t * (b.latitude - a.latitude)
        val projLon = a.longitude + t * (b.longitude - a.longitude)

        return haversineDistanceMeters(p.latitude, p.longitude, projLat, projLon)
    }

    /**
     * Projects point [p] perpendicularly onto line segment [a, b].
     * Clamps projection strictly within segment bounds [0.0, 1.0].
     */
    fun projectPointOnSegment(p: LatLng, a: LatLng, b: LatLng): LatLng {
        val midLat = Math.toRadians((a.latitude + b.latitude) / 2.0)
        val cosMidLat = cos(midLat)
        val dx = Math.toRadians(b.longitude - a.longitude) * cosMidLat
        val dy = Math.toRadians(b.latitude - a.latitude)
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-12) return a

        val px = Math.toRadians(p.longitude - a.longitude) * cosMidLat
        val py = Math.toRadians(p.latitude - a.latitude)
        val t = max(0.0, min(1.0, (px * dx + py * dy) / lenSq))

        return LatLng(
            a.latitude + t * (b.latitude - a.latitude),
            a.longitude + t * (b.longitude - a.longitude)
        )
    }

    /**
     * Pure mathematical Haversine distance in meters.
     * Safe for both JVM unit tests and Android runtime without Location dependency.
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

    /**
     * Calculates the exact remaining road distance in kilometres from the current vehicle location
     * to the destination along the planned OSRM road geometry.
     * Snaps the current position to the nearest road segment, sums remaining road polyline lengths,
     * and excludes already-travelled segments.
     *
     * Returns kilometres with 1 decimal precision (e.g. 142.3 km).
     */
    fun calculateRemainingRoadDistanceKm(
        currentLocation: LatLng,
        fullRoute: List<LatLng>
    ): Double {
        if (fullRoute.isEmpty()) return 0.0
        if (fullRoute.size == 1) {
            val dist = haversineDistanceMeters(
                currentLocation.latitude, currentLocation.longitude,
                fullRoute[0].latitude, fullRoute[0].longitude
            )
            return Math.round((dist / 1000.0) * 10.0) / 10.0
        }

        var minDistance = Double.MAX_VALUE
        var closestSegmentIndex = 0

        for (i in 0 until fullRoute.size - 1) {
            val p1 = fullRoute[i]
            val p2 = fullRoute[i + 1]
            val dist = distanceToSegmentMeters(currentLocation, p1, p2)
            if (dist < minDistance) {
                minDistance = dist
                closestSegmentIndex = i
            }
        }

        val snapped = projectPointOnSegment(
            currentLocation,
            fullRoute[closestSegmentIndex],
            fullRoute[closestSegmentIndex + 1]
        )

        // 1. Distance from snapped point to the next waypoint on the route
        var remMeters = haversineDistanceMeters(
            snapped.latitude, snapped.longitude,
            fullRoute[closestSegmentIndex + 1].latitude, fullRoute[closestSegmentIndex + 1].longitude
        )

        // 2. Perpendicular deviation offset from current vehicle location to snapped road point
        if (minDistance > 10.0) {
            remMeters += minDistance
        }

        // 3. Sum all remaining road segments to the destination
        for (i in (closestSegmentIndex + 1) until (fullRoute.size - 1)) {
            remMeters += haversineDistanceMeters(
                fullRoute[i].latitude, fullRoute[i].longitude,
                fullRoute[i + 1].latitude, fullRoute[i + 1].longitude
            )
        }

        // Destination reached threshold (< 50m)
        val dest = fullRoute.last()
        val distToDest = haversineDistanceMeters(
            currentLocation.latitude, currentLocation.longitude,
            dest.latitude, dest.longitude
        )
        if (distToDest < 50.0 || remMeters < 50.0) {
            return 0.0
        }

        val remKm = remMeters / 1000.0
        return Math.max(0.1, Math.round(remKm * 10.0) / 10.0)
    }

    /**
     * Slices the planned road route to only include the remaining path from the current
     * position forward to the destination. Snaps current position to the road and removes
     * all route segments already passed.
     *
     * This ensures the BLUE line only shows the remaining route, while the GREEN line
     * shows the actual travelled breadcrumbs, keeping the two visually distinct.
     */
    fun sliceRemainingRoute(
        currentLocation: LatLng,
        fullRoute: List<LatLng>
    ): List<LatLng> {
        if (fullRoute.size <= 1) return fullRoute

        var minDistance = Double.MAX_VALUE
        var closestSegmentIndex = 0

        // Find which road segment the vehicle is currently nearest to
        for (i in 0 until fullRoute.size - 1) {
            val p1 = fullRoute[i]
            val p2 = fullRoute[i + 1]
            val dist = distanceToSegmentMeters(currentLocation, p1, p2)
            if (dist < minDistance) {
                minDistance = dist
                closestSegmentIndex = i
            }
        }

        val snapped = projectPointOnSegment(
            currentLocation,
            fullRoute[closestSegmentIndex],
            fullRoute[closestSegmentIndex + 1]
        )

        val forwardSublist = if (closestSegmentIndex + 1 < fullRoute.size) {
            fullRoute.subList(closestSegmentIndex + 1, fullRoute.size)
        } else {
            emptyList()
        }

        return listOf(currentLocation, snapped) + forwardSublist
    }

    /**
     * Clears cache for a given trip (e.g. when route completed).
     */
    fun clearCache(tripId: Long? = null) {
        if (tripId != null) {
            tripRouteCache.remove(tripId)
            tripAlternativeRoutesCache.remove(tripId)
        } else {
            tripRouteCache.clear()
            tripAlternativeRoutesCache.clear()
        }
    }

    /**
     * Fetches multiple alternative road-following routes between origin and destination.
     * Uses OSRM driving profile with alternatives parameter.
     */
    fun fetchAlternativeRoutesFromOsrm(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        maxAlternatives: Int = 3
    ): Result<List<RouteOption>> {
        val urlString = "https://router.project-osrm.org/route/v1/driving/$originLon,$originLat;$destLon,$destLat?overview=full&geometries=geojson&alternatives=3"
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "BHPLogisticsFleetManager/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val code = json.optString("code")
                if (code.equals("Ok", ignoreCase = true)) {
                    val routes = json.getJSONArray("routes")
                    val resultRoutes = mutableListOf<RouteOption>()

                    for (rIdx in 0 until routes.length()) {
                        val routeObj = routes.getJSONObject(rIdx)
                        val distance = routeObj.optDouble("distance", 0.0)
                        val duration = routeObj.optDouble("duration", 0.0)
                        val legs = routeObj.optJSONArray("legs")
                        val summary = if (legs != null && legs.length() > 0) {
                            legs.getJSONObject(0).optString("summary", "")
                        } else ""

                        val geometry = routeObj.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val points = ArrayList<LatLng>(coordinates.length())

                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            points.add(LatLng(lat, lon))
                        }

                        val routeId = if (rIdx == 0) "route_primary" else "route_alt_$rIdx"
                        val routeName = if (rIdx == 0) {
                            if (summary.isNotBlank()) "Primary: $summary" else "Primary Highway"
                        } else {
                            if (summary.isNotBlank()) "Alternative: $summary" else "Alternative Route $rIdx"
                        }

                        resultRoutes.add(
                            RouteOption(
                                id = routeId,
                                name = routeName,
                                points = points,
                                distanceMeters = distance,
                                durationSeconds = duration,
                                summary = summary
                            )
                        )
                    }

                    // If OSRM returned only 1 route, check if we can query alternative road corridors
                    // using OSRM via-points for distant points (> 30km)
                    if (resultRoutes.size == 1) {
                        val straightDist = haversineDistanceMeters(originLat, originLon, destLat, destLon)
                        if (straightDist > 30_000.0) {
                            val altCorridors = fetchAlternativeCorridors(originLat, originLon, destLat, destLon)
                            for (corridor in altCorridors) {
                                if (resultRoutes.size >= maxAlternatives) break
                                val distDiff = abs(corridor.distanceMeters - resultRoutes[0].distanceMeters)
                                if (distDiff > 1000.0) {
                                    val idx = resultRoutes.size
                                    resultRoutes.add(
                                        corridor.copy(
                                            id = "route_alt_$idx",
                                            name = "Alternative Route $idx"
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Log.i(TAG, "Fetched ${resultRoutes.size} alternative road routes from OSRM")
                    Result.success(resultRoutes)
                } else {
                    Result.failure(Exception("OSRM routing returned code: $code"))
                }
            } else {
                Result.failure(Exception("OSRM HTTP error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM alternative routes query failed: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchAlternativeCorridors(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double
    ): List<RouteOption> {
        val corridors = mutableListOf<RouteOption>()
        val midLat = (originLat + destLat) / 2.0
        val midLon = (originLon + destLon) / 2.0
        val dLat = destLat - originLat
        val dLon = destLon - originLon

        val offsets = listOf(-0.35, 0.35)
        for (factor in offsets) {
            try {
                val pLat = -dLon * factor
                val pLon = dLat * factor
                val viaLat = round((midLat + pLat) * 10000) / 10000.0
                val viaLon = round((midLon + pLon) * 10000) / 10000.0

                val urlString = "https://router.project-osrm.org/route/v1/driving/$originLon,$originLat;$viaLon,$viaLat;$destLon,$destLat?overview=full&geometries=geojson"
                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "BHPLogisticsFleetManager/1.0 (Android)")
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(text)
                    if (json.optString("code").equals("Ok", ignoreCase = true)) {
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val r = routes.getJSONObject(0)
                            val dist = r.optDouble("distance", 0.0)
                            val dur = r.optDouble("duration", 0.0)
                            val coords = r.getJSONObject("geometry").getJSONArray("coordinates")
                            val pts = ArrayList<LatLng>(coords.length())
                            for (i in 0 until coords.length()) {
                                val c = coords.getJSONArray(i)
                                pts.add(LatLng(c.getDouble(1), c.getDouble(0)))
                            }
                            if (pts.isNotEmpty()) {
                                corridors.add(
                                    RouteOption(
                                        id = "corridor_${corridors.size}",
                                        name = "Alternative Corridor",
                                        points = pts,
                                        distanceMeters = dist,
                                        durationSeconds = dur
                                    )
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Ignore corridor fallback errors
            }
        }
        return corridors
    }

    /**
     * Calculates the forward bearing (in degrees, 0..360) from point 1 to point 2.
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360.0) % 360.0
        return bearing.toFloat()
    }

    /**
     * Finds the bearing of the closest road segment to [point] along [route].
     */
    fun findClosestSegmentBearing(point: LatLng, route: List<LatLng>): Float {
        if (route.size < 2) return 0f
        var minDistance = Double.MAX_VALUE
        var closestSegmentBearing = 0f

        for (i in 0 until route.size - 1) {
            val p1 = route[i]
            val p2 = route[i + 1]
            val dist = distanceToSegmentMeters(point, p1, p2)
            if (dist < minDistance) {
                minDistance = dist
                closestSegmentBearing = calculateBearing(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            }
        }
        return closestSegmentBearing
    }

    /**
     * Filters raw GPS breadcrumbs to remove stationary GPS jitter (spiderweb zigzags),
     * suppresses jumps across gaps (> 2000m), and segments genuine road travel.
     *
     * Rules:
     * 1. Suppresses stationary jitter: consecutive points must represent true movement
     *    (>= 25m, or >= 15m if speed indicates driving).
     * 2. Large gap segmentation: splits at jumps > 2000m (e.g. app restart or long offline travel).
     * 3. Stationary cluster elimination: any segment with total net displacement < 40m
     *    (e.g., parked in a compound, office room, or warehouse bay) is discarded.
     */
    fun buildCleanTravelledSegments(
        rawPoints: List<PathPoint>
    ): List<List<LatLng>> {
        if (rawPoints.isEmpty()) return emptyList()

        val segments = mutableListOf<List<LatLng>>()
        var currentSegment = mutableListOf<PathPoint>()

        for (pt in rawPoints) {
            if (currentSegment.isEmpty()) {
                currentSegment.add(pt)
                continue
            }

            val lastPt = currentSegment.last()
            val dist = haversineDistanceMeters(
                lastPt.latitude, lastPt.longitude,
                pt.latitude, pt.longitude
            )

            // Gap > 2km -> start new segment to avoid straight cross-country lines
            if (dist > 2000.0) {
                if (currentSegment.size >= 2) {
                    segments.add(currentSegment.map { LatLng(it.latitude, it.longitude) })
                }
                currentSegment = mutableListOf(pt)
                continue
            }

            // Stationary noise suppression:
            // Moving vehicle (speed >= 3 km/h) needs at least 15m; stationary vehicle needs at least 25m
            val isMoving = (pt.speedKmh != null && pt.speedKmh >= 3.0) ||
                    (lastPt.speedKmh != null && lastPt.speedKmh >= 3.0)
            val minMovementRequired = if (isMoving) 15.0 else 25.0

            if (dist >= minMovementRequired) {
                currentSegment.add(pt)
            }
        }

        if (currentSegment.size >= 2) {
            segments.add(currentSegment.map { LatLng(it.latitude, it.longitude) })
        }

        // Only retain segments that represent genuine road travel (net displacement >= 40m)
        return segments.filter { seg ->
            seg.size >= 2 && haversineDistanceMeters(
                seg.first().latitude, seg.first().longitude,
                seg.last().latitude, seg.last().longitude
            ) >= 40.0
        }
    }
}
