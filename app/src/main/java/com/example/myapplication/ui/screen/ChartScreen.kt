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
enum class TransactionType { INCOME, EXPENSE }

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
                    TransactionType.INCOME -> TransactionType.EXPENSE
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
                ChartPage(filteredTransactions, ChartMode.MONTH) // 传入 ChartMode 只是占位
            } else {
                EmptyState()
            }
        } else {
            if (sortedGroupKeys.isNotEmpty()) {
                val key = sortedGroupKeys[currentIndex.coerceIn(sortedGroupKeys.indices)]
                val pageData = groupedData[key] ?: emptyList()
                // 传入当前的模式，用于生成正确的折线图X轴
                ChartPage(pageData, chartMode)
            } else {
                EmptyState()
            }
        }

        if (showDateRangeDialog) {
            // (修改) 使用我们自定义的纯 Compose 日期范围选择器
            // 彻底替换原来的 DateRangeDialog
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

// (此处已移除旧的 DateRangeDialog 函数)

// ---------------------- 折线图相关逻辑 ----------------------

data class LineChartPoint(val label: String, val value: Float)

@Composable
fun LineChart(
    dataPoints: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFFFC107) // 黄色调
) {
    if (dataPoints.isEmpty()) return

    val maxValue = remember(dataPoints) { dataPoints.maxOfOrNull { it.value } ?: 0f }
    val density = LocalDensity.current.density

    // 文字画笔
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10f * density
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        // 留出底部写字的空间
        val bottomPadding = 20.dp.toPx()
        val chartHeight = height - bottomPadding

        // 简单归一化：为了不让线顶到天花板，稍微给最大值留点余地
        val yMax = if (maxValue == 0f) 100f else maxValue * 1.2f

        val spacing = width / (dataPoints.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, point ->
            val x = index * spacing
            val y = chartHeight - (point.value / yMax * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, chartHeight) // 填充起始点：左下角
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // 绘制X轴标签 (稍微稀疏一点，如果是月视图，不需要每天都画)
            // 简单策略：周视图每天画，月视图每隔5天画，年视图每个月画
            val shouldDrawLabel = when {
                dataPoints.size <= 7 -> true // 周
                dataPoints.size <= 12 -> true // 年
                else -> index % 5 == 0 || index == dataPoints.lastIndex // 月：每5天或最后一天
            }

            if (shouldDrawLabel) {
                drawContext.canvas.nativeCanvas.drawText(
                    point.label,
                    x,
                    height - 5.dp.toPx(), // 底部
                    textPaint
                )
            }

            // 绘制数据点 (圆圈)
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
        fillPath.lineTo((dataPoints.size - 1) * spacing, chartHeight)
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
    chartMode: ChartMode
): List<LineChartPoint> {
    if (expenses.isEmpty()) return emptyList()

    // 取出第一笔数据的时间作为基准，计算该时间段的完整范围
    val sampleDate = expenses.first().date
    val calendar = Calendar.getInstance()
    calendar.time = sampleDate

    val points = mutableListOf<LineChartPoint>()

    when (chartMode) {
        ChartMode.WEEK -> {
            // 找到周一
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            // 遍历7天
            for (i in 0 until 7) {
                val dayStart = calendar.time
                // 往后推一天作为结束
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time

                // 汇总当天的金额
                val sum = expenses.filter { it.date >= dayStart && it.date < dayEnd }
                    .sumOf { abs(it.amount) }
                    .toFloat()

                // 标签：MM-dd (例如 12-03)
                val label = SimpleDateFormat("dd", Locale.CHINA).format(dayStart)
                points.add(LineChartPoint(label, sum))

                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        ChartMode.MONTH -> {
            // 找到1号
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (i in 1..maxDays) {
                val dayStart = calendar.time
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time

                val sum = expenses.filter { it.date >= dayStart && it.date < dayEnd }
                    .sumOf { abs(it.amount) }
                    .toFloat()

                // 标签：只显示日期数字
                points.add(LineChartPoint(i.toString(), sum))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        ChartMode.YEAR -> {
            // 找到1月
            calendar.set(Calendar.DAY_OF_YEAR, 1) // 设置为当年的第一天

            for (i in 0 until 12) {
                val monthStart = calendar.time
                val nextMonthCal = Calendar.getInstance().apply { time = monthStart; add(Calendar.MONTH, 1) }
                val monthEnd = nextMonthCal.time

                val sum = expenses.filter { it.date >= monthStart && it.date < monthEnd }
                    .sumOf { abs(it.amount) }
                    .toFloat()

                points.add(LineChartPoint("${i + 1}月", sum))
                calendar.add(Calendar.MONTH, 1)
            }
        }
    }
    return points
}

// -----------------------------------------------------------

@Composable
fun ChartPage(data: List<Expense>, chartMode: ChartMode) {
    val categorySums = remember(data) {
        data.groupBy { it.category }.mapValues { (_, expenses) ->
            expenses.sumOf { abs(it.amount.toDouble()).toLong() }
        }
    }

    val sortedEntries = remember(categorySums) {
        categorySums.entries.sortedByDescending { it.value }
    }

    val total = sortedEntries.sumOf { it.value }.toFloat()
    val maxAmount = sortedEntries.maxOfOrNull { it.value }?.toFloat() ?: 0f

    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    val colors = getChartColors()
    val barColor = Color(0xFFF4B400)
    val barBgColor = Color(0xFFFFF7CC)
    val iconBoxSize = 36.dp

    // (新) 准备折线图数据
    val lineData = remember(data, chartMode) {
        prepareLineChartData(data, chartMode)
    }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. 顶部合计
        item {
            Text(text = "合计: $total", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. (新) 整体趋势标题 + 折线图
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
                    text = "整体趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 折线图区域
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

        // 3. 分类统计标题 + 饼图
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
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("没有数据", color = Color.LightGray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. 分类列表
        itemsIndexed(sortedEntries) { index, entry ->
            val amount = entry.value.toFloat()
            val percentage = if (total > 0) amount / total * 100f else 0f
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