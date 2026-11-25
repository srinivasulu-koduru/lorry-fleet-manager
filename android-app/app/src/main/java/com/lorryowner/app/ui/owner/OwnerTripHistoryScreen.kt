package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BhpPullToRefreshBox
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.ClockTimerIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerTripHistoryScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onNavigateToTripDetails: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember { OwnerTripHistoryViewModel(sessionManager) }
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            Surface(
                color = BhpMaroonPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trip History",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        )
                        Text(
                            text = "${state.totalElements} total trips recorded",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpGold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    IconButton(onClick = { viewModel.refreshTrips() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpBgLight)
                .padding(innerPadding)
        ) {
            // Search Bar & Filter Chips Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    // Search TextField
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search by lorry, driver, route or ID", fontSize = 13.sp, color = BhpTextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = BhpTextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = BhpTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BhpMaroonPrimary,
                            unfocusedBorderColor = BhpCardBorder,
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips Row
                    val filterScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(filterScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TripFilterChip(
                            label = "All (${state.trips.size})",
                            isSelected = state.selectedFilter == "ALL",
                            onClick = { viewModel.setFilter("ALL") }
                        )
                        TripFilterChip(
                            label = "In Progress",
                            isSelected = state.selectedFilter == "IN_PROGRESS",
                            onClick = { viewModel.setFilter("IN_PROGRESS") }
                        )
                        TripFilterChip(
                            label = "Completed",
                            isSelected = state.selectedFilter == "COMPLETED",
                            onClick = { viewModel.setFilter("COMPLETED") }
                        )
                        TripFilterChip(
                            label = "Planned",
                            isSelected = state.selectedFilter == "PLANNED",
                            onClick = { viewModel.setFilter("PLANNED") }
                        )
                        TripFilterChip(
                            label = "Cancelled",
                            isSelected = state.selectedFilter == "CANCELLED",
                            onClick = { viewModel.setFilter("CANCELLED") }
                        )
                    }
                }
            }

            // Main Content: Pull-to-refresh list
            BhpPullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refreshTrips() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.isLoading && state.trips.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BhpMaroonPrimary)
                    }
                } else if (state.errorMessage != null && state.trips.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚠️", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Failed to load trips",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BhpTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.errorMessage ?: "Unknown error",
                                    fontSize = 13.sp,
                                    color = BhpTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.loadTrips() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BhpMaroonPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (state.filteredTrips.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📋", fontSize = 38.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No trips found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BhpTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (state.searchQuery.isNotEmpty())
                                        "No trips match '${state.searchQuery}'."
                                    else
                                        "No historical trip records for the selected filter.",
                                    fontSize = 13.sp,
                                    color = BhpTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.filteredTrips, key = { it.id }) { trip ->
                            OwnerTripCardItem(
                                trip = trip,
                                onClick = { onNavigateToTripDetails(trip.id) }
                            )
                        }
                    }
                }
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
        color = if (isSelected) BhpMaroonPrimary else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) BhpMaroonPrimary else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun OwnerTripCardItem(
    trip: OwnerTripItemUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Trip Reference Badge & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = trip.tripReference,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpMaroonPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                OwnerTripStatusBadge(status = trip.tripStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Origin -> Destination Route
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LocationPinIcon(tint = BhpMaroonPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trip.originLocation,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
                Text(
                    text = "  ➔  ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BhpGold
                )
                Text(
                    text = trip.destinationLocation,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Lorry & Driver info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🚛 ",
                        fontSize = 13.sp
                    )
                    Text(
                        text = trip.lorryRegistration,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DriverCapIcon(tint = Color(0xFF0284C7), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trip.driverName,
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Divider
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Date, Revenue & View Details Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClockTimerIcon(tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trip.tripDate.ifEmpty { "Recently" },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trip.tripAmount > 0) {
                        Text(
                            text = formatIndianRupee(trip.tripAmount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = "Details",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BhpMaroonPrimary
                    )
                    ChevronRightIcon(tint = BhpMaroonPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun OwnerTripStatusBadge(status: String) {
    val (bg, text, dotColor) = when (status.uppercase()) {
        "IN_PROGRESS" -> Triple(BhpRunningGreenBg, BhpRunningGreenText, Color(0xFF16A34A))
        "COMPLETED" -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFF3B82F6))
        "CANCELLED" -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Color(0xFFEF4444))
        "PLANNED" -> Triple(Color(0xFFFFFBEB), Color(0xFFD97706), Color(0xFFF59E0B))
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Color(0xFF94A3B8))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = status.replace("_", " "),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = text
            )
        }
    }
}

private fun formatIndianRupee(amount: Double): String {
    return try {
        val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.maximumFractionDigits = 0
        formatter.format(amount)
    } catch (e: Exception) {
        "₹${amount.toLong()}"
    }
}
