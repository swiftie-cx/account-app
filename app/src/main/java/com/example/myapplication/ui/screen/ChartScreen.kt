package com.example.myapplication.ui.screen

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.Routes
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
fun ChartScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background // 统一背景色
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // 顶部筛选栏 (修改：去掉了奇怪的圆角和阴影)
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

            // 内容区域
            if (isCustomRange) {
                if (filteredTransactions.isNotEmpty()) {
                    ChartPage(filteredTransactions, ChartMode.MONTH, transactionType, navController, "", isCustomRange = true, customStart = startDateMillis, customEnd = endDateMillis)
                } else {
                    EmptyState()
                }
            } else {
                if (sortedGroupKeys.isNotEmpty()) {
                    val key = sortedGroupKeys[currentIndex.coerceIn(sortedGroupKeys.indices)]
                    val pageData = groupedData[key] ?: emptyList()
                    ChartPage(pageData, chartMode, transactionType, navController, key)
                } else {
                    EmptyState()
                }
            }
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

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("本周期没有记录", color = MaterialTheme.colorScheme.outline)
        }
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
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINA) }

    // 【修改】去掉了 Surface 的圆角和阴影，改为平铺的背景，视觉更干净
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // 纯白/深色背景
            .padding(bottom = 8.dp)
    ) {
        // 1. 第一行：返回、类型切换、日历
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isCustomRange) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp)) // 占位保持居中
            }

            // 类型切换 (支出/收入/结余)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onTypeToggle() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (transactionType) {
                        TransactionType.EXPENSE -> "支出"
                        TransactionType.INCOME -> "收入"
                        TransactionType.BALANCE -> "结余"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }

            IconButton(onClick = onCalendarClick) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = "日期",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 自定义时间段显示
        if (startDateMillis != null || endDateMillis != null) {
            val startText = startDateMillis?.let { dateFormat.format(Date(it)) } ?: "开始时间"
            val endText = endDateMillis?.let { dateFormat.format(Date(it)) } ?: "结束时间"

            Text(
                text = "$startText ~ $endText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
            )
        }

        // 2. 第二行：周/月/年 切换 (Segmented Control 风格)
        if (!isCustomRange) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh), // 浅灰槽
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartMode.values().forEach { mode ->
                    val selected = chartMode == mode
                    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(bgColor)
                            .clickable { onChartModeChange(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                ChartMode.WEEK -> "周"
                                ChartMode.MONTH -> "月"
                                ChartMode.YEAR -> "年"
                            },
                            color = textColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. 第三行：时间轴滚动条
        if (!isCustomRange && sortedGroupKeys.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = currentIndex.coerceIn(sortedGroupKeys.indices),
                containerColor = Color.Transparent,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (tabPositions.isEmpty()) return@ScrollableTabRow
                    val idx = currentIndex.coerceIn(tabPositions.indices)
                    val pos = tabPositions[idx]
                    // 自定义指示器：底部小圆点
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.BottomCenter)
                            .padding(bottom = 2.dp)
                            .offset(x = (pos.left + pos.right) / 2 - pos.width / 2)
                            .width(pos.width / 4)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                },
                divider = {}
            ) {
                sortedGroupKeys.forEachIndexed { index, key ->
                    val selected = currentIndex == index
                    Tab(
                        selected = selected,
                        onClick = { onTabClick(index) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            val style = if(selected) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium
                            when (chartMode) {
                                ChartMode.WEEK -> {
                                    val (yearStr, weekStr) = key.split("-W")
                                    val yearInt = yearStr.toInt()
                                    val weekInt = weekStr.toInt()
                                    val title = if (yearInt == currentYear) "${weekInt}周" else "${yearStr}-${weekInt}周"
                                    Text(title, style = style)
                                }
                                ChartMode.MONTH -> {
                                    val (y, m) = key.split("-")
                                    Text(if (y.toInt() == currentYear) "${m}月" else "${y}年${m}月", style = style)
                                }
                                ChartMode.YEAR -> {
                                    Text("${key}年", style = style)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ---------------------- 折线图相关逻辑 ----------------------

data class LineChartPoint(val label: String, val value: Float, val timeMillis: Long)

@Composable
fun LineChart(
    dataPoints: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    onPointClick: (LineChartPoint) -> Unit
) {
    if (dataPoints.isEmpty()) return

    val actualMax = remember(dataPoints) { dataPoints.maxOfOrNull { it.value } ?: 0f }
    val actualMin = remember(dataPoints) { dataPoints.minOfOrNull { it.value } ?: 0f }

    val density = LocalDensity.current.density

    // 文字画笔 (坐标轴)
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.LTGRAY // 浅灰色坐标
            textSize = 10f * density
            textAlign = Paint.Align.CENTER
        }
    }

    // 文字画笔 (气泡内部文字 - 白色)
    val tooltipTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12f * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    // 日期格式化器
    val tooltipDateFormat = remember { SimpleDateFormat("MM-dd", Locale.CHINA) }

    // 渐变填充颜色
    val gradientColors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f))

    var selectedIndex by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    val width = size.width
                    val leftPadding = 32.dp.toPx()
                    val chartWidth = width - leftPadding - 16.dp.toPx()
                    val spacing = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

                    // 1. 检查是否点击了已显示的 Tooltip (跳转)
                    if (selectedIndex != -1 && selectedIndex in dataPoints.indices) {
                        val point = dataPoints[selectedIndex]
                        val chartHeight = size.height - 24.dp.toPx()
                        val rangeTop = if (actualMax > 0) actualMax * 1.2f else 100f
                        val rangeBottom = if (actualMin < 0) actualMin * 1.2f else 0f
                        val drawingRange = (rangeTop - rangeBottom).coerceAtLeast(1f)

                        val x = leftPadding + selectedIndex * spacing
                        val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)

                        // 简易判断：如果在点附近点击，触发跳转
                        val clickRadius = 40.dp.toPx()
                        if (kotlin.math.abs(offset.x - x) < clickRadius && kotlin.math.abs(offset.y - y) < clickRadius) {
                            onPointClick(point)
                            return@detectTapGestures
                        }
                    }

                    // 2. 否则选中新的点
                    val index = ((offset.x - leftPadding) / spacing).MathRound().coerceIn(0, dataPoints.lastIndex)
                    selectedIndex = index
                })
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val width = size.width
                        val leftPadding = 32.dp.toPx()
                        val spacing = (width - leftPadding - 16.dp.toPx()) / (dataPoints.size - 1).coerceAtLeast(1)
                        selectedIndex = ((offset.x - leftPadding) / spacing).MathRound().coerceIn(0, dataPoints.lastIndex)
                    },
                    onDrag = { change, _ ->
                        val width = size.width
                        val leftPadding = 32.dp.toPx()
                        val spacing = (width - leftPadding - 16.dp.toPx()) / (dataPoints.size - 1).coerceAtLeast(1)
                        selectedIndex = ((change.position.x - leftPadding) / spacing).MathRound().coerceIn(0, dataPoints.lastIndex)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val bottomPadding = 24.dp.toPx()
        val leftPadding = 32.dp.toPx()

        val chartWidth = width - leftPadding - 16.dp.toPx()
        val chartHeight = height - bottomPadding

        val rangeTop = if (actualMax > 0) actualMax * 1.2f else 100f
        val rangeBottom = if (actualMin < 0) actualMin * 1.2f else 0f
        val drawingRange = (rangeTop - rangeBottom).coerceAtLeast(1f)

        // 1. 绘制虚线网格
        val gridLines = 4
        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val y = chartHeight - (ratio * chartHeight)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(leftPadding, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // 2. 绘制折线和填充
        val spacing = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, point ->
            val x = leftPadding + index * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, chartHeight)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // X轴 标签
            if (dataPoints.size <= 7 || index % (dataPoints.size / 5) == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    point.label,
                    x,
                    height - 5.dp.toPx(),
                    textPaint
                )
            }
        }

        fillPath.lineTo(leftPadding + (dataPoints.size - 1) * spacing, chartHeight)
        fillPath.close()

        // 绘制渐变填充
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = 0f,
                endY = chartHeight
            )
        )

        // 绘制线条
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )

        // 3. 绘制普通数据点 (小圈)
        dataPoints.forEachIndexed { index, point ->
            val x = leftPadding + index * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)

            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(x, y),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 4. 【恢复】选中高亮和气泡 (Tooltip)
        if (selectedIndex != -1 && selectedIndex in dataPoints.indices) {
            val point = dataPoints[selectedIndex]
            val x = leftPadding + selectedIndex * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)

            // 垂直指示线
            drawLine(
                color = lineColor.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset(x, chartHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // 选中点的大圈
            drawCircle(
                color = lineColor,
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )

            // --- 绘制气泡 ---
            val dateText = tooltipDateFormat.format(Date(point.timeMillis))
            val amountText = String.format("%.0f", point.value)

            // 气泡尺寸计算
            val textWidth = tooltipTextPaint.measureText(dateText).coerceAtLeast(tooltipTextPaint.measureText(amountText)) + 24.dp.toPx()
            val textHeight = 44.dp.toPx()

            // 确保气泡不超出边界
            var tooltipX = x - textWidth / 2
            if (tooltipX < 0) tooltipX = 0f
            if (tooltipX + textWidth > width) tooltipX = width - textWidth

            // 气泡在点上方，如果在顶部则移到下方
            val tooltipY = if (y - textHeight - 12.dp.toPx() < 0) y + 12.dp.toPx() else y - textHeight - 12.dp.toPx()

            // 画气泡背景 (圆角矩形)
            drawRoundRect(
                color = lineColor, // 使用主题色背景
                topLeft = Offset(tooltipX, tooltipY),
                size = Size(textWidth, textHeight),
                cornerRadius = CornerRadius(8.dp.toPx())
            )

            // 画气泡文字 (日期)
            drawContext.canvas.nativeCanvas.drawText(
                dateText,
                tooltipX + textWidth / 2,
                tooltipY + 16.dp.toPx(),
                tooltipTextPaint
            )
            // 画气泡文字 (金额)
            drawContext.canvas.nativeCanvas.drawText(
                amountText,
                tooltipX + textWidth / 2,
                tooltipY + 34.dp.toPx(),
                tooltipTextPaint
            )
        }
    }
}

