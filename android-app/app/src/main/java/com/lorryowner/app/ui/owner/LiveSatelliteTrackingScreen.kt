package com.lorryowner.app.ui.owner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.lorryowner.app.network.OsrmRoutingService
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.theme.*

/**
 * Satellite Live Tracking Screen for BHP Logistics Fleet Manager.
 * Matches the reference design with real satellite map tiles, floating speed indicator,
 * interactive map controls, and responsive trip info bar.
 */
@Composable
fun LiveSatelliteTrackingScreen(
    lorryId: Long? = null,
    tripId: Long? = null,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSession = remember { sessionManager ?: SessionManager(context) }
    val viewModel = remember(lorryId, tripId) {
        LiveTrackingViewModel(effectiveSession, lorryId = lorryId, tripId = tripId)
    }
    val state = viewModel.uiState
    val data = state.trackingData

    var showTripDetailsSheet by remember { mutableStateOf(false) }
    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }
    var currentTruckMarker by remember { mutableStateOf<Marker?>(null) }
    var originMarker by remember { mutableStateOf<Marker?>(null) }
    var destMarker by remember { mutableStateOf<Marker?>(null) }
    var plannedRoutePolyline by remember { mutableStateOf<Polyline?>(null) }
    var travelledPathPolylines by remember { mutableStateOf<List<Polyline>>(emptyList()) }

    // Lifecycle observer for polling
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

    // 1. Draw / update Road-Following Route to Destination & Terminal Pins
    LaunchedEffect(data?.plannedRoadRoute, data?.latitude, data?.longitude, googleMapRef, state.isSatelliteMode) {
        val map = googleMapRef ?: return@LaunchedEffect
        val route = data?.plannedRoadRoute ?: return@LaunchedEffect
        if (route.isEmpty()) return@LaunchedEffect

        plannedRoutePolyline?.remove()
        val routeColor = if (state.isSatelliteMode) {
            android.graphics.Color.parseColor("#00E5FF") // High-contrast Neon Cyan on dark satellite
        } else {
            android.graphics.Color.parseColor("#2563EB") // Royal Blue on light streets
        }

        // Slice remaining route so the BLUE line begins at current lorry position and continues to destination
        val truckLat = data.latitude
        val truckLon = data.longitude
        val pointsToDraw = if (truckLat != null && truckLon != null) {
            OsrmRoutingService.sliceRemainingRoute(LatLng(truckLat, truckLon), route)
        } else {
            route
        }

        val polylineOptions = PolylineOptions()
            .width(13f)
            .color(routeColor)
            .zIndex(2.0f)
            .geodesic(false) // Road points already follow real highway turns
            .addAll(pointsToDraw)

        plannedRoutePolyline = map.addPolyline(polylineOptions)

        // Origin Marker (Green Pin)
        if (data.originLatitude != null && data.originLongitude != null) {
            val originPos = LatLng(data.originLatitude, data.originLongitude)
            val originTitle = "Origin: ${data.originLocation.ifBlank { "Pickup" }}"
            if (originMarker == null) {
                originMarker = map.addMarker(
                    MarkerOptions()
                        .position(originPos)
                        .title(originTitle)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            } else {
                originMarker?.position = originPos
                originMarker?.title = originTitle
            }
        }

        // Destination Marker (Red Pin)
        if (data.destinationLatitude != null && data.destinationLongitude != null) {
            val destPos = LatLng(data.destinationLatitude, data.destinationLongitude)
            val destTitle = "Destination: ${data.destinationLocation.ifBlank { "Delivery" }}"
            if (destMarker == null) {
                destMarker = map.addMarker(
                    MarkerOptions()
                        .position(destPos)
                        .title(destTitle)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            } else {
                destMarker?.position = destPos
                destMarker?.title = destTitle
            }
        }
    }

    // 2. Update Truck Marker & Actual Travelled Path using Genuine GPS Breadcrumbs
    LaunchedEffect(data?.latitude, data?.longitude, data?.breadcrumbs, googleMapRef) {
        val map = googleMapRef ?: return@LaunchedEffect
        val lat = data?.latitude ?: return@LaunchedEffect
        val lon = data?.longitude ?: return@LaunchedEffect

        val truckPos = LatLng(lat, lon)

        // Update or add truck marker (zIndex = 3.0f to float on top of road routes)
        if (currentTruckMarker == null) {
            val markerOptions = MarkerOptions()
                .position(truckPos)
                .title(data.registrationNumber.ifBlank { "Truck" })
                .snippet("Speed: ${TrackingFormattingHelper.formatSpeed(data.currentSpeedKmh)}")
                .icon(createTruckMarkerBitmap(context))
                .zIndex(3.0f)
            currentTruckMarker = map.addMarker(markerOptions)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(truckPos, 15.5f))
        } else {
            currentTruckMarker?.position = truckPos
            currentTruckMarker?.snippet = "Speed: ${TrackingFormattingHelper.formatSpeed(data.currentSpeedKmh)}"
            map.animateCamera(CameraUpdateFactory.newLatLng(truckPos))
        }

        // Draw / update actual travelled path using genuine GPS breadcrumbs (zIndex = 1.5f)
        // Clean stationary jitter and segment gaps > 2000m to eliminate green zigzags and cross-field lines
        travelledPathPolylines.forEach { it.remove() }

        val pathPoints = data.breadcrumbs.map {
            OsrmRoutingService.PathPoint(
                latitude = it.latitude,
                longitude = it.longitude,
                speedKmh = it.speedKmh
            )
        } + OsrmRoutingService.PathPoint(
            latitude = lat,
            longitude = lon,
            speedKmh = data.currentSpeedKmh
        )

        val cleanSegments = OsrmRoutingService.buildCleanTravelledSegments(pathPoints)
        val newPolylines = mutableListOf<Polyline>()

        for (seg in cleanSegments) {
            val poly = map.addPolyline(
                PolylineOptions()
                    .width(10f)
                    .color(android.graphics.Color.parseColor("#10B981")) // Vibrant Green (BhpRunningGreen)
                    .zIndex(2.5f)
                    .addAll(seg)
            )
            newPolylines.add(poly)
        }
        travelledPathPolylines = newPolylines
    }

    // Toggle satellite map type
    LaunchedEffect(state.isSatelliteMode, googleMapRef) {
        googleMapRef?.mapType = if (state.isSatelliteMode) {
            GoogleMap.MAP_TYPE_SATELLITE
        } else {
            GoogleMap.MAP_TYPE_NORMAL
        }
    }

    Scaffold(
        topBar = {
            TrackingTopAppBar(
                vehicleReg = data?.registrationNumber?.ifBlank { "Live Tracking" } ?: "Live Tracking",
                gpsStatus = data?.gpsStatus ?: GpsStatus.WAITING_FOR_LOCATION,
                onBack = onBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. REAL SATELLITE MAP (Google Maps MapView)
            SatelliteMapViewContainer(
                onMapReady = { map ->
                    googleMapRef = map
                    map.mapType = if (state.isSatelliteMode) GoogleMap.MAP_TYPE_SATELLITE else GoogleMap.MAP_TYPE_NORMAL
                    map.uiSettings.isZoomControlsEnabled = false
                    map.uiSettings.isZoomGesturesEnabled = true
                    map.uiSettings.isScrollGesturesEnabled = true
                    map.uiSettings.isRotateGesturesEnabled = true
                    map.uiSettings.isTiltGesturesEnabled = true
                    map.uiSettings.isCompassEnabled = true
                    map.uiSettings.isMapToolbarEnabled = false

                    val initialLat = data?.latitude ?: 15.8281
                    val initialLon = data?.longitude ?: 78.0373
                    val initialPos = LatLng(initialLat, initialLon)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPos, 16.5f))
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. FLOATING SPEED CARD (Top-Left) - visible whenever telemetry is present
            if (data != null) {
                SpeedCard(
                    speedKmh = data.currentSpeedKmh,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 14.dp, top = 14.dp)
                )
            }

            // 2.1 FLOATING ROUTE DEVIATION ALERT (Top-Center)
            if (data?.isRouteDeviated == true) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp, start = 140.dp, end = 76.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("⚠️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Route Deviation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", data.deviationDistanceKm)} km off planned highway",
                                fontSize = 10.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }

            // 3. FLOATING MAP CONTROLS (Top-Right) - Recenter & Satellite mode (pinch to zoom used)
            MapControlsColumn(
                isSatelliteMode = state.isSatelliteMode,
                onRecenter = {
                    val lat = data?.latitude ?: 15.8281
                    val lon = data?.longitude ?: 78.0373
                    googleMapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 16.5f))
                },
                onToggleSatellite = { viewModel.toggleMapMode() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp, top = 14.dp)
            )

            // 4. Loading state overlay
            if (state.isLoading && data == null) {
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, BhpCardBorder),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = BhpNavyDark, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Acquiring Live Telemetry...",
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connecting to vehicle tracking service",
                            fontSize = 12.5.sp,
                            color = BhpTextSecondary
                        )
                    }
                }
            }

            // 5. Error state overlay
            if (state.errorMessage != null && data == null) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, BhpErrorRed),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "⚠️ Telemetry Unavailable",
                            fontWeight = FontWeight.Bold,
                            color = BhpErrorRed,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.errorMessage ?: "Failed to connect",
                            fontSize = 13.sp,
                            color = BhpTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.loadTrackingDetails() },
                            colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark)
                        ) {
                            Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 6. Empty state overlay (vehicle not on trip)
            if (data == null && !state.isLoading && state.errorMessage == null) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, BhpCardBorder),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🚚", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Active Trip Tracking",
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This vehicle does not currently have an active trip with live GPS telemetry.",
                            fontSize = 13.sp,
                            color = BhpTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // 7. BOTTOM MOBILE PORTRAIT INFO PANEL
            if (data != null) {
                BottomTripInfoPanel(
                    data = data,
                    onTripDetailsClick = { showTripDetailsSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        }
    }

    // Detailed bottom sheet
    if (showTripDetailsSheet && data != null) {
        TripDetailsBottomSheet(
            data = data,
            sessionManager = sessionManager,
            onDismiss = { showTripDetailsSheet = false }
        )
    }
}

