package com.lorryowner.app.ui.owner

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BhpPullToRefreshBox
import com.lorryowner.app.ui.components.ClockTimerIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.components.WrenchToolIcon
import com.lorryowner.app.ui.theme.*
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerTripDetailsScreen(
    tripId: Long,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember(tripId) { OwnerTripDetailsViewModel(tripId, sessionManager) }
    val state = viewModel.uiState

    var isAddExpenseDialogOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Maintenance, 2 = Operating Expenses
    var recordToDelete by remember { mutableStateOf<Pair<Boolean, Long>?>(null) } // (isMaintenance, id)

    Scaffold(
        topBar = {
            Surface(
                color = BhpMaroonPrimary,
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.tripReference.isNotEmpty()) state.tripReference else "Trip Details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        )
                        Text(
                            text = "${state.origin} ➔ ${state.destination}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpGold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { viewModel.refreshTripDetails() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddExpenseDialogOpen = true },
                containerColor = BhpMaroonPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add Expense / Service", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BhpPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refreshTripDetails() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BhpBgLight),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Trip Header Summary Card
                item {
                    TripHeaderCard(state = state)
                }

                // 2. Financial Summary & Profit Card (Phase 3)
                item {
                    TripFinancialProfitCard(state = state)
                }

                // 3. Section Title & Filter Tabs
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trip Expenses & Maintenance",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary
                            )

                            Text(
                                text = "${state.maintenanceRecords.size + state.otherExpensesList.size} entries",
                                fontSize = 12.sp,
                                color = BhpTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter Tabs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterTabPill(
                                label = "All (${state.maintenanceRecords.size + state.otherExpensesList.size})",
                                isSelected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            )
                            FilterTabPill(
                                label = "Maintenance (${state.maintenanceRecords.size})",
                                isSelected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )
                            FilterTabPill(
                                label = "Operating (${state.otherExpensesList.size})",
                                isSelected = selectedTab == 2,
                                onClick = { selectedTab = 2 }
                            )
                        }
                    }
                }

                // 4. Itemized Entries List (Phase 4)
                val allEntriesEmpty = state.maintenanceRecords.isEmpty() && state.otherExpensesList.isEmpty()

                if (state.isLoading && allEntriesEmpty) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BhpMaroonPrimary)
                        }
                    }
                } else if (allEntriesEmpty) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🧾", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No expenses logged for this trip",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BhpTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '+ Add Expense / Service' to log oil changes, tyre repairs, fuel, or driver allowances.",
                                    fontSize = 12.5.sp,
                                    color = BhpTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Show Maintenance records if tab == 0 or tab == 1
                    if (selectedTab == 0 || selectedTab == 1) {
                        items(state.maintenanceRecords, key = { "maint_${it.id}" }) { item ->
                            TripMaintenanceItemCard(
                                record = item,
                                onDelete = { recordToDelete = Pair(true, item.id) }
                            )
                        }
                    }

                    // Show Other Expense records if tab == 0 or tab == 2
                    if (selectedTab == 0 || selectedTab == 2) {
                        items(state.otherExpensesList, key = { "exp_${it.id}" }) { item ->
                            TripExpenseItemCard(
                                record = item,
                                onDelete = { recordToDelete = Pair(false, item.id) }
                            )
                        }
                    }
                }

                // Bottom padding spacer for FAB
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }

    // Modal Dialog: Add Maintenance / Expense (Phase 5: Auto Lorry Linking)
    if (isAddExpenseDialogOpen) {
        AddTripExpenseDialog(
            lorryRegistration = state.lorryRegistration,
            onDismiss = { isAddExpenseDialogOpen = false },
            onSubmit = { category, amount, desc, date, notes, workshop, odo ->
                viewModel.addTripExpense(
                    category = category,
                    amount = amount,
                    description = desc,
                    date = date,
                    notes = notes,
                    workshop = workshop,
                    odometerKm = odo
                ) { success, msg ->
                    if (success) {
                        isAddExpenseDialogOpen = false
                        Toast.makeText(context, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, msg ?: "Error saving expense", Toast.LENGTH_LONG).show()
                    }
                }
            },
            isSubmitting = state.isSubmittingExpense
        )
    }

    // Confirm Delete Dialog
    if (recordToDelete != null) {
        val (isMaint, id) = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            containerColor = Color.White,
            title = { Text("Delete Entry", fontWeight = FontWeight.Bold, color = BhpTextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete this financial record? This will automatically update trip total expenses and net profit.",
                    fontSize = 13.5.sp,
                    color = BhpTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordToDelete = null
                        if (isMaint) {
                            viewModel.deleteMaintenanceRecord(id) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.deleteExpenseRecord(id) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Delete", color = BhpErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun TripHeaderCard(state: OwnerTripDetailsUiState) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Status & Trip Reference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: #${state.id} • ${state.tripReference}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BhpMaroonPrimary
                )

                OwnerTripStatusBadge(status = state.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Route Origin -> Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                LocationPinIcon(tint = BhpMaroonPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = state.origin,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
                Text(
                    text = "  ➔  ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BhpGold
                )
                Text(
                    text = state.destination,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Lorry & Driver Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LORRY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BhpTextSecondary)
                    Text(
                        text = state.lorryRegistration,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (!state.lorryMakeModel.isNullOrBlank()) {
                        Text(state.lorryMakeModel, fontSize = 11.5.sp, color = BhpTextSecondary)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("DRIVER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BhpTextSecondary)
                    Text(
                        text = state.driverName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (!state.driverPhone.isNullOrBlank()) {
                        Text(state.driverPhone, fontSize = 11.5.sp, color = BhpTextSecondary)
                    }
                }
            }

            if (state.distanceKm > 0 || !state.loadType.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (state.distanceKm > 0) {
                        Text(
                            text = "Distance: ${state.distanceKm} km",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    if (!state.loadType.isNullOrBlank()) {
                        Text(
                            text = "Cargo: ${state.loadType}",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripFinancialProfitCard(state: OwnerTripDetailsUiState) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Financial Summary & Net Profit
            Text(
                text = "TRIP FINANCIAL STATEMENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BhpTextSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gross Revenue Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gross Trip Revenue", fontSize = 14.sp, color = BhpTextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    text = formatIndianRupee(state.tripRevenue),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Itemized Expenses
            ExpenseSubRow(label = "🔧 Vehicle Maintenance", amount = state.maintenanceExpenses)
            ExpenseSubRow(label = "⛽ Fuel / Diesel", amount = state.fuelExpenses)
            ExpenseSubRow(label = "👨‍✈️ Driver Allowance / Salary", amount = state.driverExpenses)
            ExpenseSubRow(label = "🏷️ Toll & Other Expenses", amount = state.otherExpenses)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Total Expenses Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Trip Expenses", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                Text(
                    text = "- ${formatIndianRupee(state.totalExpenses)}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Net Profit Box (Trip Profit = Revenue - Total Expenses)
            val isProfitable = state.netProfit >= 0
            val profitBg = if (isProfitable) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
            val profitColor = if (isProfitable) Color(0xFF15803D) else Color(0xFFB91C1C)

            Surface(
                color = profitBg,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, profitColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Net Trip Profit",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = profitColor
                        )
                        Text(
                            text = "Revenue - Total Expenses",
                            fontSize = 10.5.sp,
                            color = profitColor.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = formatIndianRupee(state.netProfit),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = profitColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseSubRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF475569))
        Text(
            text = formatIndianRupee(amount),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun FilterTabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) BhpMaroonPrimary else Color.White,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) BhpMaroonPrimary else Color(0xFFCBD5E1)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF334155),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TripMaintenanceItemCard(
    record: TripMaintenanceItemUiState,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                WrenchToolIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = record.serviceType,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = formatIndianRupee(record.cost),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = record.description.ifEmpty { "Vehicle Maintenance" },
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${record.maintenanceDate}${if (!record.serviceCenter.isNullOrBlank()) " • " + record.serviceCenter else ""}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripExpenseItemCard(
    record: TripExpenseItemUiState,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (iconEmoji, bgTint) = when (record.category.uppercase()) {
                "DIESEL" -> Pair("⛽", Color(0xFFEFF6FF))
                "TOLL" -> Pair("🛣️", Color(0xFFF5F3FF))
                "DRIVER_SALARY" -> Pair("👨‍✈️", Color(0xFFFFFBEB))
                else -> Pair("🏷️", Color(0xFFF8FAFC))
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = record.category.replace("_", " "),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = formatIndianRupee(record.amount),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = record.description.ifEmpty { "Trip Expense" },
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.transactionDate,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTripExpenseDialog(
    lorryRegistration: String,
    onDismiss: () -> Unit,
    onSubmit: (category: String, amount: Double, desc: String, date: String, notes: String, workshop: String, odo: Int?) -> Unit,
    isSubmitting: Boolean
) {
    val categories = remember {
        listOf(
            "Engine Oil Change",
            "Tyre Repair",
            "Puncture",
            "Brake / Clutch Overhaul",
            "General Service",
            "DIESEL",
            "TOLL",
            "DRIVER_SALARY",
            "Other Expense"
        )
    }

    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var workshopText by remember { mutableStateOf("") }
    var odometerText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column {
                Text("Add Maintenance / Expense", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = BhpTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                // AUTOMATIC LORRY LINKING: Shown as derived read-only badge
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Assigned Lorry: $lorryRegistration",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BhpMaroonPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Chips Selector
                Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BhpTextSecondary)
                val scroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        Surface(
                            color = if (selectedCategory == cat) BhpMaroonPrimary else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCategory == cat) BhpMaroonPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.replace("_", " "),
                                fontSize = 11.sp,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCategory == cat) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description Field
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description*") },
                    placeholder = { Text("e.g. Engine oil top-up, tyre fix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Service Center / Workshop Field
                OutlinedTextField(
                    value = workshopText,
                    onValueChange = { workshopText = it },
                    label = { Text("Workshop / Vendor (Optional)") },
                    placeholder = { Text("e.g. Castrol Service Hub, MRF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes / Odometer Fields
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Bill Details (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val odo = odometerText.toIntOrNull()
                    val desc = descriptionText.ifBlank { selectedCategory.replace("_", " ") }
                    onSubmit(selectedCategory, amt, desc, LocalDate.now().toString(), notesText, workshopText, odo)
                },
                enabled = !isSubmitting && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = BhpMaroonPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Save Entry", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = BhpTextSecondary)
            }
        }
    )
}

private fun formatIndianRupee(amount: Double): String {
    return try {
        val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.maximumFractionDigits = 0
        formatter.format(amount)
    } catch (e: Exception) {
        "₹${amount.toLong()}"
    }
}
