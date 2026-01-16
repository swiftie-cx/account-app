package com.swiftiecx.timeledger.ui.feature.chart.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.local.entity.Expense
import com.swiftiecx.timeledger.ui.feature.chart.util.ChartMode
import com.swiftiecx.timeledger.ui.feature.chart.util.TransactionType
import com.swiftiecx.timeledger.ui.feature.chart.util.generateBalanceReportItems
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardHeader(
    chartMode: ChartMode,
    currentDate: Calendar,
    rangeStart: Long,
    rangeEnd: Long,
    transactionType: TransactionType,
    totalExpense: Double,
    totalIncome: Double,
    totalBalance: Double,
    isCustomRange: Boolean,
    defaultCurrency: String, // [新增参数]
    onModeChange: (ChartMode) -> Unit,
    onDateChange: (Int) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCustomRangeClick: () -> Unit,
    onBackFromCustom: () -> Unit
) {
    // 【修正】将 stringResource 移到 remember 外部
    val dateFormatString = stringResource(R.string.date_format_full)
    val monthFormatString = stringResource(R.string.date_format_year_month)
    val yearFormatString = stringResource(R.string.date_format_year)

    val dateFormat = remember(dateFormatString) { SimpleDateFormat(dateFormatString, Locale.getDefault()) }
    val monthFormat = remember(monthFormatString) { SimpleDateFormat(monthFormatString, Locale.getDefault()) }
    val yearFormat = remember(yearFormatString) { SimpleDateFormat(yearFormatString, Locale.getDefault()) }

    // [i18n] 模式名称
    val modeWeek = stringResource(R.string.time_week_short)
    val modeMonth = stringResource(R.string.time_month_short)
    val modeYear = stringResource(R.string.time_year_short)
    val customRangeTitle = stringResource(R.string.time_custom)
    val weekUnit = stringResource(R.string.week_unit) // [修正] 提前获取，避免在 remember 块内报错

    val dateTitle = remember(chartMode, currentDate, isCustomRange, rangeStart, rangeEnd, weekUnit) {
        if (isCustomRange) {
            customRangeTitle
        } else {
            when (chartMode) {
                ChartMode.WEEK -> "$modeWeek ${currentDate.get(Calendar.WEEK_OF_YEAR)} $weekUnit"
                ChartMode.MONTH -> monthFormat.format(currentDate.time)
                ChartMode.YEAR -> yearFormat.format(currentDate.time)
            }
        }
    }

    val rangeText = remember(rangeStart, rangeEnd, dateFormat) {
        "${dateFormat.format(Date(rangeStart))} - ${dateFormat.format(Date(rangeEnd))}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 顶部模式切换
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .height(32.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartMode.values().forEach { mode ->
                val isSelected = chartMode == mode && !isCustomRange
                val bgColor by animateColorAsState(if (isSelected) Color.White else Color.Transparent, label = "bg")
                val textColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.White, label = "text")

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (mode) {
                            ChartMode.WEEK -> modeWeek
                            ChartMode.MONTH -> modeMonth
                            ChartMode.YEAR -> modeYear
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if(isCustomRange) Color.White else Color.Transparent)
                    .clickable { onCustomRangeClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    stringResource(R.string.time_custom),
                    tint = if(isCustomRange) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 2. 日期导航
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isCustomRange) {
                IconButton(onClick = onBackFromCustom) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                }
            } else {
                IconButton(onClick = { onDateChange(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.prev_period), tint = Color.White)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            if (isCustomRange) {
                Spacer(Modifier.width(48.dp))
            } else {
                IconButton(onClick = { onDateChange(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_period), tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. 统计卡片选择器
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 结余卡片
            val balanceSelected = transactionType == TransactionType.BALANCE
            StatSelectionCard(
                title = stringResource(R.string.chart_balance_title),
                amount = totalBalance,
                currency = defaultCurrency, // [传入货币]
                isSelected = balanceSelected,
                selectedBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                unselectedBgColor = MaterialTheme.colorScheme.surface,
                textColor = MaterialTheme.colorScheme.primary,
                onClick = { onTypeChange(TransactionType.BALANCE) },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                isHighlight = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 支出卡片
                StatSelectionCard(
                    title = stringResource(R.string.expense_label),
                    amount = totalExpense,
                    currency = defaultCurrency, // [传入货币]
                    isSelected = transactionType == TransactionType.EXPENSE,
                    selectedBgColor = Color(0xFFFFEBEE),
                    unselectedBgColor = MaterialTheme.colorScheme.surface,
                    textColor = Color(0xFFE53935),
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f).height(80.dp),
                    isHighlight = false
                )

                // 收入卡片
                StatSelectionCard(
                    title = stringResource(R.string.income_label),
                    amount = totalIncome,
                    currency = defaultCurrency, // [传入货币]
                    isSelected = transactionType == TransactionType.INCOME,
                    selectedBgColor = Color(0xFFE8F5E9),
                    unselectedBgColor = MaterialTheme.colorScheme.surface,
                    textColor = Color(0xFF4CAF50),
                    onClick = { onTypeChange(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f).height(80.dp),
                    isHighlight = false
                )
            }
        }
    }
}

// [修改 BalanceReportSection] 新增 accountMap 参数
@Composable
fun BalanceReportSection(data: List<Expense>, chartMode: ChartMode, defaultCurrency: String, accountMap: Map<Long, Account>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = when(chartMode) {
                    ChartMode.WEEK -> stringResource(R.string.chart_report_week)
                    ChartMode.MONTH -> stringResource(R.string.chart_report_month)
                    ChartMode.YEAR -> stringResource(R.string.chart_report_year)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(stringResource(R.string.chart_report_time_label), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.income_label), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.expense_label), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.chart_report_balance_label), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // BUG FIX: 注意：这里 generateBalanceReportItems 必须在 ChartUtils.kt 中修复
            // 并且需要在调用时传入 AccountMap 和 defaultCurrency (这需要在 ChartDashboard 的父级 ChartScreen 中完成)
            val reportItems = remember(data, chartMode, accountMap, defaultCurrency) {
                generateBalanceReportItems(data, chartMode, accountMap, defaultCurrency)
            }

            reportItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.timeLabel, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    // [i18n] 金额格式化
                    Text(stringResource(R.string.amount_no_decimal_format_chart, item.income), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7BC67E))
                    Text(stringResource(R.string.amount_no_decimal_format_chart, item.expense), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFA7070))
                    Text(
                        stringResource(R.string.amount_no_decimal_format_chart, item.balance),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}