private fun Float.MathRound(): Int {
    return kotlin.math.round(this).toInt()
}

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
            list.sumOf { it.amount }.toFloat()
        } else {
            list.sumOf { abs(it.amount) }.toFloat()
        }
    }

    when (chartMode) {
        ChartMode.WEEK -> {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)

            for (i in 0 until 7) {
                val dayStart = calendar.time
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time
                val sum = sumFunc(expenses.filter { it.date >= dayStart && it.date < dayEnd })
                val label = SimpleDateFormat("dd", Locale.CHINA).format(dayStart)
                points.add(LineChartPoint(label, sum, dayStart.time))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        ChartMode.MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in 1..maxDays) {
                val dayStart = calendar.time
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time
                val sum = sumFunc(expenses.filter { it.date >= dayStart && it.date < dayEnd })
                points.add(LineChartPoint(i.toString(), sum, dayStart.time))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        ChartMode.YEAR -> {
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            for (i in 0 until 12) {
                val monthStart = calendar.time
                val nextMonthCal = Calendar.getInstance().apply { time = monthStart; add(Calendar.MONTH, 1) }
                val monthEnd = nextMonthCal.time
                val sum = sumFunc(expenses.filter { it.date >= monthStart && it.date < monthEnd })
                points.add(LineChartPoint("${i + 1}月", sum, monthStart.time))
                calendar.add(Calendar.MONTH, 1)
            }
        }
    }
    return points
}

