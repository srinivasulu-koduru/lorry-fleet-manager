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
import androidx.compose.runtime.remember
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

@Composable
fun OwnerDashboardScreen(
    sessionManager: SessionManager? = null,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSession = remember { sessionManager ?: SessionManager(context) }
    val viewModel = remember { OwnerDashboardViewModel(effectiveSession) }
    val state = viewModel.uiState
    val lifecycleOwner = LocalLifecycleOwner.current

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

    Scaffold(
        bottomBar = {
            OwnerBottomNavBar(
                selectedIndex = state.selectedNavIndex,
                onSelectIndex = { index ->
                    viewModel.onNavItemSelected(index)
                    if (index == 4) {
                        viewModel.showProfileDialog()
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
            // 1. Top App Bar (Deep Navy Header)
            OwnerTopAppBar(
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
                // Greeting Card
                GreetingCard(
                    greeting = state.greeting,
                    ownerName = "Owner"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Fleet Overview Section
                Text(
                    text = "Fleet Overview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                FleetOverviewGrid(
                    fleetOverview = state.fleetOverview,
                    onTotalClick = { viewModel.showLorriesDialog() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "This month" Financial Card
                FinancialSummaryCard(financialSummary = state.financialSummary)

                Spacer(modifier = Modifier.height(22.dp))

                // Quick Actions Section
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                QuickActionsRow(
                    onMyLorriesClick = { viewModel.showLorriesDialog() }
                )

                Spacer(modifier = Modifier.height(22.dp))

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
                        modifier = Modifier.clickable { viewModel.showLorriesDialog() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                RecentActivityList(
                    activities = state.recentActivities,
                    onItemClick = { viewModel.showLorriesDialog() }
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

    // Modal Dialog: Owner Profile & Sign Out
    if (state.isProfileDialogVisible) {
        OwnerProfileDialog(
            ownerName = state.ownerName,
            username = effectiveSession.getUsername(),
            onDismiss = { viewModel.dismissProfileDialog() },
            onLogout = onLogout
        )
    }
}

/**
 * Top App Bar with Navy Background, Truck Badge, Title, Bell with Badge, and Avatar.
 */
@Composable
fun OwnerTopAppBar(
    unreadNotifications: Int,
    ownerInitial: String,
    onProfileClick: () -> Unit
) {
    Surface(
        color = BhpNavyDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Truck Icon in Rounded Square
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BhpNavyContainer)
            ) {
                DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Branding Titles
            Column {
                Text(
                    text = "BHP Logistics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
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

            // Notification Bell with Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BhpNavyContainer)
            ) {
                NotificationBellIcon(
                    tint = Color.White,
                    hasBadge = unreadNotifications > 0,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar circle "O" with cream border
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BhpAvatarCream)
                    .border(2.dp, BhpAvatarBorder, CircleShape)
                    .clickable { onProfileClick() }
            ) {
                Text(
                    text = "O",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpNavyDark,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

/**
 * White card with dynamic time-based greeting (e.g. "Good Morning, Owner") and truck graphic illustration.
 */
@Composable
fun GreetingCard(
    greeting: String = GreetingHelper.getGreeting(),
    ownerName: String = "Owner"
) {
    Surface(
        color = BhpCardWhite,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BhpCardBorder, RoundedCornerShape(20.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$greeting,\n$ownerName",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Manage your fleet at a glance.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BhpTextSecondary,
                        fontSize = 13.sp
                    )
                )
            }

            GreetingTruckGraphic(
                modifier = Modifier.size(width = 104.dp, height = 52.dp)
            )
        }
    }
}

/**
 * 2x2 Grid of Status Cards (Running Now, Ready to Start, Not Ready, Total Lorries).
 */
@Composable
fun FleetOverviewGrid(
    fleetOverview: FleetOverviewState,
    onTotalClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Running Now (Green)
            OverviewCard(
                title = "Running Now",
                subtitle = "On the road",
                count = fleetOverview.runningNow.toString(),
                containerColor = BhpRunningGreenBg,
                accentColor = BhpRunningGreen,
                textColor = BhpRunningGreenText,
                icon = { DashboardTruckIcon(tint = Color.White, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f)
            )

            // Ready to Start (Blue)
            OverviewCard(
                title = "Ready to Start",
                subtitle = "Ready for dispatch",
                count = fleetOverview.readyToStart.toString(),
                containerColor = BhpReadyBlueBg,
                accentColor = BhpReadyBlue,
                textColor = BhpReadyBlueText,
                icon = { CheckmarkCircleIcon(tint = Color.White, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Not Ready (Amber)
            OverviewCard(
                title = "Not Ready",
                subtitle = "Loading / Pending",
                count = fleetOverview.notReady.toString(),
                containerColor = BhpNotReadyAmberBg,
                accentColor = BhpNotReadyAmber,
                textColor = BhpNotReadyAmberText,
                icon = { ClockTimerIcon(tint = Color.White, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f)
            )

            // Total Lorries (Deep Navy)
            OverviewCard(
                title = "Total Lorries",
                subtitle = "Registered vehicles",
                count = fleetOverview.totalLorries.toString(),
                containerColor = BhpNavyDark,
                accentColor = Color(0xFF233B5D),
                textColor = Color.White,
                subtitleColor = Color(0xFF8EA6C5),
                icon = { FormatListIcon(tint = Color.White, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTotalClick() }
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
 * Quick Actions row (Track Lorry, Maintenance, Diesel Records, My Lorries).
 */
@Composable
fun QuickActionsRow(
    onMyLorriesClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        QuickActionButton(
            label = "Track Lorry",
            icon = { LocationPinIcon(tint = BhpRunningGreen, modifier = Modifier.size(24.dp)) },
            onClick = { },
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "Maintenance",
            icon = { WrenchToolIcon(tint = BhpReadyBlue, modifier = Modifier.size(22.dp)) },
            onClick = { },
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "Diesel\nRecords",
            icon = { GasPumpIcon(tint = BhpDieselOrange, modifier = Modifier.size(22.dp)) },
            onClick = { },
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            label = "My Lorries",
            icon = { DashboardTruckIcon(tint = BhpNavyDark, modifier = Modifier.size(22.dp)) },
            onClick = onMyLorriesClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Surface(
            color = BhpCardWhite,
            shape = CircleShape,
            shadowElevation = 2.dp,
            modifier = Modifier
                .size(52.dp)
                .border(1.dp, BhpCardBorder, CircleShape)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = BhpTextPrimary,
                fontSize = 9.5.sp,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.5.sp
            ),
            softWrap = false,
            maxLines = 2
        )
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

            // 3. Maintenance
            BottomNavItem(
                label = "Maintenance",
                isSelected = selectedIndex == 2,
                icon = { WrenchToolIcon(tint = if (selectedIndex == 2) BhpRunningGreen else BhpTextSecondary, modifier = Modifier.size(19.dp)) },
                onClick = { onSelectIndex(2) },
                modifier = Modifier.weight(1f)
            )

            // 4. Diesel
            BottomNavItem(
                label = "Diesel",
                isSelected = selectedIndex == 3,
                icon = { GasPumpIcon(tint = if (selectedIndex == 3) BhpRunningGreen else BhpTextSecondary, modifier = Modifier.size(19.dp)) },
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

/**
 * Modal dialog showing Owner details with sign out option.
 */
@Composable
fun OwnerProfileDialog(
    ownerName: String,
    username: String,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BhpCardWhite,
        titleContentColor = BhpNavyDark,
        textContentColor = BhpTextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldVectorIcon(tint = BhpDarkForestGreen, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Owner Account",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpDarkForestGreen
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = ownerName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BhpTextPrimary
                    )
                )
                Text(
                    text = "Username: $username • Role: OWNER",
                    style = MaterialTheme.typography.bodyMedium.copy(color = BhpTextSecondary)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BHP Logistics Fleet Management System",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BhpDarkForestGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Sign Out / Lock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BhpTextSecondary)
            }
        }
    )
}
