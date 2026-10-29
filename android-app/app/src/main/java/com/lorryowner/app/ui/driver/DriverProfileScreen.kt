package com.lorryowner.app.ui.driver

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.theme.*

@Composable
fun DriverProfileScreen(
    viewModel: DriverDashboardViewModel,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.profileState
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BhpScreenBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BhpTextPrimary
                )
                IconButton(
                    onClick = { /* Edit profile shortcut */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    EditPencilIcon(tint = BhpDriverBlue, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Driver Avatar & Header Info
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(18.dp)
            ) {
                // Driver Avatar circle with peaked cap
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, BhpDriverBlue, CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = state.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BhpTextPrimary
                    )
                    Text(
                        text = state.role,
                        fontSize = 13.sp,
                        color = BhpTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isOnline) "Online" else "Offline",
                            fontSize = 12.sp,
                            color = if (state.isOnline) Color(0xFF16A34A) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Personal & Asset Details Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileDetailRow(
                    icon = { PhoneCallVectorIcon(tint = BhpDriverBlue, modifier = Modifier.size(16.dp)) },
                    label = state.phoneNumber.ifEmpty { "+91 98765 43210" }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BhpScreenBg)

                ProfileDetailRow(
                    icon = { DocumentTextIcon(tint = BhpDriverBlue, modifier = Modifier.size(16.dp)) },
                    label = state.email
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BhpScreenBg)

                ProfileDetailRow(
                    icon = { DashboardTruckIcon(tint = BhpDriverBlue, modifier = Modifier.size(16.dp)) },
                    label = "Assigned Lorry: ${state.assignedLorryRegistration} (${state.assignedLorryMakeModel})"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BhpScreenBg)

                ProfileDetailRow(
                    icon = { CheckSquareIcon(tint = BhpDriverBlue, modifier = Modifier.size(16.dp)) },
                    label = "License: ${state.licenseNumber}"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BhpScreenBg)

                ProfileDetailRow(
                    icon = { CalendarIcon(tint = BhpDriverBlue, modifier = Modifier.size(16.dp)) },
                    label = "Joined On: ${state.joinedOn}"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Settings & Actions Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ProfileSettingRow(
                    icon = { LockVectorIcon(tint = BhpTextPrimary, modifier = Modifier.size(18.dp)) },
                    label = "Change Password",
                    onClick = onChangePassword
                )
                HorizontalDivider(color = BhpScreenBg)

                ProfileSettingRow(
                    icon = { GlobeVectorIcon(tint = BhpTextPrimary, modifier = Modifier.size(18.dp)) },
                    label = "Language",
                    trailingText = "English",
                    onClick = {}
                )
                HorizontalDivider(color = BhpScreenBg)

                ProfileSettingRow(
                    icon = { HelpQuestionVectorIcon(tint = BhpTextPrimary, modifier = Modifier.size(18.dp)) },
                    label = "Help & Support",
                    onClick = { showHelpDialog = true }
                )
                HorizontalDivider(color = BhpScreenBg)

                // Logout Action Row (Red)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    LogoutVectorIcon(tint = BhpErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Logout",
                        color = BhpErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "End Shift & Logout?",
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to end your driver shift and sign out of BHP Logistics?",
                    color = BhpTextSecondary,
                    fontSize = 13.sp
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BhpErrorRed)
                ) {
                    Text("Logout", color = Color.White)
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Driver Support", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "For emergency road assistance or route support, please contact the central dispatch team at support@bhp-logistics.com or call toll-free 1800-BHP-HELP.",
                    fontSize = 13.sp,
                    color = BhpTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpDriverBlue)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: @Composable () -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = BhpTextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProfileSettingRow(
    icon: @Composable () -> Unit,
    label: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = BhpTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 13.sp,
                    color = BhpTextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            ChevronRightIcon(tint = BhpTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
