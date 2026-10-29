package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BackArrowIcon
import com.lorryowner.app.ui.components.CheckmarkCircleIcon
import com.lorryowner.app.ui.components.ErrorVectorIcon
import com.lorryowner.app.ui.components.KeyVectorIcon
import com.lorryowner.app.ui.components.LockVectorIcon
import com.lorryowner.app.ui.components.ShieldVectorIcon
import com.lorryowner.app.ui.components.VisibilityOffVectorIcon
import com.lorryowner.app.ui.components.VisibilityVectorIcon
import com.lorryowner.app.ui.theme.BhpCardBorder
import com.lorryowner.app.ui.theme.BhpCardWhite
import com.lorryowner.app.ui.theme.BhpDarkForestGreen
import com.lorryowner.app.ui.theme.BhpErrorRed
import com.lorryowner.app.ui.theme.BhpInputBg
import com.lorryowner.app.ui.theme.BhpInputBorder
import com.lorryowner.app.ui.theme.BhpNavyDark
import com.lorryowner.app.ui.theme.BhpRunningGreen
import com.lorryowner.app.ui.theme.BhpRunningGreenBg
import com.lorryowner.app.ui.theme.BhpScreenBg
import com.lorryowner.app.ui.theme.BhpTextBlack
import com.lorryowner.app.ui.theme.BhpTextMuted
import com.lorryowner.app.ui.theme.BhpTextPrimary
import com.lorryowner.app.ui.theme.BhpTextSecondary

/**
 * Change Password Screen for authenticated Owner.
 *
 * Requirements enforced:
 * - Current Password, New Password, Confirm New Password fields with visibility toggles.
 * - Dynamic visual checklist of backend security requirements.
 * - Prevents multiple submissions while request in-progress.
 * - Cancels/navigates back to dashboard.
 * - Invokes onPasswordChanged() callback on success for session invalidation and login redirect.
 */
@Composable
fun ChangePasswordScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit,
    onPasswordChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSession = remember { sessionManager ?: SessionManager(context) }
    val viewModel = remember { ChangePasswordViewModel(effectiveSession) }
    val state = viewModel.uiState
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            ChangePasswordTopBar(onBack = onBack)
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpScreenBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header Info Card
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BhpRunningGreenBg)
                    ) {
                        ShieldVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Account Security",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary
                            )
                        )
                        Text(
                            text = "For your protection, changing your password invalidates existing sessions. You will be redirected to log in again.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Form Card
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        text = "Update Credentials",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = "Enter your current and new password below.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BhpTextSecondary)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Current Password
                    Text(
                        text = "CURRENT PASSWORD *",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextMuted,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.currentPassword,
                        onValueChange = { viewModel.onCurrentPasswordChange(it) },
                        placeholder = { Text("Enter current password", color = BhpTextMuted, fontSize = 14.sp) },
                        leadingIcon = { LockVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleCurrentPasswordVisibility() }) {
                                if (state.isCurrentPasswordVisible) {
                                    VisibilityOffVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp))
                                } else {
                                    VisibilityVectorIcon(tint = BhpTextMuted, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        visualTransformation = if (state.isCurrentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BhpInputBg,
                            unfocusedContainerColor = BhpInputBg,
                            focusedBorderColor = BhpDarkForestGreen,
                            unfocusedBorderColor = BhpInputBorder,
                            focusedTextColor = BhpTextBlack,
                            unfocusedTextColor = BhpTextBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. New Password
                    Text(
                        text = "NEW PASSWORD *",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextMuted,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = { viewModel.onNewPasswordChange(it) },
                        placeholder = { Text("At least 8 chars with upper, lower & digit", color = BhpTextMuted, fontSize = 14.sp) },
                        leadingIcon = { KeyVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleNewPasswordVisibility() }) {
                                if (state.isNewPasswordVisible) {
                                    VisibilityOffVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp))
                                } else {
                                    VisibilityVectorIcon(tint = BhpTextMuted, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        visualTransformation = if (state.isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BhpInputBg,
                            unfocusedContainerColor = BhpInputBg,
                            focusedBorderColor = BhpDarkForestGreen,
                            unfocusedBorderColor = BhpInputBorder,
                            focusedTextColor = BhpTextBlack,
                            unfocusedTextColor = BhpTextBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Confirm New Password
                    Text(
                        text = "CONFIRM NEW PASSWORD *",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextMuted,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChange(it) },
                        placeholder = { Text("Re-enter new password", color = BhpTextMuted, fontSize = 14.sp) },
                        leadingIcon = { LockVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                                if (state.isConfirmPasswordVisible) {
                                    VisibilityOffVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp))
                                } else {
                                    VisibilityVectorIcon(tint = BhpTextMuted, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.submitChangePassword(onSuccess = onPasswordChanged)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BhpInputBg,
                            unfocusedContainerColor = BhpInputBg,
                            focusedBorderColor = BhpDarkForestGreen,
                            unfocusedBorderColor = BhpInputBorder,
                            focusedTextColor = BhpTextBlack,
                            unfocusedTextColor = BhpTextBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Password Requirements Checklist Card
                    Surface(
                        color = BhpInputBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BhpInputBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "PASSWORD REQUIREMENTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextMuted,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            RequirementRow(label = "Minimum 8 characters", isMet = state.hasMinLength)
                            Spacer(modifier = Modifier.height(4.dp))
                            RequirementRow(label = "At least one uppercase letter (A-Z)", isMet = state.hasUppercase)
                            Spacer(modifier = Modifier.height(4.dp))
                            RequirementRow(label = "At least one lowercase letter (a-z)", isMet = state.hasLowercase)
                            Spacer(modifier = Modifier.height(4.dp))
                            RequirementRow(label = "At least one number (0-9)", isMet = state.hasDigit)
                            Spacer(modifier = Modifier.height(4.dp))
                            RequirementRow(label = "New password and confirmation match", isMet = state.passwordsMatch)
                        }
                    }

                    // Error Message Banner
                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ErrorVectorIcon(tint = BhpErrorRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BhpErrorRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save Password Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.submitChangePassword(onSuccess = onPasswordChanged)
                        },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BhpDarkForestGreen,
                            contentColor = Color.White,
                            disabledContainerColor = BhpDarkForestGreen.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Saving Password...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Text("Save Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cancel Button
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BhpInputBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BhpTextSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun RequirementRow(label: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isMet) {
            CheckmarkCircleIcon(tint = BhpRunningGreen, modifier = Modifier.size(16.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = if (isMet) BhpDarkForestGreen else BhpTextMuted,
                fontWeight = if (isMet) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

/**
 * Top App Bar for Change Password screen.
 */
@Composable
private fun ChangePasswordTopBar(onBack: () -> Unit) {
    Surface(
        color = BhpNavyDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                BackArrowIcon(tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Owner Account Security",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
