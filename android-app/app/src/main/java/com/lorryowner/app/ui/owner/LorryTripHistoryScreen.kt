package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.ClockTimerIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.theme.*

@Composable
fun LorryTripHistoryScreen(
    lorryId: Long,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember(lorryId) { LorryDetailsViewModel(lorryId, sessionManager) }
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            Surface(
                color = BhpNavyDark,
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
                        Text(
                            text = "←",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Trip History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpScreenBg)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Filter chips: All, Completed, In Progress, Cancelled
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedTripFilter == "ALL",
                    onClick = { viewModel.applyTripFilter("ALL") },
                    label = { Text("All (${state.trips.size})") }
                )
                FilterChip(
                    selected = state.selectedTripFilter == "COMPLETED",
                    onClick = { viewModel.applyTripFilter("COMPLETED") },
                    label = { Text("Completed") }
                )
                FilterChip(
                    selected = state.selectedTripFilter == "IN_PROGRESS",
                    onClick = { viewModel.applyTripFilter("IN_PROGRESS") },
                    label = { Text("In Progress") }
                )
                FilterChip(
                    selected = state.selectedTripFilter == "CANCELLED",
                    onClick = { viewModel.applyTripFilter("CANCELLED") },
                    label = { Text("Cancelled") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BhpDarkForestGreen)
                }
            } else if (state.filteredTrips.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📋", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No trips found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No historical trip records match the selected filter.",
                            fontSize = 13.sp,
                            color = BhpTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredTrips, key = { it.id }) { trip ->
                        TripCardItem(
                            trip = trip,
                            onClick = { viewModel.selectTripForDetails(trip) }
                        )
                    }
                }
            }

            // Screen 6: Trip Details Dialog
            if (state.selectedTripForDetails != null) {
                TripDetailsModal(
                    trip = state.selectedTripForDetails!!,
                    onDismiss = { viewModel.selectTripForDetails(null) }
                )
            }
        }
    }
}

@Composable
fun TripCardItem(
    trip: TripItemUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Reference & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trip.tripReference,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 15.sp
                        )
                    )
                }
                TripStatusBadge(status = trip.tripStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Origin -> Destination
            Text(
                text = "${trip.originLocation} → ${trip.destinationLocation}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BhpTextPrimary,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Date & Distance & Driver
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClockTimerIcon(
                            tint = BhpTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = trip.tripDate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                        if (trip.distanceKm != null) {
                            Text(
                                text = " • ${trip.distanceKm} km",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DriverCapIcon(
                            tint = BhpTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = trip.driverName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                ChevronRightIcon(
                    tint = BhpTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TripStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "COMPLETED" -> Triple(BhpRunningGreenBg, BhpRunningGreenText, "Completed")
        "IN_PROGRESS" -> Triple(BhpReadyBlueBg, BhpReadyBlueText, "In Progress")
        "CANCELLED" -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "Cancelled")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TripDetailsModal(
    trip: TripItemUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trip.tripReference,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TripStatusBadge(status = trip.tripStatus)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📍 ${trip.originLocation} → ${trip.destinationLocation}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = BhpTextPrimary
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text("📅 Date: ${trip.tripDate}", fontSize = 13.sp, color = BhpTextSecondary)
                if (trip.distanceKm != null) {
                    Text("🛣️ Distance: ${trip.distanceKm} km", fontSize = 13.sp, color = BhpTextSecondary)
                }
                Text("👤 Driver: ${trip.driverName}", fontSize = 13.sp, color = BhpTextSecondary)
                Text("🚚 Lorry: ${trip.lorryRegistration}", fontSize = 13.sp, color = BhpTextSecondary)

                HorizontalDivider(color = Color(0xFFF1F5F9))
                Text("Trip Timeline", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BhpTextPrimary)

                Text(
                    text = "🟢 Started: ${trip.startTime?.take(16)?.replace("T", " ") ?: "Not recorded"}",
                    fontSize = 12.sp,
                    color = BhpTextSecondary
                )
                Text(
                    text = if (trip.tripStatus == "COMPLETED")
                        "🏁 Reached Destination: ${trip.endTime?.take(16)?.replace("T", " ") ?: "Not recorded"}"
                    else
                        "⏳ In Progress",
                    fontSize = 12.sp,
                    color = BhpTextSecondary
                )

                if (trip.plannedDeliveryTime != null) {
                    Text(
                        text = "🎯 Planned Delivery: ${trip.plannedDeliveryTime.take(16).replace("T", " ")}",
                        fontSize = 12.sp,
                        color = BhpTextSecondary
                    )
                }

                if (trip.deliveryNotes != null && trip.deliveryNotes.isNotBlank()) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Text("📝 Notes:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(trip.deliveryNotes, fontSize = 12.sp, color = BhpTextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BhpDarkForestGreen)
            ) {
                Text("Close")
            }
        }
    )
}
