package com.lorryowner.app.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BackArrowIcon
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.components.DistancePinIcon
import com.lorryowner.app.ui.components.TripRouteIcon
import com.lorryowner.app.ui.theme.*

@Composable
fun DriverTripHistoryScreen(
    driverId: Long,
    driverName: String,
    driverUsername: String,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSession = remember { sessionManager ?: SessionManager(context) }
    val viewModel = remember(driverId) {
        DriverTripHistoryViewModel(driverId, driverName, driverUsername, effectiveSession)
    }
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TripHistoryTopBar(
                driverName = driverName,
                driverUsername = driverUsername,
                onBack = onBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpScreenBg)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Performance Summary Cards (2x2 Grid)
                    TripPerformanceSummaryGrid(performance = state.performance)

                    Spacer(modifier = Modifier.height(18.dp))

                    // Section Title: "Trip History"
                    Text(
                        text = "Trip History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search & Filter Input Controls
                    TripSearchAndFilterControls(
                        searchQuery = state.searchQuery,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        selectedFilter = state.selectedStatusFilter,
                        onFilterSelect = { viewModel.onStatusFilterSelected(it) },
                        totalCount = state.allTrips.size,
                        filteredCount = state.trips.size,
                        onClearFilters = { viewModel.clearFilters() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Loading State
                if (state.isLoading && state.allTrips.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = BhpDriverBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading trip records...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = BhpTextSecondary)
                                )
                            }
                        }
                    }
                }

                // Error State
                if (state.errorMessage != null && state.allTrips.isEmpty()) {
                    item {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "Could not load trip records",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF7F1D1D),
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.loadData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Empty State (No Trips Recorded or Filters Empty)
                if (!state.isLoading && state.errorMessage == null && state.trips.isEmpty()) {
                    item {
                        Surface(
                            color = BhpCardWhite,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BhpCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(28.dp)
                            ) {
                                Surface(
                                    color = BhpDriverMetricBg,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        TripRouteIcon(tint = BhpDriverBlue, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (state.searchQuery.isNotBlank() || state.selectedStatusFilter != TripStatusFilter.ALL)
                                        "No Matching Trips"
                                    else
                                        "No Historical Trips Recorded",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BhpTextPrimary,
                                        fontSize = 17.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (state.searchQuery.isNotBlank() || state.selectedStatusFilter != TripStatusFilter.ALL)
                                        "No trips match your current filter criteria. Try resetting filters."
                                    else
                                        "Completed and in-progress trips for this commercial driver will appear here.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BhpTextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                )
                                if (state.searchQuery.isNotBlank() || state.selectedStatusFilter != TripStatusFilter.ALL) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.clearFilters() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Clear Filters", color = BhpDriverBlue, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Trip List Items
                items(state.trips, key = { it.id }) { trip ->
                    TripCard(
                        trip = trip,
                        onViewDetails = { viewModel.showTripDetails(trip) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Trip Details Dialog
    if (state.selectedTripForDetails != null) {
        TripDetailsDialog(
            trip = state.selectedTripForDetails,
            onDismiss = { viewModel.dismissTripDetails() }
        )
    }
}

@Composable
private fun TripHistoryTopBar(
    driverName: String,
    driverUsername: String,
    onBack: () -> Unit
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
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                BackArrowIcon(tint = BhpTextPrimary, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Driver Trip History",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = "$driverName (@$driverUsername)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun TripPerformanceSummaryGrid(performance: DriverPerformanceUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TripMetricCard(
                title = "Total Trips",
                primaryValue = "${performance.totalTrips}",
                secondaryLabel = "Completed: ${performance.completedTrips}",
                accentColor = BhpDriverBlue,
                modifier = Modifier.weight(1f)
            )
            TripMetricCard(
                title = "Recorded Distance",
                primaryValue = performance.totalRecordedDistanceKm,
                secondaryLabel = "Verified transit",
                accentColor = BhpDarkForestGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TripMetricCard(
                title = "In Progress / Active",
                primaryValue = "${performance.inProgressTrips}",
                secondaryLabel = "Cancelled: ${performance.cancelledTrips}",
                accentColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
            TripMetricCard(
                title = "Distinct Lorries",
                primaryValue = "${performance.distinctLorriesUsed}",
                secondaryLabel = if (performance.mostRecentTripReference != null) "Latest: ${performance.mostRecentTripReference}" else "No trips",
                accentColor = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TripMetricCard(
    title: String,
    primaryValue: String,
    secondaryLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BhpCardWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BhpCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BhpTextSecondary,
                    fontSize = 11.5.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BhpTextMuted,
                    fontSize = 10.5.sp
                )
            )
        }
    }
}

@Composable
private fun TripSearchAndFilterControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: TripStatusFilter,
    onFilterSelect: (TripStatusFilter) -> Unit,
    totalCount: Int,
    filteredCount: Int,
    onClearFilters: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by origin, destination, lorry...", fontSize = 13.sp, color = BhpTextMuted) },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Text("✕", color = BhpTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BhpDriverBlue,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Status Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            TripFilterChip(
                label = "All",
                isSelected = selectedFilter == TripStatusFilter.ALL,
                onClick = { onFilterSelect(TripStatusFilter.ALL) }
            )
            TripFilterChip(
                label = "Completed",
                isSelected = selectedFilter == TripStatusFilter.COMPLETED,
                onClick = { onFilterSelect(TripStatusFilter.COMPLETED) }
            )
            TripFilterChip(
                label = "In Progress",
                isSelected = selectedFilter == TripStatusFilter.IN_PROGRESS,
                onClick = { onFilterSelect(TripStatusFilter.IN_PROGRESS) }
            )
            TripFilterChip(
                label = "Cancelled",
                isSelected = selectedFilter == TripStatusFilter.CANCELLED,
                onClick = { onFilterSelect(TripStatusFilter.CANCELLED) }
            )
        }

        // Results counter and clear link
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Showing $filteredCount of $totalCount trips",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BhpTextSecondary,
                    fontSize = 11.5.sp
                )
            )
            if (searchQuery.isNotBlank() || selectedFilter != TripStatusFilter.ALL) {
                Text(
                    text = "Reset Filters",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpDriverBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    modifier = Modifier.clickable { onClearFilters() }
                )
            }
        }
    }
}

@Composable
private fun TripFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) BhpDriverBlue else Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) BhpDriverBlue else Color(0xFFCBD5E1)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else BhpTextPrimary,
                fontSize = 11.5.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TripCard(
    trip: TripItemUiState,
    onViewDetails: () -> Unit
) {
    Surface(
        color = BhpCardWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BhpCardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Reference code and Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "#${trip.tripReference}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 15.sp
                    )
                )

                TripStatusBadge(status = trip.tripStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Origin -> Destination Route
            Row(verticalAlignment = Alignment.CenterVertically) {
                TripRouteIcon(tint = BhpDriverBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "${trip.originLocation} → ${trip.destinationLocation}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BhpTextPrimary,
                            fontSize = 13.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Historical Lorry & Distance Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DashboardTruckIcon(tint = BhpTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lorry: ${trip.lorryRegistration}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpDriverBlue,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DistancePinIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trip.distanceKm ?: "Distance not recorded",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (trip.distanceKm != null) BhpTextPrimary else BhpTextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date and Timing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Date: ${trip.tripDate}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextSecondary,
                        fontSize = 11.5.sp
                    )
                )

                TextButton(
                    onClick = onViewDetails,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "View Details",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpDriverBlue,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TripStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "COMPLETED" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Completed")
        "IN_PROGRESS" -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), "In Progress")
        "CANCELLED" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Cancelled")
        else -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TripDetailsDialog(
    trip: TripItemUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BhpCardWhite,
        titleContentColor = BhpTextPrimary,
        textContentColor = BhpTextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TripRouteIcon(tint = BhpDriverBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Trip Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp)
            ) {
                TripDetailRow("Trip Reference", "#${trip.tripReference}")
                TripDetailRow("Status", trip.tripStatus)
                TripDetailRow("Origin", trip.originLocation)
                TripDetailRow("Destination", trip.destinationLocation)
                TripDetailRow("Historical Lorry", "${trip.lorryRegistration}${if (trip.lorryMakeModel != null) " (${trip.lorryMakeModel})" else ""}")
                TripDetailRow("Trip Date", trip.tripDate)
                TripDetailRow("Start Time", trip.startTime ?: "Not recorded")
                TripDetailRow("Completion Time", trip.endTime ?: "Not recorded")
                TripDetailRow("Recorded Distance", trip.distanceKm ?: "Not recorded")
                if (!trip.deliveryNotes.isNullOrBlank()) {
                    TripDetailRow("Delivery / Cargo Notes", trip.deliveryNotes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = BhpDriverBlue)
            }
        }
    )
}

@Composable
private fun TripDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = BhpTextSecondary,
                fontSize = 11.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = BhpTextPrimary,
                fontSize = 13.sp
            )
        )
    }
}
