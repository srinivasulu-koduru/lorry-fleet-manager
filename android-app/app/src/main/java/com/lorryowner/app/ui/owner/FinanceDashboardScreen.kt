package com.lorryowner.app.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.BhpPullToRefreshBox
import com.lorryowner.app.ui.components.*

@Composable
fun FinanceDashboardScreen(
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onNavigateToMonthDetails: (year: Int, month: Int) -> Unit = { _, _ -> },
    onNavigateToHome: () -> Unit = {},
    onNavigateToMaintenance: () -> Unit = {},
    onNavigateToLiveTracking: () -> Unit = {},
    onNavigateToMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember { FinanceDashboardViewModel(sessionManager = sessionManager) }
    val state = viewModel.uiState
    var isYearDropdownExpanded by remember { mutableStateOf(false) }

    // Automatically reload latest data when screen enters composition
    LaunchedEffect(state.selectedYear) {
        viewModel.loadSummary(state.selectedYear)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadSummary(state.selectedYear)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        HamburgerMenuIcon(
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Finance",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Track your business performance",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadSummary(state.selectedYear) },
                        enabled = !state.isLoading,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF10B981)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Finance Data",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            onNavigateToMonthDetails(state.selectedYear, state.currentMonthNumber)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        FinanceTrendingIcon(
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            OwnerBottomNavBar(
                selectedIndex = 3,
                onSelectIndex = { index ->
                    when (index) {
                        0 -> onNavigateToHome()
                        1 -> onNavigateToLiveTracking()
                        2 -> onNavigateToMaintenance()
                        3 -> { /* Already on Finance */ }
                        4 -> onNavigateToMore()
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BhpPullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadSummary(state.selectedYear) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. Year Selector Pill Button
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .clickable { isYearDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CalendarIcon(
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${state.selectedYear}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "▼",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        DropdownMenu(
                            expanded = isYearDropdownExpanded,
                            onDismissRequest = { isYearDropdownExpanded = false }
                        ) {
                            for (year in state.availableYears) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$year",
                                            fontWeight = if (year == state.selectedYear) FontWeight.Bold else FontWeight.Normal,
                                            color = if (year == state.selectedYear) Color(0xFF10B981) else Color(0xFF0F172A)
                                        )
                                    },
                                    onClick = {
                                        isYearDropdownExpanded = false
                                        viewModel.selectYear(year)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Current Month Summary Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigateToMonthDetails(state.selectedYear, state.currentMonthNumber)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF064E3B), // Deep emerald green
                                        Color(0xFF065F46),
                                        Color(0xFF047857)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            // Top Row: Title + Lorry Illustration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CURRENT MONTH SUMMARY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA7F3D0),
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val monthTitle = if (state.currentMonth.isNotBlank()) {
                                        "${state.currentMonth} ${state.selectedYear}"
                                    } else {
                                        "October 2026"
                                    }
                                    Text(
                                        text = monthTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD1FAE5)
                                    )
                                }

                                HeroWhiteTruckGraphic(
                                    modifier = Modifier.size(width = 85.dp, height = 55.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3 Financial Columns: Income | Expense | Profit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Income
                                FinancialMetricColumn(
                                    icon = { UpTrendCircleIcon(modifier = Modifier.size(26.dp)) },
                                    amount = state.currentMonthIncome,
                                    label = "Income",
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(1.dp)
                                        .background(Color.White.copy(alpha = 0.20f))
                                )

                                // Expense
                                FinancialMetricColumn(
                                    icon = { DownTrendCircleIcon(modifier = Modifier.size(26.dp)) },
                                    amount = state.currentMonthExpense,
                                    label = "Expense",
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(1.dp)
                                        .background(Color.White.copy(alpha = 0.20f))
                                )

                                // Profit
                                FinancialMetricColumn(
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(Color(0xFF10B981).copy(alpha = 0.25f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("📊", fontSize = 12.sp)
                                        }
                                    },
                                    amount = state.currentMonthProfit,
                                    label = "Profit",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Section Header: ANALYSIS OVERVIEW
            item {
                Text(
                    text = "ANALYSIS OVERVIEW",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // 4. 12-Month Grouped Bar Chart
            item {
                Finance12MonthBarChart(
                    monthlySummaries = state.monthlySummaries,
                    onMonthClick = { month ->
                        onNavigateToMonthDetails(state.selectedYear, month)
                    }
                )
            }

            // 5. Section Header: QUICK INSIGHTS
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("💡", fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "QUICK INSIGHTS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 6. 3 Quick Insights Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Profit Margin Card
                    QuickInsightCard(
                        icon = "📈",
                        value = "${state.quickInsights.profitMargin}%",
                        valueColor = Color(0xFF059669),
                        label = "Profit Margin",
                        containerColor = Color(0xFFECFDF5),
                        borderColor = Color(0xFFA7F3D0),
                        modifier = Modifier.weight(1f)
                    )

                    // 2. This Month Profit Card
                    QuickInsightCard(
                        icon = "📅",
                        value = FinanceFormattingHelper.formatIndianRupee(state.quickInsights.thisMonthProfit),
                        valueColor = Color(0xFF2563EB),
                        label = "This Month Profit",
                        containerColor = Color(0xFFEFF6FF),
                        borderColor = Color(0xFFBFDBFE),
                        modifier = Modifier.weight(1f)
                    )

                    // 3. Total Income (YTD) Card
                    QuickInsightCard(
                        icon = "💰",
                        value = FinanceFormattingHelper.formatIndianRupee(state.quickInsights.ytdIncome),
                        valueColor = Color(0xFFB45309),
                        label = "Total Income (YTD)",
                        containerColor = Color(0xFFFFFBEB),
                        borderColor = Color(0xFFFDE68A),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
}

@Composable
private fun FinancialMetricColumn(
    icon: @Composable () -> Unit,
    amount: Double,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = FinanceFormattingHelper.formatIndianRupee(amount),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFD1FAE5)
        )
    }
}

@Composable
private fun QuickInsightCard(
    icon: String,
    value: String,
    valueColor: Color,
    label: String,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                maxLines = 1
            )
        }
    }
}
