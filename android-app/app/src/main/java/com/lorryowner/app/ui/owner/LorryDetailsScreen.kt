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

                    // VEHICLE DOCUMENTS SECTION
                    val context = androidx.compose.ui.platform.LocalContext.current
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

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Vehicle Documents",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BhpTextPrimary,
                                            fontSize = 16.sp
                                        )
                                    )
                                    Text(
                                        text = "RC, Insurance, Fitness, Permit, PUC",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BhpTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        selectedDocUri = null
                                        selectedDocName = ""
                                        docTitleInput = ""
                                        uploadDialogError = null
                                        showUploadDocDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BhpDarkForestGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("+ Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (state.isUploadingDocument) {
                                LinearProgressIndicator(
                                    color = BhpDarkForestGreen,
                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                )
                            }

                            if (state.documentError != null) {
                                Text(
                                    text = state.documentError,
                                    color = BhpErrorRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (state.vehicleDocuments.isEmpty()) {
                                com.lorryowner.app.ui.components.EmptyDocumentsView("No vehicle documents uploaded yet.")
                            } else {
                                for (doc in state.vehicleDocuments) {
                                    com.lorryowner.app.ui.components.DocumentCard(
                                        document = doc,
                                        sessionManager = sessionManager,
                                        canDelete = true,
                                        onDelete = { viewModel.deleteVehicleDocument(it.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (showUploadDocDialog) {
                        AlertDialog(
                            onDismissRequest = { showUploadDocDialog = false },
                            containerColor = Color.White,
                            title = { Text("Upload Vehicle Document", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val suggestions = listOf("RC", "Insurance", "Fitness Certificate", "Permit", "PUC")
                                    Text("Suggested Titles:", fontSize = 11.sp, color = BhpTextSecondary)
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
                                        placeholder = { Text("e.g. Registration Certificate (RC)") },
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
                                        viewModel.uploadVehicleDocument(context, docTitleInput.trim(), uri)
                                        showUploadDocDialog = false
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
                                text = lorry.activeTripReference ?: "Active Trip",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = BhpTextPrimary
                            )
                            TripStatusBadge(status = "IN_PROGRESS")
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "📍 ${lorry.activeTripOrigin ?: "Origin"} → ${lorry.activeTripDestination ?: "Destination"}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = BhpTextPrimary
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Text("👤 Driver: ${lorry.assignedDriverName ?: "Unassigned"}", fontSize = 13.sp, color = BhpTextSecondary)
                            Text("🚚 Lorry: ${lorry.registrationNumber} (${lorry.makeModel})", fontSize = 13.sp, color = BhpTextSecondary)
                            Text("⏳ Status: In Progress (${lorry.activeTripEtaFormatted})", fontSize = 13.sp, color = BhpTextSecondary)
                            if (lorry.activeTripStartTime != null) {
                                Text("🕒 Started: ${lorry.activeTripStartTime.take(16).replace("T", " ")}", fontSize = 13.sp, color = BhpTextSecondary)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { isCurrentTripDialogVisible = false },
                            colors = ButtonDefaults.buttonColors(containerColor = BhpDarkForestGreen)
                        ) {
                            Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
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
