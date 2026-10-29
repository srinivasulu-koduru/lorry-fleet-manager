package com.lorryowner.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.service.DriverTripTrackingService
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.theme.*

@Composable
fun DriverTripDetailsScreen(
    trip: DriverTripUiItem,
    onBack: () -> Unit,
    onUpdateStatusClick: (DriverTripUiItem) -> Unit,
    viewModel: DriverDashboardViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showStartConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }
    var completeNotesText by remember { mutableStateOf("") }

    val isProgress = trip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)
    val isCompleted = trip.tripStatus.equals("COMPLETED", ignoreCase = true)
    val isPlanned = trip.tripStatus.equals("PLANNED", ignoreCase = true)
    val isMutating = viewModel?.isMutatingTrip ?: false

    val (chipBg, chipColor, chipLabel) = when {
        isProgress -> Triple(BhpNotReadyAmberBg, BhpNotReadyAmberText, "IN PROGRESS")
        isCompleted -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "COMPLETED")
        isPlanned -> Triple(BhpRunningGreenBg, BhpRunningGreenText, "PLANNED")
        else -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), trip.tripStatus.uppercase())
    }

    Scaffold(
        topBar = {
            DriverTopAppBar(
                title = "Trip Details",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when {
                        isPlanned -> {
                            Button(
                                onClick = { showStartConfirmDialog = true },
                                enabled = !isMutating,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BhpNavyDark,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isMutating) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    PlayVectorIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Start Trip",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onUpdateStatusClick(trip) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "View Status Timeline",
                                    color = BhpDriverBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        isProgress -> {
                            Button(
                                onClick = { showCompleteConfirmDialog = true },
                                enabled = !isMutating,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isMutating) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    CheckmarkCircleIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Complete Trip",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onUpdateStatusClick(trip) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "Update Status Timeline",
                                    color = BhpDriverBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        isCompleted -> {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    CheckmarkCircleIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Trip Completed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onUpdateStatusClick(trip) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "View Status Timeline",
                                    color = BhpDriverBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = { onUpdateStatusClick(trip) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BhpNavyDark,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Trip Closed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpScreenBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Error banner
            if (viewModel?.mutationErrorMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.mutationErrorMessage ?: "An error occurred",
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearMutationMessages() }) {
                            Text("Dismiss", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Success banner
            if (viewModel?.mutationSuccessMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckmarkCircleIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = viewModel.mutationSuccessMessage ?: "Operation succeeded",
                            color = Color(0xFF16A34A),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearMutationMessages() }) {
                            Text("OK", color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Header: Status badge + Reference
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Surface(
                    color = chipBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = chipLabel,
                        color = chipColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = trip.tripReference,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BhpDriverBlue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Origin to Destination Route Connector Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Origin
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                LocationPinIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = trip.originLocation,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BhpTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "Origin",
                            fontSize = 11.sp,
                            color = BhpTextSecondary
                        )
                    }

                    // Connected Arrow
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "───────►",
                            color = BhpDriverBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (!trip.distanceKm.isNullOrBlank()) {
                            Text(
                                text = "${trip.distanceKm} km",
                                fontSize = 11.sp,
                                color = BhpTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Destination
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color(0xFFDBEAFE),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                LocationPinIcon(tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = trip.destinationLocation,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BhpTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "Destination",
                            fontSize = 11.sp,
                            color = BhpTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Asset & Operational Details Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Trip Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BhpTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Lorry
                    DetailRow(
                        icon = { DashboardTruckIcon(tint = BhpDriverBlue, modifier = Modifier.size(18.dp)) },
                        title = "Assigned Lorry",
                        value = trip.lorryRegistration,
                        subValue = trip.lorryMakeModel
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BhpScreenBg)

                    // Driver
                    DetailRow(
                        icon = { DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(18.dp)) },
                        title = "Driver",
                        value = trip.driverName.ifEmpty { "Ramesh Kumar" },
                        subValue = null
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BhpScreenBg)

                    // Planned Departure / Start Time
                    DetailRow(
                        icon = { CalendarIcon(tint = BhpDriverBlue, modifier = Modifier.size(18.dp)) },
                        title = if (trip.startTime != null) "Departure Time (Actual)" else "Scheduled Departure",
                        value = trip.startTime ?: "${trip.tripDate}, 06:00 AM",
                        subValue = null
                    )

                    // End Time (if completed)
                    if (trip.endTime != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BhpScreenBg)
                        DetailRow(
                            icon = { CheckmarkCircleIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp)) },
                            title = "Completion Time",
                            value = trip.endTime,
                            subValue = null
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BhpScreenBg)

                    // Planned Delivery / ETA
                    DetailRow(
                        icon = { ClockTimerIcon(tint = BhpDriverBlue, modifier = Modifier.size(18.dp)) },
                        title = "Planned Delivery / ETA",
                        value = trip.plannedDeliveryTime ?: "Scheduled on Delivery",
                        subValue = null
                    )

                    if (!trip.loadType.isNullOrBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BhpScreenBg)
                        DetailRow(
                            icon = { PackageBoxIcon(tint = BhpDriverBlue, modifier = Modifier.size(18.dp)) },
                            title = "Load Type",
                            value = trip.loadType,
                            subValue = null
                        )
                    }

                    if (!trip.deliveryNotes.isNullOrBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BhpScreenBg)
                        DetailRow(
                            icon = { DocumentTextIcon(tint = BhpDriverBlue, modifier = Modifier.size(18.dp)) },
                            title = "Remarks / Delivery Notes",
                            value = trip.deliveryNotes,
                            subValue = null
                        )
                    }
                }
            }
        }
    }

    // Start Trip Confirmation Dialog
    if (showStartConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isMutating) showStartConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            DashboardTruckIcon(tint = BhpDriverBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Start Trip?", fontWeight = FontWeight.Bold, color = BhpTextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "You are about to start this trip. Confirming will record departure and set vehicle status to On Trip.",
                        color = BhpTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = BhpInputBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${trip.originLocation} → ${trip.destinationLocation}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BhpTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lorry: ${trip.lorryRegistration}",
                                fontSize = 12.sp,
                                color = BhpTextSecondary
                            )
                            Text(
                                text = "Ref: ${trip.tripReference}",
                                fontSize = 12.sp,
                                color = BhpDriverBlue
                            )
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showStartConfirmDialog = false },
                    enabled = !isMutating,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartConfirmDialog = false
                        viewModel?.startTrip(
                            trip.id,
                            onSuccess = { updatedTrip ->
                                DriverTripTrackingService.startTracking(context, updatedTrip.id, updatedTrip.tripReference)
                            }
                        )
                    },
                    enabled = !isMutating,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark)
                ) {
                    Text("Confirm Start", color = Color.White)
                }
            }
        )
    }

    // Complete Trip Confirmation Dialog
    if (showCompleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isMutating) showCompleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CheckmarkCircleIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Complete Trip?", fontWeight = FontWeight.Bold, color = BhpTextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Have you arrived at ${trip.destinationLocation} and finished delivery? Confirming will record trip completion and release the lorry back to available status.",
                        color = BhpTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = BhpInputBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${trip.originLocation} → ${trip.destinationLocation}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BhpTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lorry: ${trip.lorryRegistration}",
                                fontSize = 12.sp,
                                color = BhpTextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = completeNotesText,
                        onValueChange = { completeNotesText = it },
                        placeholder = { Text("Add delivery remarks (optional)...", color = BhpTextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCompleteConfirmDialog = false },
                    enabled = !isMutating,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteConfirmDialog = false
                        viewModel?.completeTrip(
                            trip.id,
                            completeNotesText.ifBlank { null },
                            onSuccess = {
                                DriverTripTrackingService.stopTracking(context)
                            }
                        )
                    },
                    enabled = !isMutating,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Confirm Complete", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    subValue: String?
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = BhpTextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BhpTextPrimary
            )
            if (!subValue.isNullOrBlank()) {
                Text(
                    text = subValue,
                    fontSize = 12.sp,
                    color = BhpTextSecondary
                )
            }
        }
    }
}
