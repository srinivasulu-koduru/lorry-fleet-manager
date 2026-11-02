package com.lorryowner.app.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.lorryowner.app.ui.components.*

@Composable
fun MonthlyFinanceDetailsScreen(
    year: Int,
    month: Int,
    sessionManager: SessionManager? = null,
    onBack: () -> Unit = {},
    onViewFullYearAnalysis: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = remember(year, month) {
        MonthlyFinanceDetailsViewModel(initialYear = year, initialMonth = month, sessionManager = sessionManager)
    }
    val state = viewModel.uiState
    var isYearDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(year, month) {
        viewModel.loadDetails(year, month)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDetails(state.year, state.month)
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
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "←",
                            color = Color(0xFF0F172A),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Yearly Finance History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { /* filter action */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        FilterSlidersIcon(
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { onViewFullYearAnalysis(state.year) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("📊", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Full Year Analysis",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Year Selector Pill
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { isYearDropdownExpanded = true }
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
                                text = "${state.year}",
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
                            val years = listOf(state.year, state.year - 1).distinct()
                            for (y in years) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$y",
                                            fontWeight = if (y == state.year) FontWeight.Bold else FontWeight.Normal,
                                            color = if (y == state.year) Color(0xFF10B981) else Color(0xFF0F172A)
                                        )
                                    },
                                    onClick = {
                                        isYearDropdownExpanded = false
                                        viewModel.selectYear(y)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. 12-Month Interactive Grid Chips (2 Rows of 6)
            item {
                val monthsRow1 = listOf(1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr", 5 to "May", 6 to "Jun")
                val monthsRow2 = listOf(7 to "Jul", 8 to "Aug", 9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dec")

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1 (Jan - Jun)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((mNum, mName) in monthsRow1) {
                            MonthChipItem(
                                monthNum = mNum,
                                monthName = mName,
                                isSelected = mNum == state.month,
                                onClick = { viewModel.selectMonth(mNum) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Row 2 (Jul - Dec)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((mNum, mName) in monthsRow2) {
                            MonthChipItem(
                                monthNum = mNum,
                                monthName = mName,
                                isSelected = mNum == state.month,
                                onClick = { viewModel.selectMonth(mNum) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Selected Month Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF064E3B),
                                        Color(0xFF065F46),
                                        Color(0xFF047857)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            val monthUpper = if (state.monthName.isNotBlank()) state.monthName.uppercase() else "OCTOBER"
                            Text(
                                text = "$monthUpper ${state.year} SUMMARY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0),
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Income
                                MonthSummaryValueColumn(
                                    icon = { UpTrendCircleIcon(modifier = Modifier.size(24.dp)) },
                                    amount = state.totalIncome,
                                    label = "Income",
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(1.dp)
                                        .background(Color.White.copy(alpha = 0.20f))
                                )

                                // Expense
                                MonthSummaryValueColumn(
                                    icon = { DownTrendCircleIcon(modifier = Modifier.size(24.dp)) },
                                    amount = state.totalExpenses,
                                    label = "Expense",
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(1.dp)
                                        .background(Color.White.copy(alpha = 0.20f))
                                )

                                // Profit
                                MonthSummaryValueColumn(
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFF10B981).copy(alpha = 0.25f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("📊", fontSize = 11.sp)
                                        }
                                    },
                                    amount = state.netProfit,
                                    label = "Profit",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Section: FINANCIAL DETAILS
            item {
                Text(
                    text = "FINANCIAL DETAILS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // 5. 3-Bar Comparison Graph
            item {
                FinanceSelectedMonthBarChart(
                    income = state.totalIncome,
                    expense = state.totalExpenses,
                    profit = state.netProfit
                )
            }

            // 6. Section: TRANSACTION BREAKDOWN
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("⏱️", fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TRANSACTION BREAKDOWN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 7. Totals Card (Income, Expense, Net Profit Trophy)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Total Income Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UpTrendCircleIcon(modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Total Income",
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = FinanceFormattingHelper.formatIndianRupee(state.totalIncome),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Total Expense Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DownTrendCircleIcon(modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Total Expense",
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = FinanceFormattingHelper.formatIndianRupee(state.totalExpenses),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Highlighted Net Profit Row with Trophy
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TrophyIcon(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Net Profit",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Text(
                                    text = FinanceFormattingHelper.formatIndianRupee(state.netProfit),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }
            }

            // 8. Detailed Transactions List
            if (state.incomeTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = "INCOME RECORDS (${state.incomeTransactions.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                items(state.incomeTransactions, key = { "inc_${it.id}" }) { item ->
                    TransactionItemCard(item = item, isIncome = true)
                }
            }

            if (state.expenseTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = "EXPENSE RECORDS (${state.expenseTransactions.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                items(state.expenseTransactions, key = { "exp_${it.id}_${it.category}" }) { item ->
                    TransactionItemCard(item = item, isIncome = false)
                }
            }
        }
    }
}

@Composable
private fun MonthChipItem(
    monthNum: Int,
    monthName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF10B981) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF059669) else Color(0xFFE2E8F0)),
        modifier = modifier
            .height(38.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = monthName,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun MonthSummaryValueColumn(
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
            fontSize = 16.sp,
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
private fun TransactionItemCard(
    item: FinanceTransactionItem,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            if (isIncome) {
                UpTrendCircleIcon(modifier = Modifier.size(32.dp))
            } else {
                DownTrendCircleIcon(modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                    if (!item.lorryRegistration.isNullOrBlank()) {
                        Text(
                            text = " • ${item.lorryRegistration}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = " • ${FinanceFormattingHelper.formatDisplayDate(item.transactionDate)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${if (isIncome) "+" else "-"}${FinanceFormattingHelper.formatIndianRupee(item.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
            )
        }
    }
}
