package com.lorryowner.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.components.BhpPullToRefreshBox
import com.lorryowner.app.ui.theme.*

@Composable
fun DriverTripsScreen(
    viewModel: DriverDashboardViewModel,
    onBack: () -> Unit,
    onTripClick: (DriverTripUiItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab = viewModel.selectedTripsTab
    val trips = viewModel.getFilteredTrips()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // 1. Top App Bar
        DriverTripsTopBar(
            title = "My Trips",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { viewModel.loadDashboardData(isRefresh = true) },
                    enabled = !viewModel.dashboardState.isRefreshing
                ) {
                    if (viewModel.dashboardState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF10B981)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Trips",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )

        // 2. 4 Organized Tabs: Active, Upcoming, Completed, Cancelled
        DriverTripsTabRow(
            selectedTab = selectedTab,
            onTabSelected = { viewModel.selectTripsTab(it) }
        )

        // 3. Trips List with Pull-to-refresh
        BhpPullToRefreshBox(
            isRefreshing = viewModel.dashboardState.isRefreshing,
            onRefresh = { viewModel.loadDashboardData(isRefresh = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            if (trips.isEmpty()) {
                DriverTripsEmptyState(tab = selectedTab)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(trips, key = { it.id }) { trip ->
                        DriverTripCard(
                            trip = trip,
                            onClick = { onTripClick(trip) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverTripsTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFFE2E8F0))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                BackArrowIcon(tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}

@Composable
fun DriverTopAppBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    DriverTripsTopBar(title = title, onBack = onBack, actions = actions)
}

@Composable
private fun DriverTripsTabRow(
    selectedTab: DriverTripsTab,
    onTabSelected: (DriverTripsTab) -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DriverTripsTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFECFDF5) else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = tab.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isSelected) Color(0xFF15803D) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun DriverTripCard(
    trip: DriverTripUiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProgress = trip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)
    val isCompleted = trip.tripStatus.equals("COMPLETED", ignoreCase = true)
    val isPlanned = trip.tripStatus.equals("PLANNED", ignoreCase = true)
    val isCancelled = trip.tripStatus.equals("CANCELLED", ignoreCase = true)

    val (chipBg, chipColor, chipLabel) = when {
        isProgress -> Triple(Color(0xFFECFDF5), Color(0xFF15803D), "IN PROGRESS")
        isCompleted -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "COMPLETED")
        isPlanned -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), "SCHEDULED")
        isCancelled -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), "CANCELLED")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), trip.tripStatus.uppercase())
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Reference + Status Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = trip.tripReference,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )

                Surface(
                    color = chipBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = chipLabel,
                        color = chipColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle Row: Lorry registration & Model
            Row(verticalAlignment = Alignment.CenterVertically) {
                DashboardTruckIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val lorryText = buildString {
                    append(trip.lorryRegistration)
                    if (!trip.lorryMakeModel.isNullOrBlank()) {
                        append(" • ")
                        append(trip.lorryMakeModel)
                    }
                }
                Text(
                    text = lorryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Route: Source -> Destination
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trip.originLocation,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    LocationPinIcon(tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trip.destinationLocation,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Info: Date & View Details Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalendarIcon(tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trip.tripDate.ifEmpty { "Active Schedule" },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Details",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ChevronRightIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun DriverTripsEmptyState(tab: DriverTripsTab) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = Color(0xFFF1F5F9),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DashboardTruckIcon(tint = Color(0xFF64748B), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            val message = when (tab) {
                DriverTripsTab.CURRENT -> "No trips currently in progress"
                DriverTripsTab.UPCOMING -> "No upcoming trips scheduled"
                DriverTripsTab.COMPLETED -> "No completed trips on record"
                DriverTripsTab.CANCELLED -> "No cancelled trips on record"
            }
            Text(
                text = message,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When trips are assigned by the fleet owner, they will appear here.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}
