package com.lorryowner.app.ui.driver

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.R
import com.lorryowner.app.service.DriverTripTrackingService
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Premium Driver Dashboard matching the visual quality and design language of the Owner Dashboard:
 * - White/light background, Navy headings, Emerald green primary accent.
 * - Header with BHP Logistics branding, Driver Dashboard subtitle, Menu icon, and Notification icon.
 * - 5 auto-rotating promotional/welcome banners with smooth slide transitions and page indicators.
 * - Driver profile & assigned vehicle card with real backend data.
 * - Active or upcoming trip card with live GPS status, Start/Complete actions.
 * - 3-column Trip Overview statistic cards.
 * - 2-column Quick Actions section with emerald banner header.
 */
@Composable
fun DriverDashboardScreen(
    viewModel: DriverDashboardViewModel,
    onNavigateToTrips: () -> Unit,
    onNavigateToStatus: (DriverTripUiItem?) -> Unit,
    onNavigateToTripDetails: (DriverTripUiItem) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.dashboardState
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDispatchDialog by remember { mutableStateOf(false) }
    var showStartConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }
    var completeNotesText by remember { mutableStateOf("") }

    val isTrackingActive by DriverTripTrackingService.isTrackingActive.collectAsState()
    val isMutating = viewModel.isMutatingTrip

    // Permission coordinator for starting trips directly from the dashboard
    DriverTrackingPermissionCoordinator(
        onPermissionsGranted = {
            showStartConfirmDialog = true
        }
    ) { requestPermissions ->

        Scaffold(
            topBar = {
                DriverDashboardHeader(
                    onMenuClick = { showDispatchDialog = true },
                    onNotificationClick = { showDispatchDialog = true }
                )
            },
            containerColor = Color(0xFFF8FAFC),
            modifier = modifier
        ) { innerPadding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Five Rotating Welcome Banners
                item {
                    DriverRotatingWelcomeBanner()
                }

                // 2. Driver & Assigned Vehicle Card
                item {
                    DriverAndVehicleCard(
                        driverName = state.driverName,
                        driverUsername = state.driverUsername,
                        assignedLorryReg = state.assignedLorryRegistration,
                        assignedLorryModel = state.assignedLorryMakeModel,
                        isOnline = state.isOnline,
                        isOnTrip = state.todayTrip?.tripStatus.equals("IN_PROGRESS", ignoreCase = true),
                        onProfileClick = onNavigateToProfile
                    )
                }

                // 3. Active / Upcoming Trip Card
                item {
                    DriverActiveTripCard(
                        todayTrip = state.todayTrip,
                        isTrackingActive = isTrackingActive,
                        isMutating = isMutating,
                        onStartTripClick = { requestPermissions() },
                        onCompleteTripClick = { showCompleteConfirmDialog = true },
                        onViewRouteClick = { trip -> onNavigateToTripDetails(trip) },
                        onBrowseTripsClick = onNavigateToTrips
                    )
                }

                // 4. Trip Overview Statistics (Active, Upcoming, Completed)
                item {
                    DriverTripOverviewSection(
                        activeCount = state.currentTripsCount,
                        upcomingCount = state.upcomingTripsCount,
                        completedCount = state.completedTripsCount,
                        onViewTrips = onNavigateToTrips
                    )
                }

                // 5. Quick Actions Section (2-Column Grid with Deep Emerald Header)
                item {
                    DriverQuickActionsSection(
                        onMyTripsClick = onNavigateToTrips,
                        onUpdateStatusClick = { onNavigateToStatus(state.todayTrip) },
                        onLiveRouteClick = {
                            if (state.todayTrip != null) {
                                onNavigateToTripDetails(state.todayTrip)
                            } else {
                                onNavigateToTrips()
                            }
                        },
                        onMyProfileClick = onNavigateToProfile
                    )
                }
            }
        }

        // Start Trip Confirmation Dialog
        if (showStartConfirmDialog && state.todayTrip != null) {
            val trip = state.todayTrip
            AlertDialog(
                onDismissRequest = { showStartConfirmDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Start Trip ${trip.tripReference}?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "This will update your trip status to In Progress and activate real-time phone GPS tracking.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Vehicle: ${trip.lorryRegistration}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Route: ${trip.originLocation} → ${trip.destinationLocation}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStartConfirmDialog = false
                            viewModel.startTrip(
                                tripId = trip.id,
                                onSuccess = { startedTrip ->
                                    DriverTripTrackingService.startTracking(
                                        context = context,
                                        tripId = startedTrip.id,
                                        tripReference = startedTrip.tripReference
                                    )
                                }
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("Confirm & Start", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartConfirmDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }

        // Complete Trip Confirmation Dialog
        if (showCompleteConfirmDialog && state.todayTrip != null) {
            val trip = state.todayTrip
            AlertDialog(
                onDismissRequest = { showCompleteConfirmDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Complete Trip ${trip.tripReference}?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Please enter optional delivery remarks before marking this consignment as completed:",
                            color = Color(0xFF475569),
                            fontSize = 13.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = completeNotesText,
                            onValueChange = { completeNotesText = it },
                            placeholder = { Text("e.g. Delivered safely at warehouse bay 4", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCompleteConfirmDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCompleteConfirmDialog = false
                            viewModel.completeTrip(
                                tripId = trip.id,
                                notes = completeNotesText.ifBlank { null },
                                onSuccess = {
                                    DriverTripTrackingService.stopTracking(context)
                                }
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("Complete Trip", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Fleet Dispatch Support Dialog
        if (showDispatchDialog) {
            AlertDialog(
                onDismissRequest = { showDispatchDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "BHP Fleet Dispatch Desk",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Central dispatch is available 24/7 for route assistance, breakdown support, and permit queries:",
                            color = Color(0xFF475569),
                            fontSize = 13.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Operations Helpline",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+91 94400 12345",
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Emergency Breakdowns: Dial 108 / Operations Desk",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDispatchDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }
    }
}

/**
 * 1. Premium Header matching Owner Dashboard:
 * - White surface with subtle border
 * - Menu icon on left
 * - "BHP Logistics" + "Fleet Driver Dashboard" in center
 * - Notification bell icon with unread dot on right
 */
@Composable
fun DriverDashboardHeader(
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFFE2E8F0))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Left: Hamburger Menu Icon
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(38.dp)
            ) {
                HamburgerMenuIcon(tint = Color(0xFF0F172A), modifier = Modifier.size(22.dp))
            }

            // Center: BHP Logistics Branding + Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "BHP Logistics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                        letterSpacing = 0.2.sp
                    )
                )
                Text(
                    text = "Fleet Driver Dashboard",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Right: Notification Bell Icon with unread badge
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.size(38.dp)
            ) {
                NotificationBellIcon(
                    tint = Color(0xFF0F172A),
                    hasBadge = true,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * 2. Rotating Welcome Banner with exactly 5 slides:
 * 1. Welcome Back, Driver! — "Ready for another safe journey?"
 * 2. Your Journey, Your Responsibility — "Drive More. Drive Safe."
 * 3. Every Trip Counts — "Keep your deliveries moving."
 * 4. Drive Safe, Reach Safe — "Your safety matters."
 * 5. Your Fleet, Your Control — "Stay connected with every trip."
 */
private data class DriverBannerSlide(
    val title: String,
    val subtitle: String,
    val brush: Brush,
    val titleColor: Color,
    val subtitleColor: Color
)

@Composable
fun DriverRotatingWelcomeBanner(
    modifier: Modifier = Modifier
) {
    val slides = remember {
        listOf(
            // Slide 1
            DriverBannerSlide(
                title = "Welcome Back,\nDriver! 👋",
                subtitle = "Ready for another safe journey?",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE8FDF0), Color(0xFFD1FAE5), Color(0xFFE0F2FE))
                ),
                titleColor = Color(0xFF0F172A),
                subtitleColor = Color(0xFF059669)
            ),
            // Slide 2
            DriverBannerSlide(
                title = "Your Journey,\nYour Responsibility",
                subtitle = "Drive More. Drive Safe.",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF059669))
                ),
                titleColor = Color.White,
                subtitleColor = Color(0xFFA7F3D0)
            ),
            // Slide 3
            DriverBannerSlide(
                title = "Every Trip\nCounts",
                subtitle = "Keep your deliveries moving.",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0369A1))
                ),
                titleColor = Color.White,
                subtitleColor = Color(0xFFBAE6FD)
            ),
            // Slide 4
            DriverBannerSlide(
                title = "Drive Safe,\nReach Safe",
                subtitle = "Your safety matters.",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF14532D), Color(0xFF166534), Color(0xFF15803D))
                ),
                titleColor = Color.White,
                subtitleColor = Color(0xFFBBF7D0)
            ),
            // Slide 5
            DriverBannerSlide(
                title = "Your Fleet,\nYour Control",
                subtitle = "Stay connected with every trip.",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF115E59), Color(0xFF047857))
                ),
                titleColor = Color.White,
                subtitleColor = Color(0xFFCCFBF1)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Auto-rotate every 4.5 seconds with smooth animation
    LaunchedEffect(pagerState) {
        while (isActive) {
            delay(4500L)
            val next = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) { page ->
            val slide = slides[page]
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(slide.brush)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = slide.titleColor,
                                    fontSize = 17.sp,
                                    lineHeight = 22.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = slide.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = slide.subtitleColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Commercial Lorry Graphic Image
                        Image(
                            painter = painterResource(id = R.drawable.bhp_truck_highway),
                            contentDescription = "BHP Commercial Lorry",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 115.dp, height = 80.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Page Dots Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 6.dp,
                    animationSpec = tween(300),
                    label = "banner_dot_width"
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFF10B981) else Color(0xFFCBD5E1)
                        )
                )
            }
        }
    }
}

