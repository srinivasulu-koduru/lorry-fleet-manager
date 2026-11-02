package com.lorryowner.app.ui.owner

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.CalendarIcon
import com.lorryowner.app.ui.theme.*
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceScreen(
    initialLorryId: Long? = null,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onRecordSaved: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember(initialLorryId) {
        AddMaintenanceViewModel(
            sessionManager = sessionManager,
            initialLorryId = initialLorryId
        )
    }
    val state = viewModel.formState

    var lorryMenuExpanded by remember { mutableStateOf(false) }
    var serviceTypeExpanded by remember { mutableStateOf(false) }

    fun showDatePicker(currentDateStr: String, onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        if (currentDateStr.isNotBlank()) {
            try {
                val parsed = LocalDate.parse(currentDateStr)
                cal.set(parsed.year, parsed.monthValue - 1, parsed.dayOfMonth)
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = LocalDate.of(year, month + 1, dayOfMonth)
                onDateSelected(chosen.toString())
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
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
                        .padding(horizontal = 12.dp, vertical = 14.dp),
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Maintenance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            viewModel.submitRecord(onSuccess = onRecordSaved)
                        },
                        enabled = !state.isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save Record",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = onBack,
                        enabled = !state.isSubmitting
                    ) {
                        Text(
                            text = "Cancel",
                            color = BhpTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Error banner
            if (state.errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Dismiss", color = Color(0xFFDC2626), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Success banner
            if (state.successMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.successMessage,
                            color = Color(0xFF16A34A),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 1. Select Lorry Dropdown
            ExposedDropdownMenuBox(
                expanded = lorryMenuExpanded,
                onExpandedChange = { lorryMenuExpanded = !lorryMenuExpanded }
            ) {
                OutlinedTextField(
                    value = state.selectedLorryDisplay.ifEmpty { "Select Lorry" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Lorry") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lorryMenuExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = lorryMenuExpanded,
                    onDismissRequest = { lorryMenuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    state.lorries.forEach { lorry ->
                        DropdownMenuItem(
                            text = { Text("${lorry.registrationNumber} (${lorry.makeModel})", color = BhpTextPrimary) },
                            colors = MenuDefaults.itemColors(textColor = BhpTextPrimary),
                            onClick = {
                                viewModel.onLorrySelected(lorry)
                                lorryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Maintenance Type Dropdown
            ExposedDropdownMenuBox(
                expanded = serviceTypeExpanded,
                onExpandedChange = { serviceTypeExpanded = !serviceTypeExpanded }
            ) {
                OutlinedTextField(
                    value = state.serviceType.ifEmpty { "Maintenance Type —" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Maintenance Type —") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceTypeExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = serviceTypeExpanded,
                    onDismissRequest = { serviceTypeExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    AddMaintenanceViewModel.MAINTENANCE_TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = BhpTextPrimary) },
                            colors = MenuDefaults.itemColors(textColor = BhpTextPrimary),
                            onClick = {
                                viewModel.onServiceTypeChanged(type)
                                serviceTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. Service Date (Date picker)
            OutlinedTextField(
                value = state.serviceDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Service Date —") },
                trailingIcon = {
                    IconButton(onClick = {
                        showDatePicker(state.serviceDate) { chosen ->
                            viewModel.onServiceDateChanged(chosen)
                        }
                    }) {
                        CalendarIcon(tint = BhpTextSecondary, modifier = Modifier.size(20.dp))
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePicker(state.serviceDate) { chosen ->
                            viewModel.onServiceDateChanged(chosen)
                        }
                    }
            )

            // 4. Description (Multi-line)
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("Description —") },
                placeholder = { Text("Describe service or repairs performed...", color = BhpTextMuted) },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 5. Maintenance Cost (₹)
            OutlinedTextField(
                value = state.cost,
                onValueChange = { viewModel.onCostChanged(it) },
                label = { Text("Maintenance Cost (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Odometer Reading (km)
            OutlinedTextField(
                value = state.odometer,
                onValueChange = { viewModel.onOdometerChanged(it) },
                label = { Text("Odometer Reading (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 7. Next Service Date (optional)
            OutlinedTextField(
                value = state.nextServiceDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Next Service Date — optional") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.nextServiceDate.isNotBlank()) {
                            TextButton(onClick = { viewModel.onNextServiceDateChanged("") }) {
                                Text("Clear", fontSize = 11.sp, color = BhpTextSecondary)
                            }
                        }
                        IconButton(onClick = {
                            showDatePicker(state.nextServiceDate.ifEmpty { state.serviceDate }) { chosen ->
                                viewModel.onNextServiceDateChanged(chosen)
                            }
                        }) {
                            CalendarIcon(tint = BhpTextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePicker(state.nextServiceDate.ifEmpty { state.serviceDate }) { chosen ->
                            viewModel.onNextServiceDateChanged(chosen)
                        }
                    }
            )

            // 8. Garage / Mechanic (optional)
            OutlinedTextField(
                value = state.garage,
                onValueChange = { viewModel.onGarageChanged(it) },
                label = { Text("Garage / Mechanic — optional") },
                placeholder = { Text("e.g. SRS Auto Center, Vijayawada", color = BhpTextMuted) },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 9. Additional Notes (optional)
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                label = { Text("Additional Notes — optional") },
                placeholder = { Text("Any warranty, parts replaced or follow-up notes...", color = BhpTextMuted) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
