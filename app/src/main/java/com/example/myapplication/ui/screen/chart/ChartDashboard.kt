package com.example.myapplication.ui.screen.chart

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Expense
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
    onModeChange: (ChartMode) -> Unit,
    onDateChange: (Int) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCustomRangeClick: () -> Unit,
    onBackFromCustom: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA) }
    val monthFormat = remember { SimpleDateFormat("yyyy年MM月", Locale.CHINA) }
    val yearFormat = remember { SimpleDateFormat("yyyy年", Locale.CHINA) }

    val dateTitle = remember(chartMode, currentDate, isCustomRange, rangeStart, rangeEnd) {
        if (isCustomRange) {
            "自定义范围"
        } else {
            when (chartMode) {
                ChartMode.WEEK -> "第 ${currentDate.get(Calendar.WEEK_OF_YEAR)} 周"
                ChartMode.MONTH -> monthFormat.format(currentDate.time)
                ChartMode.YEAR -> yearFormat.format(currentDate.time)
            }
        }
    }

    val rangeText = remember(rangeStart, rangeEnd) {
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
                            ChartMode.WEEK -> "周"
                            ChartMode.MONTH -> "月"
                            ChartMode.YEAR -> "年"
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
                    null,
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                }
            } else {
                IconButton(onClick = { onDateChange(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "前一页", tint = Color.White)
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
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "后一页", tint = Color.White)
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
                title = "本期结余",
                amount = totalBalance,
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
                    title = "支出",
                    amount = totalExpense,
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
                    title = "收入",
                    amount = totalIncome,
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

@Composable
fun BalanceReportSection(data: List<Expense>, chartMode: ChartMode) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = when(chartMode) {
                    ChartMode.WEEK -> "周报表"
                    ChartMode.MONTH -> "月报表"
                    ChartMode.YEAR -> "年报表"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("时间", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Text("收入", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Text("支出", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Text("结余", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            val reportItems = remember(data, chartMode) { generateBalanceReportItems(data, chartMode) }
            reportItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.timeLabel, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    Text(String.format("%.0f", item.income), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7BC67E))
                    Text(String.format("%.0f", item.expense), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFA7070))
                    Text(
                        String.format("%.0f", item.balance),
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