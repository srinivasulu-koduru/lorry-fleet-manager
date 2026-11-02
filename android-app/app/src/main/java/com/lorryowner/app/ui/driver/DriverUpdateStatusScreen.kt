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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.service.DriverTripTrackingService
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.theme.*

@Composable
fun DriverUpdateStatusScreen(
    trip: DriverTripUiItem?,
    onBack: () -> Unit,
    viewModel: DriverDashboardViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var notesText by remember { mutableStateOf("") }
    var showStartConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }

    val isMutating = viewModel?.isMutatingTrip ?: false

    DriverTrackingPermissionCoordinator(
        onPermissionsGranted = {
            showStartConfirmDialog = true
        }
    ) { requestPermissions ->
        Scaffold(
            topBar = {
                DriverTopAppBar(
                    title = "Update Trip Status",
                    onBack = onBack
                )
            },
            bottomBar = {
                if (trip != null) {
                    val isPlanned = trip.tripStatus.equals("PLANNED", ignoreCase = true)
                    val isProgress = trip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)
                    val isCompleted = trip.tripStatus.equals("COMPLETED", ignoreCase = true)

                    Surface(
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            when {
                                isPlanned -> {
                                    Button(
                                        onClick = { requestPermissions() },
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
                                        PlayVectorIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Start Trip",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
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
                            }
                            else -> {
                                Button(
                                    onClick = onBack,
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
                                        text = "Trip Closed (${trip.tripStatus})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
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

            if (trip == null) {
                // Empty state when no trip is selected
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                DashboardTruckIcon(tint = BhpDriverBlue, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Active Trip Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Select an assigned trip from Home or My Trips to update its status.",
                            fontSize = 13.sp,
                            color = BhpTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel?.selectNavDestination(DriverNavDestination.TRIPS)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue)
                        ) {
                            Text("Go to My Trips", color = Color.White)
                        }
                    }
                }
            } else {
                val effectiveTrip = trip
                val isPlanned = effectiveTrip.tripStatus.equals("PLANNED", ignoreCase = true)
                val isProgress = effectiveTrip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)
                val isCompleted = effectiveTrip.tripStatus.equals("COMPLETED", ignoreCase = true)

                // 1. Trip Summary Header Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                DashboardTruckIcon(tint = BhpDriverBlue, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = effectiveTrip.tripReference,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BhpDriverBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${effectiveTrip.originLocation} → ${effectiveTrip.destinationLocation}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${effectiveTrip.lorryRegistration} | ${effectiveTrip.lorryMakeModel ?: "Commercial Lorry"}",
                                fontSize = 12.sp,
                                color = BhpTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Vertical 4-Step Timeline
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Trip Progress Timeline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        // Step 1: Trip Assigned (Completed)
                        TimelineStep(
                            stepNumber = "1",
                            title = "Trip Assigned",
                            description = "Trip dispatched and scheduled",
                            timestamp = effectiveTrip.tripDate.ifEmpty { "Assigned" },
                            isCompleted = true,
                            isCurrent = false,
                            showLine = true
                        )

                        // Step 2: Start Trip
                        TimelineStep(
                            stepNumber = "2",
                            title = "Start Trip",
                            description = if (isPlanned) "Tap Start Trip when ready to begin journey" else "Trip departed",
                            timestamp = if (!isPlanned) effectiveTrip.startTime ?: "Started" else null,
                            isCompleted = !isPlanned,
                            isCurrent = isPlanned,
                            showLine = true
                        )

                        // Step 3: At Destination
                        TimelineStep(
                            stepNumber = "3",
                            title = "At Destination",
                            description = when {
                                isPlanned -> "En route when journey starts"
                                isProgress -> "En route to destination. Complete delivery when arrived."
                                else -> "Arrived at destination"
                            },
                            timestamp = if (isCompleted) "Arrived" else null,
                            isCompleted = isCompleted,
                            isCurrent = isProgress,
                            showLine = true
                        )

                        // Step 4: Trip Completed
                        TimelineStep(
                            stepNumber = "4",
                            title = "Trip Completed",
                            description = if (isCompleted) "Delivery verified & trip finished" else "Complete when delivery is done",
                            timestamp = if (isCompleted) effectiveTrip.endTime ?: "Completed" else null,
                            isCompleted = isCompleted,
                            isCurrent = false,
                            showLine = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Notes Section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Update Notes (Optional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BhpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (isCompleted && !effectiveTrip.deliveryNotes.isNullOrBlank()) {
                            Surface(
                                color = BhpInputBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = effectiveTrip.deliveryNotes,
                                    fontSize = 13.sp,
                                    color = BhpTextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                placeholder = { Text("Add any notes about departure or delivery...", color = BhpTextMuted, fontSize = 13.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                enabled = !isCompleted && !isMutating,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BhpDriverBlue,
                                    unfocusedBorderColor = BhpCardBorder
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Start Trip Confirmation Dialog
    if (showStartConfirmDialog && trip != null) {
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
                        text = "You are about to start this trip. Please confirm departure details.",
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
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Confirm Start", color = Color.White)
                }
            }
        )
    }

    // Complete Trip Confirmation Dialog
    if (showCompleteConfirmDialog && trip != null) {
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
                        text = "Have you reached ${trip.destinationLocation} and finished delivery? Confirming will record this trip as finished.",
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
                            if (notesText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Notes: $notesText",
                                    fontSize = 12.sp,
                                    color = BhpTextSecondary
                                )
                            }
                        }
                    }
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
                            notesText.ifBlank { null },
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
}

@Composable
private fun TimelineStep(
    stepNumber: String,
    title: String,
    description: String,
    timestamp: String?,
    isCompleted: Boolean,
    isCurrent: Boolean,
    showLine: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Step Icon and Vertical Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            when {
                isCompleted -> {
                    Surface(
                        color = Color(0xFF10B981),
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CheckmarkCircleIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                isCurrent -> {
                    Surface(
                        color = BhpDriverBlue,
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stepNumber,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    Surface(
                        color = Color(0xFFE2E8F0),
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stepNumber,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isCompleted) Color(0xFF10B981) else Color(0xFFE2E8F0))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BhpTextPrimary
                )
                if (isCompleted) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Completed",
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isCurrent) {
                    Surface(
                        color = Color(0xFFDBEAFE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Current",
                            color = BhpDriverBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (!timestamp.isNullOrBlank()) {
                Text(
                    text = timestamp,
                    fontSize = 11.sp,
                    color = BhpTextMuted
                )
            }
            Text(
                text = description,
                fontSize = 12.sp,
                color = BhpTextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
