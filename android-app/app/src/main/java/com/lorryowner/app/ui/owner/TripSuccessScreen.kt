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

/**
 * Screen 3 of Stage 5: Trip Creation Success Screen.
 *
 * Displays the newly created trip with its backend-generated tripReference
 * (e.g. TRIP-2026-1004) and confirmed asset assignments.
 * Offers "View Trip Details" and "Back to Home".
 */
@Composable
fun TripSuccessScreen(
    createdTrip: CreatedTripUiState,
    onViewTripDetails: (lorryId: Long, tripReference: String) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

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
                        onClick = onBackToHome,
                        modifier = Modifier.size(36.dp)
                    ) {
                        BackArrowIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trip Created",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        },
        containerColor = BhpScreenBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Large Success Checkmark Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(BhpRunningGreenBg)
                    .border(3.dp, BhpRunningGreen, CircleShape)
            ) {
                CheckmarkCircleIcon(
                    tint = BhpRunningGreen,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Trip Created Successfully!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Trip Reference Pill
            Surface(
                color = BhpReadyBlueBg,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpReadyBlue.copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Reference ID: ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = createdTrip.tripReference,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpReadyBlueText,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status chip: PLANNED
            Surface(
                color = BhpNotReadyAmberBg,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpNotReadyAmber.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BhpNotReadyAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STATUS: ${createdTrip.status}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpNotReadyAmberText,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Card
            Surface(
                color = BhpCardWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "TRIP SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )

                    // Route Row
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
                            LocationPinIcon(tint = BhpReadyBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Route",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "${createdTrip.origin}  →  ${createdTrip.destination}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Lorry Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BhpReadyBlueBg)
                        ) {
                            DashboardTruckIcon(tint = BhpReadyBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lorry Registration",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = createdTrip.lorryRegistration,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Driver Row
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
                            DriverCapIcon(tint = BhpRunningGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assigned Driver",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = createdTrip.driverName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpTextPrimary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Planned Departure Row
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
                                text = createdTrip.plannedDepartureFormatted,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Planned Delivery Row
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
                                text = "Planned Delivery",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = createdTrip.plannedDeliveryFormatted ?: "Not specified (ETA unavailable)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (createdTrip.plannedDeliveryFormatted != null) BhpTextPrimary else BhpTextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                    // Cargo Type Row
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
                                text = "Cargo Type",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = createdTrip.loadType,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    if (!createdTrip.remarks.isNullOrBlank()) {
                        HorizontalDivider(color = BhpCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                        Column {
                            Text(
                                text = "Special Instructions",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BhpTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = createdTrip.remarks,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BhpTextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            // Button 1: View Trip Details (Primary)
            Button(
                onClick = { onViewTripDetails(createdTrip.lorryId, createdTrip.tripReference) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BhpNavyDark,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                EyeIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Trip Details",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button 2: Back to Home (Secondary outlined)
            OutlinedButton(
                onClick = onBackToHome,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BhpNavyDark
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, BhpCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Back to Home",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
