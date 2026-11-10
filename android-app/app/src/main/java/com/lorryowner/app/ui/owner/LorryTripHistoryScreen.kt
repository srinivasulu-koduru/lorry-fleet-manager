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
import com.lorryowner.app.ui.components.BhpPullToRefreshBox
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.ClockTimerIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LorryTripHistoryScreen(
    lorryId: Long,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember(lorryId) { LorryDetailsViewModel(lorryId, sessionManager) }
    val state = viewModel.uiState
    var tripToCancel by remember { mutableStateOf<TripItemUiState?>(null) }

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
                    IconButton(onClick = { viewModel.loadData() }) {
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
                BhpPullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.filteredTrips, key = { it.id }) { trip ->
                            TripCardItem(
                                trip = trip,
                                onClick = { viewModel.selectTripForDetails(trip) },
                                onCancelTripClick = { tripToCancel = trip }
                            )
                        }
                    }
                }
            }

            // Trip Details Dialog
            if (state.selectedTripForDetails != null) {
                TripDetailsModal(
                    trip = state.selectedTripForDetails!!,
                    sessionManager = sessionManager,
                    onDismiss = { viewModel.selectTripForDetails(null) },
                    onCancelClick = {
                        val current = state.selectedTripForDetails
                        viewModel.selectTripForDetails(null)
                        tripToCancel = current
                    }
                )
            }

            // Cancel Trip Confirmation Dialog
            if (tripToCancel != null) {
                val trip = tripToCancel!!
                AlertDialog(
                    onDismissRequest = { tripToCancel = null },
                    containerColor = Color.White,
                    titleContentColor = BhpTextPrimary,
                    textContentColor = BhpTextPrimary,
                    title = {
                        Text("Cancel Trip", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BhpTextPrimary)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Are you sure you want to cancel this planned trip?",
                                color = BhpTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Trip: ${trip.tripReference} (${trip.originLocation} → ${trip.destinationLocation})",
                                color = BhpTextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "This action will permanently set the trip status to Cancelled.",
                                color = BhpErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val idToCancel = trip.id
                                tripToCancel = null
                                viewModel.cancelTrip(idToCancel)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BhpErrorRed)
                        ) {
                            Text("Yes, Cancel Trip", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { tripToCancel = null }) {
                            Text("Keep Trip", color = BhpTextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TripCardItem(
    trip: TripItemUiState,
    onClick: () -> Unit,
    onCancelTripClick: (TripItemUiState) -> Unit = {},
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

            // Cancel action button when trip is in PLANNED status
            if (trip.tripStatus.equals("PLANNED", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onCancelTripClick(trip) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, BhpErrorRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✕ Cancel Trip",
                            color = BhpErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
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
        "PLANNED" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Planned")
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
    sessionManager: SessionManager? = null,
    onDismiss: () -> Unit,
    onCancelClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var tripDocuments by remember { mutableStateOf<List<com.lorryowner.app.network.DocumentUiItem>>(emptyList()) }
    var isLoadingDocs by remember { mutableStateOf(true) }
    var showUploadDocDialog by remember { mutableStateOf(false) }
    var selectedDocUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedDocName by remember { mutableStateOf("") }
    var docTitleInput by remember { mutableStateOf("") }
    var uploadDialogError by remember { mutableStateOf<String?>(null) }

    val docPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val (fname, _) = com.lorryowner.app.network.DocumentApiService.extractFileInfoFromUri(context, uri)
            selectedDocUri = uri
            selectedDocName = fname
            if (docTitleInput.isBlank()) {
                docTitleInput = fname.substringBeforeLast(".")
            }
        }
    }

    fun reloadDocs() {
        coroutineScope.launch {
            isLoadingDocs = true
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.lorryowner.app.network.DocumentApiService.fetchDocuments(
                    sessionManager = sessionManager,
                    endpointPath = "/api/owner/trips/${trip.id}/documents"
                )
            }
            isLoadingDocs = false
            if (res.isSuccess) {
                tripDocuments = res.getOrDefault(emptyList())
            }
        }
    }

    LaunchedEffect(trip.id) {
        reloadDocs()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = BhpTextPrimary,
        textContentColor = BhpTextPrimary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trip.tripReference,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BhpTextPrimary
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
                if (trip.tripAmountFormatted != null) {
                    Text("💰 Trip Amount: ${trip.tripAmountFormatted}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BhpRunningGreen)
                }

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
                    else if (trip.tripStatus == "CANCELLED")
                        "🚫 Cancelled"
                    else if (trip.tripStatus == "PLANNED")
                        "⏳ Scheduled (Not Started)"
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
                    Text("📝 Notes:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BhpTextPrimary)
                    Text(trip.deliveryNotes, fontSize = 12.sp, color = BhpTextSecondary)
                }

                // Trip Documents Section
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trip Documents (${tripDocuments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BhpTextPrimary
                    )

                    TextButton(
                        onClick = {
                            selectedDocUri = null
                            selectedDocName = ""
                            docTitleInput = ""
                            uploadDialogError = null
                            showUploadDocDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("+ Add Document", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BhpDarkForestGreen)
                    }
                }

                if (isLoadingDocs) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BhpDarkForestGreen)
                    }
                } else if (tripDocuments.isEmpty()) {
                    Text(
                        text = "No trip documents uploaded yet.",
                        fontSize = 12.sp,
                        color = BhpTextSecondary
                    )
                } else {
                    for (doc in tripDocuments) {
                        com.lorryowner.app.ui.components.DocumentCard(
                            document = doc,
                            sessionManager = sessionManager,
                            canDelete = true,
                            onDelete = { toDelete ->
                                coroutineScope.launch {
                                    val delRes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        com.lorryowner.app.network.DocumentApiService.deleteDocument(
                                            sessionManager = sessionManager,
                                            endpointPath = "/api/owner/trips/${trip.id}/documents/${toDelete.id}"
                                        )
                                    }
                                    if (delRes.isSuccess) reloadDocs()
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (trip.tripStatus.equals("PLANNED", ignoreCase = true) && onCancelClick != null) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BhpErrorRed)
                    ) {
                        Text("Cancel Trip", color = BhpErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BhpDarkForestGreen)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    )

    if (showUploadDocDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDocDialog = false },
            containerColor = Color.White,
            title = { Text("Upload Trip Document", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val suggestions = listOf("E-Way Bill", "Tax Invoice", "Delivery Challan", "Lorry Receipt (LR)")
                    Text("Suggested Types:", fontSize = 11.sp, color = BhpTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestions.take(3).forEach { s ->
                            FilterChip(
                                selected = docTitleInput == s,
                                onClick = { docTitleInput = s },
                                label = { Text(s, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = docTitleInput,
                        onValueChange = {
                            docTitleInput = it
                            uploadDialogError = null
                        },
                        label = { Text("Document Title *") },
                        placeholder = { Text("e.g. E-Way Bill") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedButton(
                        onClick = { docPickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedDocName.isNotBlank()) selectedDocName else "Choose PDF or Image")
                    }

                    if (uploadDialogError != null) {
                        Text(uploadDialogError!!, color = BhpErrorRed, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docTitleInput.trim().isBlank()) {
                            uploadDialogError = "Document title is required"
                            return@Button
                        }
                        val uri = selectedDocUri
                        if (uri == null) {
                            uploadDialogError = "Please select a file"
                            return@Button
                        }
                        coroutineScope.launch {
                            val upRes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.lorryowner.app.network.DocumentApiService.uploadDocument(
                                    context = context,
                                    sessionManager = sessionManager,
                                    endpointPath = "/api/owner/trips/${trip.id}/documents",
                                    documentTitle = docTitleInput.trim(),
                                    fileUri = uri
                                )
                            }
                            if (upRes.isSuccess) {
                                showUploadDocDialog = false
                                reloadDocs()
                            } else {
                                uploadDialogError = upRes.exceptionOrNull()?.message ?: "Upload failed"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpDarkForestGreen)
                ) {
                    Text("Upload", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDocDialog = false }) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            }
        )
    }
}
