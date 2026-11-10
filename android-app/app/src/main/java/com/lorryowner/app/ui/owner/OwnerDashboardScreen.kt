package com.lorryowner.app.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.lorryowner.app.R
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BhpPullToRefreshBox
import com.lorryowner.app.ui.components.CheckmarkCircleIcon
import com.lorryowner.app.ui.components.ChevronRightIcon
import com.lorryowner.app.ui.components.ClockTimerIcon
import com.lorryowner.app.ui.components.DashboardTruckIcon
import com.lorryowner.app.ui.components.DriverCapIcon
import com.lorryowner.app.ui.components.FinanceTrendingIcon
import com.lorryowner.app.ui.components.FormatListIcon
import com.lorryowner.app.ui.components.GasPumpIcon
import com.lorryowner.app.ui.components.GreetingTruckGraphic
import com.lorryowner.app.ui.components.HamburgerMenuIcon
import com.lorryowner.app.ui.components.HomeNavIcon
import com.lorryowner.app.ui.components.LocationPinIcon
import com.lorryowner.app.ui.components.MoreDotsIcon
import com.lorryowner.app.ui.components.NotificationBellIcon
import com.lorryowner.app.ui.components.ShieldVectorIcon
import com.lorryowner.app.ui.components.WrenchToolIcon
import com.lorryowner.app.ui.theme.BhpAvatarBorder
import com.lorryowner.app.ui.theme.BhpAvatarCream
import com.lorryowner.app.ui.theme.BhpBgLight
import com.lorryowner.app.ui.theme.BhpCardBorder
import com.lorryowner.app.ui.theme.BhpCardWhite
import com.lorryowner.app.ui.theme.BhpDarkForestGreen
import com.lorryowner.app.ui.theme.BhpDieselOrange
import com.lorryowner.app.ui.theme.BhpDriverBlue
import com.lorryowner.app.ui.theme.BhpGold
import com.lorryowner.app.ui.theme.BhpGoldAccent
import com.lorryowner.app.ui.theme.BhpGoldDark
import com.lorryowner.app.ui.theme.BhpMaroonDark
import com.lorryowner.app.ui.theme.BhpMaroonDeep
import com.lorryowner.app.ui.theme.BhpMaroonPrimary
import com.lorryowner.app.ui.theme.BhpMaroonSubtext
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
    LIVE_SATELLITE_MAP,
    FINANCE_DASHBOARD,
    MONTHLY_FINANCE_DETAILS
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
    var selectedFinanceYear by remember { mutableStateOf(java.time.LocalDate.now().year) }
    var selectedFinanceMonth by remember { mutableStateOf(java.time.LocalDate.now().monthValue) }
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

    // Refresh dashboard metrics dynamically when navigating or returning to the dashboard
    LaunchedEffect(currentDestination) {
        if (currentDestination == OwnerDestination.DASHBOARD) {
            viewModel.refreshDashboard()
        }
    }

    // Auto-refresh dashboard metrics when returning to the application from background (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (currentDestination == OwnerDestination.DASHBOARD) {
                    viewModel.refreshDashboard()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
            OwnerDestination.MONTHLY_FINANCE_DETAILS -> currentDestination = OwnerDestination.FINANCE_DASHBOARD
            OwnerDestination.FINANCE_DASHBOARD -> currentDestination = OwnerDestination.DASHBOARD
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
                onAddRecordForLorry = { lorryId ->
                    selectedLorryIdForMaintenance = lorryId
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
        OwnerDestination.FINANCE_DASHBOARD -> {
            FinanceDashboardScreen(
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.DASHBOARD },
                onNavigateToMonthDetails = { year, month ->
                    selectedFinanceYear = year
                    selectedFinanceMonth = month
                    currentDestination = OwnerDestination.MONTHLY_FINANCE_DETAILS
                },
                onNavigateToHome = { currentDestination = OwnerDestination.DASHBOARD },
                onNavigateToMaintenance = { currentDestination = OwnerDestination.MAINTENANCE_DASHBOARD },
                onNavigateToLiveTracking = { currentDestination = OwnerDestination.LIVE_TRACKING_LIST },
                onNavigateToMore = {
                    currentDestination = OwnerDestination.DASHBOARD
                    viewModel.showProfileDialog()
                },
                modifier = modifier
            )
        }
        OwnerDestination.MONTHLY_FINANCE_DETAILS -> {
            MonthlyFinanceDetailsScreen(
                year = selectedFinanceYear,
                month = selectedFinanceMonth,
                sessionManager = effectiveSession,
                onBack = { currentDestination = OwnerDestination.FINANCE_DASHBOARD },
                onViewFullYearAnalysis = { year ->
                    selectedFinanceYear = year
                    currentDestination = OwnerDestination.FINANCE_DASHBOARD
                },
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
                onNavigateToFinance = { currentDestination = OwnerDestination.FINANCE_DASHBOARD },
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
    onNavigateToFinance: () -> Unit = {},
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
                        1 -> onNavigateToLiveTracking()
                        2 -> onNavigateToMaintenance()
                        3 -> onNavigateToFinance()
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
                .background(BhpBgLight)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // 1. Premium Branded Maroon Wave Header with Real Logo & System Insets
            OwnerDashboardTopBar(
                onMenuClick = { viewModel.showProfileDialog() },
                unreadNotifications = state.unreadNotificationsCount,
                onNotificationsClick = { viewModel.showProfileDialog() },
                onRefreshClick = { viewModel.refreshDashboard() },
                isRefreshing = state.isRefreshing
            )

            // 2. Scrollable Body with Pull-to-refresh
            BhpPullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refreshDashboard() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // A. Hero Welcome Banner (Your Fleet, Your Control • Smooth Auto-Carousel)
                    OwnerRotatingWelcomeBanner()

                    Spacer(modifier = Modifier.height(16.dp))

                    // B. Circular Fleet Overview (Donut Chart, Status Counts, Watermark & Month Filter)
                    CircularFleetOverviewCard(
                        fleetOverview = state.fleetOverview,
                        onRunningClick = onNavigateToRunningLorries,
                        onReadyClick = onNavigateToMyLorries,
                        onNotReadyClick = onNavigateToNotRunningLorries
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // C. Financial Summary Cards (2x2 Grid: Total Trips, Total Income, Total Expenses, Net Profit)
                    DashboardFinancialCardsGrid(
                        financialSummary = state.financialSummary,
                        onTripsClick = onNavigateToAddTrip,
                        onIncomeClick = onNavigateToFinance,
                        onExpensesClick = onNavigateToMaintenance,
                        onProfitClick = onNavigateToFinance
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // D. Quick Actions Section (Exactly 3 Actions: Add Trip, Manage Drivers, My Lorries)
                    QuickActionsSection(
                        onAddTripClick = onNavigateToAddTrip,
                        onManageDriversClick = onNavigateToDrivers,
                        onMyLorriesClick = onNavigateToMyLorries
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
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
 * Top Header: BHP Logistics Branding, Subtitle, Real Logo, Refresh, Notification Bell.
 * Rendered with an Organic Curved Wave filled with Deep Maroon Gradient and a Gold Accent Line,
 * seamlessly transitioning to the light background underneath.
 */
@Composable
fun OwnerDashboardTopBar(
    onMenuClick: () -> Unit,
    unreadNotifications: Int,
    onNotificationsClick: () -> Unit = onMenuClick,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val waveHeightPx = with(density) { 26.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val w = size.width
                val h = size.height

                // Wave path defining the organic bottom boundary of the header:
                // Dips gently on the left under the logo badge (up to h),
                // then swoops up smoothly towards the right (up to h - waveHeightPx).
                val wavePath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, h - waveHeightPx * 0.55f)
                    cubicTo(
                        w * 0.08f, h,
                        w * 0.22f, h,
                        w * 0.38f, h - waveHeightPx * 0.35f
                    )
                    cubicTo(
                        w * 0.58f, h - waveHeightPx * 0.85f,
                        w * 0.80f, h - waveHeightPx * 1.0f,
                        w, h - waveHeightPx * 0.65f
                    )
                    lineTo(w, 0f)
                    close()
                }

                // Fill only above the wave with rich maroon gradient
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(BhpMaroonPrimary, BhpMaroonDark)
                    )
                )

                // Gold trim line tracing the exact wave
                val goldPath = Path().apply {
                    moveTo(0f, h - waveHeightPx * 0.55f)
                    cubicTo(
                        w * 0.08f, h,
                        w * 0.22f, h,
                        w * 0.38f, h - waveHeightPx * 0.35f
                    )
                    cubicTo(
                        w * 0.58f, h - waveHeightPx * 0.85f,
                        w * 0.80f, h - waveHeightPx * 1.0f,
                        w, h - waveHeightPx * 0.65f
                    )
                }

                drawPath(
                    path = goldPath,
                    color = BhpGold,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
            .statusBarsPadding()
            .padding(bottom = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Official BHP Logistics Logo in Rounded White Container
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMenuClick() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bhp_logo),
                    contentDescription = "BHP Logistics Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BHP Logistics",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        letterSpacing = 0.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "Fleet Owner Dashboard",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BhpMaroonSubtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            // Quick Refresh Action
            if (onRefreshClick != null) {
                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = BhpGold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Fleet Data",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Notification Bell with Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onNotificationsClick() }
            ) {
                NotificationBellIcon(
                    tint = Color.White,
                    hasBadge = unreadNotifications > 0,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Hero Welcome Banner: Real Compose Card with Commercial Lorry, Sunset Scenery,
 * "Your Fleet, Your Control", Gold Pin, and 4-Slide Auto-Rotating Carousel.
 */
private data class BannerSlide(
    val titleTop: String,
    val titleBottom: String,
    val subtitle: String
)

@Composable
fun OwnerRotatingWelcomeBanner(
    modifier: Modifier = Modifier
) {
    val slides = remember {
        listOf(
            BannerSlide(
                titleTop = "Your Fleet,",
                titleBottom = "Your Control",
                subtitle = "Track every lorry in one place."
            ),
            BannerSlide(
                titleTop = "Drive More,",
                titleBottom = "Earn More",
                subtitle = "Smart Fleet. Smart Business."
            ),
            BannerSlide(
                titleTop = "Keep Your",
                titleBottom = "Business Moving",
                subtitle = "Manage trips and drivers effortlessly."
            ),
            BannerSlide(
                titleTop = "Real-Time",
                titleBottom = "Live Tracking",
                subtitle = "Monitor every kilometer on the road."
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Auto-rotate every 4.5 seconds
    LaunchedEffect(pagerState) {
        while (isActive) {
            delay(4500L)
            val next = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(138.dp)
        ) { page ->
            val slide = slides[page]
            Card(
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Approved High-Res BHP Commercial Lorry Artwork
                    Image(
                        painter = painterResource(id = R.drawable.bhp_hero_banner),
                        contentDescription = "BHP Logistics Hero Banner",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Prominent Gold Curve Swoosh at Bottom Left - visible on EVERY slide
                    Canvas(
                        modifier = Modifier
                            .size(width = 145.dp, height = 28.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.35f)
                            cubicTo(
                                size.width * 0.35f, size.height * 0.45f,
                                size.width * 0.70f, size.height * 0.82f,
                                size.width, size.height
                            )
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path = path, color = BhpGold)
                    }

                    // Slide Typography & Gold Pin (No ghost text!)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 160.dp, top = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        LocationPinIcon(
                            tint = BhpGold,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = slide.titleTop,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            maxLines = 1
                        )

                        Text(
                            text = slide.titleBottom,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BhpGold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = slide.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 10.5.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Page Dots Indicator: 4 dots, active dot is deep maroon #790018
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) BhpMaroonPrimary else Color(0xFFCBD5E1))
                )
            }
        }
    }
}

/**
 * Circular Fleet Overview Card: Donut Chart + Filter + Status Rows + Truck Watermark.
 * Designed to fit comfortably on any standard mobile viewport without text wrapping.
 */
@Composable
fun CircularFleetOverviewCard(
    fleetOverview: FleetOverviewState,
    onRunningClick: () -> Unit,
    onReadyClick: () -> Unit,
    onNotReadyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDropdownOpen by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("This Month") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            // Header: "Fleet Overview" with 3-bar chart icon + "This Month ▼"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE8F5E9))
                ) {
                    Canvas(modifier = Modifier.size(15.dp)) {
                        val barW = 2.6.dp.toPx()
                        // Bar 1: Red/Maroon
                        drawRoundRect(
                            color = Color(0xFFEF4444),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.45f),
                            size = androidx.compose.ui.geometry.Size(barW, size.height * 0.55f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                        )
                        // Bar 2: Dark green
                        drawRoundRect(
                            color = Color(0xFF15803D),
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.12f),
                            size = androidx.compose.ui.geometry.Size(barW, size.height * 0.88f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                        )
                        // Bar 3: Bright green
                        drawRoundRect(
                            color = Color(0xFF10B981),
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.76f, size.height * 0.28f),
                            size = androidx.compose.ui.geometry.Size(barW, size.height * 0.72f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fleet Overview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // "This Month ▼" Filter Pill with Maroon Border
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BhpMaroonPrimary.copy(alpha = 0.7f)),
                        modifier = Modifier.clickable { isDropdownOpen = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = selectedFilter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BhpMaroonPrimary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "▼",
                                fontSize = 7.sp,
                                color = BhpMaroonPrimary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownOpen,
                        onDismissRequest = { isDropdownOpen = false }
                    ) {
                        listOf("This Month", "Today", "All Time").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, fontSize = 13.sp) },
                                onClick = {
                                    selectedFilter = option
                                    isDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Row: Donut Chart on Left, Status Counts in Middle, Brand Watermark on Right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Donut Chart - Compact 86.dp to comfortably fit any phone screen
                val total = fleetOverview.totalLorries
                val runningSweep = if (total > 0) (fleetOverview.runningNow.toFloat() / total.toFloat()) * 360f else 0f
                val readySweep = if (total > 0) (fleetOverview.readyToStart.toFloat() / total.toFloat()) * 360f else 0f
                val notReadySweep = if (total > 0) (fleetOverview.notReady.toFloat() / total.toFloat()) * 360f else 0f

                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        drawArc(
                            color = Color(0xFFF1F5F9),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )

                        if (total > 0) {
                            var currentAngle = -90f
                            if (runningSweep > 0) {
                                drawArc(
                                    color = Color(0xFF10B981), // Green: Running Now
                                    startAngle = currentAngle,
                                    sweepAngle = runningSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                currentAngle += runningSweep
                            }
                            if (readySweep > 0) {
                                drawArc(
                                    color = Color(0xFFF59E0B), // Orange/Gold: Ready to Start
                                    startAngle = currentAngle,
                                    sweepAngle = readySweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                currentAngle += readySweep
                            }
                            if (notReadySweep > 0) {
                                drawArc(
                                    color = Color(0xFF94A3B8), // Grey: Not Loaded
                                    startAngle = currentAngle,
                                    sweepAngle = notReadySweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${fleetOverview.totalLorries}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 21.sp
                            )
                        )
                        Text(
                            text = "Total Lorries",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 8.5.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Middle: 3 Status Rows - guaranteed single-line fit on any device
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FleetStatusRow(
                        color = Color(0xFF10B981),
                        label = "Running Now",
                        count = fleetOverview.runningNow,
                        badgeBg = Color(0xFFDCFCE7),
                        badgeTextColor = Color(0xFF15803D),
                        onClick = onRunningClick
                    )
                    FleetStatusRow(
                        color = Color(0xFFF59E0B),
                        label = "Ready to Start",
                        count = fleetOverview.readyToStart,
                        badgeBg = Color(0xFFFEF3C7),
                        badgeTextColor = Color(0xFFD97706),
                        onClick = onReadyClick
                    )
                    FleetStatusRow(
                        color = Color(0xFF94A3B8),
                        label = "Not Loaded",
                        count = fleetOverview.notReady,
                        badgeBg = Color(0xFFF1F5F9),
                        badgeTextColor = Color(0xFF475569),
                        onClick = onNotReadyClick
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(72.dp)
                        .width(1.dp)
                        .background(Color(0xFFE2E8F0))
                        .padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Right: Watermark Branding Column - Compact width (76.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(76.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bhp_truck_watermark),
                        contentDescription = "BHP Watermark",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(width = 62.dp, height = 30.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "BHP Logistics",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpMaroonPrimary,
                            fontSize = 10.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "Move Forward Together...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A),
                            fontSize = 6.5.sp
                        ),
                        maxLines = 1
                    )
                    Canvas(modifier = Modifier.size(width = 44.dp, height = 3.dp)) {
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.5f)
                            quadraticTo(size.width * 0.5f, size.height, size.width, 0f)
                        }
                        drawPath(path = path, color = BhpGold, style = Stroke(width = 1.5.dp.toPx()))
                    }
                }
            }
        }
    }
}

@Composable
private fun FleetStatusRow(
    color: Color,
    label: String,
    count: Long,
    badgeBg: Color,
    badgeTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 11.5.sp,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = badgeBg
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    fontSize = 10.5.sp
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

/**
 * 4 Financial Summary Cards in a 2x2 Grid (Total Trips, Total Income, Total Expenses, Net Profit).
 */
@Composable
fun DashboardFinancialCardsGrid(
    financialSummary: FinancialSummaryState,
    onTripsClick: () -> Unit,
    onIncomeClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onProfitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Row 1: Total Trips & Total Income
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FinancialMetricTile(
                title = "Total Trips",
                value = financialSummary.totalTrips,
                trend = financialSummary.totalTripsTrend,
                iconBg = Color(0xFFDCFCE7),
                icon = {
                    LocationPinIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(19.dp))
                },
                accentColor = Color(0xFF10B981),
                chartType = 0,
                onClick = onTripsClick,
                modifier = Modifier.weight(1f)
            )

            FinancialMetricTile(
                title = "Total Income",
                value = financialSummary.income,
                trend = financialSummary.incomeTrend,
                iconBg = Color(0xFFE0F2FE),
                icon = {
                    Text("₹", color = Color(0xFF0284C7), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                accentColor = Color(0xFF0284C7),
                chartType = 1,
                onClick = onIncomeClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Total Expenses & Net Profit
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FinancialMetricTile(
                title = "Total Expenses",
                value = financialSummary.expenses,
                trend = financialSummary.expensesTrend,
                iconBg = Color(0xFFFEE2E2),
                icon = {
                    WrenchToolIcon(tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                },
                accentColor = Color(0xFFEF4444),
                chartType = 1,
                onClick = onExpensesClick,
                modifier = Modifier.weight(1f)
            )

            FinancialMetricTile(
                title = "Net Profit",
                value = financialSummary.profit,
                trend = financialSummary.profitTrend,
                iconBg = Color(0xFFDCFCE7),
                icon = {
                    FinanceTrendingIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                },
                accentColor = Color(0xFF10B981),
                chartType = 1,
                onClick = onProfitClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FinancialMetricTile(
    title: String,
    value: String,
    trend: String,
    iconBg: Color,
    icon: @Composable () -> Unit,
    accentColor: Color,
    chartType: Int, // 0 = wave, 1 = bars
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle graphic in bottom-right corner
            Canvas(
                modifier = Modifier
                    .size(width = 48.dp, height = 36.dp)
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
            ) {
                val w = size.width
                val h = size.height
                if (chartType == 0) {
                    val wavePath = Path().apply {
                        moveTo(0f, h * 0.85f)
                        quadraticTo(w * 0.35f, h * 0.2f, w * 0.7f, h * 0.6f)
                        quadraticTo(w * 0.85f, h * 0.8f, w, h * 0.15f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.05f))
                        )
                    )
                } else {
                    val barWidth = 6.dp.toPx()
                    val gap = 4.dp.toPx()
                    val heights = listOf(0.4f, 0.7f, 1.0f)
                    var xOffset = w - (3 * barWidth + 2 * gap)
                    for (barH in heights) {
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.35f * barH),
                            topLeft = androidx.compose.ui.geometry.Offset(xOffset, h * (1f - barH)),
                            size = androidx.compose.ui.geometry.Size(barWidth, h * barH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                        xOffset += barWidth + gap
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBg)
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = trend,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Quick Actions Section: Exactly Three Shortcuts (Add Trip, Manage Drivers, My Lorries).
 * Branded Deep Maroon Header Container with View All in Gold.
 */
@Composable
fun QuickActionsSection(
    onAddTripClick: () -> Unit,
    onManageDriversClick: () -> Unit,
    onMyLorriesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BhpMaroonPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Lightning bolt + "Quick Actions" on left, "View All →" in gold on right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text("⚡", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        letterSpacing = 0.2.sp
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "View All →",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = BhpGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    modifier = Modifier
                        .clickable { onMyLorriesClick() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Exactly THREE Equal-width Action Cards in a single row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Add Trip
                QuickActionCard(
                    title = "Add Trip",
                    icon = { Text("+", color = Color(0xFF0284C7), fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                    iconBg = Color(0xFFE0F2FE),
                    onClick = onAddTripClick,
                    modifier = Modifier.weight(1f)
                )

                // 2. Manage Drivers
                QuickActionCard(
                    title = "Manage Drivers",
                    icon = { DriverCapIcon(tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp)) },
                    iconBg = Color(0xFFDCFCE7),
                    onClick = onManageDriversClick,
                    modifier = Modifier.weight(1f)
                )

                // 3. My Lorries
                QuickActionCard(
                    title = "My Lorries",
                    icon = { DashboardTruckIcon(tint = Color(0xFFD97706), modifier = Modifier.size(20.dp)) },
                    iconBg = Color(0xFFFEF3C7),
                    onClick = onMyLorriesClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: @Composable () -> Unit,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .height(86.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg)
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
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
fun FinancialSummaryCard(
    financialSummary: FinancialSummaryState,
    onClick: () -> Unit = {}
) {
    Surface(
        color = BhpCardWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BhpCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
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
                Text(
                    text = "View Breakdown →",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                )
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
 * Bottom Navigation Bar matching the approved BHP Logistics Maroon & Gold reference design.
 */
@Composable
fun OwnerBottomNavBar(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    Surface(
        color = BhpMaroonPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            BottomNavItem(
                label = "Home",
                isSelected = selectedIndex == 0,
                icon = { HomeNavIcon(tint = if (selectedIndex == 0) BhpMaroonPrimary else Color.White, modifier = Modifier.size(20.dp)) },
                onClick = { onSelectIndex(0) },
                modifier = Modifier.weight(1f)
            )

            // Divider
            Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color.White.copy(alpha = 0.2f)))

            // 2. Tracking
            BottomNavItem(
                label = "Tracking",
                isSelected = selectedIndex == 1,
                icon = { LocationPinIcon(tint = if (selectedIndex == 1) BhpMaroonPrimary else Color.White, modifier = Modifier.size(20.dp)) },
                onClick = { onSelectIndex(1) },
                modifier = Modifier.weight(1f)
            )

            // Divider
            Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color.White.copy(alpha = 0.2f)))

            // 3. Maintenance
            BottomNavItem(
                label = "Maintenance",
                isSelected = selectedIndex == 2,
                icon = { WrenchToolIcon(tint = if (selectedIndex == 2) BhpMaroonPrimary else Color.White, modifier = Modifier.size(20.dp)) },
                onClick = { onSelectIndex(2) },
                modifier = Modifier.weight(1f)
            )

            // Divider
            Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color.White.copy(alpha = 0.2f)))

            // 4. Finance
            BottomNavItem(
                label = "Finance",
                isSelected = selectedIndex == 3,
                icon = { FinanceTrendingIcon(tint = if (selectedIndex == 3) BhpMaroonPrimary else Color.White, modifier = Modifier.size(20.dp)) },
                onClick = { onSelectIndex(3) },
                modifier = Modifier.weight(1f)
            )

            // Divider
            Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color.White.copy(alpha = 0.2f)))

            // 5. More
            BottomNavItem(
                label = "More",
                isSelected = selectedIndex == 4,
                icon = { MoreDotsIcon(tint = if (selectedIndex == 4) BhpMaroonPrimary else Color.White, modifier = Modifier.size(18.dp)) },
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
                color = BhpGold,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(width = 54.dp, height = 30.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    icon()
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(width = 54.dp, height = 30.dp)
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) BhpGold else Color.White,
                fontSize = 10.sp
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

