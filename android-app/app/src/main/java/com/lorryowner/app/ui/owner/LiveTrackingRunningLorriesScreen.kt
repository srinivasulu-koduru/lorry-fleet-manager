package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BhpPullToRefreshBox
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.theme.*

/**
 * Live Tracking running lorries screen.
 * Displays only running lorries with active trips, real GPS metrics,
 * and a "VIEW LIVE LOCATION" button to enter the real satellite map.
 */
@Composable
fun LiveTrackingRunningLorriesScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onViewLiveLocation: (lorryId: Long, tripId: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val viewModel = remember { RunningLorriesViewModel(sessionManager) }
    val state = viewModel.uiState

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

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
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Tracking",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        )
                        Text(
                            text = "Running lorries on active trips",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        )
                    }
                    IconButton(onClick = { viewModel.loadRunningLorries() }) {
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
        BhpPullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadRunningLorries() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BhpScreenBg)
                    .padding(16.dp)
            ) {
            // Count pill
            Surface(
                color = BhpRunningGreenBg,
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.lorries.size} Running on Active Trips",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpRunningGreenText,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            if (state.isLoading && state.lorries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BhpDarkForestGreen)
                }
            } else if (state.lorries.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🚚", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No lorries currently running",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When a driver starts an assigned trip, the vehicle will appear here with live GPS coordinates.",
                            fontSize = 13.sp,
                            color = BhpTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.lorries, key = { it.lorryId }) { lorry ->
                        LiveTrackingLorryCard(
                            lorry = lorry,
                            onViewLiveLocation = { onViewLiveLocation(lorry.lorryId, lorry.tripId) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun LiveTrackingLorryCard(
    lorry: LiveTrackingLorryUiState,
    onViewLiveLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Vehicle Reg, Model, and GPS Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, BhpCardBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        DashboardTruckIcon(
                            color = BhpDarkForestGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = lorry.registrationNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = lorry.makeModel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                GpsStatusBadge(status = lorry.gpsStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Assigned Driver
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DriverCapIcon(
                    tint = BhpDarkForestGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = lorry.driverName.ifBlank { "Assigned Driver" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = BhpTextPrimary,
                        fontSize = 13.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Starting Location -> Destination
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationPinIcon(
                    tint = BhpRunningGreen,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${lorry.originLocation} → ${lorry.destinationLocation}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = BhpTextPrimary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row: Speed, Distance Travelled, Remaining, ETA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Speed", fontSize = 10.sp, color = BhpTextSecondary)
                    Text(
                        text = if (lorry.hasGpsFix) TrackingFormattingHelper.formatSpeed(lorry.currentSpeedKmh) else "--",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                }
                Column {
                    Text(text = "Travelled", fontSize = 10.sp, color = BhpTextSecondary)
                    Text(
                        text = TrackingFormattingHelper.formatDistance(lorry.distanceTravelledKm),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                }
                Column {
                    Text(text = "Remaining", fontSize = 10.sp, color = BhpTextSecondary)
                    Text(
                        text = TrackingFormattingHelper.formatDistance(lorry.remainingDistanceKm),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                }
                Column {
                    Text(text = "ETA", fontSize = 10.sp, color = BhpTextSecondary)
                    Text(
                        text = lorry.estimatedDrivingTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location Fix Text & Last Updated
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lorry.hasGpsFix) lorry.currentLocationDescription else "Waiting for driver location",
                    fontSize = 11.sp,
                    color = if (lorry.hasGpsFix) BhpTextSecondary else Color(0xFFE11D48),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full width VIEW LIVE LOCATION button
            Button(
                onClick = onViewLiveLocation,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "VIEW LIVE LOCATION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}
