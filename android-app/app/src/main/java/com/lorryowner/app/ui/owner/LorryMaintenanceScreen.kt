package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LorryMaintenanceScreen(
    lorryId: Long,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember(lorryId) { LorryDetailsViewModel(lorryId, sessionManager) }
    val state = viewModel.uiState
    val lorry = state.lorry

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
                        text = "Maintenance History",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Thumbnail & Reg Number Card
            if (lorry != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                }
            }

            // Total Maintenance Cost Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🪙", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Total Maintenance Cost",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatIndianRupee(state.totalMaintenanceCost),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpDarkForestGreen,
                                fontSize = 24.sp
                            )
                        )
                    }
                }
            }

            // Maintenance Records List Section
            Text(
                text = "Maintenance Records (${state.maintenanceRecords.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary,
                    fontSize = 16.sp
                )
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BhpDarkForestGreen)
                }
            } else if (state.maintenanceRecords.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔧", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No maintenance records",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No recorded service or repairs found for this vehicle.",
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
                    items(state.maintenanceRecords, key = { it.id }) { item ->
                        MaintenanceCardItem(
                            item = item,
                            onClick = { viewModel.selectMaintenanceForDetails(item) }
                        )
                    }
                }
            }

            // Maintenance Record Details Dialog
            if (state.selectedMaintenanceForDetails != null) {
                val item = state.selectedMaintenanceForDetails!!
                AlertDialog(
                    onDismissRequest = { viewModel.selectMaintenanceForDetails(null) },
                    title = {
                        Text(item.serviceType, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📅 Date: ${item.maintenanceDate}", fontSize = 14.sp)
                            Text("💰 Cost: ${formatIndianRupee(item.cost)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BhpDarkForestGreen)
                            Text("🏢 Workshop: ${item.serviceCenter}", fontSize = 14.sp)
                            if (item.odometerReadingKm != null) {
                                Text("🛣️ Odometer: ${NumberFormat.getIntegerInstance().format(item.odometerReadingKm)} km", fontSize = 14.sp)
                            }
                            if (item.nextServiceDate != null) {
                                Text("📆 Next Service: ${item.nextServiceDate}", fontSize = 14.sp)
                            }
                            if (item.description.isNotBlank()) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Text("📝 Work Details:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(item.description, fontSize = 13.sp, color = BhpTextSecondary)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.selectMaintenanceForDetails(null) }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MaintenanceCardItem(
    item: MaintenanceItemUiState,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.maintenanceDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.serviceType,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 15.sp
                        )
                    )
                }

                Text(
                    text = formatIndianRupee(item.cost),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpDarkForestGreen,
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BhpTextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏢 ${item.serviceCenter}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                if (item.odometerReadingKm != null) {
                    Text(
                        text = "Odometer: ${NumberFormat.getIntegerInstance().format(item.odometerReadingKm)} km",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
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
        "₹ ${amount.toLong()}"
    }
}
