package com.lorryowner.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.CheckmarkCircleIcon
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.ClockTimerIcon
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.FormatListIcon
import com.lorryowner.app.ui.components.GasPumpIcon
import com.lorryowner.app.ui.components.GreetingTruckGraphic
import com.lorryowner.app.ui.components.HomeNavIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.components.MoreDotsIcon
import com.lorryowner.app.ui.components.NotificationBellIcon
import com.lorryowner.app.ui.components.ShieldVectorIcon
import com.lorryowner.app.ui.components.WrenchToolIcon
import com.lorryowner.app.ui.theme.BhpAvatarBorder
import com.lorryowner.app.ui.theme.BhpAvatarCream
import com.lorryowner.app.ui.theme.BhpCardBorder
import com.lorryowner.app.ui.theme.BhpCardWhite
import com.lorryowner.app.ui.theme.BhpDarkForestGreen
import com.lorryowner.app.ui.theme.BhpDieselOrange
import com.lorryowner.app.ui.theme.BhpDriverBlue
import com.lorryowner.app.ui.theme.BhpNavyContainer
import com.lorryowner.app.ui.theme.BhpNavyDark
import com.lorryowner.app.ui.theme.BhpNotReadyAmber
import com.lorryowner.app.ui.theme.BhpNotReadyAmberBg
import com.lorryowner.app.ui.theme.BhpNotReadyAmberText
import com.lorryowner.app.ui.theme.BhpReadyBlue
import com.lorryowner.app.ui.theme.BhpReadyBlueBg
import com.lorryowner.app.ui.theme.BhpReadyBlueText
import com.lorryowner.app.ui.theme.BhpRunningGreen
import com.lorryowner.app.ui.theme.BhpRunningGreenBg
import com.lorryowner.app.ui.theme.BhpRunningGreenText
import com.lorryowner.app.ui.theme.BhpScreenBg
import com.lorryowner.app.ui.theme.BhpTextPrimary
import com.lorryowner.app.ui.theme.BhpTextSecondary

enum class OwnerDestination {
    DASHBOARD,
    MY_LORRIES,
    RUNNING_LORRIES,
    NOT_RUNNING_LORRIES,
    LORRY_DETAILS,
    LORRY_TRIP_HISTORY,
    LORRY_MAINTENANCE_HISTORY,
    FLEET_SUMMARY,
    MANAGE_DRIVERS,
    ADD_DRIVER,
    CHANGE_PASSWORD,
    DRIVER_TRIP_HISTORY,
    ADD_TRIP,
    TRIP_CONFIRMATION,
    TRIP_CREATION_SUCCESS,
    ADD_LORRY,
    MAINTENANCE_DASHBOARD,
    VEHICLE_MAINTENANCE_DETAILS,
    ADD_MAINTENANCE,
    LIVE_TRACKING_LIST,
    LIVE_SATELLITE_MAP
}

