package com.example.myapplication.ui.screen

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class ChartMode { WEEK, MONTH, YEAR }
enum class TransactionType { INCOME, EXPENSE }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChartScreen(viewModel: ExpenseViewModel) {
    val allTransactions by viewModel.allExpenses.collectAsState(initial = emptyList())

    var transactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var chartMode by remember { mutableStateOf(ChartMode.WEEK) }

    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDateRangeDialog by remember { mutableStateOf(false) }

    // 是否为自定义日期整体统计模式
    val isCustomRange by remember(startDateMillis, endDateMillis) {
        mutableStateOf(startDateMillis != null && endDateMillis != null)
    }

    // 类型 + 日期过滤
    val filteredTransactions = remember(
        allTransactions,
        transactionType,
        startDateMillis,
        endDateMillis
    ) {
        allTransactions.filter { expense ->
            val matchType = when (transactionType) {
                TransactionType.EXPENSE -> expense.amount < 0
                TransactionType.INCOME  -> expense.amount > 0
            }
            val time = expense.date.time
            val inRange =
                (startDateMillis == null || time >= startDateMillis!!) &&
                        (endDateMillis == null || time <= endDateMillis!!)
            matchType && inRange
        }
    }

    // 普通模式下按周 / 月 / 年分组
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

    // 进入 / 切换模式时默认停在当前周 / 月 / 年
    LaunchedEffect(chartMode, sortedGroupKeys, isCustomRange) {
        if (!isCustomRange && sortedGroupKeys.isNotEmpty()) {
            val currentKey = getCurrentGroupKey(chartMode)
            val idx = sortedGroupKeys.indexOf(currentKey)
            val targetIndex = if (idx >= 0) idx else sortedGroupKeys.lastIndex.coerceAtLeast(0)
            if (targetIndex in 0 until pagerState.pageCount) {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        FilterBar(
            transactionType = transactionType,
            chartMode = chartMode,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            isCustomRange = isCustomRange,
            onBackClick = {
                // 退出自定义模式
                startDateMillis = null
                endDateMillis = null
            },
            onTypeToggle = {
                transactionType = when (transactionType) {
                    TransactionType.EXPENSE -> TransactionType.INCOME
                    TransactionType.INCOME  -> TransactionType.EXPENSE
                }
            },
            onChartModeChange = { chartMode = it },
            onCalendarClick = { showDateRangeDialog = true },
            pagerState = pagerState,
            sortedGroupKeys = sortedGroupKeys,
            onTabClick = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            }
        )

        if (isCustomRange) {
            // 自定义日期：整段统计
            if (filteredTransactions.isNotEmpty()) {
                ChartPage(filteredTransactions)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("没有记录")
                }
            }
        } else {
            // 普通模式：按周 / 月 / 年分页
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

        if (showDateRangeDialog) {
            DateRangeDialog(
                startDate = startDateMillis,
                endDate = endDateMillis,
                onDismiss = { showDateRangeDialog = false },
                onConfirm = { start, end ->
                    startDateMillis = start
                    endDateMillis = end
                    showDateRangeDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterBar(
    transactionType: TransactionType,
    chartMode: ChartMode,
    startDateMillis: Long?,
    endDateMillis: Long?,
    isCustomRange: Boolean,
    onBackClick: () -> Unit,
    onTypeToggle: () -> Unit,
    onChartModeChange: (ChartMode) -> Unit,
    onCalendarClick: () -> Unit,
    pagerState: PagerState,
    sortedGroupKeys: List<String>,
    onTabClick: (Int) -> Unit
) {
    val yellow = MaterialTheme.colorScheme.primaryContainer
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(yellow)
            .padding(bottom = 4.dp)
    ) {
        // 第一行：返回 / 标题 / 日历
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isCustomRange) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onBackClick() }
                )
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }

            Row(
                modifier = Modifier.clickable { onTypeToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (transactionType == TransactionType.EXPENSE) "支出" else "收入",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }

            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = "日期",
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onCalendarClick() }
            )
        }

