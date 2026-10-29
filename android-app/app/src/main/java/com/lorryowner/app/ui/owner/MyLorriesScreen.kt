package com.lorryowner.app.ui.owner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                                modifier = Modifier.fillMaxWidth(),
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
                                    selected = state.filter == LorryFilter.NOT_RUNNING,
                                    onClick = { viewModel.onFilterSelected(LorryFilter.NOT_RUNNING) },
                                    label = { Text("Not Running (${state.summary.notRunningLorries})") }
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
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Donut Chart with center count
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val runningSweep = if (summary.totalLorries > 0) {
                        (summary.runningLorries.toFloat() / summary.totalLorries.toFloat()) * 360f
                    } else 0f

                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        val strokeWidth = 16.dp.toPx()
                        // Track background (Not running arc)
                        drawArc(
                            color = Color(0xFFE2E8F0),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Running arc (Green)
                        if (runningSweep > 0) {
                            drawArc(
                                color = BhpRunningGreen,
                                startAngle = -90f,
                                sweepAngle = runningSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
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

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(BhpRunningGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Running",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "${summary.runningLorries} (${summary.runningPercentage}%)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Not Running",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "${summary.notRunningLorries} (${summary.notRunningPercentage}%)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Two Clickable Stat Cards below chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Running Button Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BhpRunningGreenBg,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onRunningClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🟢", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Running",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpRunningGreenText,
                                    fontSize = 14.sp
                                )
                            )
                        }
                        Text(
                            text = "${summary.runningLorries}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpRunningGreenText,
                                fontSize = 18.sp
                            )
                        )
                    }
                }

                // Not Running Button Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNotRunningClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚪", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Not Running",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    fontSize = 14.sp
                                )
                            )
                        }
                        Text(
                            text = "${summary.notRunningLorries}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            }
        }
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
                StatusPill(running = lorry.running)
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
fun StatusPill(running: Boolean, modifier: Modifier = Modifier) {
    val bgColor = if (running) BhpRunningGreenBg else Color(0xFFF1F5F9)
    val textColor = if (running) BhpRunningGreenText else Color(0xFF475569)
    val text = if (running) "Running" else "Not Running"

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
                    .background(if (running) BhpRunningGreen else Color(0xFF94A3B8))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = 12.sp
                )
            )
        }
    }
}
