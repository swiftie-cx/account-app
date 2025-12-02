package com.example.myapplication.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class ChartMode { WEEK, MONTH, YEAR }
enum class TransactionType { INCOME, EXPENSE }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChartScreen(viewModel: ExpenseViewModel) {
    val allTransactions by viewModel.allExpenses.collectAsState(initial = emptyList())

    var transactionType by remember { mutableStateOf(TransactionType.EXPENSE) } // 默认支出
    var chartMode by remember { mutableStateOf(ChartMode.WEEK) }               // 默认周

    // 过滤收入 / 支出
    val filteredTransactions = remember(allTransactions, transactionType) {
        allTransactions.filter { expense ->
            when (transactionType) {
                TransactionType.EXPENSE -> expense.amount < 0
                TransactionType.INCOME -> expense.amount > 0
            }
        }
    }

    // 按周 / 月 / 年分组
    val groupedData = remember(filteredTransactions, chartMode) {
        filteredTransactions.groupBy { expense ->
            val calendar = Calendar.getInstance().apply { time = expense.date }
            when (chartMode) {
                ChartMode.WEEK -> {
                    val year = calendar.get(Calendar.YEAR)
                    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                    "${year}-W$weekOfYear"
                }

                ChartMode.MONTH -> {
                    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    monthFormat.format(calendar.time)
                }

                ChartMode.YEAR -> {
                    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                    yearFormat.format(calendar.time)
                }
            }
        }
    }

    val sortedGroupKeys = remember(groupedData) {
        groupedData.keys.sorted()
    }

    val pagerState = rememberPagerState(pageCount = { sortedGroupKeys.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        FilterBar(
            transactionType = transactionType,
            chartMode = chartMode,
            onTypeToggle = {
                transactionType = when (transactionType) {
                    TransactionType.EXPENSE -> TransactionType.INCOME
                    TransactionType.INCOME -> TransactionType.EXPENSE
                }
            },
            onChartModeChange = { chartMode = it },
            pagerState = pagerState,
            sortedGroupKeys = sortedGroupKeys,
            onTabClick = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            }
        )

        if (sortedGroupKeys.isNotEmpty()) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState
            ) { page ->
                val key = sortedGroupKeys[page]
                val dataForPage = groupedData[key] ?: emptyList()
                ChartPage(dataForPage)
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("没有记录")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilterBar(
    transactionType: TransactionType,
    chartMode: ChartMode,
    onTypeToggle: () -> Unit,
    onChartModeChange: (ChartMode) -> Unit,
    pagerState: PagerState,
    sortedGroupKeys: List<String>,
    onTabClick: (Int) -> Unit
) {
    val yellow = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(yellow)
            .padding(bottom = 4.dp)
    ) {
        // 第一行：收入 / 支出 标题 + 日历图标
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clickable { onTypeToggle() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (transactionType == TransactionType.EXPENSE) "支出" else "收入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = "日期",
            )
        }

        // 第二行：周 / 月 / 年 三段式切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
        ) {
            ChartMode.values().forEach { mode ->
                val selected = chartMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) Color.Black else yellow
                        )
                        .clickable { onChartModeChange(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (mode) {
                            ChartMode.WEEK -> "周"
                            ChartMode.MONTH -> "月"
                            ChartMode.YEAR -> "年"
                        },
                        color = if (selected) Color.White else Color.Black,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第三行：具体时间 Tab（周 / 月 / 年）
        if (sortedGroupKeys.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    val currentTab = pagerState.currentPage.coerceIn(tabPositions.indices)
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                        color = yellow
                    )
                },
                divider = {}
            ) {
                sortedGroupKeys.forEachIndexed { index, key ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { onTabClick(index) },
                        text = {
                            when (chartMode) {
                                ChartMode.WEEK -> {
                                    val (year, week) = key.split("-W")
                                    val (start, end) = getWeekDateRange(year.toInt(), week.toInt())
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${week}周",
                                            fontWeight = if (pagerState.currentPage == index)
                                                FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "$start - $end",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                ChartMode.MONTH -> {
                                    val (y, m) = key.split("-")
                                    Text(
                                        text = "${y}年${m.toInt()}月",
                                        fontWeight = if (pagerState.currentPage == index)
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                ChartMode.YEAR -> {
                                    Text(
                                        text = "${key}年",
                                        fontWeight = if (pagerState.currentPage == index)
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChartPage(data: List<Expense>) {
    val categorySums = remember(data) {
        data.groupBy { it.category }.mapValues { (_, expenses) ->
            expenses.sumOf { abs(it.amount.toDouble()).toLong() }
        }
    }

    val total = categorySums.values.sum().toFloat()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "合计: $total", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (total > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                PieChart(categorySums)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("没有数据")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            itemsIndexed(categorySums.entries.toList()) { _, entry ->
                val percentage = entry.value / total * 100
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = getChartColors()[entry.key.hashCode()
                                    .mod(getChartColors().size)],
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "${entry.key}: ${entry.value} (${String.format("%.1f", percentage)}%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PieChart(data: Map<String, Long>) {
    val total = data.values.sum().toFloat()
    val sweepAngles = data.values.map { value ->
        360f * (value / total)
    }

    val colors = getChartColors()

    Canvas(modifier = Modifier.size(200.dp)) {
        var startAngle = -90f
        sweepAngles.forEachIndexed { index, sweepAngle ->
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 40f, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

private fun getWeekDateRange(year: Int, week: Int): Pair<String, String> {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.WEEK_OF_YEAR, week)
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    val format = SimpleDateFormat("MM月dd日", Locale.getDefault())
    val start = format.format(calendar.time)
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    val end = format.format(calendar.time)
    return Pair(start, end)
}

private fun getChartColors(): List<Color> {
    return listOf(
        Color(0xFFF4B400),
        Color(0xFFFF7043),
        Color(0xFF29B6F6),
        Color(0xFF66BB6A),
        Color(0xFFAB47BC),
        Color(0xFFFFCA28),
        Color(0xFFEC407A),
        Color(0xFF26A69A)
    )
}
