package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsBottomSheet(
    data: LiveTrackingLorryUiState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with vehicle and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trip Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpNavyDark,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = data.tripReference,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = BhpTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                GpsStatusBadge(status = data.gpsStatus)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(16.dp))

            // Spec grid
            TrackingDetailItem(label = "Vehicle Registration", value = data.registrationNumber)
            TrackingDetailItem(label = "Make & Model", value = data.makeModel)
            TrackingDetailItem(label = "Assigned Driver", value = data.driverName)
            if (!data.driverPhone.isNullOrBlank()) {
                TrackingDetailItem(label = "Driver Phone", value = data.driverPhone)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            TrackingDetailItem(label = "Starting Location", value = data.originLocation)
            TrackingDetailItem(label = "Current Location", value = data.currentLocationDescription)
            TrackingDetailItem(label = "Destination", value = data.destinationLocation)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            TrackingDetailItem(label = "Current Speed", value = TrackingFormattingHelper.formatSpeed(data.currentSpeedKmh))
            TrackingDetailItem(label = "Distance Travelled", value = TrackingFormattingHelper.formatDistance(data.distanceTravelledKm))
            TrackingDetailItem(label = "Distance Remaining", value = TrackingFormattingHelper.formatDistance(data.remainingDistanceKm))
            TrackingDetailItem(label = "Estimated Driving Time", value = data.estimatedDrivingTime)
            TrackingDetailItem(label = "Estimated Arrival", value = data.estimatedArrival)

            if (data.lastGpsUpdate != null) {
                TrackingDetailItem(label = "Last GPS Update", value = data.lastGpsUpdate)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Close Details", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun TrackingDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = BhpTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = BhpTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GpsStatusBadge(status: GpsStatus) {
    val (bgColor, textColor, text) = when (status) {
        GpsStatus.LIVE -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "● LIVE")
        GpsStatus.STALE -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "● STALE")
        GpsStatus.WAITING_FOR_LOCATION -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "WAITING")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
