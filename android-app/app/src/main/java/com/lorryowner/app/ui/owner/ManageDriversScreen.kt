package com.lorryowner.app.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BackArrowIcon
import com.lorryowner.app.ui.components.CheckSquareIcon
import com.lorryowner.app.ui.components.DistancePinIcon
import com.lorryowner.app.ui.components.DriverAvatarGraphic
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.TripRouteIcon
import com.lorryowner.app.ui.theme.BhpCardBorder
import com.lorryowner.app.ui.theme.BhpCardWhite
import com.lorryowner.app.ui.theme.BhpDarkForestGreen
import com.lorryowner.app.ui.theme.BhpDriverBlue
import com.lorryowner.app.ui.theme.BhpDriverInactiveBg
import com.lorryowner.app.ui.theme.BhpDriverInactiveText
import com.lorryowner.app.ui.theme.BhpDriverMetricBg
import com.lorryowner.app.ui.theme.BhpDriverMetricIconBg
import com.lorryowner.app.ui.theme.BhpScreenBg
import com.lorryowner.app.ui.theme.BhpTextMuted
import com.lorryowner.app.ui.theme.BhpTextPrimary
import com.lorryowner.app.ui.theme.BhpTextSecondary

/**
 * Manage Drivers Screen matching the reference design.
 * Displays top bar with back navigation, "+ Add New Driver" button,
 * 2x2 summary metric cards, search and filter controls, and list of driver cards.
 */
