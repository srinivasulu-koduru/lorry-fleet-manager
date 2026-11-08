package com.lorryowner.app.ui.owner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.theme.*
import java.util.Calendar

val LOAD_TYPE_OPTIONS = listOf(
    "General Goods",
    "Food and Groceries",
    "Construction Materials",
    "Industrial Goods",
    "Agricultural Products",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    viewModel: AddTripViewModel,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit,
    onNavigateToConfirmation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showLorryDialog by remember { mutableStateOf(false) }
    var showDriverDialog by remember { mutableStateOf(false) }
    var showLoadTypeDropdown by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }

    Scaffold(
        topBar = {
            Surface(color = BhpNavyDark, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        BackArrowIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Trip Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = BhpCardWhite,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BhpCardBorder.copy(alpha = 0.5f), RoundedCornerShape(0.dp))
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (viewModel.validateForm()) {
                                onNavigateToConfirmation()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BhpNavyDark,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Create Trip",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // General Error Banner if network or server error
            if (state.generalError != null) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = state.generalError,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { viewModel.clearGeneralError() }
                        )
                    }
                }
            }

            // SECTION A: Trip Information
            Text(
                text = "Trip Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Select Lorry Required
            Text(
                text = "Select Lorry *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BhpTextPrimary,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            SelectableBox(
                icon = { DashboardTruckIcon(tint = BhpNavyDark, modifier = Modifier.size(20.dp)) },
                text = if (state.selectedLorry != null) {
                    "${state.selectedLorry.registrationNumber} (${state.selectedLorry.makeModel})"
                } else "Choose a lorry",
                isPlaceholder = state.selectedLorry == null,
                isError = state.lorryError != null,
                onClick = { showLorryDialog = true }
            )
            if (state.lorryError != null) {
                Text(
                    text = state.lorryError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Select Driver Required
            Text(
                text = "Select Driver *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BhpTextPrimary,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            SelectableBox(
                icon = { DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(20.dp)) },
                text = if (state.selectedDriver != null) {
                    state.selectedDriver.fullName + (state.selectedDriver.phoneNumber?.let { " • $it" } ?: "")
                } else "Choose a driver",
                isPlaceholder = state.selectedDriver == null,
                isError = state.driverError != null,
                onClick = { showDriverDialog = true }
            )
            if (state.driverError != null) {
                Text(
                    text = state.driverError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 & 4. Origin & Destination
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Origin
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Origin *",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BhpTextPrimary,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.origin,
                        onValueChange = { viewModel.onOriginChange(it) },
                        placeholder = { Text("Enter origin location", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { LocationPinIcon(tint = BhpRunningGreen, modifier = Modifier.size(16.dp)) },
                        isError = state.originError != null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BhpCardWhite,
                            unfocusedContainerColor = BhpCardWhite,
                            focusedBorderColor = BhpRunningGreen,
                            unfocusedBorderColor = BhpCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.originError != null) {
                        Text(
                            text = state.originError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Destination
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Destination *",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BhpTextPrimary,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.destination,
                        onValueChange = { viewModel.onDestinationChange(it) },
                        placeholder = { Text("Enter destination location", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { LocationPinIcon(tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
                        isError = state.destinationError != null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BhpCardWhite,
                            unfocusedContainerColor = BhpCardWhite,
                            focusedBorderColor = BhpRunningGreen,
                            unfocusedBorderColor = BhpCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.destinationError != null) {
                        Text(
                            text = state.destinationError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 & 6. Planned Departure Date & Time Required
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Departure Date
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Planned Departure Date *",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BhpTextPrimary,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectableBox(
                        icon = { CalendarIcon(tint = BhpNavyDark, modifier = Modifier.size(18.dp)) },
                        text = state.plannedDepartureDate.ifEmpty { "Select date" },
                        isPlaceholder = state.plannedDepartureDate.isEmpty(),
                        isError = state.departureDateError != null,
                        onClick = {
                            val c = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val formatted = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    viewModel.onDepartureDateSelected(formatted)
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                    if (state.departureDateError != null) {
                        Text(
                            text = state.departureDateError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Departure Time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Planned Departure Time *",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BhpTextPrimary,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectableBox(
                        icon = { ClockTimerIcon(tint = BhpNavyDark, modifier = Modifier.size(18.dp)) },
                        text = state.plannedDepartureTime.ifEmpty { "Select time" },
                        isPlaceholder = state.plannedDepartureTime.isEmpty(),
                        isError = state.departureTimeError != null,
                        onClick = {
                            val c = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val formatted = String.format("%02d:%02d", hour, minute)
                                    viewModel.onDepartureTimeSelected(formatted)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        }
                    )
                    if (state.departureTimeError != null) {
                        Text(
                            text = state.departureTimeError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7 & 8. Planned Delivery Date & Time Optional
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Planned Delivery Schedule (Optional)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = BhpTextPrimary,
                        fontSize = 13.sp
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.plannedDeliveryDate.isNotBlank() || state.plannedDeliveryTime.isNotBlank()) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { viewModel.onClearDeliverySchedule() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Delivery Date
                Column(modifier = Modifier.weight(1f)) {
                    SelectableBox(
                        icon = { CalendarIcon(tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp)) },
                        text = state.plannedDeliveryDate.ifEmpty { "Select date (opt)" },
                        isPlaceholder = state.plannedDeliveryDate.isEmpty(),
                        isError = state.deliveryDateError != null,
                        onClick = {
                            val c = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val formatted = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    viewModel.onDeliveryDateSelected(formatted)
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                    if (state.deliveryDateError != null) {
                        Text(
                            text = state.deliveryDateError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Delivery Time
                Column(modifier = Modifier.weight(1f)) {
                    SelectableBox(
                        icon = { ClockTimerIcon(tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp)) },
                        text = state.plannedDeliveryTime.ifEmpty { "Select time (opt)" },
                        isPlaceholder = state.plannedDeliveryTime.isEmpty(),
                        isError = state.deliveryTimeError != null,
                        onClick = {
                            val c = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val formatted = String.format("%02d:%02d", hour, minute)
                                    viewModel.onDeliveryTimeSelected(formatted)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        }
                    )
                    if (state.deliveryTimeError != null) {
                        Text(
                            text = state.deliveryTimeError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION B: Additional Details
            Text(
                text = "Additional Details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Trip Amount Required
            Text(
                text = "Trip Amount (₹) *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BhpTextPrimary,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.tripAmount,
                onValueChange = { viewModel.onTripAmountChange(it) },
                placeholder = { Text("e.g. 25000", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                prefix = {
                    Text(
                        text = "₹ ",
                        fontWeight = FontWeight.Bold,
                        color = BhpNavyDark,
                        fontSize = 14.sp
                    )
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                isError = state.tripAmountError != null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BhpCardWhite,
                    unfocusedContainerColor = BhpCardWhite,
                    focusedBorderColor = BhpRunningGreen,
                    unfocusedBorderColor = BhpCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (state.tripAmountError != null) {
                Text(
                    text = state.tripAmountError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 9. Load Type Dropdown
            Text(
                text = "Load Type *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BhpTextPrimary,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                SelectableBox(
                    icon = { PackageBoxIcon(tint = BhpNavyDark, modifier = Modifier.size(18.dp)) },
                    text = state.loadType,
                    isPlaceholder = false,
                    isError = state.loadTypeError != null,
                    onClick = { showLoadTypeDropdown = true }
                )
                DropdownMenu(
                    expanded = showLoadTypeDropdown,
                    onDismissRequest = { showLoadTypeDropdown = false },
                    modifier = Modifier.background(BhpCardWhite)
                ) {
                    LOAD_TYPE_OPTIONS.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, fontWeight = if (opt == state.loadType) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.onLoadTypeSelected(opt)
                                showLoadTypeDropdown = false
                            }
                        )
                    }
                }
            }
            if (state.loadType == "Other") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.customLoadType,
                    onValueChange = { viewModel.onCustomLoadTypeChange(it) },
                    placeholder = { Text("Specify custom load type...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BhpCardWhite,
                        unfocusedContainerColor = BhpCardWhite,
                        focusedBorderColor = BhpRunningGreen,
                        unfocusedBorderColor = BhpCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.loadTypeError != null) {
                Text(
                    text = state.loadTypeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 10. Remarks Multiline Optional
            Text(
                text = "Remarks (Optional)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BhpTextPrimary,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.remarks,
                onValueChange = { viewModel.onRemarksChange(it) },
                placeholder = { Text("Enter any additional remarks...", fontSize = 12.5.sp, color = Color(0xFF94A3B8)) },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BhpCardWhite,
                    unfocusedContainerColor = BhpCardWhite,
                    focusedBorderColor = BhpRunningGreen,
                    unfocusedBorderColor = BhpCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 11. Trip Documents (Optional)
            DocumentUploadStagingSection(
                title = "Trip Documents",
                subtitle = "E-Way Bill, Tax Invoice, Delivery Challan, LR",
                stagedDocuments = state.stagedDocuments,
                onAddStagedDocument = { viewModel.onAddStagedDocument(it) },
                onRemoveStagedDocument = { viewModel.onRemoveStagedDocument(it) },
                suggestedDocumentTypes = listOf("E-Way Bill", "Tax Invoice", "Delivery Challan", "Lorry Receipt (LR)")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Dialog: Select Lorry
    if (showLorryDialog) {
        AlertDialog(
            onDismissRequest = { showLorryDialog = false },
            containerColor = BhpCardWhite,
            title = {
                Text(
                    text = "Select Commercial Lorry",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BhpNavyDark)
                )
            },
            text = {
                if (state.lorries.isEmpty()) {
                    Text(
                        text = "No eligible commercial lorries found in fleet.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = BhpTextSecondary)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.lorries.forEach { lorry ->
                            val isSelected = lorry.id == state.selectedLorry?.id
                            Surface(
                                color = if (isSelected) BhpRunningGreenBg else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BhpRunningGreen else BhpCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onLorrySelected(lorry)
                                        showLorryDialog = false
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    DashboardTruckIcon(
                                        tint = if (isSelected) BhpRunningGreen else BhpNavyDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lorry.registrationNumber,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BhpTextPrimary
                                            )
                                        )
                                        Text(
                                            text = "${lorry.makeModel} • ${lorry.capacityTons} Tons",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BhpTextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLorryDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = BhpNavyDark)
                }
            }
        )
    }

    // Modal Dialog: Select Driver
    if (showDriverDialog) {
        AlertDialog(
            onDismissRequest = { showDriverDialog = false },
            containerColor = BhpCardWhite,
            title = {
                Text(
                    text = "Select Authorized Driver",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BhpNavyDark)
                )
            },
            text = {
                if (state.drivers.isEmpty()) {
                    Text(
                        text = "No active drivers available.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = BhpTextSecondary)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.drivers.forEach { driver ->
                            val isSelected = driver.id == state.selectedDriver?.id
                            Surface(
                                color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BhpDriverBlue else BhpCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onDriverSelected(driver)
                                        showDriverDialog = false
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    DriverCapIcon(
                                        tint = if (isSelected) BhpDriverBlue else BhpNavyDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = driver.fullName,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BhpTextPrimary
                                            )
                                        )
                                        val sub = driver.phoneNumber ?: "@${driver.username}"
                                        Text(
                                            text = sub + (driver.assignedLorryRegistration?.let { " • Assigned: $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BhpTextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDriverDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = BhpNavyDark)
                }
            }
        )
    }
}

@Composable
fun SelectableBox(
    icon: @Composable () -> Unit,
    text: String,
    isPlaceholder: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BhpCardWhite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error else BhpCardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            icon()
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isPlaceholder) Color(0xFF94A3B8) else BhpTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.Medium
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            ChevronRightIcon(
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
