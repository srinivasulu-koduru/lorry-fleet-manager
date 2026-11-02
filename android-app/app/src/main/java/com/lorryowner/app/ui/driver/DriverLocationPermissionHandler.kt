package com.lorryowner.app.ui.driver

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lorryowner.app.ui.theme.BhpNavyDark
import com.lorryowner.app.ui.theme.BhpTextPrimary
import com.lorryowner.app.ui.theme.BhpTextSecondary

object DriverPermissionUtils {

    private const val PREFS_NAME = "bhp_driver_battery_prefs"
    private const val KEY_BATTERY_PROMPT_DECLINED = "battery_prompt_declined"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasAllTrackingPermissions(context: Context): Boolean {
        return hasLocationPermission(context) && hasNotificationPermission(context)
    }

    fun isBatteryOptimized(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) == false
        }
        return false
    }

    fun shouldPromptBatteryOptimization(isBatteryOptimized: Boolean, hasDeclinedPrompt: Boolean): Boolean {
        return isBatteryOptimized && !hasDeclinedPrompt
    }

    fun shouldPromptBatteryOptimization(context: Context): Boolean {
        return shouldPromptBatteryOptimization(
            isBatteryOptimized = isBatteryOptimized(context),
            hasDeclinedPrompt = hasDeclinedBatteryOptimizationPrompt(context)
        )
    }

    fun hasDeclinedBatteryOptimizationPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BATTERY_PROMPT_DECLINED, false)
    }

    fun setBatteryOptimizationPromptDeclined(context: Context, declined: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BATTERY_PROMPT_DECLINED, declined).apply()
    }

    fun resetBatteryOptimizationPromptPreference(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BATTERY_PROMPT_DECLINED).apply()
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun requestBatteryOptimizationExemption(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Attempt 1: Direct system prompt via ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Attempt 2: System battery optimization list
                try {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallback)
                    return true
                } catch (e2: Exception) {
                    // Attempt 3: App details settings (OEM-safe)
                    try {
                        openAppSettings(context)
                        return true
                    } catch (ignored: Exception) {
                        return false
                    }
                }
            }
        }
        return false
    }
}

/**
 * Reusable Compose coordinator for driver location permissions and battery optimization.
 */
@Composable
fun DriverTrackingPermissionCoordinator(
    onPermissionsGranted: () -> Unit,
    content: @Composable (requestPermissions: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val locationGranted = fineGranted || coarseGranted

        if (locationGranted) {
            if (DriverPermissionUtils.shouldPromptBatteryOptimization(context)) {
                showBatteryDialog = true
            } else {
                onPermissionsGranted()
            }
        } else {
            showSettingsDialog = true
        }
    }

    val requestPermissions: () -> Unit = {
        if (DriverPermissionUtils.hasLocationPermission(context)) {
            if (DriverPermissionUtils.shouldPromptBatteryOptimization(context)) {
                showBatteryDialog = true
            } else {
                onPermissionsGranted()
            }
        } else {
            showRationaleDialog = true
        }
    }

    content(requestPermissions)

    // 1. Location & Notification Rationale Dialog
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Location Permission Required",
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
            },
            text = {
                Text(
                    text = "BHP Logistics requires high-accuracy GPS location to transmit live lorry position to the fleet owner while your trip is in progress.\n\nA persistent notification will be displayed while tracking. Tracking operates only during an active IN PROGRESS trip and stops automatically when the trip is completed.",
                    color = BhpTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        launcher.launch(requiredPermissions)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Grant Permissions", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            }
        )
    }

    // 2. Settings Redirect Dialog (if permanently denied)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Enable Location in Settings",
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
            },
            text = {
                Text(
                    text = "Location permission is disabled for BHP Logistics. To start and track your assigned trip, please enable Location in App Settings.",
                    color = BhpTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        DriverPermissionUtils.openAppSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open Settings", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            }
        )
    }

    // 3. Battery Optimization Advisory Dialog
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = {
                showBatteryDialog = false
                DriverPermissionUtils.setBatteryOptimizationPromptDeclined(context, true)
                onPermissionsGranted()
            },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Continuous GPS & Battery Usage",
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary
                )
            },
            text = {
                Text(
                    text = "To help maintain continuous GPS tracking while your phone screen is locked or during long highway trips, Android allows setting BHP Logistics battery usage to 'Unrestricted'.\n\nSetting this avoids aggressive power-saving closures by the operating system during active trips.\n\nNote: Background tracking reliability depends on device operating system policies and satellite reception; exemption does not guarantee uninterrupted GPS.",
                    color = BhpTextSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryDialog = false
                        DriverPermissionUtils.setBatteryOptimizationPromptDeclined(context, true)
                        DriverPermissionUtils.requestBatteryOptimizationExemption(context)
                        onPermissionsGranted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpNavyDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Battery Settings", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    DriverPermissionUtils.setBatteryOptimizationPromptDeclined(context, true)
                    onPermissionsGranted()
                }) {
                    Text("Not Now / Continue", color = BhpTextSecondary)
                }
            }
        )
    }
}
