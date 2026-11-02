package com.lorryowner.app.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.theme.*

@Composable
fun MyLorriesScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onAddLorryClick: () -> Unit = {},
    onLorryClick: (Long) -> Unit = {},
    onRunningClick: () -> Unit = {},
    onNotRunningClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember { MyLorriesViewModel(sessionManager) }
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.loadFleetData()
    }

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
                        text = "My Lorries",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.refresh() }) {
                        Text(
                            text = "🔄",
                            fontSize = 18.sp
                        )
                    }
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
            if (state.isLoading && state.lorries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BhpDarkForestGreen)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Circular Fleet Status Chart Card
                    item {
                        CircularFleetStatusCard(
                            summary = state.summary,
                            onRunningClick = onRunningClick,
                            onNotRunningClick = onNotRunningClick
                        )
                    }

                    // 2. Section Header & Search/Filters
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "All My Lorries",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BhpTextPrimary,
                                            fontSize = 18.sp
                                        )
                                    )
                                    Text(
                                        text = "${state.filteredLorries.size} of ${state.lorries.size} lorries",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BhpTextSecondary,
                                            fontSize = 13.sp
                                        )
                                    )
                                }

                                Button(
                                    onClick = onAddLorryClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BhpNavyDark,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+ Add Lorry",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Search Field
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Search by registration, model, driver...", fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = BhpDarkForestGreen,
                                    unfocusedBorderColor = BhpCardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = state.filter == LorryFilter.ALL,
                                    onClick = { viewModel.onFilterSelected(LorryFilter.ALL) },
                                    label = { Text("All (${state.lorries.size})") }
                                )
                                FilterChip(
                                    selected = state.filter == LorryFilter.RUNNING,
                                    onClick = { viewModel.onFilterSelected(LorryFilter.RUNNING) },
                                    label = { Text("Running (${state.summary.runningLorries})") }
                                )
                                FilterChip(
                                    selected = state.filter == LorryFilter.LOADING,
                                    onClick = { viewModel.onFilterSelected(LorryFilter.LOADING) },
                                    label = { Text("Loading (${state.summary.loadingLorries})") }
                                )
                                FilterChip(
                                    selected = state.filter == LorryFilter.AVAILABLE,
                                    onClick = { viewModel.onFilterSelected(LorryFilter.AVAILABLE) },
                                    label = { Text("Available (${state.summary.availableLorries})") }
                                )
                                FilterChip(
                                    selected = state.filter == LorryFilter.NOT_RUNNING,
                                    onClick = { viewModel.onFilterSelected(LorryFilter.NOT_RUNNING) },
                                    label = { Text("Not Ready (${state.summary.notReadyLorries})") }
                                )
                            }
                        }
                    }

                    // 3. Lorries List
                    if (state.filteredLorries.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🚚", fontSize = 40.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No matching lorries found",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = BhpTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Try adjusting your search query or filter.",
                                        fontSize = 13.sp,
                                        color = BhpTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    if (state.searchQuery.isNotEmpty() || state.filter != LorryFilter.ALL) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        OutlinedButton(onClick = { viewModel.clearFilters() }) {
                                            Text("Clear Filters")
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = onAddLorryClick,
                                            colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("+ Add First Lorry", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(state.filteredLorries, key = { it.id }) { lorry ->
                            LorryCardItem(
                                lorry = lorry,
                                onClick = { onLorryClick(lorry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircularFleetStatusCard(
    summary: FleetSummaryUiState,
    onRunningClick: () -> Unit = {},
    onNotRunningClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Chart + Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut Chart with center count
                val total = summary.totalLorries
                val runningSweep = if (total > 0) (summary.runningLorries.toFloat() / total.toFloat()) * 360f else 0f
                val loadingSweep = if (total > 0) (summary.loadingLorries.toFloat() / total.toFloat()) * 360f else 0f
                val availableSweep = if (total > 0) (summary.availableLorries.toFloat() / total.toFloat()) * 360f else 0f
                val notReadySweep = if (total > 0) (summary.notReadyLorries.toFloat() / total.toFloat()) * 360f else 0f

                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        // Track background when 0
                        drawArc(
                            color = Color(0xFFE2E8F0),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        if (total > 0) {
                            var currentAngle = -90f

                            if (runningSweep > 0) {
                                drawArc(
                                    color = BhpRunningGreen,
                                    startAngle = currentAngle,
                                    sweepAngle = runningSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                currentAngle += runningSweep
                            }

                            if (loadingSweep > 0) {
                                drawArc(
                                    color = Color(0xFFF59E0B),
                                    startAngle = currentAngle,
                                    sweepAngle = loadingSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                currentAngle += loadingSweep
                            }

                            if (availableSweep > 0) {
                                drawArc(
                                    color = BhpReadyBlue,
                                    startAngle = currentAngle,
                                    sweepAngle = availableSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                currentAngle += availableSweep
                            }

                            if (notReadySweep > 0) {
                                drawArc(
                                    color = Color(0xFF94A3B8),
                                    startAngle = currentAngle,
                                    sweepAngle = notReadySweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Lorries",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = BhpTextSecondary
                            )
                        )
                        Text(
                            text = "${summary.totalLorries}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary,
                                fontSize = 28.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 4-category Legend with text labels and percentages
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    StatusLegendItem(
                        color = BhpRunningGreen,
                        label = "Running",
                        count = summary.runningLorries,
                        percentage = summary.runningPercentage
                    )
                    StatusLegendItem(
                        color = Color(0xFFF59E0B),
                        label = "Loading",
                        count = summary.loadingLorries,
                        percentage = summary.loadingPercentage
                    )
                    StatusLegendItem(
                        color = BhpReadyBlue,
                        label = "Available",
                        count = summary.availableLorries,
                        percentage = summary.availablePercentage
                    )
                    StatusLegendItem(
                        color = Color(0xFF94A3B8),
                        label = "Not Ready",
                        count = summary.notReadyLorries,
                        percentage = summary.notReadyPercentage
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Status Summary Metric Chips below chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Running Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BhpRunningGreenBg,
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Running", fontSize = 10.5.sp, color = BhpRunningGreenText, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${summary.runningLorries}", fontSize = 16.sp, color = BhpRunningGreenText, fontWeight = FontWeight.Bold)
                    }
                }

                // Loading Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Loading", fontSize = 10.5.sp, color = Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${summary.loadingLorries}", fontSize = 16.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                    }
                }

                // Available Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BhpReadyBlueBg,
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Available", fontSize = 10.5.sp, color = BhpReadyBlueText, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${summary.availableLorries}", fontSize = 16.sp, color = BhpReadyBlueText, fontWeight = FontWeight.Bold)
                    }
                }

                // Not Ready Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Not Ready", fontSize = 10.5.sp, color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${summary.notReadyLorries}", fontSize = 16.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLegendItem(
    color: Color,
    label: String,
    count: Long,
    percentage: Double
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = BhpTextPrimary,
                fontSize = 12.sp
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count (${percentage}%)",
            style = MaterialTheme.typography.bodySmall.copy(
                color = BhpTextSecondary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun LorryCardItem(
    lorry: LorryCardUiState,
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Truck Icon Thumbnail Box
                    Box(
                        modifier = Modifier
                            .size(46.dp)
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
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Pill
                StatusPill(operationalStatus = lorry.operationalStatus, running = lorry.running)
            }

            // Driver & Destination details
            if (lorry.assignedDriverName != null || lorry.activeTripDestination != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lorry.assignedDriverName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DriverCapIcon(
                                tint = BhpTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lorry.assignedDriverName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "Unassigned driver",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }

                    if (lorry.running && lorry.activeTripDestination != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LocationPinIcon(
                                tint = BhpRunningGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "→ ${lorry.activeTripDestination}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpRunningGreenText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    operationalStatus: String? = null,
    running: Boolean = false,
    modifier: Modifier = Modifier
) {
    val normStatus = operationalStatus?.uppercase() ?: if (running) "ON_TRIP" else "AVAILABLE"
    val bgColor: Color
    val textColor: Color
    val dotColor: Color
    val label: String

    when {
        normStatus == "ON_TRIP" || (running && normStatus != "LOADING" && normStatus != "MAINTENANCE") -> {
            bgColor = BhpRunningGreenBg
            textColor = BhpRunningGreenText
            dotColor = BhpRunningGreen
            label = "Running"
        }
        normStatus == "LOADING" -> {
            bgColor = Color(0xFFFEF3C7)
            textColor = Color(0xFFB45309)
            dotColor = Color(0xFFF59E0B)
            label = "Loading"
        }
        normStatus == "AVAILABLE" -> {
            bgColor = BhpReadyBlueBg
            textColor = BhpReadyBlueText
            dotColor = BhpReadyBlue
            label = "Available"
        }
        normStatus == "MAINTENANCE" -> {
            bgColor = Color(0xFFFEE2E2)
            textColor = Color(0xFFDC2626)
            dotColor = Color(0xFFDC2626)
            label = "Maintenance"
        }
        normStatus == "UNAVAILABLE" -> {
            bgColor = Color(0xFFF1F5F9)
            textColor = Color(0xFF64748B)
            dotColor = Color(0xFF94A3B8)
            label = "Unavailable"
        }
        else -> {
            bgColor = Color(0xFFF1F5F9)
            textColor = Color(0xFF475569)
            dotColor = Color(0xFF94A3B8)
            label = normStatus
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = 12.sp
                )
            )
        }
    }
}
