package com.lorryowner.app.ui.login

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.R
import com.lorryowner.app.network.ApiConfig
import com.lorryowner.app.ui.components.ArrowForwardVectorIcon
import com.lorryowner.app.ui.components.ErrorVectorIcon
import com.lorryowner.app.ui.components.LockVectorIcon
import com.lorryowner.app.ui.components.PersonVectorIcon
import com.lorryowner.app.ui.components.ShieldVectorIcon
import com.lorryowner.app.ui.components.VisibilityOffVectorIcon
import com.lorryowner.app.ui.components.VisibilityVectorIcon
import com.lorryowner.app.ui.theme.BhpCardWhite
import com.lorryowner.app.ui.theme.BhpDarkForestGreen
import com.lorryowner.app.ui.theme.BhpDeepTeal
import com.lorryowner.app.ui.theme.BhpFooterDivider
import com.lorryowner.app.ui.theme.BhpGold
import com.lorryowner.app.ui.theme.BhpInputBg
import com.lorryowner.app.ui.theme.BhpInputBorder
import com.lorryowner.app.ui.theme.BhpInputDivider
import com.lorryowner.app.ui.theme.BhpMaroonPrimary
import com.lorryowner.app.ui.theme.BhpTextBlack
import com.lorryowner.app.ui.theme.BhpTextMuted

/**
 * BHP Logistics Login Screen faithfully reproducing the reference image:
 *
 * 1. Upper Section:
 *    - Real truck-and-highway scene dedicated asset (R.drawable.bhp_truck_highway)
 *    - Dark green & white heavy goods vehicle with "BHP Logistics" on the container
 *    - Realistic road, hills, vegetation, and sunset glow
 *    - White truck logo near upper left
 *    - "BHP Logistics" brand name (BHP in white, Logistics in green)
 *    - Tagline: "Your Fleet. Your Control." with green accent underline
 *
 * 2. Lower Section (Real Compose Components):
 *    - White login panel with rounded top corners (32.dp)
 *    - "Welcome Back" bold heading
 *    - "Sign in to continue" subtitle
 *    - Username input with person icon & vertical divider
 *    - Password input with lock icon, vertical divider & visibility toggle
 *    - Dark Forest Green "Login" button with white right arrow
 *    - Shield icon with "Secure Fleet Management" footer & horizontal lines
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val passwordFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BhpCardWhite) // White base ensures no dark gaps on tall devices
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Upper Section: Photographic Truck & Highway Header Scene with Official BHP Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BhpMaroonPrimary)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bhp_truck_highway),
                    contentDescription = "BHP Logistics Goods Transport Lorry on Highway at Sunset",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )

                // Official BHP Logistics Badge
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, top = 12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bhp_logo),
                        contentDescription = "BHP Logistics Official Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(4.dp)
                    )
                }
            }

            // 2. Lower Section: Pure White Card Overlapping the Bottom Edge of the Road
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp) // Seamlessly overlaps the road so rounded corners reveal the road on left & right
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 20.dp)
                ) {
                    // "Welcome Back" Heading
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = BhpTextBlack
                        )
                    )

                    // "Sign in to continue" Subtitle
                    Text(
                        text = "Sign in to continue",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            color = BhpTextMuted
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Informational Banner (e.g., after password change)
                    if (uiState.infoMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            ShieldVectorIcon(
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.infoMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Validation / Error Message Banner (if error exists)
                    if (uiState.errorMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            ErrorVectorIcon(
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Username Input Field
                    OutlinedTextField(
                        value = uiState.usernameInput,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        placeholder = {
                            Text(
                                text = "Enter username",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 15.sp
                                )
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 14.dp, end = 10.dp)
                            ) {
                                PersonVectorIcon(
                                    tint = BhpDarkForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                // Vertical divider line
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(20.dp)
                                        .background(BhpInputDivider)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BhpTextBlack,
                            unfocusedTextColor = BhpTextBlack,
                            focusedContainerColor = BhpInputBg,
                            unfocusedContainerColor = BhpInputBg,
                            focusedBorderColor = BhpDarkForestGreen,
                            unfocusedBorderColor = BhpInputBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Username input field" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input Field
                    OutlinedTextField(
                        value = uiState.passwordInput,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        placeholder = {
                            Text(
                                text = "Enter password",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 15.sp
                                )
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 14.dp, end = 10.dp)
                            ) {
                                LockVectorIcon(
                                    tint = BhpDarkForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                // Vertical divider line
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(20.dp)
                                        .background(BhpInputDivider)
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.togglePasswordVisibility() },
                                modifier = Modifier.semantics {
                                    contentDescription = if (uiState.isPasswordVisible) "Hide password" else "Show password"
                                }
                            ) {
                                if (uiState.isPasswordVisible) {
                                    VisibilityVectorIcon(
                                        tint = BhpTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    VisibilityOffVectorIcon(
                                        tint = BhpTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.login(onLoginSuccess)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BhpTextBlack,
                            unfocusedTextColor = BhpTextBlack,
                            focusedContainerColor = BhpInputBg,
                            unfocusedContainerColor = BhpInputBg,
                            focusedBorderColor = BhpDarkForestGreen,
                            unfocusedBorderColor = BhpInputBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                            .semantics { contentDescription = "Password input field" }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Maroon & Gold Login Button with Arrow
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.login(onLoginSuccess)
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BhpMaroonPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = BhpMaroonPrimary.copy(alpha = 0.6f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .semantics { contentDescription = "Login button" }
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = BhpGold,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Login",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                ArrowForwardVectorIcon(
                                    tint = BhpGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Footer: Shield Icon, "Secure Fleet Management", and Side Horizontal Dividers
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = BhpFooterDivider,
                            thickness = 1.dp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            ShieldVectorIcon(
                                tint = BhpDarkForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Secure Fleet Management",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpTextMuted,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = BhpFooterDivider,
                            thickness = 1.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
