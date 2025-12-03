package com.example.myapplication.ui.screen

import android.content.res.Configuration
// (修复) 补全了原生图形库的引用，解决 Paint, Typeface 报错
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
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
            if (sortedGroupKeys.isNotEmpty()) {
                val key = sortedGroupKeys[currentIndex.coerceIn(sortedGroupKeys.indices)]
                val pageData = groupedData[key] ?: emptyList()
                ChartPage(pageData)
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

// (重要) 使用 Material3 DatePicker + LocalConfiguration 完美解决闪退和中文问题
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeDialog(
    startDate: Long?,
    endDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINA) }
    val today = remember { System.currentTimeMillis() }

    var tempStart by remember { mutableStateOf(startDate ?: today) }
    var tempEnd by remember { mutableStateOf(endDate ?: today) }

    var showInnerPicker by remember { mutableStateOf(false) }
    var isPickingStart by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期范围") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isPickingStart = true
                            showInnerPicker = true
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("开始时间", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        dateFormat.format(Date(tempStart)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isPickingStart = false
                            showInnerPicker = true
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("结束时间", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        dateFormat.format(Date(tempEnd)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
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

    if (showInnerPicker) {
        val initialDate = if (isPickingStart) tempStart else tempEnd
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

        // (关键修复) 使用 CompositionLocalProvider 覆盖配置，强制设为中文
        // 这种方式是 Compose 原生的，非常安全，不会导致 Crash
        val configuration = LocalConfiguration.current
        val chineseConfig = remember(configuration) {
            Configuration(configuration).apply { setLocale(Locale.CHINA) }
        }

        CompositionLocalProvider(LocalConfiguration provides chineseConfig) {
            DatePickerDialog(
                onDismissRequest = { showInnerPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            if (isPickingStart) tempStart = selected else tempEnd = selected
                        }
                        showInnerPicker = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showInnerPicker = false }) { Text("取消") }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    // 自定义标题显示，避免出现英文格式
                    headline = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val headerFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
                            Text(
                                text = headerFormat.format(Date(selectedDate)),
                                modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    },
                    title = {
                        Text(
                            text = if (isPickingStart) "选择开始日期" else "选择结束日期",
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                        )
                    },
                    showModeToggle = false
                )
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