package com.lorryowner.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.theme.*

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
    var showContactDialog by remember { mutableStateOf(false) }
    var showDocumentsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BhpScreenBg),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Top Bar: Branding + Notifications + Avatar Shortcut
        item {
            DriverTopBar(
                driverName = state.driverName,
                onProfileClick = onNavigateToProfile
            )
        }

        // 2. Greeting Header Card with Truck Graphic
        item {
            DriverHeaderCard(
                greeting = state.greeting,
                driverName = state.driverName,
                isOnline = state.isOnline
            )
        }

        // 3. Today's Trip Card
        item {
            TodayTripSection(
                todayTrip = state.todayTrip,
                assignedLorry = state.assignedLorryRegistration,
                onTripClick = { trip ->
                    onNavigateToTripDetails(trip)
                }
            )
        }

        // 4. Quick Actions (2x2 Grid)
        item {
            DriverQuickActionsSection(
                onViewMyTrips = onNavigateToTrips,
                onUpdateStatus = {
                    onNavigateToStatus(state.todayTrip)
                },
                onContactOwner = { showContactDialog = true },
                onTripDocuments = { showDocumentsDialog = true }
            )
        }

        // 5. Trip Overview (3 Metric Cards)
        item {
            DriverTripOverviewSection(
                currentCount = state.currentTripsCount,
                upcomingCount = state.upcomingTripsCount,
                completedCount = state.completedTripsCount
            )
        }
    }

    // Contact Owner Dialog
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = {
                Text(
                    text = "Contact Fleet Owner",
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Need assistance or have an update regarding your route?",
                        color = BhpTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = BhpInputBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "BHP Logistics Dispatch Office",
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Phone: +91 94400 12345",
                                color = BhpDriverBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Available 24/7 for operational support",
                                color = BhpTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showContactDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    // Trip Documents Dialog
    if (showDocumentsDialog) {
        AlertDialog(
            onDismissRequest = { showDocumentsDialog = false },
            title = {
                Text(
                    text = "Trip Documents",
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Documents attached to active trip:",
                        color = BhpTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "E-Way Bill (EWB-2026-90412)",
                        "Vehicle RC & National Permit (AP 31 TC 1234)",
                        "Consignment Delivery Challan"
                    ).forEach { doc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            DocumentTextIcon(tint = BhpDriverBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = doc, fontSize = 13.sp, color = BhpTextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDocumentsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun DriverTopBar(
    driverName: String,
    onProfileClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Left Branding
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = BhpNavyDark,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "BHP Logistics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BhpNavyDark
                )
                Text(
                    text = "Driver Portal",
                    fontSize = 12.sp,
                    color = BhpDriverBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Right Actions (Bell + Avatar)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, BhpCardBorder, CircleShape)
            ) {
                NotificationBellIcon(tint = BhpTextSecondary, hasBadge = true)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BhpDriverBlue)
                    .clickable { onProfileClick() }
            ) {
                val initial = driverName.firstOrNull()?.uppercaseChar()?.toString() ?: "D"
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun DriverHeaderCard(
    greeting: String,
    driverName: String,
    isOnline: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BhpNavyDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = driverName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                // Driver Online Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E3A5F))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline) "Driver Online" else "Driver Offline",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // High fidelity truck illustration
            CommercialTruckHeaderGraphic(
                modifier = Modifier
                    .size(110.dp, 75.dp)
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun TodayTripSection(
    todayTrip: DriverTripUiItem?,
    assignedLorry: String?,
    onTripClick: (DriverTripUiItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Today's Trip",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BhpTextPrimary
            )
            if (todayTrip != null) {
                Surface(
                    color = Color(0xFFE0F2FE),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "1 Trip",
                        color = BhpDriverBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (todayTrip != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onTripClick(todayTrip) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header row: Status chip + Reference
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isProgress = todayTrip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)
                        val chipBg = if (isProgress) BhpNotReadyAmberBg else BhpRunningGreenBg
                        val chipColor = if (isProgress) BhpNotReadyAmberText else BhpRunningGreenText
                        val statusLabel = if (isProgress) "IN PROGRESS" else "PLANNED"

                        Surface(
                            color = chipBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                color = chipColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = todayTrip.tripReference,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BhpDriverBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Route: Origin -> Destination
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LocationPinIcon(tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = todayTrip.originLocation,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "→", color = BhpTextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        LocationPinIcon(tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = todayTrip.destinationLocation,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = BhpTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lorry & Load row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DashboardTruckIcon(tint = BhpTextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val lorryText = buildString {
                            append(todayTrip.lorryRegistration)
                            if (!todayTrip.lorryMakeModel.isNullOrBlank()) {
                                append(" | ")
                                append(todayTrip.lorryMakeModel)
                            }
                        }
                        Text(
                            text = lorryText,
                            fontSize = 13.sp,
                            color = BhpTextSecondary
                        )
                    }
                }
            }
        } else {
            // Polished Empty State
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            DashboardTruckIcon(tint = BhpDriverBlue, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No trip assigned yet.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BhpTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your fleet owner has not dispatched a new trip today. Assigned lorry: ${assignedLorry ?: "Ready"}",
                        fontSize = 13.sp,
                        color = BhpTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverQuickActionsSection(
    onViewMyTrips: () -> Unit,
    onUpdateStatus: () -> Unit,
    onContactOwner: () -> Unit,
    onTripDocuments: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Quick Actions",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BhpTextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            QuickActionTile(
                title = "View My Trips",
                subtitle = "Check assigned trips",
                icon = { FormatListIcon(tint = BhpDriverBlue, modifier = Modifier.size(20.dp)) },
                onClick = onViewMyTrips,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            QuickActionTile(
                title = "Update Status",
                subtitle = "Start / Complete trip",
                icon = { PlayVectorIcon(tint = BhpDriverBlue, modifier = Modifier.size(20.dp)) },
                onClick = onUpdateStatus,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            QuickActionTile(
                title = "Contact Owner",
                subtitle = "Call / Message",
                icon = { PhoneCallVectorIcon(tint = BhpDriverBlue, modifier = Modifier.size(20.dp)) },
                onClick = onContactOwner,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            QuickActionTile(
                title = "Trip Documents",
                subtitle = "View documents",
                icon = { DocumentTextIcon(tint = BhpDriverBlue, modifier = Modifier.size(20.dp)) },
                onClick = onTripDocuments,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BhpTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = BhpTextSecondary
            )
        }
    }
}

@Composable
private fun DriverTripOverviewSection(
    currentCount: Long,
    upcomingCount: Long,
    completedCount: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Trip Overview",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BhpTextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DriverMetricCard(
                count = currentCount.toString(),
                label = "Current Trip",
                iconBg = Color(0xFFDCFCE7),
                iconColor = Color(0xFF16A34A),
                icon = { DashboardTruckIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f)
            )
            DriverMetricCard(
                count = upcomingCount.toString(),
                label = "Upcoming",
                iconBg = Color(0xFFDBEAFE),
                iconColor = Color(0xFF2563EB),
                icon = { CalendarIcon(tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f)
            )
            DriverMetricCard(
                count = completedCount.toString(),
                label = "Completed",
                iconBg = Color(0xFFF1F5F9),
                iconColor = Color(0xFF64748B),
                icon = { CheckmarkCircleIcon(tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DriverMetricCard(
    count: String,
    label: String,
    iconBg: Color,
    iconColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                color = iconBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BhpTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = BhpTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
