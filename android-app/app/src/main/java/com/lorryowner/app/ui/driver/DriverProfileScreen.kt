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
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        Surface(
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFFE2E8F0))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Driver Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Profile Hero Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(18.dp)
            ) {
                // Large Avatar Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFECFDF5))
                        .border(2.dp, Color(0xFF10B981), CircleShape)
                ) {
                    val initial = state.fullName.firstOrNull()?.uppercaseChar()?.toString()
                        ?: state.username.firstOrNull()?.uppercaseChar()?.toString() ?: "D"
                    Text(
                        text = initial,
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "@${state.username}",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = state.role.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Status dot
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isOnline) Color(0xFF16A34A) else Color(0xFFDC2626))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isOnline) "Active" else "Offline",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (state.isOnline) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Identity & Contact Information Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Contact & Credentials",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProfileInfoRow(
                    label = "Phone Number",
                    value = state.phoneNumber.ifEmpty { "Not specified" },
                    icon = { PhoneCallVectorIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                ProfileInfoRow(
                    label = "Email Address",
                    value = state.email.ifEmpty { "driver@bhp-logistics.com" },
                    icon = { DocumentTextIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                ProfileInfoRow(
                    label = "Driving License",
                    value = state.licenseNumber.ifEmpty { "DL Verified" },
                    icon = { CheckSquareIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Assigned Fleet Vehicle Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Assigned Vehicle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProfileInfoRow(
                    label = "Lorry Registration",
                    value = state.assignedLorryRegistration.ifEmpty { "Unassigned" },
                    icon = { DashboardTruckIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) }
                )

                if (state.assignedLorryMakeModel.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                    ProfileInfoRow(
                        label = "Vehicle Model",
                        value = state.assignedLorryMakeModel,
                        icon = { DashboardTruckIcon(tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Security & Account Actions
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                ProfileClickableRow(
                    title = "Change Password",
                    icon = { LockVectorIcon(tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp)) },
                    onClick = onChangePassword
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                ProfileClickableRow(
                    title = "Dispatch Support & Help",
                    icon = { HelpQuestionVectorIcon(tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp)) },
                    onClick = { showHelpDialog = true }
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Logout Action Row (Red)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    LogoutVectorIcon(tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "End Shift & Logout",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "End Shift & Logout?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to end your driver shift and sign out of BHP Logistics?",
                    color = Color(0xFF475569),
                    fontSize = 13.5.sp
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = Color(0xFF475569))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "Driver Support Desk",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "For emergency breakdown assistance or route coordination, contact the central dispatch team at support@bhp-logistics.com or call toll-free +91 94400 12345.",
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.5.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProfileClickableRow(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Medium
            )
        }

        ChevronRightIcon(tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
    }
}