        // 第二行：日期区间显示
        if (startDateMillis != null || endDateMillis != null) {
            val startText = startDateMillis?.let { dateFormat.format(Date(it)) } ?: "开始时间"
            val endText = endDateMillis?.let { dateFormat.format(Date(it)) } ?: "结束时间"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(startText)
                Text("  ~  ")
                Text(endText)
            }
        }

        // 第三行：周 / 月 / 年（自定义模式隐藏）
        if (!isCustomRange) {
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
                            .background(if (selected) Color.Black else yellow)
                            .clickable { onChartModeChange(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                ChartMode.WEEK  -> "周"
                                ChartMode.MONTH -> "月"
                                ChartMode.YEAR  -> "年"
                            },
                            color = if (selected) Color.White else Color.Black,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // 第四行：TabRow（自定义模式隐藏）
        if (!isCustomRange && sortedGroupKeys.isNotEmpty()) {
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
                                    val (yearStr, weekStr) = key.split("-W")
                                    val yearInt = yearStr.toInt()
                                    val (start, end) = getWeekDateRange(yearInt, weekStr.toInt())
                                    val title =
                                        if (yearInt == currentYear) "${weekStr}周"
                                        else "${yearStr}年 - ${weekStr}周"
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(title)
                                        Text(
                                            "$start - $end",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                ChartMode.MONTH -> {
                                    val (y, m) = key.split("-")
                                    Text("${y}年${m}月")
                                }
                                ChartMode.YEAR -> {
                                    Text("${key}年")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// 日期范围弹窗
@Composable
fun DateRangeDialog(
    startDate: Long?,
    endDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
    val today = remember { System.currentTimeMillis() }

    var tempStart by remember { mutableStateOf(startDate ?: today) }
    var tempEnd   by remember { mutableStateOf(endDate ?: today) }

    fun openPicker(isStart: Boolean) {
        val millis = if (isStart) tempStart else tempEnd
        val cal = Calendar.getInstance().apply { timeInMillis = millis }

        DatePickerDialog(
            context,
            { _, y, m, d ->
                val c = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (isStart) tempStart = c.timeInMillis else tempEnd = c.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openPicker(true) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("开始时间")
                    Text(dateFormat.format(Date(tempStart)))
                }

                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openPicker(false) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("结束时间")
                    Text(dateFormat.format(Date(tempEnd)))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempStart, tempEnd) }) { Text("确定") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 图表 + 条形列表
@Composable
fun ChartPage(data: List<Expense>) {
    val categorySums = remember(data) {
        data.groupBy { it.category }.mapValues { (_, expenses) ->
            expenses.sumOf { abs(it.amount.toDouble()).toLong() }
        }
    }

    val sortedEntries = remember(categorySums) {
        categorySums.entries.sortedByDescending { it.value }
    }

    val total     = sortedEntries.sumOf { it.value }.toFloat()
    val maxAmount = sortedEntries.maxOfOrNull { it.value }?.toFloat() ?: 0f

    // 直接复用“添加记录 / 类别设置”里的分类列表
    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    val colors     = getChartColors()
    val barColor   = Color(0xFFF4B400)
    val barBgColor = Color(0xFFFFF7CC)

    val iconBoxSize = 36.dp   // 图标背景圆的尺寸

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "合计: $total", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (total > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
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
            itemsIndexed(sortedEntries) { index, entry ->
                val amount     = entry.value.toFloat()
                val percentage = if (total > 0) amount / total * 100f else 0f
                val color      = colors[index % colors.size]
                val barRatio   = if (maxAmount > 0) amount / maxAmount else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    // 第一行：图标 + 名称 + 百分比 + 金额
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = categoryIconMap[entry.key]
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(iconBoxSize)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = entry.key,
                                    tint = color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(iconBoxSize)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lens,
                                    contentDescription = entry.key,
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = entry.key,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = String.format("%.2f%%", percentage),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = entry.value.toString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 第二行：条形图，从图标右边开始
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 左侧空出图标宽度，使条形从图标右侧开始
                        Spacer(modifier = Modifier.width(iconBoxSize + 8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(barBgColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barRatio.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(barColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 环形图（加粗）
@Composable
fun PieChart(data: Map<String, Long>) {
    if (data.isEmpty()) return

    val total = data.values.sum().toFloat()
    if (total <= 0f) return

    val sweepAngles = data.values.map { value ->
        360f * (value / total)
    }
    val colors = getChartColors()

    Canvas(modifier = Modifier.size(220.dp)) {
        val strokeWidth = size.minDimension * 0.2f
        var startAngle = -90f

        sweepAngles.forEachIndexed { index, sweepAngle ->
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

// 当前周 / 月 / 年的 key
private fun getCurrentGroupKey(mode: ChartMode): String {
    val calendar = Calendar.getInstance()
    return when (mode) {
        ChartMode.WEEK -> {
            val year = calendar.get(Calendar.YEAR)
            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            "${year}-W$week"
        }
        ChartMode.MONTH -> {
            val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            fmt.format(calendar.time)
        }
        ChartMode.YEAR -> {
            val fmt = SimpleDateFormat("yyyy", Locale.getDefault())
            fmt.format(calendar.time)
        }
    }
}

// 某年某周的起止日期
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
    return start to end
}

// 颜色表
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
