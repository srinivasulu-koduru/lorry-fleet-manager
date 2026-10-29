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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.ui.components.*
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
            .background(BhpScreenBg)
    ) {
        // Top App Bar
        DriverTopAppBar(
            title = "My Trips",
            onBack = onBack
        )

        // Segmented Tabs (Current, Upcoming, Completed)
        DriverTripsTabRow(
            selectedTab = selectedTab,
            onTabSelected = { viewModel.selectTripsTab(it) }
        )

        // Trips List
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

@Composable
fun DriverTopAppBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                BackArrowIcon(tint = BhpTextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BhpTextPrimary,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}

@Composable
private fun DriverTripsTabRow(
    selectedTab: DriverTripsTab,
    onTabSelected: (DriverTripsTab) -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            DriverTripsTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = tab.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (isSelected) BhpDriverBlue else BhpTextSecondary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (isSelected) BhpDriverBlue else Color.Transparent)
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

    val (chipBg, chipColor, chipLabel) = when {
        isProgress -> Triple(BhpNotReadyAmberBg, BhpNotReadyAmberText, "IN PROGRESS")
        isCompleted -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "COMPLETED")
        else -> Triple(BhpRunningGreenBg, BhpRunningGreenText, "PLANNED")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Status badge + Reference + Chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = chipBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = chipLabel,
                        color = chipColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trip.tripReference,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BhpDriverBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ChevronRightIcon(tint = BhpTextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date Range
            Row(verticalAlignment = Alignment.CenterVertically) {
                CalendarIcon(tint = BhpTextSecondary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trip.tripDate.ifEmpty { "Active Schedule" },
                    fontSize = 12.sp,
                    color = BhpTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Route
            Row(verticalAlignment = Alignment.CenterVertically) {
                LocationPinIcon(tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trip.originLocation,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = BhpTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "→", color = BhpTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                LocationPinIcon(tint = Color(0xFF3B82F6), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trip.destinationLocation,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = BhpTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lorry registration
            Row(verticalAlignment = Alignment.CenterVertically) {
                DashboardTruckIcon(tint = BhpTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val lorryText = buildString {
                    append(trip.lorryRegistration)
                    if (!trip.lorryMakeModel.isNullOrBlank()) {
                        append(" | ")
                        append(trip.lorryMakeModel)
                    }
                }
                Text(
                    text = lorryText,
                    fontSize = 12.sp,
                    color = BhpTextSecondary
                )
            }

            // Driver row
            if (trip.driverName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DriverCapIcon(tint = BhpTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trip.driverName,
                        fontSize = 12.sp,
                        color = BhpTextSecondary
                    )
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
                color = Color(0xFFEFF6FF),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DashboardTruckIcon(tint = BhpDriverBlue, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            val message = when (tab) {
                DriverTripsTab.CURRENT -> "No trips currently in progress"
                DriverTripsTab.UPCOMING -> "No upcoming trips scheduled"
                DriverTripsTab.COMPLETED -> "No completed trips on record"
            }
            Text(
                text = message,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BhpTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When trips are assigned by the fleet owner, they will appear here.",
                fontSize = 13.sp,
                color = BhpTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
