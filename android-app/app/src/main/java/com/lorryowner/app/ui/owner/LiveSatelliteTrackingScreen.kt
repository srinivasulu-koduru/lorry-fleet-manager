package com.lorryowner.app.ui.owner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
    var currentPolyline by remember { mutableStateOf<Polyline?>(null) }

    // Lifecycle observer for polling
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

    // Update map marker and camera when new GPS coordinates arrive
    LaunchedEffect(data?.latitude, data?.longitude, googleMapRef) {
        val map = googleMapRef ?: return@LaunchedEffect
        val lat = data?.latitude ?: return@LaunchedEffect
        val lon = data?.longitude ?: return@LaunchedEffect

        val truckPos = LatLng(lat, lon)

        // Update or add truck marker
        if (currentTruckMarker == null) {
            val markerOptions = MarkerOptions()
                .position(truckPos)
                .title(data.registrationNumber.ifBlank { "Truck" })
                .snippet("Speed: ${TrackingFormattingHelper.formatSpeed(data.currentSpeedKmh)}")
                .icon(createTruckMarkerBitmap(context))
            currentTruckMarker = map.addMarker(markerOptions)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(truckPos, 16.5f))
        } else {
            currentTruckMarker?.position = truckPos
            currentTruckMarker?.snippet = "Speed: ${TrackingFormattingHelper.formatSpeed(data.currentSpeedKmh)}"
            map.animateCamera(CameraUpdateFactory.newLatLng(truckPos))
        }

        // Draw / update route line
        currentPolyline?.remove()
        val polylineOptions = PolylineOptions()
            .width(14f)
            .color(android.graphics.Color.parseColor("#22C55E")) // Bright Green Route Line
            .geodesic(true)

        if (data.originLatitude != null && data.originLongitude != null) {
            polylineOptions.add(LatLng(data.originLatitude, data.originLongitude))
        }
        for (b in data.breadcrumbs) {
            polylineOptions.add(LatLng(b.latitude, b.longitude))
        }
        polylineOptions.add(truckPos)
        if (data.destinationLatitude != null && data.destinationLongitude != null) {
            polylineOptions.add(LatLng(data.destinationLatitude, data.destinationLongitude))
        }
        currentPolyline = map.addPolyline(polylineOptions)
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
                vehicleReg = data?.registrationNumber ?: "AP 31 TC 1234",
                gpsStatus = data?.gpsStatus ?: GpsStatus.LIVE,
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
                    map.uiSettings.isCompassEnabled = true
                    map.uiSettings.isMapToolbarEnabled = false

                    val initialLat = data?.latitude ?: 15.8281
                    val initialLon = data?.longitude ?: 78.0373
                    val initialPos = LatLng(initialLat, initialLon)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPos, 16.5f))
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. FLOATING SPEED CARD (Top-Left)
            SpeedCard(
                speedKmh = data?.currentSpeedKmh,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
            )

            // 3. FLOATING MAP CONTROLS (Top-Right)
            MapControlsColumn(
                isSatelliteMode = state.isSatelliteMode,
                onZoomIn = { googleMapRef?.animateCamera(CameraUpdateFactory.zoomIn()) },
                onZoomOut = { googleMapRef?.animateCamera(CameraUpdateFactory.zoomOut()) },
                onRecenter = {
                    val lat = data?.latitude ?: 15.8281
                    val lon = data?.longitude ?: 78.0373
                    googleMapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 16.5f))
                },
                onToggleSatellite = { viewModel.toggleMapMode() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp)
            )

            // 4. BOTTOM FLOATING INFO PANEL
            BottomTripInfoPanel(
                data = data,
                onTripDetailsClick = { showTripDetailsSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }

    // Detailed bottom sheet
    if (showTripDetailsSheet && data != null) {
        TripDetailsBottomSheet(
            data = data,
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEA580C)),
                contentAlignment = Alignment.Center
            ) {
                DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "BHP Logistics",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Text(
                text = " | ",
                color = Color(0xFF64748B),
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            )

            Text(
                text = "Live Tracking",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            // Vehicle Registration
            Text(
                text = "Vehicle: $vehicleReg",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(10.dp))

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
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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
                        fontSize = 28.sp,
                        lineHeight = 28.sp
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

/**
 * Top-right floating map controls (Zoom in, Zoom out, Recenter, Satellite mode).
 */
@Composable
fun MapControlsColumn(
    isSatelliteMode: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    onToggleSatellite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // Zoom Controls Card
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column {
                IconButton(onClick = onZoomIn, modifier = Modifier.size(38.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = BhpNavyDark)
                }
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                IconButton(onClick = onZoomOut, modifier = Modifier.size(38.dp)) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = BhpNavyDark)
                }
            }
        }

        // Recenter Button
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            IconButton(onClick = onRecenter, modifier = Modifier.size(38.dp)) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Re-center", tint = BhpNavyDark)
            }
        }

        // Satellite Mode Toggle Button
        Surface(
            color = if (isSatelliteMode) BhpNavyDark else Color.White,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.clickable { onToggleSatellite() }
        ) {
            Text(
                text = "SATELLITE",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isSatelliteMode) Color.White else BhpNavyDark,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Bottom responsive trip information bar matching reference screenshot.
 */
@Composable
fun BottomTripInfoPanel(
    data: LiveTrackingLorryUiState?,
    onTripDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Info Row: Driver | Current Location | Distance Travelled | Distance Remaining | ETA | CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Driver
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "Driver",
                        fontSize = 11.sp,
                        color = BhpTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = data?.driverName?.ifBlank { "Ramesh Kumar" } ?: "Ramesh Kumar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Current Location
                Column(modifier = Modifier.weight(1.4f)) {
                    Text(
                        text = "Current Location",
                        fontSize = 11.sp,
                        color = BhpTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = data?.currentLocationDescription ?: "Near Kurnool (NH 44)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Distance Travelled
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "Distance Travelled",
                        fontSize = 11.sp,
                        color = BhpTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = TrackingFormattingHelper.formatDistance(data?.distanceTravelledKm ?: 145.0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                }

                // Distance Remaining
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "Distance Remaining",
                        fontSize = 11.sp,
                        color = BhpTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = TrackingFormattingHelper.formatDistance(data?.remainingDistanceKm ?: 285.0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                }

                // ETA
                Column(modifier = Modifier.weight(0.9f)) {
                    Text(
                        text = "ETA",
                        fontSize = 11.sp,
                        color = BhpTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = data?.estimatedDrivingTime?.ifBlank { "4h 55m" } ?: "4h 55m",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Trip Details Button
                Button(
                    onClick = onTripDetailsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Trip Details",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
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