@Composable
fun OwnerDashboardScreen(
    sessionManager: SessionManager? = null,
    onLogout: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSession = remember { sessionManager ?: SessionManager(context) }
    val viewModel = remember { OwnerDashboardViewModel(effectiveSession) }
    val state = viewModel.uiState
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentDestination by remember { mutableStateOf(OwnerDestination.DASHBOARD) }
    var selectedDriverForHistory by remember { mutableStateOf<DriverItemUiState?>(null) }
    var selectedLorryId by remember { mutableStateOf<Long?>(null) }
    var selectedLorryIdForMaintenance by remember { mutableStateOf<Long?>(null) }
    var selectedTrackingLorryId by remember { mutableStateOf<Long?>(null) }
    var selectedTrackingTripId by remember { mutableStateOf<Long?>(null) }
    var createdTripResult by remember { mutableStateOf<CreatedTripUiState?>(null) }
    val addTripViewModel = remember(effectiveSession) { AddTripViewModel(effectiveSession) }

    // Auto-update dynamic greeting across time boundaries without background wakeups
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                viewModel.updateGreeting()
                val nextDelay = minOf(GreetingHelper.getMillisUntilNextTransition(), 30_000L)
                delay(maxOf(nextDelay, 1000L))
            }
        }
    }

    // Automatically navigate back to login screen if backend invalidates session (HTTP 401)
    LaunchedEffect(state.isSessionExpired) {
        if (state.isSessionExpired) {
            onLogout("Your session has expired. Please log in again.")
        }
    }

    // Intercept back button when within child screens
    BackHandler(enabled = currentDestination != OwnerDestination.DASHBOARD) {
        when (currentDestination) {
            OwnerDestination.LORRY_TRIP_HISTORY -> currentDestination = OwnerDestination.LORRY_DETAILS
            OwnerDestination.LORRY_MAINTENANCE_HISTORY -> currentDestination = OwnerDestination.LORRY_DETAILS
            OwnerDestination.LORRY_DETAILS -> currentDestination = OwnerDestination.MY_LORRIES
            OwnerDestination.RUNNING_LORRIES -> currentDestination = OwnerDestination.MY_LORRIES
            OwnerDestination.NOT_RUNNING_LORRIES -> currentDestination = OwnerDestination.MY_LORRIES
            OwnerDestination.MY_LORRIES -> currentDestination = OwnerDestination.DASHBOARD
            OwnerDestination.FLEET_SUMMARY -> currentDestination = OwnerDestination.DASHBOARD
            OwnerDestination.DRIVER_TRIP_HISTORY -> currentDestination = OwnerDestination.MANAGE_DRIVERS
            OwnerDestination.ADD_DRIVER -> currentDestination = OwnerDestination.MANAGE_DRIVERS
            OwnerDestination.MANAGE_DRIVERS -> currentDestination = OwnerDestination.DASHBOARD
            OwnerDestination.CHANGE_PASSWORD -> currentDestination = OwnerDestination.DASHBOARD
            OwnerDestination.ADD_TRIP -> currentDestination = OwnerDestination.DASHBOARD
            OwnerDestination.TRIP_CONFIRMATION -> currentDestination = OwnerDestination.ADD_TRIP
            OwnerDestination.TRIP_CREATION_SUCCESS -> currentDestination = OwnerDestination.DASHBOARD
            OwnerDestination.ADD_LORRY -> currentDestination = OwnerDestination.MY_LORRIES
            OwnerDestination.ADD_MAINTENANCE -> {
                currentDestination = if (selectedLorryIdForMaintenance != null) {
                    OwnerDestination.VEHICLE_MAINTENANCE_DETAILS
                } else {
                    OwnerDestination.MAINTENANCE_DASHBOARD
                }
            }
            OwnerDestination.VEHICLE_MAINTENANCE_DETAILS -> currentDestination = OwnerDestination.MAINTENANCE_DASHBOARD
            OwnerDestination.MAINTENANCE_DASHBOARD -> currentDestination = OwnerDestination.DASHBOARD
            OwnerDestination.LIVE_SATELLITE_MAP -> currentDestination = OwnerDestination.LIVE_TRACKING_LIST
            OwnerDestination.LIVE_TRACKING_LIST -> currentDestination = OwnerDestination.DASHBOARD
            OwnerDestination.DASHBOARD -> { }
        }
    }

    when (currentDestination) {
        OwnerDestination.MY_LORRIES -> {
            MyLorriesScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.DASHBOARD },
                onAddLorryClick = { currentDestination = OwnerDestination.ADD_LORRY },
                onLorryClick = { lorryId ->
                    selectedLorryId = lorryId
                    currentDestination = OwnerDestination.LORRY_DETAILS
                },
                onRunningClick = { currentDestination = OwnerDestination.RUNNING_LORRIES },
                onNotRunningClick = { currentDestination = OwnerDestination.NOT_RUNNING_LORRIES },
                modifier = modifier
            )
        }
        OwnerDestination.RUNNING_LORRIES -> {
            RunningLorriesScreen(
                isRunningFilter = true,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.MY_LORRIES },
                onLorryClick = { lorryId ->
                    selectedLorryId = lorryId
                    currentDestination = OwnerDestination.LORRY_DETAILS
                },
                modifier = modifier
            )
        }
        OwnerDestination.NOT_RUNNING_LORRIES -> {
            RunningLorriesScreen(
                isRunningFilter = false,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.MY_LORRIES },
                onLorryClick = { lorryId ->
                    selectedLorryId = lorryId
                    currentDestination = OwnerDestination.LORRY_DETAILS
                },
                modifier = modifier
            )
        }
        OwnerDestination.LORRY_DETAILS -> {
            val lorryId = selectedLorryId ?: 1L
            LorryDetailsScreen(
                lorryId = lorryId,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.MY_LORRIES },
                onNavigateToTripHistory = { currentDestination = OwnerDestination.LORRY_TRIP_HISTORY },
                onNavigateToMaintenanceHistory = {
                    selectedLorryIdForMaintenance = lorryId
                    currentDestination = OwnerDestination.VEHICLE_MAINTENANCE_DETAILS
                },
                onNavigateToTripSummary = { currentDestination = OwnerDestination.FLEET_SUMMARY },
                modifier = modifier
            )
        }
        OwnerDestination.LORRY_TRIP_HISTORY -> {
            val lorryId = selectedLorryId ?: 1L
            LorryTripHistoryScreen(
                lorryId = lorryId,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.LORRY_DETAILS },
                modifier = modifier
            )
        }
        OwnerDestination.LORRY_MAINTENANCE_HISTORY -> {
            val lorryId = selectedLorryId ?: 1L
            LorryMaintenanceScreen(
                lorryId = lorryId,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.LORRY_DETAILS },
                modifier = modifier
            )
        }
        OwnerDestination.FLEET_SUMMARY -> {
            FleetSummaryScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.DASHBOARD },
                modifier = modifier
            )
        }
        OwnerDestination.MANAGE_DRIVERS -> {
            ManageDriversScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.DASHBOARD },
                onAddDriverClick = { currentDestination = OwnerDestination.ADD_DRIVER },
                onNavigateToTripHistory = { driver ->
                    selectedDriverForHistory = driver
                    currentDestination = OwnerDestination.DRIVER_TRIP_HISTORY
                },
                modifier = modifier
            )
        }
        OwnerDestination.DRIVER_TRIP_HISTORY -> {
            val driver = selectedDriverForHistory
            if (driver != null) {
                DriverTripHistoryScreen(
                    driverId = driver.id,
                    driverName = driver.fullName,
                    driverUsername = driver.username,
                    sessionManager = effectiveSession,
                    onBack = { currentDestination = OwnerDestination.MANAGE_DRIVERS },
                    modifier = modifier
                )
            } else {
                currentDestination = OwnerDestination.MANAGE_DRIVERS
            }
        }
        OwnerDestination.ADD_DRIVER -> {
            AddDriverScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.MANAGE_DRIVERS },
                onDriverCreated = { currentDestination = OwnerDestination.MANAGE_DRIVERS },
                modifier = modifier
            )
        }
        OwnerDestination.CHANGE_PASSWORD -> {
            ChangePasswordScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.DASHBOARD },
                onPasswordChanged = {
                    // Password successfully changed: invalidate session & redirect to login
                    onLogout("Password changed successfully. Please log in with your new password.")
                },
                modifier = modifier
            )
        }
        OwnerDestination.ADD_TRIP -> {
            AddTripScreen(
                viewModel = addTripViewModel,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.DASHBOARD },
                onNavigateToConfirmation = {
                    currentDestination = OwnerDestination.TRIP_CONFIRMATION
                },
                modifier = modifier
            )
        }
        OwnerDestination.TRIP_CONFIRMATION -> {
            TripConfirmationScreen(
                viewModel = addTripViewModel,
                onBack = { currentDestination = OwnerDestination.ADD_TRIP },
                onEdit = { currentDestination = OwnerDestination.ADD_TRIP },
                onTripCreated = { created ->
                    createdTripResult = created
                    viewModel.refreshDashboard()
                    currentDestination = OwnerDestination.TRIP_CREATION_SUCCESS
                },
                modifier = modifier
            )
        }
        OwnerDestination.TRIP_CREATION_SUCCESS -> {
            val created = createdTripResult
            if (created != null) {
                TripSuccessScreen(
                    createdTrip = created,
                    onViewTripDetails = { lorryId, _ ->
                        selectedLorryId = lorryId
                        currentDestination = OwnerDestination.LORRY_TRIP_HISTORY
                    },
                    onBackToHome = {
                        currentDestination = OwnerDestination.DASHBOARD
                        addTripViewModel.loadEligibleLorriesAndDrivers()
                    },
                    modifier = modifier
                )
            } else {
                currentDestination = OwnerDestination.DASHBOARD
            }
        }
        OwnerDestination.ADD_LORRY -> {
            AddLorryScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.MY_LORRIES },
                onLorryCreated = { createdLorryId ->
                    viewModel.refreshDashboard()
                    selectedLorryId = createdLorryId
                    currentDestination = OwnerDestination.LORRY_DETAILS
                },
                modifier = modifier
            )
        }
        OwnerDestination.MAINTENANCE_DASHBOARD -> {
            MaintenanceDashboardScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.DASHBOARD },
                onViewVehicleDetails = { lorryId ->
                    selectedLorryIdForMaintenance = lorryId
                    currentDestination = OwnerDestination.VEHICLE_MAINTENANCE_DETAILS
                },
                onAddMaintenanceClick = {
                    selectedLorryIdForMaintenance = null
                    currentDestination = OwnerDestination.ADD_MAINTENANCE
                },
                modifier = modifier
            )
        }
        OwnerDestination.VEHICLE_MAINTENANCE_DETAILS -> {
            val targetLorryId = selectedLorryIdForMaintenance ?: selectedLorryId ?: 1L
            VehicleMaintenanceDetailsScreen(
                lorryId = targetLorryId,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.MAINTENANCE_DASHBOARD },
                onAddServiceRecordClick = { id ->
                    selectedLorryIdForMaintenance = id
                    currentDestination = OwnerDestination.ADD_MAINTENANCE
                },
                modifier = modifier
            )
        }
        OwnerDestination.ADD_MAINTENANCE -> {
            AddMaintenanceScreen(
                initialLorryId = selectedLorryIdForMaintenance ?: selectedLorryId,
                sessionManager = effectiveSession,
                onBack = {
                    if (selectedLorryIdForMaintenance != null) {
                        currentDestination = OwnerDestination.VEHICLE_MAINTENANCE_DETAILS
                    } else {
                        currentDestination = OwnerDestination.MAINTENANCE_DASHBOARD
                    }
                },
                onRecordSaved = {
                    if (selectedLorryIdForMaintenance != null) {
                        currentDestination = OwnerDestination.VEHICLE_MAINTENANCE_DETAILS
                    } else {
                        currentDestination = OwnerDestination.MAINTENANCE_DASHBOARD
                    }
                },
                modifier = modifier
            )
        }
        OwnerDestination.LIVE_TRACKING_LIST -> {
            LiveTrackingRunningLorriesScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.DASHBOARD },
                onViewLiveLocation = { lorryId, tripId ->
                    selectedTrackingLorryId = lorryId
                    selectedTrackingTripId = tripId
                    currentDestination = OwnerDestination.LIVE_SATELLITE_MAP
                },
                modifier = modifier
            )
        }
        OwnerDestination.LIVE_SATELLITE_MAP -> {
            LiveSatelliteTrackingScreen(
                lorryId = selectedTrackingLorryId,
                tripId = selectedTrackingTripId,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.LIVE_TRACKING_LIST },
                modifier = modifier
            )
        }
        OwnerDestination.DASHBOARD -> {
            DashboardMainScaffold(
                state = state,
                viewModel = viewModel,
                effectiveSession = effectiveSession,
                onLogout = { onLogout(null) },
                onNavigateToAddTrip = { currentDestination = OwnerDestination.ADD_TRIP },
                onNavigateToDrivers = { currentDestination = OwnerDestination.MANAGE_DRIVERS },
                onNavigateToChangePassword = { currentDestination = OwnerDestination.CHANGE_PASSWORD },
                onNavigateToMyLorries = { currentDestination = OwnerDestination.MY_LORRIES },
                onNavigateToRunningLorries = { currentDestination = OwnerDestination.RUNNING_LORRIES },
                onNavigateToNotRunningLorries = { currentDestination = OwnerDestination.NOT_RUNNING_LORRIES },
                onNavigateToReports = { currentDestination = OwnerDestination.FLEET_SUMMARY },
                onNavigateToMaintenance = { currentDestination = OwnerDestination.MAINTENANCE_DASHBOARD },
                onNavigateToLiveTracking = { currentDestination = OwnerDestination.LIVE_TRACKING_LIST },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DashboardMainScaffold(
    state: OwnerDashboardUiState,
    viewModel: OwnerDashboardViewModel,
    effectiveSession: SessionManager,
    onLogout: () -> Unit,
    onNavigateToAddTrip: () -> Unit,
    onNavigateToDrivers: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToMyLorries: () -> Unit,
    onNavigateToRunningLorries: () -> Unit,
    onNavigateToNotRunningLorries: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToLiveTracking: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    Scaffold(
        bottomBar = {
            OwnerBottomNavBar(
                selectedIndex = state.selectedNavIndex,
                onSelectIndex = { index ->
                    viewModel.onNavItemSelected(index)
                    when (index) {
                        0 -> { /* Home */ }
                        1 -> onNavigateToMyLorries()
                        2 -> onNavigateToDrivers()
                        3 -> onNavigateToReports()
                        4 -> viewModel.showProfileDialog()
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BhpScreenBg)
                .padding(innerPadding)
        ) {
            // 1. Redesigned Premium Navy Header with Branding, Greeting & Truck Graphic
            RedesignedOwnerHeader(
                ownerName = if (state.ownerName.isNotBlank() && state.ownerName != "Owner") state.ownerName else effectiveSession.getFullName().ifEmpty { "Fleet Owner" },
                greeting = state.greeting,
                unreadNotifications = state.unreadNotificationsCount,
                ownerInitial = state.ownerName.firstOrNull()?.uppercase() ?: "O",
                onProfileClick = { viewModel.showProfileDialog() }
            )

            // 2. Scrollable Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Quick Actions Section (Prompt Part 1 C & D - Prominent My Lorries Card + Actions)
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                RedesignedQuickActionsGrid(
                    onMyLorriesClick = onNavigateToMyLorries,
                    onLiveTrackingClick = onNavigateToLiveTracking,
                    runningCount = state.fleetOverview?.runningNow?.toInt() ?: 0,
                    onAddTripClick = onNavigateToAddTrip,
                    onManageDriversClick = onNavigateToDrivers,
                    onTripHistoryClick = onNavigateToDrivers,
                    onReportsClick = onNavigateToReports,
                    onMaintenanceClick = onNavigateToMaintenance,
                    onSettingsClick = { viewModel.showProfileDialog() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Fleet Overview Section (Prompt Part 1 B - 3 Summary Cards: Total, Running, Not Running)
                Text(
                    text = "Fleet Overview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                RedesignedFleetOverviewCard(
                    totalLorries = state.fleetOverview.totalLorries,
                    running = state.fleetOverview.runningNow,
                    notRunning = state.fleetOverview.readyToStart + state.fleetOverview.notReady,
                    onTotalClick = onNavigateToMyLorries,
                    onRunningClick = onNavigateToRunningLorries,
                    onNotRunningClick = onNavigateToNotRunningLorries
                )

                Spacer(modifier = Modifier.height(18.dp))

                // "This month" Financial Card
                FinancialSummaryCard(financialSummary = state.financialSummary)

                Spacer(modifier = Modifier.height(20.dp))

                // Recent Activity Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BhpReadyBlue,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.clickable { onNavigateToMyLorries() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                RecentActivityList(
                    activities = state.recentActivities,
                    onItemClick = onNavigateToMyLorries
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Dialog: Fleet Lorries Inspector
    if (state.isLorriesDialogVisible) {
        LorriesListDialog(
            lorries = state.lorriesList,
            onDismiss = { viewModel.dismissLorriesDialog() }
        )
    }

    // Modal Dialog: Owner Profile & Security Menu
    if (state.isProfileDialogVisible) {
        OwnerProfileDialog(
            ownerName = state.ownerName.ifEmpty { effectiveSession.getFullName() },
            username = effectiveSession.getUsername(),
            onDismiss = { viewModel.dismissProfileDialog() },
            onChangePasswordClick = {
                viewModel.dismissProfileDialog()
                onNavigateToChangePassword()
            },
            onLogout = onLogout
        )
    }
}

/**
 * Redesigned Premium Navy Header with Branding, Dynamic Greeting, Truck Graphic, and Owner Profile.
 */
@Composable
fun RedesignedOwnerHeader(
    ownerName: String,
    greeting: String,
    unreadNotifications: Int,
    ownerInitial: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BhpNavyDark,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Row: Branding on left, Notification & Profile Avatar on right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top-left profile/truck icon in rounded square
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BhpNavyContainer)
                        .clickable { onProfileClick() }
                ) {
                    DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Branding Titles (clickable to open profile menu)
                Column(
                    modifier = Modifier.clickable { onProfileClick() }
                ) {
                    Text(
                        text = "BHP Logistics",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp,
                            letterSpacing = 0.2.sp
                        )
                    )
                    Text(
                        text = "Fleet Manager",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Notification Bell
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BhpNavyContainer)
                ) {
                    NotificationBellIcon(
                        tint = Color.White,
                        hasBadge = unreadNotifications > 0,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar circle with owner's initial and cream border
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BhpAvatarCream)
                        .border(2.dp, BhpAvatarBorder, CircleShape)
                        .clickable { onProfileClick() }
                ) {
                    Text(
                        text = ownerInitial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpNavyDark,
                            fontSize = 17.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Greeting Banner inside Header with subtle glowing container
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting,",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = ownerName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 20.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(BhpRunningGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fleet Active • Owner Mode",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    GreetingTruckGraphic(
                        modifier = Modifier.size(width = 88.dp, height = 44.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3-Column Fleet Overview Card (Total Lorries, Running Lorries, Not Running Lorries).
 * Real backend values derived directly from database queries.
 */
@Composable
fun RedesignedFleetOverviewCard(
    totalLorries: Long,
    running: Long,
    notRunning: Long,
    onTotalClick: () -> Unit,
    onRunningClick: () -> Unit,
    onNotRunningClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Total Lorries Card
        FleetSummaryColumnCard(
            label = "Total Lorries",
            count = totalLorries.toString(),
            subLabel = "All Fleet",
            containerColor = BhpCardWhite,
            accentColor = BhpNavyDark,
            badgeBg = BhpNavyContainer,
            icon = { FormatListIcon(tint = Color.White, modifier = Modifier.size(18.dp)) },
            onClick = onTotalClick,
            modifier = Modifier.weight(1f)
        )

        // 2. Running Lorries Card (Green)
        FleetSummaryColumnCard(
            label = "Running",
            count = running.toString(),
            subLabel = "On Road",
            containerColor = BhpCardWhite,
            accentColor = BhpRunningGreen,
            badgeBg = BhpRunningGreenBg,
            icon = { DashboardTruckIcon(tint = BhpRunningGreen, modifier = Modifier.size(18.dp)) },
            onClick = onRunningClick,
            modifier = Modifier.weight(1f)
        )

        // 3. Not Running Lorries Card (Amber)
        FleetSummaryColumnCard(
            label = "Not Running",
            count = notRunning.toString(),
            subLabel = "Idle / Off",
            containerColor = BhpCardWhite,
            accentColor = BhpNotReadyAmber,
            badgeBg = BhpNotReadyAmberBg,
            icon = { ClockTimerIcon(tint = BhpNotReadyAmber, modifier = Modifier.size(18.dp)) },
            onClick = onNotRunningClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FleetSummaryColumnCard(
    label: String,
    count: String,
    subLabel: String,
    containerColor: Color,
    accentColor: Color,
    badgeBg: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.weight(1f))

                ChevronRightIcon(
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 24.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BhpTextPrimary,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )

            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BhpTextSecondary,
                    fontSize = 10.5.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun OverviewCard(
    title: String,
    subtitle: String,
    count: String,
    containerColor: Color,
    accentColor: Color,
    textColor: Color,
    subtitleColor: Color? = null,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor)
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = count,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 28.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 15.sp
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = subtitleColor ?: textColor.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            )
        }
    }
}

/**
 * Financial metrics summary card ("This month").
 */
@Composable
fun FinancialSummaryCard(financialSummary: FinancialSummaryState) {
    Surface(
        color = BhpCardWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = financialSummary.monthLabel,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                if (financialSummary.isSample) {
                    Text(
                        text = "Sample figures",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Income
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = financialSummary.income,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1
                    )
                }

                // Column 2: Expenses
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = financialSummary.expenses,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1
                    )
                }

                // Column 3: Profit
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Profit",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = financialSummary.profit,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpRunningGreenText,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Redesigned Quick Actions Grid featuring a prominent hero card for My Lorries (Entry Point to Part 2)
 * and accessible action cards for Manage Drivers, Fleet Reports, Trip History, and Owner Security.
 */
@Composable
fun RedesignedQuickActionsGrid(
    onMyLorriesClick: () -> Unit,
    onLiveTrackingClick: () -> Unit = {},
    runningCount: Int = 0,
    onAddTripClick: () -> Unit,
    onManageDriversClick: () -> Unit,
    onTripHistoryClick: () -> Unit,
    onReportsClick: () -> Unit,
    onMaintenanceClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // PROMINENT HERO CARD 1: My Lorries (Entry point to Part 2 My Lorries Dashboard)
        Surface(
            color = BhpCardWhite,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BhpRunningGreen.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMyLorriesClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BhpRunningGreenBg)
                ) {
                    DashboardTruckIcon(tint = BhpRunningGreen, modifier = Modifier.size(26.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "My Lorries",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = BhpRunningGreenBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "FEATURED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BhpRunningGreen,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Fleet status donut, active routes & vehicle specs",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ChevronRightIcon(tint = BhpRunningGreen, modifier = Modifier.size(16.dp))
            }
        }

        // PROMINENT HERO CARD 2: Live Tracking
        Surface(
            color = BhpCardWhite,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLiveTrackingClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0F2FE))
                ) {
                    LocationPinIcon(tint = Color(0xFF0284C7), modifier = Modifier.size(26.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Live Tracking",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpTextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (runningCount > 0) "$runningCount ACTIVE" else "LIVE GPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Track your running lorries in real time",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ChevronRightIcon(tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
            }
        }

        // Secondary Row 1: Add Trip Details & Manage Drivers
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Add Trip Details Card (Stage 5 Feature Entry Point)
            QuickActionItemCard(
                title = "Add Trip Details",
                subtitle = "Schedule & assign routes",
                icon = { LocationPinIcon(tint = BhpReadyBlue, modifier = Modifier.size(22.dp)) },
                iconBg = BhpReadyBlueBg,
                onClick = onAddTripClick,
                modifier = Modifier.weight(1f)
            )

            // Manage Drivers Card
            QuickActionItemCard(
                title = "Manage Drivers",
                subtitle = "Profiles & assignments",
                icon = { DriverCapIcon(tint = BhpDriverBlue, modifier = Modifier.size(22.dp)) },
                iconBg = Color(0xFFEFF6FF),
                onClick = onManageDriversClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary Row 2: Maintenance & Fleet Reports
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Vehicle Maintenance Card (Stage Owner Maintenance Entry Point)
            QuickActionItemCard(
                title = "Maintenance",
                subtitle = "Servicing & repairs",
                icon = { WrenchToolIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp)) },
                iconBg = Color(0xFFDCFCE7),
                onClick = onMaintenanceClick,
                modifier = Modifier.weight(1f)
            )

            // Fleet Summary / Reports Card
            QuickActionItemCard(
                title = "Fleet Reports",
                subtitle = "Summary & stats",
                icon = { FormatListIcon(tint = Color(0xFF7C3AED), modifier = Modifier.size(22.dp)) },
                iconBg = Color(0xFFF5F3FF),
                onClick = onReportsClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary Row 3: Owner Security Full Width Card
        Surface(
            color = BhpCardWhite,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    ShieldVectorIcon(tint = BhpNavyDark, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Owner Security",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = "Password & credentials",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
                ChevronRightIcon(tint = BhpTextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun QuickActionItemCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BhpCardWhite,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg)
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextSecondary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Recent Activity list of white cards with colored dot, registration, notes, and time ago.
 */
@Composable
fun RecentActivityList(
    activities: List<ActivityItemState>,
    onItemClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (item in activities) {
            ActivityCard(item = item, onClick = onItemClick)
        }
    }
}

@Composable
fun ActivityCard(
    item: ActivityItemState,
    onClick: () -> Unit
) {
    val dotColor = when (item.colorIndicator) {
        "GREEN" -> BhpRunningGreen
        "ORANGE" -> BhpDieselOrange
        "BLUE" -> BhpReadyBlue
        else -> BhpRunningGreen
    }

    Surface(
        color = BhpCardWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            // Solid colored status dot
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                // Top sub-row: Registration, Time Ago, Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.registrationNumber,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = item.timeAgo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    ChevronRightIcon(
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Bottom sub-row: Status description spanning across full width
                Text(
                    text = item.statusDescription,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpTextSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Bottom Navigation Bar matching the 5 tabs in the reference design.
 */
@Composable
fun OwnerBottomNavBar(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    Surface(
        color = BhpCardWhite,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BhpCardBorder.copy(alpha = 0.6f), RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            BottomNavItem(
                label = "Home",
                isSelected = selectedIndex == 0,
                icon = { HomeNavIcon(tint = if (selectedIndex == 0) BhpRunningGreen else BhpTextSecondary, modifier = Modifier.size(20.dp)) },
                onClick = { onSelectIndex(0) },
                modifier = Modifier.weight(1f)
            )

            // 2. Lorries
            BottomNavItem(
                label = "Lorries",
                isSelected = selectedIndex == 1,
                icon = { DashboardTruckIcon(tint = if (selectedIndex == 1) BhpRunningGreen else BhpTextSecondary, modifier = Modifier.size(20.dp)) },
                onClick = { onSelectIndex(1) },
                modifier = Modifier.weight(1f)
            )

            // 3. Drivers
            BottomNavItem(
                label = "Drivers",
                isSelected = selectedIndex == 2,
                icon = { DriverCapIcon(tint = if (selectedIndex == 2) BhpRunningGreen else BhpTextSecondary, modifier = Modifier.size(20.dp)) },
                onClick = { onSelectIndex(2) },
                modifier = Modifier.weight(1f)
            )

            // 4. Reports
            BottomNavItem(
                label = "Reports",
                isSelected = selectedIndex == 3,
                icon = { FormatListIcon(tint = if (selectedIndex == 3) BhpRunningGreen else BhpTextSecondary, modifier = Modifier.size(19.dp)) },
                onClick = { onSelectIndex(3) },
                modifier = Modifier.weight(1f)
            )

            // 5. More
            BottomNavItem(
                label = "More",
                isSelected = selectedIndex == 4,
                icon = { MoreDotsIcon(tint = if (selectedIndex == 4) BhpRunningGreen else BhpTextSecondary, modifier = Modifier.size(18.dp)) },
                onClick = { onSelectIndex(4) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 2.dp)
    ) {
        if (isSelected) {
            Surface(
                color = BhpRunningGreenBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(width = 46.dp, height = 28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    icon()
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(width = 46.dp, height = 28.dp)
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) BhpRunningGreen else BhpTextSecondary,
                fontSize = 9.sp,
                letterSpacing = (-0.3).sp
            ),
            softWrap = false,
            maxLines = 1
        )
    }
}

/**
 * Modal dialog showing all registered fleet vehicles fetched from backend.
 */
@Composable
fun LorriesListDialog(
    lorries: List<LorryItemState>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BhpCardWhite,
        titleContentColor = BhpNavyDark,
        textContentColor = BhpTextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DashboardTruckIcon(tint = BhpNavyDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fleet Commercial Lorries",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpNavyDark
                    )
                )
            }
        },
        text = {
            if (lorries.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Loading vehicles from backend...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = BhpTextSecondary)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(lorries) { lorry ->
                        val badgeColor = when (lorry.operationalStatus) {
                            "ON_TRIP" -> BhpRunningGreen
                            "AVAILABLE" -> BhpReadyBlue
                            else -> BhpNotReadyAmber
                        }
                        Surface(
                            color = BhpCardWhite,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BhpCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lorry.registrationNumber,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BhpTextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = lorry.operationalStatus,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${lorry.makeModel} • ${lorry.capacityTons} Tons",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BhpTextSecondary)
                                )
                                if (lorry.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lorry.notes,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BhpTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = BhpDarkForestGreen)
            }
        }
    )
}

