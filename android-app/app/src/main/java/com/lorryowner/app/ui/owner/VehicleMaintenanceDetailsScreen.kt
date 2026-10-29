package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.CommercialTruckHeaderGraphic
import com.lorryowner.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VehicleMaintenanceDetailsScreen(
    lorryId: Long,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onAddServiceRecordClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember(lorryId) {
        VehicleMaintenanceDetailsViewModel(lorryId = lorryId, sessionManager = sessionManager)
    }
    val state = viewModel.uiState
    var selectedRecordForDialog by remember { mutableStateOf<MaintenanceHistoryRecordItem?>(null) }

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
                    Text(
                        text = "Vehicle Maintenance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
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
                        onClick = { onAddServiceRecordClick(lorryId) },
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
                            text = "Add Service Record",
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
            // 1. Vehicle Info Top Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top: Truck Illustration + Reg + Make
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommercialTruckHeaderGraphic(
                            modifier = Modifier
                                .size(width = 110.dp, height = 70.dp)
                                .padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Registration",
                                fontSize = 11.sp,
                                color = BhpTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = state.registrationNumber.ifEmpty { "Vehicle #$lorryId" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Make",
                                fontSize = 11.sp,
                                color = BhpTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = state.makeModel.ifEmpty { "Commercial Lorry" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Row inside Card: Last Service | Next Service | Total Cost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Last Service
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Last Service",
                                fontSize = 11.sp,
                                color = BhpTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = MaintenanceFormattingHelper.formatDisplayDate(state.lastServiceDate),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary
                            )
                        }

                        // Next Service
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next Service",
                                fontSize = 11.sp,
                                color = BhpTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = MaintenanceFormattingHelper.formatDisplayDate(state.nextServiceDate),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary
                            )
                        }

                        // Total Maintenance
                        Column(
                            modifier = Modifier.weight(1.2f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Total Maintenance",
                                fontSize = 11.sp,
                                color = BhpTextSecondary
                            )
                            Text(
                                text = "Total Maintenance Cost",
                                fontSize = 10.sp,
                                color = BhpTextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = MaintenanceFormattingHelper.formatIndianRupee(state.totalMaintenanceCost),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Service History Title
            Text(
                text = "Service History",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = BhpTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Service History Cards List
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF16A34A))
                }
            } else if (state.records.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BhpCardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Service History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No maintenance records logged yet for this vehicle.",
                            fontSize = 13.sp,
                            color = BhpTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(state.records, key = { _, r -> r.id }) { index, record ->
                        ServiceHistoryCard(
                            recordNumber = index + 1,
                            item = record,
                            onClick = { selectedRecordForDialog = record }
                        )
                    }
                }
            }
        }
    }

    // Record details inspection dialog
    if (selectedRecordForDialog != null) {
        val record = selectedRecordForDialog!!
        AlertDialog(
            onDismissRequest = { selectedRecordForDialog = null },
            title = {
                Text(
                    text = record.serviceType,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = BhpTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Service Date: ${MaintenanceFormattingHelper.formatDisplayDate(record.maintenanceDate)}", fontSize = 13.sp)
                    Text("Cost: ${MaintenanceFormattingHelper.formatIndianRupee(record.cost)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    if (record.odometerReadingKm != null) {
                        val odoStr = NumberFormat.getIntegerInstance(Locale.US).format(record.odometerReadingKm)
                        Text("Odometer: $odoStr km", fontSize = 13.sp)
                    }
                    if (!record.serviceCenter.isNullOrBlank()) {
                        Text("Workshop: ${record.serviceCenter}", fontSize = 13.sp)
                    }
                    if (!record.nextServiceDate.isNullOrBlank()) {
                        Text("Next Service: ${MaintenanceFormattingHelper.formatDisplayDate(record.nextServiceDate)}", fontSize = 13.sp)
                    }
                    if (record.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Description:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(record.description, fontSize = 12.sp, color = BhpTextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRecordForDialog = null }) {
                    Text("Close", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ServiceHistoryCard(
    recordNumber: Int,
    item: MaintenanceHistoryRecordItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BhpCardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Record $recordNumber",
                fontSize = 11.sp,
                color = BhpTextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.serviceType,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BhpTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Date | Odometer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date: ${MaintenanceFormattingHelper.formatDisplayDate(item.maintenanceDate)}",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )

                val odoFormatted = if (item.odometerReadingKm != null) {
                    NumberFormat.getIntegerInstance(Locale.US).format(item.odometerReadingKm)
                } else "N/A"
                Text(
                    text = "Odometer: $odoFormatted km",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Cost | Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cost: ${MaintenanceFormattingHelper.formatIndianRupee(item.cost)}",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Status: ",
                        fontSize = 13.sp,
                        color = BhpTextSecondary
                    )
                    Text(
                        text = "Completed",
                        fontSize = 13.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