@Composable
fun ChartPage(
    data: List<Expense>,
    chartMode: ChartMode,
    transactionType: TransactionType,
    navController: NavHostController,
    currentGroupKey: String,
    isCustomRange: Boolean = false,
    customStart: Long? = null,
    customEnd: Long? = null
) {
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
    val lineData = remember(data, chartMode, transactionType) {
        prepareLineChartData(data, chartMode, transactionType)
    }

    val dateRange = remember(currentGroupKey, chartMode, isCustomRange, customStart, customEnd) {
        if (isCustomRange) {
            (customStart ?: -1L) to (customEnd ?: -1L)
        } else {
            getDateRangeFromKey(currentGroupKey, chartMode)
        }
    }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 总览大字
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = when (transactionType) {
                        TransactionType.BALANCE -> "总结余"
                        TransactionType.EXPENSE -> "总支出"
                        TransactionType.INCOME -> "总收入"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.2f", total),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 2. 趋势图卡片
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (transactionType == TransactionType.BALANCE) "结余趋势" else "整体趋势",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        LineChart(
                            dataPoints = lineData,
                            modifier = Modifier.fillMaxSize(),
                            lineColor = MaterialTheme.colorScheme.primary, // 使用主题色
                            onPointClick = { point ->
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = point.timeMillis
                                val start = calendar.timeInMillis
                                val end = when(chartMode) {
                                    ChartMode.WEEK, ChartMode.MONTH -> {
                                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                                        calendar.timeInMillis - 1
                                    }
                                    ChartMode.YEAR -> {
                                        calendar.add(Calendar.MONTH, 1)
                                        calendar.timeInMillis - 1
                                    }
                                }
                                val searchType = when(transactionType) {
                                    TransactionType.BALANCE -> 0
                                    TransactionType.EXPENSE -> 1
                                    TransactionType.INCOME -> 2
                                }
                                navController.navigate(Routes.searchRoute(startDate = start, endDate = end, type = searchType))
                            }
                        )
                    }
                }
            }
        }

        if (transactionType == TransactionType.BALANCE) {
            item { BalanceReportSection(data, chartMode) }
        } else {
            // 3. 分类统计卡片
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "分类统计",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 饼图
                        val pieTotal = sortedEntries.sumOf { it.value }.toFloat()
                        if (pieTotal > 0f) {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                PieChart(categorySums)
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("没有数据", color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 分类列表 (重构为更现代的样式)
                        sortedEntries.forEachIndexed { index, entry ->
                            val amount = entry.value.toFloat()
                            val percentage = if (pieTotal > 0) amount / pieTotal * 100f else 0f
                            val color = colors[index % colors.size]
                            val barRatio = if (maxAmount > 0) amount / maxAmount else 0f

                            CategoryRankItem(
                                name = entry.key,
                                amount = entry.value,
                                percentage = percentage,
                                color = color,
                                ratio = barRatio,
                                icon = categoryIconMap[entry.key],
                                onClick = {
                                    val searchType = when(transactionType) {
                                        TransactionType.EXPENSE -> 1
                                        TransactionType.INCOME -> 2
                                        else -> 0
                                    }
                                    navController.navigate(Routes.searchRoute(category = entry.key, startDate = dateRange.first, endDate = dateRange.second, type = searchType))
                                }
                            )
                            if (index < sortedEntries.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 现代化的分类列表项
@Composable
fun CategoryRankItem(
    name: String,
    amount: Long,
    percentage: Float,
    color: Color,
    ratio: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)), // 浅色背景
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = name, tint = color, modifier = Modifier.size(20.dp))
            } else {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row {
                    Text("${String.format("%.1f", percentage)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(amount.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            // 圆角进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
    }
}

// ... [BalanceReportSection, generateBalanceReportItems, PieChart 保持原有逻辑] ...
// 以下代码未变动，为保证文件完整性再次列出

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

            // 表头
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
                    Text(String.format("%.0f", item.income), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50))
                    Text(String.format("%.0f", item.expense), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE53935))
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

fun generateBalanceReportItems(data: List<Expense>, chartMode: ChartMode): List<BalanceReportItem> {
    val calendar = Calendar.getInstance()
    val groupedByDate = data.groupBy { expense ->
        calendar.time = expense.date
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        if (chartMode == ChartMode.YEAR) calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.timeInMillis
    }
    return groupedByDate.entries.sortedByDescending { it.key }.map { (timeMillis, expenses) ->
        calendar.timeInMillis = timeMillis
        val timeLabel = when (chartMode) {
            ChartMode.WEEK, ChartMode.MONTH -> calendar.get(Calendar.DAY_OF_MONTH).toString() + "日"
            ChartMode.YEAR -> (calendar.get(Calendar.MONTH) + 1).toString() + "月"
        }
        val income = expenses.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = expenses.filter { it.amount < 0 }.sumOf { abs(it.amount) }
        val balance = income - expense
        BalanceReportItem(timeLabel, income, expense, balance)
    }
}

@Composable
fun PieChart(data: Map<String, Long>) {
    if (data.isEmpty()) return
    val chartData = remember(data) {
        val sorted = data.entries.sortedByDescending { it.value }
        val allColors = getChartColors()
        if (sorted.size <= 8) {
            sorted.mapIndexed { index, entry -> ChartData(entry.key, entry.value, allColors[index % allColors.size]) }
        } else {
            val top7 = sorted.take(7).mapIndexed { index, entry -> ChartData(entry.key, entry.value, allColors[index % allColors.size]) }
            val otherSum = sorted.drop(7).sumOf { it.value }
            top7 + ChartData("其他", otherSum, Color(0xFF9E9E9E))
        }
    }
    val total = chartData.sumOf { it.value }.toFloat()
    if (total <= 0f) return

    val density = LocalDensity.current.density
    val textPaint = remember { Paint().apply { textSize = 11f * density; typeface = Typeface.DEFAULT } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.6f
        val strokeWidth = radius * 0.5f

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

            if (sweepAngle > 8f) {
                val midAngle = startAngle + sweepAngle / 2
                val midRad = Math.toRadians(midAngle.toDouble())
                val outerRadius = radius + strokeWidth / 2
                val lineStart = Offset((center.x + outerRadius * cos(midRad)).toFloat(), (center.y + outerRadius * sin(midRad)).toFloat())
                val lineOffset = 15.dp.toPx()
                val elbow = Offset((center.x + (outerRadius + lineOffset) * cos(midRad)).toFloat(), (center.y + (outerRadius + lineOffset) * sin(midRad)).toFloat())
                val isRightSide = cos(midRad) > 0
                val endX = if (isRightSide) elbow.x + 20.dp.toPx() else elbow.x - 20.dp.toPx()
                val lineEnd = Offset(endX, elbow.y)

                val path = Path().apply { moveTo(lineStart.x, lineStart.y); lineTo(elbow.x, elbow.y); lineTo(lineEnd.x, lineEnd.y) }
                drawPath(path = path, color = slice.color, style = Stroke(width = 1.dp.toPx()))

                val percent = (slice.value / total * 100).toInt()
                textPaint.color = slice.color.toArgb()
                textPaint.textAlign = if (isRightSide) Paint.Align.LEFT else Paint.Align.RIGHT
                drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText("$percent%", if (isRightSide) endX + 8 else endX - 8, elbow.y + textPaint.textSize / 3, textPaint) }
            }
            startAngle += sweepAngle
        }
    }
}

