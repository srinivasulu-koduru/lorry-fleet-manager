package com.lorryowner.app.ui.driver

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.*
import com.lorryowner.app.ui.owner.ChangePasswordScreen
import com.lorryowner.app.ui.theme.*

enum class DriverSubScreen {
    NONE,
    TRIP_DETAILS,
    UPDATE_STATUS,
    CHANGE_PASSWORD
}

@Composable
fun DriverPortalHostScreen(
    sessionManager: SessionManager,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel = remember { DriverDashboardViewModel(sessionManager = sessionManager) }
    var currentSubScreen by remember { mutableStateOf(DriverSubScreen.NONE) }
    var viewingTrip by remember { mutableStateOf<DriverTripUiItem?>(null) }

    val isTrackingActive by com.lorryowner.app.service.DriverTripTrackingService.isTrackingActive.collectAsState()
    val trackingTripId by com.lorryowner.app.service.DriverTripTrackingService.currentTrackingTripId.collectAsState()

    // Reconcile active trip with tracking service on app open / resume
    LaunchedEffect(viewModel.dashboardState.todayTrip, viewModel.allTrips) {
        val activeTrip = viewModel.dashboardState.todayTrip?.takeIf { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }
            ?: viewModel.allTrips.firstOrNull { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }

        if (activeTrip != null) {
            // If service is not running or tracking a different trip, resume tracking
            if (!isTrackingActive || trackingTripId != activeTrip.id) {
                if (DriverPermissionUtils.hasLocationPermission(context)) {
                    android.util.Log.i("DriverPortal", "Auto-reconciling active IN_PROGRESS trip #${activeTrip.id}. Starting tracking service.")
                    com.lorryowner.app.service.DriverTripTrackingService.startTracking(context, activeTrip.id, activeTrip.tripReference)
                }
            }
        } else {
            // No trip is IN_PROGRESS: if service is still running, shut it down cleanly
            if (isTrackingActive) {
                android.util.Log.i("DriverPortal", "No active IN_PROGRESS trip detected. Stopping tracking service.")
                com.lorryowner.app.service.DriverTripTrackingService.stopTracking(context)
            }
        }
    }

    // Double-back to exit safeguard
    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            kotlinx.coroutines.delay(2000L)
            backPressedOnce = false
        }
    }

    // Intercept system back button for sub-screens, child views, and navigation tabs
    BackHandler(enabled = true) {
        when {
            currentSubScreen == DriverSubScreen.UPDATE_STATUS -> {
                if (viewingTrip != null) {
                    currentSubScreen = DriverSubScreen.TRIP_DETAILS
                } else {
                    currentSubScreen = DriverSubScreen.NONE
                }
            }
            currentSubScreen == DriverSubScreen.TRIP_DETAILS -> {
                currentSubScreen = DriverSubScreen.NONE
                viewingTrip = null
            }
            currentSubScreen == DriverSubScreen.CHANGE_PASSWORD -> {
                currentSubScreen = DriverSubScreen.NONE
            }
            viewModel.currentNavDestination != DriverNavDestination.HOME -> {
                viewModel.selectNavDestination(DriverNavDestination.HOME)
            }
            else -> {
                // On Home Dashboard with no open sub-screen: require double back press to exit safely
                if (backPressedOnce) {
                    (context as? android.app.Activity)?.finish()
                } else {
                    backPressedOnce = true
                    android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Change Password sub-view
    if (currentSubScreen == DriverSubScreen.CHANGE_PASSWORD) {
        ChangePasswordScreen(
            sessionManager = sessionManager,
            onBack = { currentSubScreen = DriverSubScreen.NONE },
            onPasswordChanged = {
                currentSubScreen = DriverSubScreen.NONE
                onLogout()
            },
            modifier = modifier
        )
        return
    }

    // Trip Details sub-view
    if (currentSubScreen == DriverSubScreen.TRIP_DETAILS && viewingTrip != null) {
        val activeTrip = viewModel.allTrips.firstOrNull { it.id == viewingTrip!!.id } ?: viewingTrip!!
        DriverTripDetailsScreen(
            trip = activeTrip,
            onBack = { currentSubScreen = DriverSubScreen.NONE },
            onUpdateStatusClick = { trip ->
                viewingTrip = trip
                viewModel.selectTrip(trip)
                currentSubScreen = DriverSubScreen.UPDATE_STATUS
            },
            viewModel = viewModel,
            modifier = modifier
        )
        return
    }

    // Update Status sub-view
    if (currentSubScreen == DriverSubScreen.UPDATE_STATUS) {
        val candidateTrip = viewingTrip
            ?: viewModel.selectedTripForDetails
            ?: viewModel.dashboardState.todayTrip
            ?: viewModel.allTrips.firstOrNull { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }
            ?: viewModel.allTrips.firstOrNull()
        val synchronizedTrip = if (candidateTrip != null) {
            viewModel.allTrips.firstOrNull { it.id == candidateTrip.id } ?: candidateTrip
        } else null

        DriverUpdateStatusScreen(
            trip = synchronizedTrip,
            onBack = { currentSubScreen = DriverSubScreen.NONE },
            viewModel = viewModel,
            modifier = modifier
        )
        return
    }

    // Main 4-Tab Scaffolding
    Scaffold(
        bottomBar = {
            DriverBottomNavigationBar(
                currentDestination = viewModel.currentNavDestination,
                onDestinationSelected = { dest ->
                    viewModel.selectNavDestination(dest)
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (viewModel.currentNavDestination) {
                DriverNavDestination.HOME -> DriverDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTrips = { viewModel.selectNavDestination(DriverNavDestination.TRIPS) },
                    onNavigateToStatus = { trip ->
                        viewingTrip = trip
                        viewModel.selectTrip(trip)
                        currentSubScreen = DriverSubScreen.UPDATE_STATUS
                    },
                    onNavigateToTripDetails = { trip ->
                        viewingTrip = trip
                        currentSubScreen = DriverSubScreen.TRIP_DETAILS
                    },
                    onNavigateToProfile = { viewModel.selectNavDestination(DriverNavDestination.PROFILE) },
                    modifier = Modifier.fillMaxSize()
                )
                DriverNavDestination.TRIPS -> DriverTripsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.selectNavDestination(DriverNavDestination.HOME) },
                    onTripClick = { trip ->
                        viewingTrip = trip
                        currentSubScreen = DriverSubScreen.TRIP_DETAILS
                    },
                    modifier = Modifier.fillMaxSize()
                )
                DriverNavDestination.STATUS -> {
                    val candidateTrip = viewModel.selectedTripForDetails
                        ?: viewModel.dashboardState.todayTrip
                        ?: viewModel.allTrips.firstOrNull { it.tripStatus.equals("IN_PROGRESS", ignoreCase = true) }
                        ?: viewModel.allTrips.firstOrNull()
                    val synchronizedTrip = if (candidateTrip != null) {
                        viewModel.allTrips.firstOrNull { it.id == candidateTrip.id } ?: candidateTrip
                    } else null

                    DriverUpdateStatusScreen(
                        trip = synchronizedTrip,
                        onBack = { viewModel.selectNavDestination(DriverNavDestination.HOME) },
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                DriverNavDestination.PROFILE -> DriverProfileScreen(
                    viewModel = viewModel,
                    onChangePassword = { currentSubScreen = DriverSubScreen.CHANGE_PASSWORD },
                    onLogout = onLogout,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun DriverBottomNavigationBar(
    currentDestination: DriverNavDestination,
    onDestinationSelected: (DriverNavDestination) -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFE2E8F0)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            DriverBottomNavItem(
                label = "Home",
                isSelected = currentDestination == DriverNavDestination.HOME,
                icon = { tint -> HomeNavIcon(tint = tint, modifier = Modifier.size(22.dp)) },
                onClick = { onDestinationSelected(DriverNavDestination.HOME) }
            )

            DriverBottomNavItem(
                label = "My Trips",
                isSelected = currentDestination == DriverNavDestination.TRIPS || currentDestination == DriverNavDestination.STATUS,
                icon = { tint -> DashboardTruckIcon(tint = tint, modifier = Modifier.size(22.dp)) },
                onClick = { onDestinationSelected(DriverNavDestination.TRIPS) }
            )

            DriverBottomNavItem(
                label = "Profile",
                isSelected = currentDestination == DriverNavDestination.PROFILE,
                icon = { tint -> PersonVectorIcon(tint = tint, modifier = Modifier.size(22.dp)) },
                onClick = { onDestinationSelected(DriverNavDestination.PROFILE) }
            )
        }
    }
}

@Composable
private fun DriverBottomNavItem(
    label: String,
    isSelected: Boolean,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFF16A34A) // Primary emerald green
    val inactiveColor = Color(0xFF64748B) // Slate gray
    val pillBg = if (isSelected) Color(0xFFECFDF5) else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pillBg)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        icon(if (isSelected) activeColor else inactiveColor)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}
