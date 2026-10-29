package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.theme.*

val VEHICLE_TYPE_OPTIONS = listOf(
    "Heavy Commercial Vehicle (HCV)",
    "Medium Commercial Vehicle (MCV)",
    "Light Commercial Vehicle (LCV)",
    "Multi-Axle Truck",
    "Container Trailer",
    "Tipper / Dump Truck",
    "Tanker"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLorryScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit,
    onLorryCreated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = remember { AddLorryViewModel(sessionManager) }
    val state = viewModel.uiState
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var showVehicleTypeDropdown by remember { mutableStateOf(false) }
    var showDriverDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = BhpNavyDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.size(36.dp)
                    ) {
                        BackArrowIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Commercial Lorry",
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !state.isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BhpNavyDark),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BhpCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }

                    // Save Lorry Button
                    Button(
                        onClick = {
                            viewModel.submitLorry { createdId ->
                                onLorryCreated(createdId)
                            }
                        },
                        enabled = !state.isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BhpNavyDark,
                            contentColor = Color.White,
                            disabledContainerColor = BhpNavyDark.copy(alpha = 0.6f),
                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saving...",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Save Lorry",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = BhpScreenBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Error Banner (e.g. duplicate registration)
            if (state.errorMessage != null) {
                Surface(
                    color = BhpDriverInactiveBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BhpErrorRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(BhpErrorRed)
                        ) {
                            Text(
                                text = "!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Registration Failed",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpDriverInactiveText,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpDriverInactiveText,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Header Hero Banner
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BhpRunningGreenBg)
                    ) {
                        DashboardTruckIcon(tint = BhpRunningGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Register Fleet Vehicle",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add a commercial lorry to your fleet assets",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Section 1: Vehicle Information
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "VEHICLE INFORMATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )

                    // Registration Number Field
                    Column {
                        Text(
                            text = "Registration Number *",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.registrationNumber,
                            onValueChange = { viewModel.onRegistrationNumberChange(it) },
                            placeholder = { Text("e.g. AP 31 TC 1234 or AP04-TU-1234", fontSize = 13.sp) },
                            isError = state.registrationNumberError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BhpNavyDark,
                                unfocusedBorderColor = BhpCardBorder,
                                errorBorderColor = BhpErrorRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.registrationNumberError != null) {
                            Text(
                                text = state.registrationNumberError,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpErrorRed,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }

                    // Vehicle Make & Model Field
                    Column {
                        Text(
                            text = "Vehicle Make & Model *",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.makeModel,
                            onValueChange = { viewModel.onMakeModelChange(it) },
                            placeholder = { Text("e.g. Tata Prima 407, Ashok Leyland 2820", fontSize = 13.sp) },
                            isError = state.makeModelError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BhpNavyDark,
                                unfocusedBorderColor = BhpCardBorder,
                                errorBorderColor = BhpErrorRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.makeModelError != null) {
                            Text(
                                text = state.makeModelError,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpErrorRed,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }

                    // Vehicle Type / Category Dropdown
                    Column {
                        Text(
                            text = "Vehicle Category *",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box {
                            Surface(
                                color = BhpCardWhite,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showVehicleTypeDropdown = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = state.vehicleType,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = BhpTextPrimary,
                                            fontSize = 13.sp
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "▼",
                                        color = BhpTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showVehicleTypeDropdown,
                                onDismissRequest = { showVehicleTypeDropdown = false }
                            ) {
                                VEHICLE_TYPE_OPTIONS.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.onVehicleTypeChange(option)
                                            showVehicleTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Payload Capacity (Tons) & Manufacturing Year
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Capacity Tons
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Capacity (Tons) *",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = state.capacityTons,
                                onValueChange = { viewModel.onCapacityTonsChange(it) },
                                placeholder = { Text("e.g. 16.0", fontSize = 13.sp) },
                                isError = state.capacityTonsError != null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BhpNavyDark,
                                    unfocusedBorderColor = BhpCardBorder,
                                    errorBorderColor = BhpErrorRed
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.capacityTonsError != null) {
                                Text(
                                    text = state.capacityTonsError,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BhpErrorRed,
                                        fontSize = 10.5.sp
                                    ),
                                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                                )
                            }
                        }

                        // Manufacturing Year
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Year (Optional)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = state.manufacturingYear,
                                onValueChange = { viewModel.onManufacturingYearChange(it) },
                                placeholder = { Text("e.g. 2023", fontSize = 13.sp) },
                                isError = state.manufacturingYearError != null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BhpNavyDark,
                                    unfocusedBorderColor = BhpCardBorder,
                                    errorBorderColor = BhpErrorRed
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.manufacturingYearError != null) {
                                Text(
                                    text = state.manufacturingYearError,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BhpErrorRed,
                                        fontSize = 10.5.sp
                                    ),
                                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                                )
                            }
                        }
                    }

                    // iAlert Telematics Unit ID (Optional)
                    Column {
                        Text(
                            text = "iAlert / Telematics Unit ID (Optional)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.ialertUnitId,
                            onValueChange = { viewModel.onIalertUnitIdChange(it) },
                            placeholder = { Text("e.g. IALERT-UNIT-407", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BhpNavyDark,
                                unfocusedBorderColor = BhpCardBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section 2: Driver Assignment (Optional)
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DRIVER ASSIGNMENT (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextSecondary,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                        if (state.selectedDriver != null) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpErrorRed,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.onDriverSelected(null) }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Driver selection card
                    Surface(
                        color = if (state.selectedDriver != null) Color(0xFFEFF6FF) else BhpInputBg,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.selectedDriver != null) BhpReadyBlue.copy(alpha = 0.4f) else BhpCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDriverDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (state.selectedDriver != null) BhpReadyBlueBg else Color(0xFFE2E8F0))
                            ) {
                                DriverCapIcon(
                                    tint = if (state.selectedDriver != null) BhpReadyBlue else BhpTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.selectedDriver?.fullName ?: "None (Unassigned)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.selectedDriver != null) BhpTextPrimary else BhpTextSecondary,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = state.selectedDriver?.let {
                                        it.phoneNumber?.let { p -> "Phone: $p" } ?: "Username: ${it.username}"
                                    } ?: "Tap to assign an available driver",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BhpTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Text(
                                text = "Choose",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpDriverBlue,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    if (state.availableDrivers.isEmpty() && !state.isLoadingDrivers) {
                        Text(
                            text = "No active drivers available. You can register drivers in Manage Drivers or assign one later.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }

            // Section 3: Notes / Additional Specifications
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ADDITIONAL NOTES (OPTIONAL)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { viewModel.onNotesChange(it) },
                        placeholder = { Text("e.g. Regular route: Hyderabad to Bengaluru. Insured till Dec 2026.", fontSize = 13.sp) },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BhpNavyDark,
                            unfocusedBorderColor = BhpCardBorder
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Driver Selection Dialog
    if (showDriverDialog) {
        AlertDialog(
            onDismissRequest = { showDriverDialog = false },
            title = {
                Text(
                    text = "Select Driver",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: None (Unassigned)
                    Surface(
                        color = if (state.selectedDriver == null) Color(0xFFEFF6FF) else BhpCardWhite,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.selectedDriver == null) BhpReadyBlue else BhpCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.onDriverSelected(null)
                                showDriverDialog = false
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "None (Leave Unassigned)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (state.selectedDriver == null) {
                                Text(
                                    text = "✓",
                                    color = BhpReadyBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // List of available drivers
                    state.availableDrivers.forEach { driver ->
                        val isSelected = state.selectedDriver?.id == driver.id
                        Surface(
                            color = if (isSelected) Color(0xFFEFF6FF) else BhpCardWhite,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BhpReadyBlue else BhpCardBorder
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = driver.fullName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BhpTextPrimary
                                        )
                                    )
                                    Text(
                                        text = driver.phoneNumber?.let { "Phone: $it" } ?: "Username: ${driver.username}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BhpTextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    if (driver.assignedLorryRegistration != null) {
                                        Text(
                                            text = "Currently assigned to: ${driver.assignedLorryRegistration}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = BhpNotReadyAmberText,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = BhpReadyBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDriverDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
