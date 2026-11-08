package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.theme.*
import java.util.Locale

/**
 * Screen 2 of Stage 5: Trip Confirmation Screen.
 *
 * Displays a complete summary of the configured trip details before final submission.
 * Provides "Edit Details" (returns to form with state preserved) and "Confirm & Create"
 * (submits trip creation request exactly once).
 */
@Composable
fun TripConfirmationScreen(
    viewModel: AddTripViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTripCreated: (CreatedTripUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState
    val scrollState = rememberScrollState()

    val lorry = state.selectedLorry
    val driver = state.selectedDriver
    val effectiveLoadType = if (state.loadType == "Other") {
        state.customLoadType.ifBlank { "Other" }
    } else {
        state.loadType
    }

    val plannedDeliveryFormatted = if (state.plannedDeliveryDate.isNotBlank() && state.plannedDeliveryTime.isNotBlank()) {
        "${state.plannedDeliveryDate} at ${state.plannedDeliveryTime}"
    } else {
        "Not specified (ETA unavailable)"
    }

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
                        text = "Add Trip Confirmation",
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
                    // Edit Details Button
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !state.isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BhpNavyDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BhpCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        EditPencilIcon(tint = BhpNavyDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Details",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }

                    // Confirm & Create Button
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            viewModel.submitTrip(context) { created ->
                                onTripCreated(created)
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
                                text = state.uploadProgressText ?: "Creating...",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Confirm & Create",
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
            // Error banner if creation failed
            if (state.generalError != null) {
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
                                text = "Creation Failed",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpDriverInactiveText,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = state.generalError,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpDriverInactiveText,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Header instructions card
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BhpReadyBlueBg)
                    ) {
                        DocumentTextIcon(tint = BhpReadyBlue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Trip Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Review all details before scheduling this trip",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BhpTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Route Details Card
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ROUTE INFORMATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Origin
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(BhpRunningGreenBg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BhpRunningGreen)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Origin (Starting Location)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = state.origin.ifBlank { "—" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    // Vertical connecting line
                    Box(
                        modifier = Modifier
                            .padding(start = 11.dp, top = 4.dp, bottom = 4.dp)
                            .width(2.dp)
                            .height(22.dp)
                            .background(BhpCardBorder)
                    )

                    // Destination
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BhpErrorRed)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Destination",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = state.destination.ifBlank { "—" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }
            }

            // Lorry & Driver Information Card
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
                        text = "ASSIGNED ASSETS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )

                    // Lorry row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BhpReadyBlueBg)
                        ) {
                            DashboardTruckIcon(tint = BhpReadyBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lorry?.registrationNumber ?: "No lorry selected",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = lorry?.let { "${it.makeModel} • ${it.capacityTons} Tons" } ?: "—",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Driver row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BhpRunningGreenBg)
                        ) {
                            DriverCapIcon(tint = BhpRunningGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = driver?.fullName ?: "No driver selected",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = driver?.phoneNumber?.let { "Phone: $it" } ?: "Username: ${driver?.username ?: "—"}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Schedule Card
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
                        text = "SCHEDULE & TIMELINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )

                    // Departure
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF))
                        ) {
                            CalendarIcon(tint = BhpReadyBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Planned Departure",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "${state.plannedDepartureDate} at ${state.plannedDepartureTime}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Delivery
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            CalendarIcon(tint = BhpTextSecondary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Planned Delivery (ETA)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = plannedDeliveryFormatted,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (state.plannedDeliveryDate.isNotBlank()) BhpTextPrimary else BhpTextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            // Cargo & Remarks Card
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
                        text = "CARGO & INSTRUCTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )

                    // Cargo Type
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF7ED))
                        ) {
                            PackageBoxIcon(tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Load Type",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = effectiveLoadType,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Trip Amount (Revenue)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BhpRunningGreenBg)
                        ) {
                            FinanceTrendingIcon(tint = BhpRunningGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trip Amount (Freight Revenue)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            val parsedAmt = state.tripAmount.toDoubleOrNull() ?: 0.0
                            Text(
                                text = "₹" + String.format(Locale.getDefault(), "%,.0f", parsedAmt),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpRunningGreen,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    // Remarks if available
                    if (state.remarks.isNotBlank()) {
                        HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                        Column {
                            Text(
                                text = "Special Instructions / Remarks",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.remarks.trim(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            // Staged Trip Documents Card (if any)
            if (state.stagedDocuments.isNotEmpty()) {
                Surface(
                    color = BhpCardWhite,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TRIP DOCUMENTS (${state.stagedDocuments.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpDarkForestGreen,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        )

                        state.stagedDocuments.forEach { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("DOC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BhpTextPrimary)
                                    Text(
                                        "${doc.originalFilename} • ${com.lorryowner.app.network.DocumentApiService.formatFileSize(doc.sizeBytes)}",
                                        fontSize = 11.sp,
                                        color = BhpTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
