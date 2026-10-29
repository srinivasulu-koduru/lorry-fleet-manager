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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.theme.*

@Composable
fun MaintenanceDashboardScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onViewVehicleDetails: (Long) -> Unit = {},
    onAddMaintenanceClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember { MaintenanceDashboardViewModel(sessionManager = sessionManager) }
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
                        .padding(horizontal = 12.dp, vertical = 14.dp),
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Maintenance",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manage vehicle servicing and repairs",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onAddMaintenanceClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Add Maintenance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
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
                .background(BhpScreenBg)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 1. Top 3 Metric Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Vehicles Card (White)
                MaintenanceMetricCard(
                    title = "Total Vehicles",
                    count = String.format("%02d", state.totalVehicles),
                    containerColor = Color.White,
                    borderColor = BhpCardBorder,
                    textColor = BhpTextPrimary,
                    countColor = BhpTextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Due Soon Card (Light Amber)
                MaintenanceMetricCard(
                    title = "Due Soon:",
                    count = String.format("%02d", state.dueSoonCount),
                    containerColor = Color(0xFFFEF3C7),
                    borderColor = Color(0xFFFDE68A),
                    textColor = Color(0xFFB45309),
                    countColor = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )

                // Overdue Card (Light Red)
                MaintenanceMetricCard(
                    title = "Overdue:",
                    count = String.format("%02d", state.overdueCount),
                    containerColor = Color(0xFFFEE2E2),
                    borderColor = Color(0xFFFECACA),
                    textColor = Color(0xFFB91C1C),
                    countColor = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Vehicles Maintenance List
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF16A34A))
                }
            } else if (state.errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadSummary() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else if (state.vehicles.isEmpty()) {
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
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                DashboardTruckIcon(tint = BhpDriverBlue, modifier = Modifier.size(26.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Vehicles Registered",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Register a lorry to start tracking maintenance and scheduled servicing.",
                            fontSize = 13.sp,
                            color = BhpTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.vehicles, key = { it.lorryId }) { item ->
                        VehicleMaintenanceCard(
                            item = item,
                            onViewDetailsClick = { onViewVehicleDetails(item.lorryId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceMetricCard(
    title: String,
    count: String,
    containerColor: Color,
    borderColor: Color,
    textColor: Color,
    countColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count,
                fontSize = 26.sp,
                color = countColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VehicleMaintenanceCard(
    item: VehicleMaintenanceSummaryItem,
    onViewDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Vehicle Thumbnail + Reg + Make
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        DashboardTruckIcon(tint = Color(0xFF1E3A5F), modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.registrationNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BhpTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.makeModel,
                        fontSize = 13.sp,
                        color = BhpTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Last Service row
            if (!item.lastServiceDate.isNullOrBlank()) {
                Text(
                    text = "Last Service: ${MaintenanceFormattingHelper.formatDisplayDate(item.lastServiceDate)}",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Next Service row
            if (!item.nextServiceDate.isNullOrBlank()) {
                Text(
                    text = "Next Service: ${MaintenanceFormattingHelper.formatDisplayDate(item.nextServiceDate)}",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Status row + View Details Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusColor = when (item.status) {
                    "Overdue" -> Color(0xFFDC2626)
                    "Due Soon" -> Color(0xFFD97706)
                    else -> Color(0xFF16A34A)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Status: ",
                        fontSize = 13.sp,
                        color = BhpTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${item.status} —",
                        fontSize = 13.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onViewDetailsClick() }
                ) {
                    Text(
                        text = "View Details",
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ChevronRightIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