/**
 * 3. Driver Profile & Assigned Lorry Card (Real existing backend data):
 * - Driver Name & Avatar
 * - Duty / Availability Status (On Trip, On Duty, Off Duty)
 * - Vehicle Plate Registration badge
 * - Vehicle Make/Model
 */
@Composable
fun DriverAndVehicleCard(
    driverName: String,
    driverUsername: String,
    assignedLorryReg: String?,
    assignedLorryModel: String?,
    isOnline: Boolean,
    isOnTrip: Boolean,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Driver Profile Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar Circle with Driver Initial
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFECFDF5))
                        .border(1.5.dp, Color(0xFF10B981), CircleShape)
                        .clickable { onProfileClick() }
                ) {
                    val initial = driverName.firstOrNull()?.uppercaseChar()?.toString() ?: "D"
                    Text(
                        text = initial,
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = driverName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (driverUsername.isNotBlank()) "@$driverUsername" else "Commercial Fleet Driver",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Duty / Availability Status Badge
                val (badgeBg, badgeText, badgeColor) = when {
                    isOnTrip -> Triple(Color(0xFFECFDF5), "On Trip", Color(0xFF15803D))
                    isOnline -> Triple(Color(0xFFECFDF5), "On Duty", Color(0xFF15803D))
                    else -> Triple(Color(0xFFF1F5F9), "Off Duty", Color(0xFF64748B))
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = badgeText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Assigned Lorry Details Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DashboardTruckIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Assigned Vehicle:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }

                // Vehicle Registration Plate Badge
                if (!assignedLorryReg.isNullOrBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = Color(0xFFFEF3C7), // Yellow plate
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706))
                        ) {
                            Text(
                                text = assignedLorryReg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (!assignedLorryModel.isNullOrBlank()) {
                            Text(
                                text = assignedLorryModel,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No Lorry Assigned",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 4. Active / Upcoming Trip Card:
 * - Shows active IN_PROGRESS trip with Complete Trip and Live Route buttons.
 * - Shows upcoming PLANNED trip with Start Trip button.
 * - Shows friendly empty state if no trips are assigned.
 */
@Composable
fun DriverActiveTripCard(
    todayTrip: DriverTripUiItem?,
    isTrackingActive: Boolean,
    isMutating: Boolean,
    onStartTripClick: () -> Unit,
    onCompleteTripClick: () -> Unit,
    onViewRouteClick: (DriverTripUiItem) -> Unit,
    onBrowseTripsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (todayTrip == null) {
        // Empty State Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    DashboardTruckIcon(tint = Color(0xFF94A3B8), modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No Active Trips",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You currently have no dispatched trips. New trip assignments will appear here.",
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onBrowseTripsClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text("Browse Trip History", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    val isProgress = todayTrip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)
    val isPlanned = todayTrip.tripStatus.equals("PLANNED", ignoreCase = true)

    val (statusLabel, statusBg, statusColor) = when {
        isProgress -> Triple("IN PROGRESS", Color(0xFFECFDF5), Color(0xFF15803D))
        isPlanned -> Triple("UPCOMING", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        else -> Triple(todayTrip.tripStatus.uppercase(), Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isProgress) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Reference & Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = if (isProgress) "CURRENT ACTIVE TRIP" else "DISPATCH ASSIGNMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = todayTrip.tripReference,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Origin -> Destination Route visualization
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Origin
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Origin", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = todayTrip.originLocation,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    // Vertical connector
                    Box(
                        modifier = Modifier
                            .padding(start = 4.5.dp, top = 2.dp, bottom = 2.dp)
                            .height(18.dp)
                            .width(1.5.dp)
                            .background(Color(0xFFCBD5E1))
                    )

                    // Destination
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LocationPinIcon(tint = Color(0xFF2563EB), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Destination", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = todayTrip.destinationLocation,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // Real-Time GPS Tracking Live Badge (if In Progress)
            if (isProgress) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = if (isTrackingActive) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isTrackingActive) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isTrackingActive) Color(0xFF16A34A) else Color(0xFFD97706))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTrackingActive) "Real-Time GPS Active" else "GPS Paused / Stopped",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isTrackingActive) Color(0xFF15803D) else Color(0xFFB45309)
                            )
                        }

                        // One-tap Restart GPS Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
                            modifier = Modifier.clickable {
                                DriverTripTrackingService.restartTracking(context, todayTrip.id, todayTrip.tripReference)
                                android.widget.Toast.makeText(context, "Restarting GPS tracking for ${todayTrip.tripReference}...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "🔄 Restart GPS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            when {
                isProgress -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCompleteTripClick,
                            enabled = !isMutating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            if (isMutating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                CheckmarkCircleIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Complete Trip", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { onViewRouteClick(todayTrip) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            LocationPinIcon(tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Route", color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
                isPlanned -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onStartTripClick,
                            enabled = !isMutating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            if (isMutating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                PlayVectorIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Trip", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { onViewRouteClick(todayTrip) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Route Details", color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = { onViewRouteClick(todayTrip) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("View Trip Summary", color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * 5. Trip Overview Section with 3 statistic cards:
 * - Active Trips
 * - Upcoming Trips
 * - Completed Trips
 */
@Composable
fun DriverTripOverviewSection(
    activeCount: Long,
    upcomingCount: Long,
    completedCount: Long,
    onViewTrips: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Trip Overview",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Active Trips Card
            DriverStatCard(
                title = "Active",
                count = activeCount.toString(),
                iconBg = Color(0xFFECFDF5),
                textColor = Color(0xFF065F46),
                icon = { DashboardTruckIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp)) },
                onClick = onViewTrips,
                modifier = Modifier.weight(1f)
            )

            // 2. Upcoming Trips Card
            DriverStatCard(
                title = "Upcoming",
                count = upcomingCount.toString(),
                iconBg = Color(0xFFEFF6FF),
                textColor = Color(0xFF1E40AF),
                icon = { ClockTimerIcon(tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp)) },
                onClick = onViewTrips,
                modifier = Modifier.weight(1f)
            )

            // 3. Completed Trips Card
            DriverStatCard(
                title = "Completed",
                count = completedCount.toString(),
                iconBg = Color(0xFFF1F5F9),
                textColor = Color(0xFF334155),
                icon = { CheckmarkCircleIcon(tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                onClick = onViewTrips,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DriverStatCard(
    title: String,
    count: String,
    iconBg: Color,
    textColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg)
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}

/**
 * 6. Quick Actions Section matching Owner Dashboard design:
 * - Deep emerald header banner ("⚡ Quick Actions")
 * - 2-column grid containing:
 *   * My Trips
 *   * Update Status
 *   * Live Route
 *   * My Profile
 */
@Composable
fun DriverQuickActionsSection(
    onMyTripsClick: () -> Unit,
    onUpdateStatusClick: () -> Unit,
    onLiveRouteClick: () -> Unit,
    onMyProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Emerald Green Header Banner matching reference design
        Surface(
            color = Color(0xFF047857),
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "⚡ Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                )
            }
        }

        // 2-Column Action Tiles Grid Container
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: My Trips & Update Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DriverActionTile(
                        title = "My Trips",
                        subtitle = "View all trips",
                        iconBg = Color(0xFFECFDF5),
                        icon = { DashboardTruckIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp)) },
                        onClick = onMyTripsClick,
                        modifier = Modifier.weight(1f)
                    )

                    DriverActionTile(
                        title = "Update Status",
                        subtitle = "Trip checkpoints",
                        iconBg = Color(0xFFEFF6FF),
                        icon = { CheckSquareIcon(tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) },
                        onClick = onUpdateStatusClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Live Route & My Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DriverActionTile(
                        title = "Live Route",
                        subtitle = "GPS navigation",
                        iconBg = Color(0xFFF0FDFA),
                        icon = { LocationPinIcon(tint = Color(0xFF0D9488), modifier = Modifier.size(20.dp)) },
                        onClick = onLiveRouteClick,
                        modifier = Modifier.weight(1f)
                    )

                    DriverActionTile(
                        title = "My Profile",
                        subtitle = "License & vehicle",
                        iconBg = Color(0xFFF8FAFC),
                        icon = { PersonVectorIcon(tint = Color(0xFF475569), modifier = Modifier.size(20.dp)) },
                        onClick = onMyProfileClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverActionTile(
    title: String,
    subtitle: String,
    iconBg: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg)
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ChevronRightIcon(tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
    }
}