/**
 * Top App Bar matching BHP Logistics Live Tracking reference design.
 */
@Composable
fun TrackingTopAppBar(
    vehicleReg: String,
    gpsStatus: GpsStatus,
    onBack: () -> Unit
) {
    Surface(
        color = BhpNavyDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // BHP Brand Icon
            Surface(
                color = Color(0xFFEA580C),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BHP Logistics • Live Tracking",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = vehicleReg,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Live Pill Badge
            GpsStatusBadge(status = gpsStatus)
        }
    }
}

/**
 * Top-left floating Current Speed indicator card.
 */
@Composable
fun SpeedCard(
    speedKmh: Double?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "CURRENT SPEED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = TrackingFormattingHelper.formatSpeedNumber(speedKmh),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = BhpTextPrimary,
                        fontSize = 26.sp,
                        lineHeight = 26.sp
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextSecondary,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * Top-right floating map controls (Recenter, Satellite toggle).
 * Pinch-to-zoom is supported natively on the map surface.
 */
@Composable
fun MapControlsColumn(
    isSatelliteMode: Boolean,
    onRecenter: () -> Unit,
    onToggleSatellite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // Recenter Button
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 5.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.size(42.dp)
        ) {
            IconButton(onClick = onRecenter, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Re-center",
                    tint = BhpNavyDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Satellite Mode Toggle Button
        Surface(
            color = if (isSatelliteMode) BhpNavyDark else Color.White,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 5.dp,
            border = BorderStroke(1.dp, if (isSatelliteMode) BhpNavyDark else Color(0xFFCBD5E1)),
            modifier = Modifier.clickable { onToggleSatellite() }
        ) {
            Text(
                text = if (isSatelliteMode) "SATELLITE" else "NORMAL",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isSatelliteMode) Color.White else BhpNavyDark,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

/**
 * Bottom expandable and swipeable portrait trip information panel.
 * Can be pulled down (kindaki) or pulled up (paiki) seamlessly.
 */
@Composable
fun BottomTripInfoPanel(
    data: LiveTrackingLorryUiState?,
    onTripDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (data == null) return

    var isExpanded by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY < -25f) {
                                isExpanded = true
                            } else if (dragOffsetY > 25f) {
                                isExpanded = false
                            }
                            dragOffsetY = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffsetY += dragAmount
                        }
                    )
                }
        ) {
            // Drag Handle Bar (Top Center) - Clickable to toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFCBD5E1))
                )
            }

            // Always-Visible Peek Header (Tappable to Expand / Collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.registrationNumber.ifBlank { "Vehicle" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = if (isExpanded) {
                            "Driver: ${data.driverName.ifBlank { "Unassigned" }}"
                        } else {
                            "Driver: ${data.driverName.ifBlank { "Unassigned" }}  •  Swipe up for details"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isExpanded) BhpTextSecondary else BhpNavyDark,
                            fontWeight = if (isExpanded) FontWeight.Normal else FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    GpsStatusBadge(status = data.gpsStatus)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isExpanded) "▼" else "▲",
                                color = BhpNavyDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Expandable Content (Sliding up/down)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    // Visible Stale Location Banner
                    if (data.gpsStatus == GpsStatus.STALE || data.gpsStatus == GpsStatus.WAITING_FOR_LOCATION) {
                        Surface(
                            color = if (data.gpsStatus == GpsStatus.STALE) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (data.gpsStatus == GpsStatus.STALE) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (data.gpsStatus == GpsStatus.STALE) "⚠️ Stale GPS" else "⏳ Waiting for Fix",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (data.gpsStatus == GpsStatus.STALE) Color(0xFFB45309) else Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (!data.lastGpsUpdate.isNullOrBlank()) "Last seen: ${data.lastGpsUpdate}" else "Awaiting telemetry from driver app",
                                    fontSize = 11.sp,
                                    color = if (data.gpsStatus == GpsStatus.STALE) Color(0xFF92400E) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Visible Route Deviation Alert
                    if (data.isRouteDeviated) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ Route Deviation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Vehicle is ~${String.format(java.util.Locale.US, "%.1f", data.deviationDistanceKm)} km away from designated road path",
                                    fontSize = 11.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 6.dp))

                    // Row 2: Route Origin -> Destination
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("📍", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${data.originLocation.ifBlank { "Origin" }} → ${data.destinationLocation.ifBlank { "Destination" }}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary,
                                fontSize = 13.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: 3 Compact Metric Chips (Location, Distance, ETA)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current Location
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text("Current Location", fontSize = 10.5.sp, color = BhpTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = data.currentLocationDescription.ifBlank { "Pending location" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Remaining Distance
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text("Remaining", fontSize = 10.5.sp, color = BhpTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (data.remainingDistanceKm > 0) TrackingFormattingHelper.formatDistance(data.remainingDistanceKm) else "--",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    maxLines = 1
                                )
                            }
                        }

                        // ETA
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text("ETA", fontSize = 10.5.sp, color = BhpTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = data.estimatedDrivingTime.ifBlank { "--" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 4: Touch-friendly full-width "View Full Trip Details" button
                    Button(
                        onClick = onTripDetailsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "View Full Trip Details",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Container embedding Google Maps MapView with lifecycle handling.
 */
@Composable
fun SatelliteMapViewContainer(
    onMapReady: (GoogleMap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
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

    LaunchedEffect(mapView) {
        mapView.getMapAsync { googleMap ->
            onMapReady(googleMap)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

/**
 * Helper generating a custom vehicle bitmap descriptor for real map rendering.
 */
private fun createTruckMarkerBitmap(context: Context): BitmapDescriptor {
    val width = 120
    val height = 60
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A") // Navy body
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(10f, 10f, 110f, 50f, 8f, 8f, paint)

    paint.color = android.graphics.Color.parseColor("#E2E8F0") // Cargo trailer
    canvas.drawRoundRect(15f, 15f, 75f, 45f, 4f, 4f, paint)

    paint.color = android.graphics.Color.parseColor("#EA580C") // Cabin orange
    canvas.drawRoundRect(80f, 15f, 105f, 45f, 4f, 4f, paint)

    paint.color = android.graphics.Color.parseColor("#38BDF8") // Windshield
    canvas.drawRoundRect(92f, 20f, 103f, 40f, 2f, 2f, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
