package com.lorryowner.app.ui.driver

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

    var tripDocuments by remember { mutableStateOf<List<com.lorryowner.app.network.DocumentUiItem>>(emptyList()) }
    var vehicleDocuments by remember { mutableStateOf<List<com.lorryowner.app.network.DocumentUiItem>>(emptyList()) }
    var isLoadingDocuments by remember { mutableStateOf(true) }

    val sessionManager = viewModel?.sessionManager

    LaunchedEffect(trip.id, trip.lorryId) {
        isLoadingDocuments = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val tripDocsRes = com.lorryowner.app.network.DocumentApiService.fetchDocuments(
                sessionManager = sessionManager,
                endpointPath = "/api/driver/trips/${trip.id}/documents"
            )
            if (tripDocsRes.isSuccess) {
                tripDocuments = tripDocsRes.getOrDefault(emptyList())
            }

            val lorryId = trip.lorryId
            if (lorryId != null) {
                val vehDocsRes = com.lorryowner.app.network.DocumentApiService.fetchDocuments(
                    sessionManager = sessionManager,
                    endpointPath = "/api/driver/lorries/$lorryId/documents"
                )
                if (vehDocsRes.isSuccess) {
                    vehicleDocuments = vehDocsRes.getOrDefault(emptyList())
                }
            }
        }
        isLoadingDocuments = false
    }

    val isProgress = trip.tripStatus.equals("IN_PROGRESS", ignoreCase = true)
    val isCompleted = trip.tripStatus.equals("COMPLETED", ignoreCase = true)
    val isPlanned = trip.tripStatus.equals("PLANNED", ignoreCase = true)
    val isMutating = viewModel?.isMutatingTrip ?: false

    val isTrackingActive by DriverTripTrackingService.isTrackingActive.collectAsState()

    val (chipBg, chipColor, chipLabel) = when {
        isProgress -> Triple(Color(0xFFECFDF5), Color(0xFF15803D), "IN PROGRESS")
        isCompleted -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "COMPLETED")
        isPlanned -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), "SCHEDULED")
        else -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), trip.tripStatus.uppercase())
    }

    DriverTrackingPermissionCoordinator(
        onPermissionsGranted = {
            showStartConfirmDialog = true
        }
    ) { requestPermissions ->
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
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
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
                                        .height(48.dp)
                                ) {
                                    if (isMutating) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        PlayVectorIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Start Trip & Begin Tracking",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { onUpdateStatusClick(trip) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text(
                                        text = "Status Timeline",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
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
                                        .height(48.dp)
                                ) {
                                    if (isMutating) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        CheckmarkCircleIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Complete Trip",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { onUpdateStatusClick(trip) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text(
                                        text = "Update Status Timeline",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                            isCompleted -> {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
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
                                            fontSize = 14.5.sp,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { onUpdateStatusClick(trip) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text(
                                        text = "View Status Timeline",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { onUpdateStatusClick(trip) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0F172A),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "Trip Closed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
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
                    .background(Color(0xFFF8FAFC))
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Error message banner
                if (viewModel?.mutationErrorMessage != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = viewModel.mutationErrorMessage ?: "",
                                color = Color(0xFFDC2626),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearMutationMessages() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }

                // Success message banner
                if (viewModel?.mutationSuccessMessage != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            CheckmarkCircleIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = viewModel.mutationSuccessMessage ?: "",
                                color = Color(0xFF15803D),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearMutationMessages() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            }
                        }
                    }
                }

                // 1. Hero Card: Trip Reference + Status
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "TRIP REFERENCE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = trip.tripReference,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Surface(
                                color = chipBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = chipLabel,
                                    color = chipColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Live GPS Indicator (if IN_PROGRESS)
                        if (isProgress) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = if (isTrackingActive) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isTrackingActive) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isTrackingActive) Color(0xFF16A34A) else Color(0xFFD97706))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isTrackingActive) "Real-Time GPS Active" else "GPS Paused / Stopped",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isTrackingActive) Color(0xFF15803D) else Color(0xFFB45309)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                                        modifier = Modifier.clickable {
                                            DriverTripTrackingService.restartTracking(context, trip.id, trip.tripReference)
                                            android.widget.Toast.makeText(context, "Restarting GPS tracking for ${trip.tripReference}...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "🔄 Restart GPS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1.5 Intelligent Real-Time Road Route & Dynamic Selection Map
                if (isProgress || isPlanned) {
                    Spacer(modifier = Modifier.height(14.dp))
                    DriverLiveRouteMapCard(
                        trip = trip,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Route & Transit Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Route & Transit Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Origin
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Origin / Dispatch Location",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = trip.originLocation,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        // Connecting line
                        Box(
                            modifier = Modifier
                                .padding(start = 4.5.dp)
                                .height(22.dp)
                                .width(1.5.dp)
                                .background(Color(0xFFCBD5E1))
                        )

                        // Destination
                        Row(verticalAlignment = Alignment.Top) {
                            LocationPinIcon(
                                tint = Color(0xFF2563EB),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(11.dp))
                            Column {
                                Text(
                                    text = "Destination / Unloading Point",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = trip.destinationLocation,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = Color(0xFFF1F5F9)
                        )

                        // Date & Distance row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Scheduled Date", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                Text(
                                    text = trip.tripDate.ifEmpty { "Today" },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            if (!trip.distanceKm.isNullOrBlank()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Est. Distance", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = "${trip.distanceKm} km",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Vehicle & Assignment Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Vehicle & Consignment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TripDetailItem(
                            label = "Lorry Registration",
                            value = trip.lorryRegistration,
                            icon = { DashboardTruckIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) }
                        )

                        if (!trip.lorryMakeModel.isNullOrBlank()) {
                            TripDetailItem(
                                label = "Vehicle Model",
                                value = trip.lorryMakeModel,
                                icon = { DashboardTruckIcon(tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)) }
                            )
                        }

                        TripDetailItem(
                            label = "Assigned Driver",
                            value = trip.driverName.ifEmpty { "Assigned Driver" },
                            icon = { DriverCapIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) }
                        )

                        if (!trip.loadType.isNullOrBlank()) {
                            TripDetailItem(
                                label = "Consignment Cargo",
                                value = trip.loadType,
                                icon = { DocumentTextIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) }
                            )
                        }

                        if (!trip.deliveryNotes.isNullOrBlank()) {
                            TripDetailItem(
                                label = "Delivery Notes",
                                value = trip.deliveryNotes,
                                icon = { DocumentTextIcon(tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Trip Documents Card (Assigned Trip)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trip Documents",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${tripDocuments.size} files",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Documents attached by the fleet owner for this trip (E-Way Bill, Tax Invoice, LR, Challan).",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        if (isLoadingDocuments) {
                            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF16A34A), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        } else if (tripDocuments.isEmpty()) {
                            com.lorryowner.app.ui.components.EmptyDocumentsView("No trip documents uploaded for this trip.")
                        } else {
                            for (doc in tripDocuments) {
                                com.lorryowner.app.ui.components.DocumentCard(
                                    document = doc,
                                    sessionManager = sessionManager,
                                    canDelete = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Vehicle Documents Card (Assigned Lorry)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vehicle Documents",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${vehicleDocuments.size} files",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Statutory commercial documents for vehicle ${trip.lorryRegistration} (RC, Insurance, Fitness, Permit, PUC).",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        if (isLoadingDocuments) {
                            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF16A34A), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        } else if (vehicleDocuments.isEmpty()) {
                            com.lorryowner.app.ui.components.EmptyDocumentsView("No vehicle documents uploaded for this lorry.")
                        } else {
                            for (doc in vehicleDocuments) {
                                com.lorryowner.app.ui.components.DocumentCard(
                                    document = doc,
                                    sessionManager = sessionManager,
                                    canDelete = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Start Confirmation Dialog
        if (showStartConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showStartConfirmDialog = false },
                title = {
                    Text(
                        text = "Start Trip ${trip.tripReference}?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "This will update your trip status to In Progress and activate real-time phone GPS tracking in the background.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vehicle: ${trip.lorryRegistration}\nRoute: ${trip.originLocation} → ${trip.destinationLocation}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showStartConfirmDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF475569))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStartConfirmDialog = false
                            viewModel?.startTrip(
                                tripId = trip.id,
                                onSuccess = { updatedTrip ->
                                    DriverTripTrackingService.startTracking(
                                        context = context,
                                        tripId = updatedTrip.id,
                                        tripReference = updatedTrip.tripReference
                                    )
                                }
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("Confirm & Start", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Complete Confirmation Dialog
        if (showCompleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showCompleteConfirmDialog = false },
                title = {
                    Text(
                        text = "Complete Trip ${trip.tripReference}?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "This will conclude your trip, log final records, and stop phone GPS tracking.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = completeNotesText,
                            onValueChange = { completeNotesText = it },
                            label = { Text("Delivery Notes (Optional)") },
                            placeholder = { Text("e.g. Unloaded successfully at warehouse") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showCompleteConfirmDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF475569))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCompleteConfirmDialog = false
                            viewModel?.completeTrip(
                                tripId = trip.id,
                                notes = completeNotesText.ifBlank { null },
                                onSuccess = {
                                    DriverTripTrackingService.stopTracking(context)
                                }
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("Complete Trip", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun TripDetailItem(
    label: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.5.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }
    }
}
