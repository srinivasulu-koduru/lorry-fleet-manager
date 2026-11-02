package com.lorryowner.app.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lorryowner.app.R
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.CoinsStackIcon
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

    // Explicitly reload vehicle maintenance details whenever screen enters composition
    LaunchedEffect(lorryId) {
        viewModel.loadDetails()
    }

    // Lifecycle observer to automatically reload fresh maintenance data on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDetails()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "←",
                            color = Color(0xFF0F172A),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lorry Maintenance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFF0F172A),
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
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { onAddServiceRecordClick(lorryId) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Maintenance",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Vehicle Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Commercial Truck Photo Thumbnail
                        Image(
                            painter = painterResource(id = R.drawable.bhp_truck_highway),
                            contentDescription = "Lorry Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 80.dp, height = 64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.registrationNumber.ifEmpty { "Vehicle #$lorryId" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.makeModel.ifEmpty { "Commercial Lorry" },
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // 2. Total Maintenance Spent Card (Mint Light-Green)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coin Stack Icon
                        CoinsStackIcon(
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(46.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TOTAL MAINTENANCE SPENT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MaintenanceFormattingHelper.formatIndianRupee(state.totalMaintenanceCost),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "All recorded maintenance",
                                fontSize = 12.sp,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            // 3. Section Header: MAINTENANCE HISTORY
            item {
                Text(
                    text = "MAINTENANCE HISTORY",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // 4. Maintenance History Records List
            if (state.isLoading && state.records.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF16A34A))
                    }
                }
            } else if (state.records.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Maintenance History",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No maintenance records logged yet for this vehicle.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                items(state.records, key = { it.id }) { record ->
                    MaintenanceHistoryRecordCard(
                        record = record,
                        onClick = { selectedRecordForDialog = record }
                    )
                }
            }
        }
    }

    // Record details inspection dialog
    if (selectedRecordForDialog != null) {
        val record = selectedRecordForDialog!!
        AlertDialog(
            onDismissRequest = { selectedRecordForDialog = null },
            containerColor = Color.White,
            titleContentColor = Color(0xFF0F172A),
            textContentColor = Color(0xFF475569),
            title = {
                Text(
                    text = record.serviceType,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Service Date: ${MaintenanceFormattingHelper.formatDisplayDate(record.maintenanceDate)}",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Cost: ${MaintenanceFormattingHelper.formatIndianRupee(record.cost)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    if (record.odometerReadingKm != null) {
                        val odoStr = NumberFormat.getIntegerInstance(Locale.US).format(record.odometerReadingKm)
                        Text(text = "Odometer: $odoStr km", fontSize = 13.sp)
                    }
                    if (!record.serviceCenter.isNullOrBlank()) {
                        Text(text = "Workshop: ${record.serviceCenter}", fontSize = 13.sp)
                    }
                    if (!record.nextServiceDate.isNullOrBlank()) {
                        Text(
                            text = "Next Service: ${MaintenanceFormattingHelper.formatDisplayDate(record.nextServiceDate)}",
                            fontSize = 13.sp
                        )
                    }
                    if (record.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Description:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(text = record.description, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRecordForDialog = null }) {
                    Text(
                        text = "Close",
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun MaintenanceHistoryRecordCard(
    record: MaintenanceHistoryRecordItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Badge on Left (Day, Month, Year)
            val dateParts = MaintenanceFormattingHelper.parseDateBadge(record.maintenanceDate)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.width(52.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateParts.day,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = dateParts.month,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = dateParts.year,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle Column: Service Type & Workshop
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.serviceType,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val workshopText = if (!record.serviceCenter.isNullOrBlank()) {
                    "Service Centre: ${record.serviceCenter}"
                } else if (record.description.isNotBlank()) {
                    record.description
                } else {
                    "General Service"
                }

                Text(
                    text = workshopText,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Cost + Chevron
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MaintenanceFormattingHelper.formatIndianRupee(record.cost),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )

                Spacer(modifier = Modifier.width(6.dp))

                ChevronRightIcon(
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