@Composable
fun ManageDriversScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit,
    onAddDriverClick: () -> Unit,
    onNavigateToTripHistory: ((DriverItemUiState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSession = remember { sessionManager ?: SessionManager(context) }
    val viewModel = remember { ManageDriversViewModel(effectiveSession) }
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.refreshDrivers()
    }

    Scaffold(
        topBar = {
            ManageDriversTopBar(
                onBack = onBack,
                onAddDriverClick = onAddDriverClick
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpScreenBg)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // 2x2 Driver Summary Metrics Grid
                    DriverSummaryGrid(summary = state.summary)

                    Spacer(modifier = Modifier.height(18.dp))

                    // Section Title: "Drivers"
                    Text(
                        text = "Drivers",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search & Filter Controls
                    DriverSearchAndFilterBar(
                        searchQuery = state.searchQuery,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        selectedFilter = state.selectedFilter,
                        onFilterSelect = { viewModel.onFilterSelected(it) },
                        totalCount = state.allDrivers.size,
                        filteredCount = state.drivers.size,
                        onClearFilters = { viewModel.clearFilters() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Loading State
                if (state.isLoading && state.drivers.isEmpty() && state.allDrivers.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = BhpDriverBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading fleet drivers...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = BhpTextSecondary)
                                )
                            }
                        }
                    }
                }

                // Error State
                if (state.errorMessage != null && state.allDrivers.isEmpty()) {
                    item {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "Could not load drivers",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF7F1D1D),
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.loadDrivers() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Empty State (No Drivers Registered at all)
                if (!state.isLoading && state.errorMessage == null && state.allDrivers.isEmpty()) {
                    item {
                        Surface(
                            color = BhpCardWhite,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BhpCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(28.dp)
                            ) {
                                Surface(
                                    color = BhpDriverMetricBg,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No Drivers Registered Yet",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BhpTextPrimary,
                                        fontSize = 17.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add your first commercial fleet driver using the button above.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BhpTextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onAddDriverClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+ Add New Driver", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Empty State (Filters returned no results)
                if (!state.isLoading && state.allDrivers.isNotEmpty() && state.drivers.isEmpty()) {
                    item {
                        Surface(
                            color = BhpCardWhite,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BhpCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = CircleShape,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        DriverCapIcon(tint = BhpTextSecondary, modifier = Modifier.size(26.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Matching Drivers",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BhpTextPrimary,
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No drivers match your current search query or filter. Try clearing filters.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BhpTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { viewModel.clearFilters() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Clear Filters", color = BhpDriverBlue, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Driver List Items
                itemsIndexed(state.drivers, key = { _, driver -> driver.id }) { index, driver ->
                    DriverCard(
                        driver = driver,
                        avatarIndex = index,
                        onViewDetails = { viewModel.showDriverDetails(driver) },
                        onViewTrips = { onNavigateToTripHistory?.invoke(driver) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Driver Details Modal Dialog
    if (state.selectedDriverForDetails != null) {
        DriverDetailsDialog(
            driver = state.selectedDriverForDetails,
            isActionLoading = state.isActionLoading,
            onToggleStatus = { newStatus ->
                viewModel.toggleDriverStatus(state.selectedDriverForDetails.id, newStatus)
            },
            onAssignLorry = { regNumber ->
                viewModel.assignLorry(state.selectedDriverForDetails.id, regNumber)
            },
            onDelete = {
                viewModel.deleteDriver(state.selectedDriverForDetails.id)
            },
            onViewTrips = {
                viewModel.dismissDriverDetails()
                onNavigateToTripHistory?.invoke(state.selectedDriverForDetails)
            },
            onDismiss = { viewModel.dismissDriverDetails() }
        )
    }
}

/**
 * Top App Bar matching the reference design with back button, titles, and "+ Add New Driver" button.
 */
@Composable
fun ManageDriversTopBar(
    onBack: () -> Unit,
    onAddDriverClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                BackArrowIcon(tint = BhpTextPrimary, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Manage Drivers",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = "Fleet commercial drivers & lorry assignments",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextSecondary,
                        fontSize = 11.5.sp
                    )
                )
            }

            // "+ Add New Driver" Button
            Button(
                onClick = onAddDriverClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BhpDriverBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "+ Add Driver",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Search and Filter bar for Drivers list.
 */
@Composable
private fun DriverSearchAndFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: DriverFilter,
    onFilterSelect: (DriverFilter) -> Unit,
    totalCount: Int,
    filteredCount: Int,
    onClearFilters: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by name, username, phone, or lorry...", fontSize = 13.sp, color = BhpTextMuted) },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Text("✕", color = BhpTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BhpDriverBlue,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Filter Chips Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            DriverFilterPill("All", selectedFilter == DriverFilter.ALL) { onFilterSelect(DriverFilter.ALL) }
            DriverFilterPill("Active", selectedFilter == DriverFilter.ACTIVE) { onFilterSelect(DriverFilter.ACTIVE) }
            DriverFilterPill("Inactive", selectedFilter == DriverFilter.INACTIVE) { onFilterSelect(DriverFilter.INACTIVE) }
            DriverFilterPill("Assigned", selectedFilter == DriverFilter.ASSIGNED) { onFilterSelect(DriverFilter.ASSIGNED) }
            DriverFilterPill("Unassigned", selectedFilter == DriverFilter.UNASSIGNED) { onFilterSelect(DriverFilter.UNASSIGNED) }
        }

        // Counter & Reset
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Showing $filteredCount of $totalCount drivers",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BhpTextSecondary,
                    fontSize = 11.5.sp
                )
            )
            if (searchQuery.isNotBlank() || selectedFilter != DriverFilter.ALL) {
                Text(
                    text = "Reset Filters",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpDriverBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    modifier = Modifier.clickable { onClearFilters() }
                )
            }
        }
    }
}

@Composable
private fun DriverFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) BhpDriverBlue else Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) BhpDriverBlue else Color(0xFFCBD5E1)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else BhpTextPrimary,
                fontSize = 11.5.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 2x2 Grid of Summary Metrics Cards for drivers.
 */
@Composable
fun DriverSummaryGrid(summary: DriverSummaryUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DriverMetricCard(
                title = "Total Drivers",
                value = summary.totalDrivers.toString(),
                subtitle = "Registered",
                icon = { DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f)
            )

            DriverMetricCard(
                title = "Active Drivers",
                value = summary.activeDrivers.toString(),
                subtitle = "Authorized",
                icon = { CheckSquareIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DriverMetricCard(
                title = "Completed Trips",
                value = summary.completedTrips.toString(),
                subtitle = "Fleet total",
                icon = { TripRouteIcon(tint = Color(0xFFD97706), modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f)
            )

            DriverMetricCard(
                title = "Total Distance",
                value = if (summary.totalDistanceKm != "0") "${summary.totalDistanceKm} km" else "0 km",
                subtitle = "Recorded transit",
                icon = { DistancePinIcon(tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DriverMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BhpCardWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BhpCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Surface(
                    color = BhpDriverMetricIconBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        icon()
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary,
                    fontSize = 24.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BhpTextMuted,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Driver Card showing driver profile, status, completed trips, distance, and actions.
 */
@Composable
fun DriverCard(
    driver: DriverItemUiState,
    avatarIndex: Int,
    onViewDetails: () -> Unit,
    onViewTrips: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BhpCardWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BhpCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Section: Avatar + Name/Phone/Lorry + Status Badge
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Driver Avatar
                DriverAvatarGraphic(
                    avatarIndex = avatarIndex,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name and details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = driver.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "@${driver.username}${if (!driver.phoneNumber.isNullOrBlank()) " • ${driver.phoneNumber}" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (!driver.assignedLorry.isNullOrBlank())
                            "Assigned: ${driver.assignedLorry}"
                        else
                            "No lorry currently assigned",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (!driver.assignedLorry.isNullOrBlank()) BhpDriverBlue else BhpTextSecondary,
                            fontWeight = if (!driver.assignedLorry.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    )
                }

                // Active / Inactive Badge
                if (driver.isActive) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        color = BhpDriverInactiveBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Inactive",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpDriverInactiveText,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Section: Trips, Divider, Distance, Trips Button, View Details Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Column 1: Completed Trips
                Column {
                    Text(
                        text = "Completed Trips",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (driver.completedTrips > 0) driver.completedTrips.toString() else "--",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Column 2: Total Distance Travelled
                Column {
                    Text(
                        text = "Total Distance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (driver.totalDistanceKm != "0 km") driver.totalDistanceKm else "--",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Trips Button
                OutlinedButton(
                    onClick = onViewTrips,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BhpDriverBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Trips",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpDriverBlue,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // View Details Button
                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BhpDriverBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Driver Details Dialog showing full profile information and actions.
 */
@Composable
fun DriverDetailsDialog(
    driver: DriverItemUiState,
    isActionLoading: Boolean,
    onToggleStatus: (Boolean) -> Unit,
    onAssignLorry: (String?) -> Unit,
    onDelete: () -> Unit,
    onViewTrips: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingLorry by remember { mutableStateOf(false) }
    var lorryInput by remember(driver.assignedLorry) { mutableStateOf(driver.assignedLorry ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BhpCardWhite,
        titleContentColor = BhpTextPrimary,
        textContentColor = BhpTextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Driver Information",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp)
            ) {
                DetailRow(label = "Full Name", value = driver.fullName)
                DetailRow(label = "Username", value = "@${driver.username}")
                DetailRow(
                    label = "Account Status",
                    value = if (driver.isActive) "Active (Authorized)" else "Inactive (Suspended)"
                )
                if (!driver.phoneNumber.isNullOrBlank()) {
                    DetailRow(label = "Phone Number", value = driver.phoneNumber)
                }
                if (!driver.licenseNumber.isNullOrBlank()) {
                    DetailRow(label = "License Number", value = driver.licenseNumber)
                }
                DetailRow(label = "Role", value = "COMMERCIAL FLEET DRIVER")

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // Trip History & Performance button
                Button(
                    onClick = {
                        onDismiss()
                        onViewTrips?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📊 View Trip History & Performance", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // Assigned Lorry Section
                Text(
                    text = "Assigned Lorry",
                    style = MaterialTheme.typography.bodySmall.copy(color = BhpTextSecondary, fontSize = 11.5.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (!editingLorry) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = driver.assignedLorry?.ifBlank { "None (Unassigned)" } ?: "None (Unassigned)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (driver.assignedLorry != null) BhpDriverBlue else BhpTextSecondary
                            )
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    lorryInput = driver.assignedLorry ?: ""
                                    editingLorry = true
                                },
                                enabled = !isActionLoading
                            ) {
                                Text(if (driver.assignedLorry != null) "Change" else "Assign", fontSize = 12.sp)
                            }
                            if (driver.assignedLorry != null) {
                                TextButton(
                                    onClick = { onAssignLorry(null) },
                                    enabled = !isActionLoading
                                ) {
                                    Text("Unassign", color = Color(0xFFDC2626), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = lorryInput,
                        onValueChange = { lorryInput = it.uppercase() },
                        label = { Text("Lorry Registration") },
                        placeholder = { Text("e.g. AP 26 TE 1234") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BhpDriverBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { editingLorry = false },
                            enabled = !isActionLoading
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                editingLorry = false
                                onAssignLorry(lorryInput.trim().ifEmpty { null })
                            },
                            enabled = !isActionLoading && lorryInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue)
                        ) {
                            Text("Save", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(12.dp))

                if (isActionLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BhpDriverBlue)
                    }
                } else {
                    // Actions: Activate/Deactivate & Delete
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (driver.isActive) {
                            OutlinedButton(
                                onClick = { onToggleStatus(false) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Deactivate", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { onToggleStatus(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Activate", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        TextButton(
                            onClick = { showDeleteConfirm = true }
                        ) {
                            Text("Delete Driver", color = Color(0xFFDC2626), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = BhpDriverBlue)
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Driver", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to remove driver ${driver.fullName}? If this driver has associated trips, they will be safely deactivated to preserve trip history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = BhpTextSecondary, fontSize = 11.5.sp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = BhpTextPrimary)
        )
    }
}
