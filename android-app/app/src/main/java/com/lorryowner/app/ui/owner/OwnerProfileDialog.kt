package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.KeyVectorIcon
import com.lorryowner.app.ui.components.LogoutVectorIcon
import com.lorryowner.app.ui.components.ShieldVectorIcon
import com.lorryowner.app.ui.theme.BhpAvatarBorder
import com.lorryowner.app.ui.theme.BhpAvatarCream
import com.lorryowner.app.ui.theme.BhpCardBorder
import com.lorryowner.app.ui.theme.BhpCardWhite
import com.lorryowner.app.ui.theme.BhpDarkForestGreen
import com.lorryowner.app.ui.theme.BhpErrorRed
import com.lorryowner.app.ui.theme.BhpInputBg
import com.lorryowner.app.ui.theme.BhpInputBorder
import com.lorryowner.app.ui.theme.BhpNavyDark
import com.lorryowner.app.ui.theme.BhpRunningGreen
import com.lorryowner.app.ui.theme.BhpRunningGreenBg
import com.lorryowner.app.ui.theme.BhpRunningGreenText
import com.lorryowner.app.ui.theme.BhpTextBlack
import com.lorryowner.app.ui.theme.BhpTextMuted
import com.lorryowner.app.ui.theme.BhpTextPrimary
import com.lorryowner.app.ui.theme.BhpTextSecondary

/**
 * Polished modal profile screen for the authenticated Owner.
 *
 * Requirements enforced:
 * - Real authenticated user data (Name, Username, Role: OWNER).
 * - Change Password navigation action.
 * - Logout action with mandatory confirmation dialog.
 * - Premium BHP Logistics styling matching the brand identity.
 */
@Composable
fun OwnerProfileDialog(
    ownerName: String,
    username: String,
    onDismiss: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            containerColor = BhpCardWhite,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2))
                    ) {
                        LogoutVectorIcon(tint = BhpErrorRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary
                        )
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of BHP Logistics Fleet Manager? You will need to enter your password to sign in again.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BhpTextSecondary,
                        lineHeight = 20.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onDismiss()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BhpErrorRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel", color = BhpTextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = BhpCardWhite,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BhpCardBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar with Initial
                val initial = ownerName.firstOrNull()?.uppercase()
                    ?: username.firstOrNull()?.uppercase()
                    ?: "O"

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BhpAvatarCream)
                        .border(3.dp, BhpAvatarBorder, CircleShape)
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpNavyDark,
                            fontSize = 32.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real Owner Name
                Text(
                    text = ownerName.ifEmpty { "Fleet Owner" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 20.sp
                    )
                )

                // Username
                Text(
                    text = "@${username.ifEmpty { "owner" }}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BhpTextSecondary,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Role and Status Badges
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Role Badge
                    Surface(
                        color = BhpDarkForestGreen.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BhpDarkForestGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            ShieldVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ROLE: OWNER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpDarkForestGreen,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Active Badge
                    Surface(
                        color = BhpRunningGreenBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpRunningGreenText,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = BhpInputBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Profile Menu Options Section
                // 1. Change Password Action Card
                Surface(
                    color = BhpInputBg,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BhpInputBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDismiss()
                            onChangePasswordClick()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BhpDarkForestGreen.copy(alpha = 0.12f))
                        ) {
                            KeyVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Change Password",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "Update account security credentials",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        ChevronRightIcon(tint = BhpTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Logout Action Card
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showLogoutConfirmation = true
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEE2E2))
                        ) {
                            LogoutVectorIcon(tint = BhpErrorRed, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Log Out",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpErrorRed,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "End active session and lock portal",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFB91C1C),
                                    fontSize = 12.sp
                                )
                            )
                        }

                        ChevronRightIcon(tint = BhpErrorRed, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BhpInputBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BhpTextSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}
