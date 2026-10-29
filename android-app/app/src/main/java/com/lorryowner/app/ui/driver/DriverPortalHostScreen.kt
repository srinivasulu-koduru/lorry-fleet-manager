package com.lorryowner.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    CHANGE_PASSWORD
}

@Composable
fun DriverPortalHostScreen(
    sessionManager: SessionManager,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = remember { DriverDashboardViewModel(sessionManager = sessionManager) }
    var currentSubScreen by remember { mutableStateOf(DriverSubScreen.NONE) }
    var viewingTrip by remember { mutableStateOf<DriverTripUiItem?>(null) }

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
                currentSubScreen = DriverSubScreen.NONE
                viewModel.selectTrip(trip)
                viewModel.selectNavDestination(DriverNavDestination.STATUS)
            },
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
                        viewModel.selectTrip(trip)
                        viewModel.selectNavDestination(DriverNavDestination.STATUS)
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
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            DriverBottomNavItem(
                label = "Home",
                isSelected = currentDestination == DriverNavDestination.HOME,
                icon = { tint -> HomeNavIcon(tint = tint, modifier = Modifier.size(20.dp)) },
                onClick = { onDestinationSelected(DriverNavDestination.HOME) }
            )

            DriverBottomNavItem(
                label = "Trips",
                isSelected = currentDestination == DriverNavDestination.TRIPS,
                icon = { tint -> DashboardTruckIcon(tint = tint, modifier = Modifier.size(20.dp)) },
                onClick = { onDestinationSelected(DriverNavDestination.TRIPS) }
            )

            DriverBottomNavItem(
                label = "Status",
                isSelected = currentDestination == DriverNavDestination.STATUS,
                icon = { tint -> CheckmarkCircleIcon(tint = tint, modifier = Modifier.size(20.dp)) },
                onClick = { onDestinationSelected(DriverNavDestination.STATUS) }
            )

            DriverBottomNavItem(
                label = "Profile",
                isSelected = currentDestination == DriverNavDestination.PROFILE,
                icon = { tint -> PersonVectorIcon(tint = tint, modifier = Modifier.size(20.dp)) },
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
    val activeColor = BhpDriverBlue
    val inactiveColor = BhpTextSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        icon(if (isSelected) activeColor else inactiveColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}
