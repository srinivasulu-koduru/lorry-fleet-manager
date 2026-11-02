package com.lorryowner.app.ui.owner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

private val ChartIncomeGreen = Color(0xFF10B981)
private val ChartExpenseRed = Color(0xFFEF4444)
private val ChartProfitBlue = Color(0xFF3B82F6)
private val ChartGridLine = Color(0xFFE2E8F0)

@Composable
fun Finance12MonthBarChart(
    monthlySummaries: List<MonthlySummaryItem>,
    onMonthClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Chart Legend (Top-Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = ChartIncomeGreen, label = "Income")
                Spacer(modifier = Modifier.width(14.dp))
                ChartLegendItem(color = ChartExpenseRed, label = "Expense")
                Spacer(modifier = Modifier.width(14.dp))
                ChartLegendItem(color = ChartProfitBlue, label = "Profit")
            }

            // Calculate scale ceiling
            var maxVal = 100_000.0
            for (m in monthlySummaries) {
                maxVal = max(maxVal, max(m.income, max(m.expense, m.profit)))
            }
            val step = when {
                maxVal > 1_000_000 -> 500_000.0
                maxVal > 200_000 -> 100_000.0
                maxVal > 50_000 -> 50_000.0
                else -> 25_000.0
            }
            val ceiling = (Math.ceil(maxVal / step) * step).coerceAtLeast(step * 4)

            val ticks = listOf(
                ceiling,
                ceiling * 0.75,
                ceiling * 0.50,
                ceiling * 0.25,
                0.0
            )

            // 2. Chart Body with Fixed Y-Axis and Horizontally Scrollable Months
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Fixed Y-Axis Labels Column
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight()
                        .padding(bottom = 24.dp), // Align with bars above X-axis labels
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (tick in ticks) {
                        Text(
                            text = FinanceFormattingHelper.formatShortRupee(tick),
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Scrollable Month Bars Area
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(scrollState)
                ) {
                    // Full 12-month width (46dp per month = 552dp)
                    val contentWidth = (monthlySummaries.size.coerceAtLeast(12) * 52).dp

                    Column(modifier = Modifier.width(contentWidth).fillMaxHeight()) {
                        // Drawing Canvas for Grid Lines and 3 Grouped Bars per Month
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            val w = size.width
                            val h = size.height

                            // Draw horizontal grid lines matching ticks
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            for (i in ticks.indices) {
                                val y = (h / 4f) * i
                                drawLine(
                                    color = ChartGridLine,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f,
                                    pathEffect = if (i < 4) dashEffect else null
                                )
                            }

                            // Draw 3 grouped bars per month
                            val count = monthlySummaries.size
                            if (count > 0) {
                                val monthSlotWidth = w / count
                                val barWidth = (monthSlotWidth * 0.22f).coerceAtMost(10.dp.toPx())
                                val gap = 3f

                                for (i in monthlySummaries.indices) {
                                    val item = monthlySummaries[i]
                                    val centerX = monthSlotWidth * i + monthSlotWidth / 2f
                                    val groupStartX = centerX - (barWidth * 1.5f + gap)

                                    // Income Bar (Green)
                                    val incH = ((item.income / ceiling) * h).toFloat().coerceIn(0f, h)
                                    if (incH > 0f) {
                                        drawRoundRect(
                                            color = ChartIncomeGreen,
                                            topLeft = Offset(groupStartX, h - incH),
                                            size = Size(barWidth, incH),
                                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                                        )
                                    }

                                    // Expense Bar (Red)
                                    val expH = ((item.expense / ceiling) * h).toFloat().coerceIn(0f, h)
                                    if (expH > 0f) {
                                        drawRoundRect(
                                            color = ChartExpenseRed,
                                            topLeft = Offset(groupStartX + barWidth + gap, h - expH),
                                            size = Size(barWidth, expH),
                                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                                        )
                                    }

                                    // Profit Bar (Blue)
                                    val profitH = ((item.profit.coerceAtLeast(0.0) / ceiling) * h).toFloat().coerceIn(0f, h)
                                    if (profitH > 0f) {
                                        drawRoundRect(
                                            color = ChartProfitBlue,
                                            topLeft = Offset(groupStartX + (barWidth + gap) * 2f, h - profitH),
                                            size = Size(barWidth, profitH),
                                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                                        )
                                    }
                                }
                            }
                        }

                        // Month X-Axis Clickable Labels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (item in monthlySummaries) {
                                Box(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .clickable { onMonthClick(item.month) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.monthName,
                                        fontSize = 11.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceSelectedMonthBarChart(
    income: Double,
    expense: Double,
    profit: Double,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val maxVal = max(income, max(expense, max(profit, 100_000.0)))
            val step = when {
                maxVal > 1_000_000 -> 500_000.0
                maxVal > 200_000 -> 100_000.0
                else -> 50_000.0
            }
            val ceiling = (Math.ceil(maxVal / step) * step).coerceAtLeast(step * 4)

            val ticks = listOf(
                ceiling,
                ceiling * 0.75,
                ceiling * 0.50,
                ceiling * 0.25,
                0.0
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Fixed Y-Axis Column
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight()
                        .padding(bottom = 44.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (tick in ticks) {
                        Text(
                            text = FinanceFormattingHelper.formatShortRupee(tick),
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Chart Area
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val w = size.width
                        val h = size.height

                        // Grid Lines
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        for (i in ticks.indices) {
                            val y = (h / 4f) * i
                            drawLine(
                                color = ChartGridLine,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f,
                                pathEffect = if (i < 4) dashEffect else null
                            )
                        }

                        // 3 Large Individual Comparison Bars
                        val slotWidth = w / 3f
                        val barWidth = slotWidth * 0.60f

                        // 1. Income Bar (Green)
                        val incH = ((income / ceiling) * h).toFloat().coerceIn(0f, h)
                        if (incH > 0f) {
                            val startX = slotWidth * 0.5f - barWidth / 2f
                            drawRoundRect(
                                color = ChartIncomeGreen,
                                topLeft = Offset(startX, h - incH),
                                size = Size(barWidth, incH),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }

                        // 2. Expense Bar (Red)
                        val expH = ((expense / ceiling) * h).toFloat().coerceIn(0f, h)
                        if (expH > 0f) {
                            val startX = slotWidth * 1.5f - barWidth / 2f
                            drawRoundRect(
                                color = ChartExpenseRed,
                                topLeft = Offset(startX, h - expH),
                                size = Size(barWidth, expH),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }

                        // 3. Profit Bar (Blue)
                        val profitH = ((profit.coerceAtLeast(0.0) / ceiling) * h).toFloat().coerceIn(0f, h)
                        if (profitH > 0f) {
                            val startX = slotWidth * 2.5f - barWidth / 2f
                            drawRoundRect(
                                color = ChartProfitBlue,
                                topLeft = Offset(startX, h - profitH),
                                size = Size(barWidth, profitH),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }

                    // Bar Labels and Amounts below Canvas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MonthlyBarLabelItem(amount = income, label = "Income", modifier = Modifier.weight(1f))
                        MonthlyBarLabelItem(amount = expense, label = "Expense", modifier = Modifier.weight(1f))
                        MonthlyBarLabelItem(amount = profit, label = "Profit", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBarLabelItem(
    amount: Double,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = FinanceFormattingHelper.formatIndianRupee(amount),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF475569)
        )
    }
}
