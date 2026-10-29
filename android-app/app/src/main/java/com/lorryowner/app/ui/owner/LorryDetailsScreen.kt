package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.components.WrenchToolIcon
import com.lorryowner.app.ui.theme.*

@Composable
fun LorryDetailsScreen(
    lorryId: Long,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onNavigateToTripHistory: () -> Unit = {},
    onNavigateToMaintenanceHistory: () -> Unit = {},
    onNavigateToTripSummary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember(lorryId) { LorryDetailsViewModel(lorryId, sessionManager) }
    val state = viewModel.uiState
    val lorry = state.lorry

    var isCurrentTripDialogVisible by remember { mutableStateOf(false) }

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
                        text = "Lorry Details",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpScreenBg)
                .padding(innerPadding)
        ) {
            if (state.isLoading && lorry == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BhpDarkForestGreen)
                }
            } else if (lorry != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION A: OVERVIEW CARD
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Truck banner & Reg info
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
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .border(1.dp, BhpCardBorder, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DashboardTruckIcon(
                                            color = BhpDarkForestGreen,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = lorry.registrationNumber,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BhpTextPrimary,
                                                fontSize = 20.sp
                                            )
                                        )
                                        Text(
                                            text = lorry.makeModel,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = BhpTextSecondary,
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                }
                                StatusPill(running = lorry.running)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Assigned Driver
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DriverCapIcon(
                                        tint = BhpDarkForestGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Assigned Driver",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BhpTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = lorry.assignedDriverName ?: "No driver assigned",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BhpTextPrimary,
                                            fontSize = 14.sp
                                        )
                                    )
                                    if (lorry.assignedDriverPhone != null) {
                                        Text(
                                            text = "📞 ${lorry.assignedDriverPhone}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BhpTextSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // Current Trip Block if running
                            if (lorry.running && (lorry.activeTripOrigin != null || lorry.activeTripDestination != null)) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(12.dp),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                LocationPinIcon(
                                                    tint = BhpRunningGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Current Trip",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = BhpTextSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }
                                            Surface(
                                                color = BhpRunningGreenBg,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = lorry.activeTripEtaFormatted,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = BhpRunningGreenText,
                                                        fontSize = 11.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "${lorry.activeTripOrigin} → ${lorry.activeTripDestination}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BhpTextPrimary,
                                                fontSize = 14.sp
                                            )
                                        )
                                        if (lorry.activeTripStartTime != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Started: ${lorry.activeTripStartTime.take(16).replace("T", " ")}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = BhpTextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { isCurrentTripDialogVisible = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = BhpDarkForestGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("View Trip Details", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // QUICK STATS ROW
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickStatColumn(title = "Total Trips", value = "${lorry.totalTrips}")
                            QuickStatColumn(title = "Completed", value = "${lorry.completedTrips}", valueColor = BhpRunningGreenText)
                            QuickStatColumn(title = "In Progress", value = "${lorry.inProgressTrips}", valueColor = BhpReadyBlueText)
                            QuickStatColumn(title = "Cancelled", value = "${lorry.cancelledTrips}", valueColor = BhpErrorRed)
                        }
                    }

                    // SECTION B, C, D NAVIGATION MENU ITEMS
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Trip History Row
                            LorryDetailMenuItem(
                                iconText = "📋",
                                title = "Trip History",
                                subtitle = "View past trips and routes",
                                onClick = onNavigateToTripHistory
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 2. Maintenance History Row
                            LorryDetailMenuItem(
                                iconText = "🔧",
                                title = "Maintenance History",
                                subtitle = "Service & maintenance records",
                                onClick = onNavigateToMaintenanceHistory
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 3. Trip Summary Row
                            LorryDetailMenuItem(
                                iconText = "📊",
                                title = "Trip Summary",
                                subtitle = "View detailed statistics",
                                onClick = onNavigateToTripSummary
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "Lorry not found",
                        color = BhpErrorRed,
                        fontSize = 15.sp
                    )
                }
            }

            // Current Trip Details Dialog
            if (isCurrentTripDialogVisible && lorry != null) {
                AlertDialog(
                    onDismissRequest = { isCurrentTripDialogVisible = false },
                    title = {
                        Text(
                            text = "Active Trip: ${lorry.activeTripReference ?: "Ongoing"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📍 Route: ${lorry.activeTripOrigin} → ${lorry.activeTripDestination}", fontSize = 14.sp)
                            Text("👤 Driver: ${lorry.assignedDriverName ?: "Unassigned"}", fontSize = 14.sp)
                            Text("🚚 Lorry: ${lorry.registrationNumber} (${lorry.makeModel})", fontSize = 14.sp)
                            Text("⏳ Status: In Progress (${lorry.activeTripEtaFormatted})", fontSize = 14.sp)
                            if (lorry.activeTripStartTime != null) {
                                Text("🕒 Started: ${lorry.activeTripStartTime.take(16).replace("T", " ")}", fontSize = 14.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { isCurrentTripDialogVisible = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun QuickStatColumn(
    title: String,
    value: String,
    valueColor: Color = BhpTextPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = BhpTextSecondary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = valueColor
            )
        )
    }
}

@Composable
fun LorryDetailMenuItem(
    iconText: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconText, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = BhpTextPrimary,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
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