private fun getCurrentGroupKey(mode: ChartMode): String {
    val calendar = Calendar.getInstance()
    return when (mode) {
        ChartMode.WEEK -> "%04d-W%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.WEEK_OF_YEAR))
        ChartMode.MONTH -> SimpleDateFormat("yyyy-MM", Locale.CHINA).format(calendar.time)
        ChartMode.YEAR -> SimpleDateFormat("yyyy", Locale.CHINA).format(calendar.time)
    }
}

private fun getChartColors(): List<Color> {
    return listOf(
        Color(0xFFF4B400), Color(0xFFFF7043), Color(0xFF29B6F6), Color(0xFF66BB6A),
        Color(0xFFAB47BC), Color(0xFFFFCA28), Color(0xFFEC407A), Color(0xFF26A69A)
    )
}

fun getDateRangeFromKey(key: String, mode: ChartMode): Pair<Long, Long> {
    val calendar = Calendar.getInstance()
    calendar.clear()
    when (mode) {
        ChartMode.WEEK -> {
            val parts = key.split("-W")
            if (parts.size == 2) {
                calendar.set(Calendar.YEAR, parts[0].toInt()); calendar.set(Calendar.WEEK_OF_YEAR, parts[1].toInt()); calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = calendar.timeInMillis; calendar.add(Calendar.DAY_OF_YEAR, 7); val end = calendar.timeInMillis - 1
                return start to end
            }
        }
        ChartMode.MONTH -> {
            val parts = key.split("-")
            if (parts.size == 2) {
                calendar.set(Calendar.YEAR, parts[0].toInt()); calendar.set(Calendar.MONTH, parts[1].toInt() - 1); calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis; calendar.add(Calendar.MONTH, 1); val end = calendar.timeInMillis - 1
                return start to end
            }
        }
        ChartMode.YEAR -> {
            calendar.set(Calendar.YEAR, key.toInt()); calendar.set(Calendar.DAY_OF_YEAR, 1)
            val start = calendar.timeInMillis; calendar.add(Calendar.YEAR, 1); val end = calendar.timeInMillis - 1
            return start to end
        }
    }
    return -1L to -1L
}

data class BalanceReportItem(val timeLabel: String, val income: Double, val expense: Double, val balance: Double)
private data class ChartData(val name: String, val value: Long, val color: Color)