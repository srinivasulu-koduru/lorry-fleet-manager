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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.theme.*

@Composable
fun RunningLorriesScreen(
    isRunningFilter: Boolean = true,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onLorryClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember { MyLorriesViewModel(sessionManager) }
    val state = viewModel.uiState

    val title = if (isRunningFilter) "Running Lorries" else "Not Running Lorries"
    val lorries = if (isRunningFilter) {
        state.lorries.filter { it.running }
    } else {
        state.lorries.filter { !it.running }
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
                        Text(
                            text = "←",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.refresh() }) {
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
            onRefresh = { viewModel.refresh() },
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
            // Header count pill
            Surface(
                color = if (isRunningFilter) BhpRunningGreenBg else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRunningFilter) "🟢" else "⚪",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${lorries.size} $title",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isRunningFilter) BhpRunningGreenText else Color(0xFF334155),
                            fontSize = 13.sp
                        )
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BhpDarkForestGreen)
                }
            } else if (lorries.isEmpty()) {
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
                        Text(
                            text = if (isRunningFilter) "🚚" else "🅿️",
                            fontSize = 40.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isRunningFilter) "No lorries currently running" else "No non-running lorries",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRunningFilter)
                                "All registered fleet lorries are currently available or parked."
                            else
                                "All registered fleet lorries are actively on trips.",
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
                    items(lorries, key = { it.id }) { lorry ->
                        RunningLorryDetailedCard(
                            lorry = lorry,
                            onClick = { onLorryClick(lorry.id) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun RunningLorryDetailedCard(
    lorry: LorryCardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Truck thumbnail, Reg number, Model, Status Pill
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
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, BhpCardBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        DashboardTruckIcon(
                            color = BhpDarkForestGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
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
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                StatusPill(running = lorry.running)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Assigned Driver row
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
                    text = lorry.assignedDriverName ?: "Unassigned Driver",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = BhpTextPrimary,
                        fontSize = 14.sp
                    )
                )
            }

            // Route & Start Time if active trip
            if (lorry.running && (lorry.activeTripOrigin != null || lorry.activeTripDestination != null)) {
                Spacer(modifier = Modifier.height(8.dp))
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
                        text = "${lorry.activeTripOrigin ?: "Origin"} → ${lorry.activeTripDestination ?: "Destination"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = BhpTextPrimary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lorry.activeTripStartTime != null)
                            "Started: ${lorry.activeTripStartTime.take(10)}"
                        else "Trip active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )

                    // ETA Badge
                    Surface(
                        color = if (lorry.activeTripEtaFormatted.contains("ETA unavailable"))
                            Color(0xFFF1F5F9)
                        else
                            BhpRunningGreenBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = lorry.activeTripEtaFormatted,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (lorry.activeTripEtaFormatted.contains("ETA unavailable"))
                                    BhpTextSecondary
                                else
                                    BhpRunningGreenText,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Operational Status: ${lorry.operationalStatus}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
