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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lorryowner.app.R
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BhpPullToRefreshBox
import com.lorryowner.app.ui.components.CalendarIcon
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.HamburgerMenuIcon
import com.lorryowner.app.ui.components.HeroWhiteTruckGraphic
import com.lorryowner.app.ui.components.NotificationBellIcon
import com.lorryowner.app.ui.components.WrenchToolIcon
import com.lorryowner.app.ui.theme.*

@Composable
fun MaintenanceDashboardScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onViewVehicleDetails: (Long) -> Unit = {},
    onAddMaintenanceClick: () -> Unit = {},
    onAddRecordForLorry: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember { MaintenanceDashboardViewModel(sessionManager = sessionManager) }
    val state = viewModel.uiState

    // Explicitly reload fresh maintenance data whenever screen enters composition
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadSummary()
    }

    // Lifecycle observer to automatically reload fresh maintenance data on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadSummary()
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        HamburgerMenuIcon(
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Maintenance",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manage your fleet's upkeep",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadSummary() },
                        enabled = !state.isLoading,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF10B981)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Maintenance Data",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { /* notification action */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        NotificationBellIcon(
                            tint = Color(0xFF0F172A),
                            hasBadge = true,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
                        onClick = onAddMaintenanceClick,
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
        BhpPullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadSummary() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. Hero Card: THIS MONTH'S EXPENSE
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF064E3B), // Deep emerald green
                                        Color(0xFF065F46),
                                        Color(0xFF047857)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Green Wrench badge + Label
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    WrenchToolIcon(
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "THIS MONTH'S EXPENSE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA7F3D0),
                                        letterSpacing = 0.6.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Expense Amount
                                Text(
                                    text = MaintenanceFormattingHelper.formatIndianRupee(state.currentMonthExpense),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Total maintenance spent",
                                    fontSize = 13.sp,
                                    color = Color(0xFFD1FAE5)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Calendar icon + Month Year
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CalendarIcon(
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val now = java.time.LocalDate.now()
                                    val defaultMonthYear = "${now.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${now.year}"
                                    val monthLabel = if (state.currentMonth.isNotBlank()) {
                                        "${state.currentMonth} ${if (state.currentYear > 0) state.currentYear else now.year}".trim()
                                    } else {
                                        defaultMonthYear
                                    }
                                    Text(
                                        text = monthLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                            }

                            // 3D Angled White Lorry Illustration on the right
                            HeroWhiteTruckGraphic(
                                modifier = Modifier
                                    .size(width = 120.dp, height = 85.dp)
                                    .align(Alignment.Bottom)
                            )
                        }
                    }
                }
            }

            // 2. Section Header: YOUR LORRIES
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR LORRIES",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${state.vehicles.size} vehicles",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 3. Vehicles List / Loading / Empty
            if (state.isLoading && state.vehicles.isEmpty()) {
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
            } else if (state.errorMessage != null && state.vehicles.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Unable to load lorries",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.errorMessage ?: "",
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadSummary() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }
            } else if (state.vehicles.isEmpty()) {
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
                                text = "No Lorries Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add lorries to your fleet to track their maintenance schedule and expenses.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(state.vehicles, key = { it.lorryId }) { vehicle ->
                    LorryMaintenanceCard(
                        vehicle = vehicle,
                        onClick = { onViewVehicleDetails(vehicle.lorryId) }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun LorryMaintenanceCard(
    vehicle: VehicleMaintenanceSummaryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
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
            // Real Commercial Truck Image
            Image(
                painter = painterResource(id = R.drawable.bhp_truck_highway),
                contentDescription = "Lorry Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 74.dp, height = 58.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Vehicle Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicle.registrationNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = vehicle.makeModel,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Records Count with Blue/Green Wrench icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WrenchToolIcon(
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${vehicle.totalRecordsCount} records",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chevron Indicator
            ChevronRightIcon(
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
