package com.lorryowner.app.ui.driver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.lorryowner.app.network.CityCoordinateResolver
import com.lorryowner.app.network.OsrmRoutingService
import com.lorryowner.app.network.OsrmRoutingService.RouteOption
import com.lorryowner.app.service.DriverRouteMatchingEngine
import com.lorryowner.app.service.DriverTripTrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Intelligent Real-Time Road Route & Dynamic Selection Map Component for Driver Portal.
 *
 * Visual Specifications:
 * - Selected road route: Prominent Blue Line (#2563EB, 12f width, zIndex 2f)
 * - Alternative road routes: Thin Grey Lines (#94A3B8, 6f width, zIndex 1f)
 * - Travelled breadcrumb path: Vibrant Green Line (#16A34A, 12f width, zIndex 3f)
 * - Lorry location: Custom truck marker (zIndex 4f)
 * - Terminals: Origin (Green pin) and Destination (Red pin)
 */
@Composable
fun DriverLiveRouteMapCard(
    trip: DriverTripUiItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val matchingEngine = remember { DriverRouteMatchingEngine() }

    var candidateRoutes by remember { mutableStateOf<List<RouteOption>>(emptyList()) }
    var matchingState by remember { mutableStateOf(DriverRouteMatchingEngine.RouteMatchingState()) }
    var isLoadingRoutes by remember { mutableStateOf(true) }
    var routeError by remember { mutableStateOf<String?>(null) }

    val latestGpsLocation by DriverTripTrackingService.latestLocation.collectAsState()
    val recentGpsLocations by DriverTripTrackingService.recentLocations.collectAsState()
    val isTrackingActive by DriverTripTrackingService.isTrackingActive.collectAsState()

    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }
    var truckMarker by remember { mutableStateOf<Marker?>(null) }
    var originMarker by remember { mutableStateOf<Marker?>(null) }
    var destMarker by remember { mutableStateOf<Marker?>(null) }

    var selectedPolyline by remember { mutableStateOf<Polyline?>(null) }
    var alternativePolylines by remember { mutableStateOf<List<Polyline>>(emptyList()) }
    var travelledPolylines by remember { mutableStateOf<List<Polyline>>(emptyList()) }

    // Resolve Origin and Destination coordinates safely
    val originCoords = remember(trip.originLocation) {
        CityCoordinateResolver.resolveCoordinates(context, trip.originLocation)
    }
    val destCoords = remember(trip.destinationLocation) {
        CityCoordinateResolver.resolveCoordinates(context, trip.destinationLocation)
    }

    // 1. Initial Route Fetch from OSRM
    LaunchedEffect(trip.id, originCoords, destCoords, latestGpsLocation) {
        // Start from driver's current position if available, otherwise origin
        val startLat = latestGpsLocation?.latitude ?: originCoords.latitude
        val startLon = latestGpsLocation?.longitude ?: originCoords.longitude

        isLoadingRoutes = true
        routeError = null
        val result = withContext(Dispatchers.IO) {
            OsrmRoutingService.getOrFetchAlternativeRoutes(
                tripId = trip.id,
                originLat = startLat,
                originLon = startLon,
                destLat = destCoords.latitude,
                destLon = destCoords.longitude
            )
        }

        if (result.isSuccess) {
            val routes = result.getOrThrow()
            candidateRoutes = routes
            matchingEngine.reset(routes.firstOrNull()?.id)
            if (routes.isNotEmpty()) {
                val primary = routes.first()
                matchingState = DriverRouteMatchingEngine.RouteMatchingState(
                    selectedRouteId = primary.id,
                    selectedRoute = primary,
                    statusDescription = if (routes.size > 1) "Observing road movement..." else "On planned route"
                )
            }
        } else {
            routeError = result.exceptionOrNull()?.message ?: "Unable to fetch road routes"
        }
        isLoadingRoutes = false
    }

    // 2. Continuous Dynamic Route Matching from genuine GPS telemetry
    LaunchedEffect(latestGpsLocation, candidateRoutes) {
        val loc = latestGpsLocation ?: return@LaunchedEffect
        if (candidateRoutes.isEmpty()) return@LaunchedEffect

        val observation = DriverRouteMatchingEngine.GpsObservation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else null,
            bearing = if (loc.hasBearing()) loc.bearing else null,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
            timestampMs = loc.time
        )

        val updatedState = matchingEngine.processGpsFix(observation, candidateRoutes)
        matchingState = updatedState

        // Check if rate-limited recalculation is needed due to genuine deviation
        if (matchingEngine.shouldTriggerRecalculation(loc.latitude, loc.longitude, updatedState.isDeviated)) {
            coroutineScope.launch(Dispatchers.IO) {
                val recalcResult = OsrmRoutingService.fetchAlternativeRoutesFromOsrm(
                    originLat = loc.latitude,
                    originLon = loc.longitude,
                    destLat = destCoords.latitude,
                    destLon = destCoords.longitude
                )
                if (recalcResult.isSuccess) {
                    val newRoutes = recalcResult.getOrThrow()
                    if (newRoutes.isNotEmpty()) {
                        candidateRoutes = newRoutes
                        matchingEngine.reset(newRoutes.first().id)
                    }
                }
            }
        }
    }

    // 3. Update Map Polylines (Selected Remaining Route, Alternatives, Terminal Markers)
    LaunchedEffect(matchingState.selectedRouteId, candidateRoutes, googleMapRef, latestGpsLocation) {
        val map = googleMapRef ?: return@LaunchedEffect
        val selectedId = matchingState.selectedRouteId ?: candidateRoutes.firstOrNull()?.id

        // Clear previous polylines
        selectedPolyline?.remove()
        alternativePolylines.forEach { it.remove() }
        val newAltPolylines = mutableListOf<Polyline>()

        val truckLoc = latestGpsLocation

        for (route in candidateRoutes) {
            if (route.id == selectedId) {
                // Slice remaining route so the BLUE line begins at the driver's current position and ends at destination
                val pointsToDraw = if (truckLoc != null && route.points.isNotEmpty()) {
                    OsrmRoutingService.sliceRemainingRoute(
                        LatLng(truckLoc.latitude, truckLoc.longitude),
                        route.points
                    )
                } else {
                    route.points
                }

                // Prominent Royal Blue Line for remaining route following road to destination
                val poly = map.addPolyline(
                    PolylineOptions()
                        .addAll(pointsToDraw)
                        .width(13f)
                        .color(android.graphics.Color.parseColor("#2563EB")) // Royal Blue
                        .zIndex(2.0f)
                        .geodesic(false)
                )
                selectedPolyline = poly
            } else {
                // Thin Grey Line for candidate alternative routes
                val poly = map.addPolyline(
                    PolylineOptions()
                        .addAll(route.points)
                        .width(6f)
                        .color(android.graphics.Color.parseColor("#94A3B8")) // Slate Grey
                        .zIndex(1.0f)
                        .geodesic(false)
                )
                newAltPolylines.add(poly)
            }
        }
        alternativePolylines = newAltPolylines

        // Ensure Origin & Destination markers exist
        if (originMarker == null) {
            originMarker = map.addMarker(
                MarkerOptions()
                    .position(originCoords)
                    .title("Origin: ${trip.originLocation}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }
        if (destMarker == null) {
            destMarker = map.addMarker(
                MarkerOptions()
                    .position(destCoords)
                    .title("Destination: ${trip.destinationLocation}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }
    }

    // 4. Update Truck Position & Actual Travelled Path
    LaunchedEffect(latestGpsLocation, recentGpsLocations, googleMapRef) {
        val map = googleMapRef ?: return@LaunchedEffect
        val loc = latestGpsLocation ?: return@LaunchedEffect
        val truckPos = LatLng(loc.latitude, loc.longitude)

        if (truckMarker == null) {
            truckMarker = map.addMarker(
                MarkerOptions()
                    .position(truckPos)
                    .title(trip.lorryRegistration)
                    .icon(createDriverTruckMarkerBitmap(context))
                    .anchor(0.5f, 0.5f)
                    .zIndex(4.0f)
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(truckPos, 15f))
        } else {
            truckMarker?.position = truckPos
            if (loc.hasBearing()) {
                truckMarker?.rotation = loc.bearing
            }
        }

        // Draw travelled path using genuine GPS breadcrumbs (zIndex 1.5f)
        // Clean stationary jitter and segment gaps > 2000m to eliminate green zigzags and cross-field lines
        travelledPolylines.forEach { it.remove() }

        val pathPoints = recentGpsLocations.map {
            OsrmRoutingService.PathPoint(
                latitude = it.latitude,
                longitude = it.longitude,
                speedKmh = if (it.hasSpeed()) (it.speed * 3.6).coerceAtLeast(0.0) else null
            )
        } + OsrmRoutingService.PathPoint(
            latitude = loc.latitude,
            longitude = loc.longitude,
            speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6).coerceAtLeast(0.0) else null
        )

        val cleanSegments = OsrmRoutingService.buildCleanTravelledSegments(pathPoints)
        val newTravelled = mutableListOf<Polyline>()

        for (seg in cleanSegments) {
            val poly = map.addPolyline(
                PolylineOptions()
                    .addAll(seg)
                    .width(10f)
                    .color(android.graphics.Color.parseColor("#10B981")) // Vibrant Green (BhpRunningGreen)
                    .zIndex(2.5f)
            )
            newTravelled.add(poly)
        }
        travelledPolylines = newTravelled
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title & Tracking Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isTrackingActive) Color(0xFF16A34A) else Color(0xFFD97706))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Road Route & Trajectory",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                            modifier = Modifier.clickable {
                                DriverTripTrackingService.restartTracking(context, trip.id, trip.tripReference)
                                android.widget.Toast.makeText(context, "Restarting GPS tracking for ${trip.tripReference}...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "🔄 Restart GPS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Surface(
                        color = if (matchingState.isDeviated) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (matchingState.isDeviated) Color(0xFFFECACA) else Color(0xFFA7F3D0)
                        )
                    ) {
                        Text(
                            text = if (matchingState.isDeviated) "ROUTE DEVIATION" else "MATCHED",
                            color = if (matchingState.isDeviated) Color(0xFFDC2626) else Color(0xFF15803D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Map View Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            ) {
                DriverMapViewContainer(
                    onMapReady = { map ->
                        googleMapRef = map
                        map.uiSettings.isZoomControlsEnabled = false // Two-finger pinch used
                        map.uiSettings.isZoomGesturesEnabled = true
                        map.uiSettings.isScrollGesturesEnabled = true
                        map.uiSettings.isRotateGesturesEnabled = true
                        map.uiSettings.isTiltGesturesEnabled = true
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.isMapToolbarEnabled = false

                        val initialPos = latestGpsLocation?.let { LatLng(it.latitude, it.longitude) } ?: originCoords
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPos, 14.5f))
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Status Pill (Top-Center)
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = matchingState.statusDescription,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (matchingState.isDeviated) Color(0xFFDC2626) else Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Floating Controls Stack (Top-Right): Recenter + Quick Zoom In/Out
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Recenter
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                val target = latestGpsLocation?.let { LatLng(it.latitude, it.longitude) } ?: originCoords
                                googleMapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎯", fontSize = 15.sp)
                        }
                    }

                    // Zoom In
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                googleMapRef?.animateCamera(CameraUpdateFactory.zoomIn())
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }

                    // Zoom Out
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                googleMapRef?.animateCamera(CameraUpdateFactory.zoomOut())
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                // Floating Speed Badge (Bottom-Start)
                val currentSpeed = latestGpsLocation?.let {
                    if (it.hasSpeed()) (it.speed * 3.6).roundToInt() else 0
                } ?: 0
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.90f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "$currentSpeed",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "km/h",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Loading overlay
                if (isLoadingRoutes) {
                    Surface(
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Calculating Road Network...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alternative Route Selector Chips (if > 1 route)
            if (candidateRoutes.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    candidateRoutes.forEachIndexed { index, route ->
                        val isSelected = (matchingState.selectedRouteId ?: candidateRoutes.first().id) == route.id
                        val km = (route.distanceMeters / 1000.0).roundToInt()
                        val hours = (route.durationSeconds / 3600.0).toInt()
                        val mins = ((route.durationSeconds % 3600) / 60.0).toInt()
                        val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                matchingEngine.manualSelectRoute(route.id)
                                matchingState = matchingState.copy(
                                    selectedRouteId = route.id,
                                    selectedRoute = route,
                                    statusDescription = "Manually selected: ${route.name}"
                                )
                            },
                            label = {
                                Text(
                                    text = "${route.name} (${km}km • $timeStr)",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEFF6FF),
                                selectedLabelColor = Color(0xFF1D4ED8)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFFCBD5E1),
                                selectedBorderColor = Color(0xFF2563EB)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp, 4.dp)
                            .background(Color(0xFF2563EB), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Selected Route", fontSize = 11.sp, color = Color(0xFF64748B))

                    if (candidateRoutes.size > 1) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp, 3.dp)
                                .background(Color(0xFF94A3B8), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Alternatives", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(14.dp, 4.dp)
                            .background(Color(0xFF16A34A), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Travelled", fontSize = 11.sp, color = Color(0xFF64748B))
                }

                matchingState.selectedRoute?.let { route ->
                    val km = (route.distanceMeters / 1000.0).roundToInt()
                    Text(
                        text = "$km km planned",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverMapViewContainer(
    onMapReady: (GoogleMap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember {
        object : MapView(context) {
            override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
                when (ev.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN,
                    android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            onCreate(Bundle())
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    onMapReady(map)
                }
            }
        },
        modifier = modifier
    )
}

private fun createDriverTruckMarkerBitmap(context: Context): BitmapDescriptor {
    val width = 110
    val height = 55
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A") // Navy Body
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(8f, 8f, 102f, 47f, 6f, 6f, paint)

    paint.color = android.graphics.Color.parseColor("#E2E8F0") // Cargo
    canvas.drawRoundRect(12f, 12f, 70f, 43f, 4f, 4f, paint)

    paint.color = android.graphics.Color.parseColor("#16A34A") // Green Cabin
    canvas.drawRoundRect(74f, 12f, 98f, 43f, 4f, 4f, paint)

    paint.color = android.graphics.Color.parseColor("#38BDF8") // Windshield
    canvas.drawRoundRect(86f, 16f, 96f, 39f, 2f, 2f, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
