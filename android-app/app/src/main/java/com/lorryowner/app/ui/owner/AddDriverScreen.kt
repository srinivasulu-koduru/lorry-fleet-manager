package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BackArrowIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.LockVectorIcon
import com.lorryowner.app.ui.components.PersonVectorIcon
import com.lorryowner.app.ui.components.VisibilityOffVectorIcon
import com.lorryowner.app.ui.components.VisibilityVectorIcon
import com.lorryowner.app.ui.theme.BhpCardBorder
import com.lorryowner.app.ui.theme.BhpCardWhite
import com.lorryowner.app.ui.theme.BhpDriverBlue
import com.lorryowner.app.ui.theme.BhpDriverMetricBg
import com.lorryowner.app.ui.theme.BhpDriverMetricIconBg
import com.lorryowner.app.ui.theme.BhpInputBorder
import com.lorryowner.app.ui.theme.BhpInputDivider
import com.lorryowner.app.ui.theme.BhpScreenBg
import com.lorryowner.app.ui.theme.BhpTextBlack
import com.lorryowner.app.ui.theme.BhpTextMuted
import com.lorryowner.app.ui.theme.BhpTextPrimary
import com.lorryowner.app.ui.theme.BhpTextSecondary

/**
 * Add New Driver Screen UI connected to backend.
 * Allows the owner to input driver credentials (Full Name, Username, Temporary Password,
 * Confirm Password) and optional details (Phone, License, Assigned Lorry).
 */
@Composable
fun AddDriverScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit,
    onDriverCreated: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSession = remember { sessionManager ?: SessionManager(context) }
    val viewModel = remember { AddDriverViewModel(effectiveSession) }
    val uiState = viewModel.uiState

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            AddDriverTopBar(onBack = onBack)
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpScreenBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header Info Card
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        color = BhpDriverMetricIconBg,
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Commercial Driver Account",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary,
                                fontSize = 15.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Provision a secure mobile login account for your commercial driver.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Form Card
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = "Driver Credentials",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter driver identification and login credentials below.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 11.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Full Name
                    FormFieldLabel("Full Name", isRequired = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = { viewModel.onFullNameChange(it) },
                        placeholder = { Text("e.g. Ramesh Kumar", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                        leadingIcon = { PersonVectorIcon(tint = Color(0xFF64748B), modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = inputFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Username
                    FormFieldLabel("Username", isRequired = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.onUsernameChange(it) },
                        placeholder = { Text("e.g. driver_ramesh", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                        leadingIcon = {
                            Text(
                                text = "@",
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        ),
                        colors = inputFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Temporary Password
                    FormFieldLabel("Temporary Password", isRequired = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.tempPassword,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder = { Text("Minimum 8 characters", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                        leadingIcon = { LockVectorIcon(tint = Color(0xFF64748B), modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                if (uiState.isPasswordVisible) {
                                    VisibilityOffVectorIcon(tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                } else {
                                    VisibilityVectorIcon(tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = inputFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Confirm Password
                    FormFieldLabel("Confirm Password", isRequired = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChange(it) },
                        placeholder = { Text("Re-enter temporary password", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                        leadingIcon = { LockVectorIcon(tint = Color(0xFF64748B), modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                                if (uiState.isConfirmPasswordVisible) {
                                    VisibilityOffVectorIcon(tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                } else {
                                    VisibilityVectorIcon(tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = inputFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section Divider: Optional Details
                    Text(
                        text = "Optional Fleet Details",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Phone Number
                    FormFieldLabel("Phone Number", isRequired = false)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.phoneNumber,
                        onValueChange = { viewModel.onPhoneNumberChange(it) },
                        placeholder = { Text("e.g. 9876543210", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        colors = inputFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6. Driving License Number
                    FormFieldLabel("Driving License Number", isRequired = false)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.licenseNumber,
                        onValueChange = { viewModel.onLicenseNumberChange(it) },
                        placeholder = { Text("e.g. DL-0420110012345", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        colors = inputFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7. Assigned Lorry
                    FormFieldLabel("Assign Lorry Registration", isRequired = false)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.assignedLorry,
                        onValueChange = { viewModel.onAssignedLorryChange(it) },
                        placeholder = { Text("e.g. AP 31 TC 1234 (Leave blank if unassigned)", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        colors = inputFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error Alert Banner
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons: Create Driver & Cancel
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.submitDriver(onSuccess = {})
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BhpDriverBlue,
                            contentColor = Color.White,
                            disabledContainerColor = BhpDriverBlue.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Creating Account...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Create Driver Account",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onBack,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BhpTextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BhpTextSecondary,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Success / Confirmation Modal
    if (uiState.isSuccess) {
        AlertDialog(
            onDismissRequest = onDriverCreated,
            shape = RoundedCornerShape(20.dp),
            containerColor = BhpCardWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = BhpDriverMetricBg,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Driver Registered",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary
                        )
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Driver account '${uiState.successDriverName ?: uiState.fullName}' has been created successfully.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = BhpTextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Username: @${uiState.username.trim()}\nAssigned Lorry: ${if (uiState.assignedLorry.isBlank()) "None" else uiState.assignedLorry.trim()}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDriverCreated,
                    colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Return to Drivers", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun FormFieldLabel(label: String, isRequired: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = BhpTextPrimary,
                fontSize = 13.sp
            )
        )
        if (isRequired) {
            Text(
                text = " *",
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun inputFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xFFF8FAFC),
    disabledContainerColor = Color(0xFFF1F5F9),
    focusedBorderColor = BhpDriverBlue,
    unfocusedBorderColor = BhpInputBorder,
    focusedTextColor = BhpTextBlack,
    unfocusedTextColor = BhpTextBlack,
    cursorColor = BhpDriverBlue
)

/**
 * Top App Bar for Add Driver screen with Back Arrow and screen title.
 */
@Composable
private fun AddDriverTopBar(
    onBack: () -> Unit
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
                    text = "Add New Driver",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 19.sp
                    )
                )
                Text(
                    text = "Register a commercial fleet driver",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextSecondary,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
