package com.example.myapplication.ui.screen

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class ChartMode { WEEK, MONTH, YEAR }
enum class TransactionType { INCOME, EXPENSE, BALANCE }

@Composable
fun ChartScreen(viewModel: ExpenseViewModel) {
    val allTransactions by viewModel.allExpenses.collectAsState(initial = emptyList())

    var transactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var chartMode by remember { mutableStateOf(ChartMode.WEEK) }

    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDateRangeDialog by remember { mutableStateOf(false) }

    val isCustomRange by remember(startDateMillis, endDateMillis) {
        mutableStateOf(startDateMillis != null && endDateMillis != null)
    }

    val filteredTransactions = remember(
        allTransactions,
        transactionType,
        startDateMillis,
        endDateMillis
    ) {
        allTransactions.filter { expense ->
            val matchType = when (transactionType) {
                TransactionType.EXPENSE -> expense.amount < 0
                TransactionType.INCOME -> expense.amount > 0
                TransactionType.BALANCE -> true
            }
            val time = expense.date.time
            val inRange =
                (startDateMillis == null || time >= startDateMillis!!) &&
                        (endDateMillis == null || time <= endDateMillis!!)
            matchType && inRange
        }
    }

    val groupedData = remember(filteredTransactions, chartMode) {
        filteredTransactions.groupBy { expense ->
            val calendar = Calendar.getInstance().apply { time = expense.date }
            when (chartMode) {
                ChartMode.WEEK -> {
                    val year = calendar.get(Calendar.YEAR)
                    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                    "%04d-W%02d".format(year, weekOfYear)
                }

                ChartMode.MONTH -> {
                    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.CHINA)
                    monthFormat.format(calendar.time)
                }

                ChartMode.YEAR -> {
                    val yearFormat = SimpleDateFormat("yyyy", Locale.CHINA)
                    yearFormat.format(calendar.time)
                }
            }
        }
    }

    val sortedGroupKeys = remember(groupedData) {
        groupedData.keys.sorted()
    }

    var currentIndex by remember(chartMode, sortedGroupKeys, isCustomRange) {
        val initialIndex = if (isCustomRange || sortedGroupKeys.isEmpty()) {
            0
        } else {
            val currentKey = getCurrentGroupKey(chartMode)
            val idx = sortedGroupKeys.indexOf(currentKey)
            (if (idx >= 0) idx else sortedGroupKeys.lastIndex).coerceAtLeast(0)
        }
        mutableStateOf(initialIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        FilterBar(
            transactionType = transactionType,
            chartMode = chartMode,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            isCustomRange = isCustomRange,
            currentIndex = currentIndex,
            sortedGroupKeys = sortedGroupKeys,
            onBackClick = {
                startDateMillis = null
                endDateMillis = null
            },
            onTypeToggle = {
                transactionType = when (transactionType) {
                    TransactionType.EXPENSE -> TransactionType.INCOME
                    TransactionType.INCOME -> TransactionType.BALANCE
                    TransactionType.BALANCE -> TransactionType.EXPENSE
                }
            },
            onChartModeChange = { chartMode = it },
            onCalendarClick = { showDateRangeDialog = true },
            onTabClick = { index ->
                currentIndex = index
            }
        )

        if (isCustomRange) {
            if (filteredTransactions.isNotEmpty()) {
                ChartPage(filteredTransactions, ChartMode.MONTH, transactionType)
            } else {
                EmptyState()
            }
        } else {
            if (sortedGroupKeys.isNotEmpty()) {
                val key = sortedGroupKeys[currentIndex.coerceIn(sortedGroupKeys.indices)]
                val pageData = groupedData[key] ?: emptyList()
                ChartPage(pageData, chartMode, transactionType)
            } else {
                EmptyState()
            }
        }

        if (showDateRangeDialog) {
            CustomDateRangePicker(
                initialStartDate = startDateMillis,
                initialEndDate = endDateMillis,
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

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("没有记录", color = Color.Gray)
    }
}

@Composable
fun FilterBar(
    transactionType: TransactionType,
    chartMode: ChartMode,
    startDateMillis: Long?,
    endDateMillis: Long?,
    isCustomRange: Boolean,
    currentIndex: Int,
    sortedGroupKeys: List<String>,
    onBackClick: () -> Unit,
    onTypeToggle: () -> Unit,
    onChartModeChange: (ChartMode) -> Unit,
    onCalendarClick: () -> Unit,
    onTabClick: (Int) -> Unit
) {
    val yellow = MaterialTheme.colorScheme.primaryContainer
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINA) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(yellow)
            .statusBarsPadding()
            .padding(bottom = 4.dp)
    ) {
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
                    text = when (transactionType) {
                        TransactionType.EXPENSE -> "支出"
                        TransactionType.INCOME -> "收入"
                        TransactionType.BALANCE -> "结余"
                    },
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

            Spacer(modifier = Modifier.height(6.dp))
        }

        if (!isCustomRange && sortedGroupKeys.isNotEmpty()) {
            key(chartMode) {
                ScrollableTabRow(
                    selectedTabIndex = currentIndex.coerceIn(sortedGroupKeys.indices),
                    containerColor = Color.White,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (tabPositions.isEmpty()) return@ScrollableTabRow
                        val idx = currentIndex.coerceIn(tabPositions.indices)
                        val pos = tabPositions[idx]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.BottomStart)
                                .padding(start = pos.left)
                                .width(pos.width)
                                .height(3.dp)
                                .background(yellow)
                        )
                    },
                    divider = {}
                ) {
                    sortedGroupKeys.forEachIndexed { index, key ->
                        Tab(
                            selected = currentIndex == index,
                            onClick = { onTabClick(index) },
                            text = {
                                when (chartMode) {
                                    ChartMode.WEEK -> {
                                        val (yearStr, weekStr) = key.split("-W")
                                        val yearInt = yearStr.toInt()
                                        val weekInt = weekStr.toInt()
                                        val (start, end) = getWeekDateRange(yearInt, weekInt)
                                        val title =
                                            if (yearInt == currentYear) "${weekInt}周"
                                            else "${yearStr}年 - ${weekInt}周"
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
}

// ---------------------- 折线图相关逻辑 ----------------------

data class LineChartPoint(val label: String, val value: Float)

@Composable
fun LineChart(
    dataPoints: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFFFC107) // 黄色调
) {
    if (dataPoints.isEmpty()) return

    val actualMax = remember(dataPoints) { dataPoints.maxOfOrNull { it.value } ?: 0f }
    val actualMin = remember(dataPoints) { dataPoints.minOfOrNull { it.value } ?: 0f }

    val density = LocalDensity.current.density

    // Y轴文字画笔 (右对齐)
    val yLabelPaint = remember {
        Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10f * density
            textAlign = Paint.Align.RIGHT
        }
    }

    // X轴文字画笔 (居中对齐)
    val xLabelPaint = remember {
        Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10f * density
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val bottomPadding = 20.dp.toPx()
        // 预留左侧 Y 轴标签的宽度
        val leftPadding = 45.dp.toPx()

        val chartWidth = width - leftPadding
        val chartHeight = height - bottomPadding

        // 动态计算Y轴范围
        val rangeTop = if (actualMax > 0) actualMax * 1.2f else 0f
        val rangeBottom = if (actualMin < 0) actualMin * 1.2f else 0f

        val finalRangeTop = if (rangeTop == 0f && rangeBottom == 0f) 100f else rangeTop
        val range = finalRangeTop - rangeBottom
        val drawingRange = if (range == 0f) 100f else range

        // 1. 绘制网格线和 Y 轴标签
        val gridLines = 5
        for (i in 0 until gridLines) {
            val ratio = i.toFloat() / (gridLines - 1)
            val value = rangeBottom + (range * ratio)
            // Y 坐标计算 (反转，0 在底部)
            val y = chartHeight - (ratio * chartHeight)

            // 绘制水平虚线/实线
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(leftPadding, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )

            // 绘制 Y 轴文字
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.US, "%.0f", value),
                leftPadding - 8.dp.toPx(), // 文字在 leftPadding 左侧
                y + yLabelPaint.textSize / 3, // 垂直居中
                yLabelPaint
            )
        }

        // 2. 绘制 0 刻度线 (强调线)
        if (rangeBottom <= 0 && finalRangeTop >= 0) {
            val zeroY = chartHeight - ((0f - rangeBottom) / drawingRange * chartHeight)
            drawLine(
                color = Color.Gray.copy(alpha = 0.8f),
                start = Offset(leftPadding, zeroY),
                end = Offset(width, zeroY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 3. 绘制折线
        val spacing = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, point ->
            val x = leftPadding + index * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
                // 填充起点：X=当前点, Y=底部
                fillPath.moveTo(x, chartHeight)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // 绘制 X 轴标签
            val shouldDrawLabel = when {
                dataPoints.size <= 7 -> true
                dataPoints.size <= 12 -> true
                else -> index % 5 == 0 || index == dataPoints.lastIndex
            }

            if (shouldDrawLabel) {
                drawContext.canvas.nativeCanvas.drawText(
                    point.label,
                    x,
                    height - 5.dp.toPx(),
                    xLabelPaint
                )
            }

            // 绘制数据点
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(x, y),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 闭合填充路径
        fillPath.lineTo(leftPadding + (dataPoints.size - 1) * spacing, chartHeight)
        fillPath.close()

        // 绘制填充渐变
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f)),
                startY = 0f,
                endY = chartHeight
            )
        )

        // 绘制折线
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}

// 准备折线图数据
fun prepareLineChartData(
    expenses: List<Expense>,
    chartMode: ChartMode,
    transactionType: TransactionType
): List<LineChartPoint> {
    if (expenses.isEmpty()) return emptyList()

    val sampleDate = expenses.first().date
    val calendar = Calendar.getInstance()
    calendar.time = sampleDate

    val points = mutableListOf<LineChartPoint>()

    val sumFunc: (List<Expense>) -> Float = { list ->
        if (transactionType == TransactionType.BALANCE) {
            // 结余：原始金额求和 (收入为正，支出为负)
            list.sumOf { it.amount }.toFloat()
        } else {
            // 收入或支出：绝对值求和
            list.sumOf { abs(it.amount) }.toFloat()
        }
    }

    when (chartMode) {
        ChartMode.WEEK -> {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            for (i in 0 until 7) {
                val dayStart = calendar.time
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time

                val sum = sumFunc(expenses.filter { it.date >= dayStart && it.date < dayEnd })

                val label = SimpleDateFormat("dd", Locale.CHINA).format(dayStart)
                points.add(LineChartPoint(label, sum))

                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        ChartMode.MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (i in 1..maxDays) {
                val dayStart = calendar.time
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time

                val sum = sumFunc(expenses.filter { it.date >= dayStart && it.date < dayEnd })

                points.add(LineChartPoint(i.toString(), sum))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        ChartMode.YEAR -> {
            calendar.set(Calendar.DAY_OF_YEAR, 1)

            for (i in 0 until 12) {
                val monthStart = calendar.time
                val nextMonthCal = Calendar.getInstance().apply { time = monthStart; add(Calendar.MONTH, 1) }
                val monthEnd = nextMonthCal.time

                val sum = sumFunc(expenses.filter { it.date >= monthStart && it.date < monthEnd })

                points.add(LineChartPoint("${i + 1}月", sum))
                calendar.add(Calendar.MONTH, 1)
            }
        }
    }
    return points
}

// -----------------------------------------------------------

@Composable
fun ChartPage(data: List<Expense>, chartMode: ChartMode, transactionType: TransactionType) {
    val categorySums = remember(data) {
        data.groupBy { it.category }.mapValues { (_, expenses) ->
            expenses.sumOf { abs(it.amount.toDouble()).toLong() }
        }
    }

    val sortedEntries = remember(categorySums) {
        categorySums.entries.sortedByDescending { it.value }
    }

    val total = remember(data, transactionType) {
        if (transactionType == TransactionType.BALANCE) {
            data.sumOf { it.amount }.toFloat()
        } else {
            sortedEntries.sumOf { it.value }.toFloat()
        }
    }

    val maxAmount = sortedEntries.maxOfOrNull { it.value }?.toFloat() ?: 0f

    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    val colors = getChartColors()
    val barColor = Color(0xFFF4B400)
    val barBgColor = Color(0xFFFFF7CC)
    val iconBoxSize = 36.dp

    val lineData = remember(data, chartMode, transactionType) {
        prepareLineChartData(data, chartMode, transactionType)
    }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. 顶部合计
        item {
            val totalLabel = when (transactionType) {
                TransactionType.BALANCE -> "总结余"
                TransactionType.EXPENSE -> "总支出"
                TransactionType.INCOME -> "总收入"
            }
            Text(text = "$totalLabel: ${String.format("%.2f", total)}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. 整体趋势标题 + 折线图
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(16.dp)
                        .background(Color(0xFFF4B400), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (transactionType == TransactionType.BALANCE) "结余趋势" else "整体趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                LineChart(
                    dataPoints = lineData,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 3. 分类/报表区域
        if (transactionType == TransactionType.BALANCE) {
            // (新) 结余模式：显示报表
            item {
                BalanceReportSection(data, chartMode)
            }
        } else {
            // (旧) 收支模式：显示饼图和列表
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .background(Color(0xFFF4B400), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "分类统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                val pieTotal = sortedEntries.sumOf { it.value }.toFloat()
                if (pieTotal > 0f) {
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
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("没有数据", color = Color.LightGray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            itemsIndexed(sortedEntries) { index, entry ->
                val amount = entry.value.toFloat()
                val pieTotal = sortedEntries.sumOf { it.value }.toFloat()
                val percentage = if (pieTotal > 0) amount / pieTotal * 100f else 0f
                val color = colors[index % colors.size]
                val barRatio = if (maxAmount > 0) amount / maxAmount else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
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

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

// ---------------- 新增：结余报表组件 ----------------

data class BalanceReportItem(
    val timeLabel: String,
    val income: Double,
    val expense: Double,
    val balance: Double
)

@Composable
fun BalanceReportSection(data: List<Expense>, chartMode: ChartMode) {
    // 1. 标题
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(Color(0xFFF4B400), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when(chartMode) {
                ChartMode.WEEK -> "周报表"
                ChartMode.MONTH -> "月报表"
                ChartMode.YEAR -> "年报表"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(modifier = Modifier.height(16.dp))

    // 2. 表头
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("时间", modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center)
        Text("收入", modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center)
        Text("支出", modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center)
        Text("结余", modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center)
    }

    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

    // 3. 数据处理与列表生成
    val reportItems = remember(data, chartMode) {
        generateBalanceReportItems(data, chartMode)
    }

    reportItems.forEach { item ->
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.timeLabel, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.Gray)
                Text(String.format("%.0f", item.income), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(String.format("%.0f", item.expense), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(
                    String.format("%.0f", item.balance),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = if (item.balance < 0) Color.Gray else Color.Unspecified
                )
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
        }
    }
}

fun generateBalanceReportItems(data: List<Expense>, chartMode: ChartMode): List<BalanceReportItem> {
    val calendar = Calendar.getInstance()

    // 按日期(天或月)对数据进行分组
    val groupedByDate = data.groupBy { expense ->
        calendar.time = expense.date
        // 将时间归一化，方便分组
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (chartMode == ChartMode.YEAR) {
            calendar.set(Calendar.DAY_OF_MONTH, 1) // 年模式按月分组，归一到1号
        } else {
            // 周/月模式按天分组，保持DAY_OF_MONTH不变
        }
        calendar.timeInMillis
    }

    // 将分组后的数据转换为列表项，并按时间倒序排列
    return groupedByDate.entries
        .sortedByDescending { it.key } // 按时间戳倒序
        .map { (timeMillis, expenses) ->
            calendar.timeInMillis = timeMillis
            val timeLabel = when (chartMode) {
                ChartMode.WEEK, ChartMode.MONTH -> calendar.get(Calendar.DAY_OF_MONTH).toString() + "日"
                ChartMode.YEAR -> (calendar.get(Calendar.MONTH) + 1).toString() + "月"
            }

            val income = expenses.filter { it.amount > 0 }.sumOf { it.amount }
            val expense = expenses.filter { it.amount < 0 }.sumOf { abs(it.amount) } // 支出显示为正数
            val balance = income - expense

            BalanceReportItem(timeLabel, income, expense, balance)
        }
}

// -----------------------------------------------------------

private data class ChartData(val name: String, val value: Long, val color: Color)

@Composable
fun PieChart(data: Map<String, Long>) {
    if (data.isEmpty()) return

    val chartData = remember(data) {
        val sorted = data.entries.sortedByDescending { it.value }
        val allColors = getChartColors()

        if (sorted.size <= 8) {
            sorted.mapIndexed { index, entry ->
                ChartData(entry.key, entry.value, allColors[index % allColors.size])
            }
        } else {
            val top7 = sorted.take(7).mapIndexed { index, entry ->
                ChartData(entry.key, entry.value, allColors[index % allColors.size])
            }
            val otherSum = sorted.drop(7).sumOf { it.value }
            top7 + ChartData("其他", otherSum, Color(0xFF9E9E9E))
        }
    }

    val total = chartData.sumOf { it.value }.toFloat()
    if (total <= 0f) return

    val density = LocalDensity.current.density
    val textPaint = remember {
        Paint().apply {
            textSize = 12f * density
            typeface = Typeface.DEFAULT
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.5f
        val strokeWidth = radius * 0.6f

        var startAngle = -90f

        chartData.forEach { slice ->
            val sweepAngle = 360f * (slice.value / total)

            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            if (sweepAngle > 5f) {
                val midAngle = startAngle + sweepAngle / 2
                val midRad = Math.toRadians(midAngle.toDouble())

                val outerDonutRadius = radius + strokeWidth / 2
                val lineStart = Offset(
                    (center.x + outerDonutRadius * cos(midRad)).toFloat(),
                    (center.y + outerDonutRadius * sin(midRad)).toFloat()
                )

                val lineOffset = 15.dp.toPx()
                val elbow = Offset(
                    (center.x + (outerDonutRadius + lineOffset) * cos(midRad)).toFloat(),
                    (center.y + (outerDonutRadius + lineOffset) * sin(midRad)).toFloat()
                )

                val isRightSide = cos(midRad) > 0
                val endX = if (isRightSide) elbow.x + 20.dp.toPx() else elbow.x - 20.dp.toPx()
                val lineEnd = Offset(endX, elbow.y)

                val path = Path().apply {
                    moveTo(lineStart.x, lineStart.y)
                    lineTo(elbow.x, elbow.y)
                    lineTo(lineEnd.x, lineEnd.y)
                }

                drawPath(
                    path = path,
                    color = slice.color,
                    style = Stroke(width = 1.dp.toPx())
                )

                val percent = (slice.value / total * 100).toInt()
                val label = "${slice.name} $percent%"

                textPaint.color = slice.color.toArgb()
                textPaint.textAlign = if (isRightSide) Paint.Align.LEFT else Paint.Align.RIGHT

                val textX = if (isRightSide) lineEnd.x + 4.dp.toPx() else lineEnd.x - 4.dp.toPx()
                val textY = lineEnd.y + textPaint.textSize / 3

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(label, textX, textY, textPaint)
                }
            }

            startAngle += sweepAngle
        }
    }
}

private fun getCurrentGroupKey(mode: ChartMode): String {
    val calendar = Calendar.getInstance()
    return when (mode) {
        ChartMode.WEEK -> {
            val year = calendar.get(Calendar.YEAR)
            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            "%04d-W%02d".format(year, week)
        }

        ChartMode.MONTH -> {
            val fmt = SimpleDateFormat("yyyy-MM", Locale.CHINA)
            fmt.format(calendar.time)
        }

        ChartMode.YEAR -> {
            val fmt = SimpleDateFormat("yyyy", Locale.CHINA)
            fmt.format(calendar.time)
        }
    }
}

private fun getWeekDateRange(year: Int, week: Int): Pair<String, String> {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.WEEK_OF_YEAR, week)
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    val format = SimpleDateFormat("MM月dd日", Locale.CHINA)
    val start = format.format(calendar.time)
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    val end = format.format(calendar.time)
    return start to end
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