package com.example.myapplication.ui.screen

import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class ChartMode { WEEK, MONTH, YEAR }
enum class TransactionType { INCOME, EXPENSE, BALANCE }

@Composable
fun ChartScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val allTransactions by viewModel.allExpenses.collectAsState(initial = emptyList())

    // 状态管理
    var transactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var chartMode by remember { mutableStateOf(ChartMode.MONTH) }
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }

    // 自定义日期范围状态
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    val isCustomRange = customStartDate != null && customEndDate != null

    // 1. 计算当前的时间范围
    val (rangeStart, rangeEnd) = remember(chartMode, currentDate, customStartDate, customEndDate) {
        if (isCustomRange) {
            (customStartDate ?: 0L) to (customEndDate ?: 0L)
        } else {
            calculateDateRange(currentDate, chartMode)
        }
    }

    // 2. 筛选当前时间范围内的所有交易
    val currentPeriodExpenses = remember(allTransactions, rangeStart, rangeEnd) {
        allTransactions.filter {
            it.date.time in rangeStart..rangeEnd
        }
    }

    // 3. 计算统计数据
    val totalExpense = remember(currentPeriodExpenses) {
        currentPeriodExpenses.filter { it.amount < 0 && !it.category.startsWith("转账") }.sumOf { abs(it.amount) }
    }
    val totalIncome = remember(currentPeriodExpenses) {
        currentPeriodExpenses.filter { it.amount > 0 && !it.category.startsWith("转账") }.sumOf { it.amount }
    }
    val totalBalance = totalIncome - totalExpense

    // 4. 根据选中的类型筛选图表数据
    val chartData = remember(currentPeriodExpenses, transactionType) {
        currentPeriodExpenses.filter { expense ->
            when (transactionType) {
                TransactionType.EXPENSE -> expense.amount < 0 && !expense.category.startsWith("转账")
                TransactionType.INCOME -> expense.amount > 0 && !expense.category.startsWith("转账")
                TransactionType.BALANCE -> !expense.category.startsWith("转账")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // --- 顶部紫色弧形背景 ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()), // 【重要】外层滚动
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 顶部仪表盘区域 ---
                DashboardHeader(
                    chartMode = chartMode,
                    currentDate = currentDate,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    transactionType = transactionType,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    totalBalance = totalBalance,
                    isCustomRange = isCustomRange,
                    onModeChange = {
                        chartMode = it
                        customStartDate = null
                        customEndDate = null
                    },
                    onDateChange = { offset ->
                        val newCal = currentDate.clone() as Calendar
                        when (chartMode) {
                            ChartMode.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, offset)
                            ChartMode.MONTH -> newCal.add(Calendar.MONTH, offset)
                            ChartMode.YEAR -> newCal.add(Calendar.YEAR, offset)
                        }
                        currentDate = newCal
                    },
                    onTypeChange = { transactionType = it },
                    onCustomRangeClick = { showDateRangeDialog = true },
                    onBackFromCustom = {
                        customStartDate = null
                        customEndDate = null
                    }
                )

                // --- 图表内容区域 ---
                if (chartData.isNotEmpty()) {
                    ChartPageContent(
                        data = chartData,
                        chartMode = if (isCustomRange) ChartMode.MONTH else chartMode,
                        transactionType = transactionType,
                        navController = navController,
                        dateRange = rangeStart to rangeEnd
                    )
                } else {
                    EmptyState()
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDateRangeDialog) {
        CustomDateRangePicker(
            initialStartDate = customStartDate,
            initialEndDate = customEndDate,
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { start, end ->
                customStartDate = start
                customEndDate = end
                showDateRangeDialog = false
            }
        )
    }
}

// --- 仪表盘头部 ---
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
        // 1. 顶部模式切换 (胶囊风格)
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
            // 结余卡片 (C位) - 选中时紫色，未选中白色
            val balanceSelected = transactionType == TransactionType.BALANCE
            StatSelectionCard(
                title = "本期结余",
                amount = totalBalance,
                isSelected = balanceSelected,
                // 选中时使用浅主题色背景，而不是描边
                selectedBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                unselectedBgColor = MaterialTheme.colorScheme.surface,
                textColor = MaterialTheme.colorScheme.primary,
                onClick = { onTypeChange(TransactionType.BALANCE) },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                isHighlight = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 支出卡片 - 选中时浅红色背景
                StatSelectionCard(
                    title = "支出",
                    amount = totalExpense,
                    isSelected = transactionType == TransactionType.EXPENSE,
                    // 【修改】选中时使用不透明的浅红色
                    selectedBgColor = Color(0xFFFFEBEE), // 极浅红
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
                    // 【修改】选中时使用不透明的浅绿色
                    selectedBgColor = Color(0xFFE8F5E9), // 极浅绿
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
fun StatSelectionCard(
    title: String,
    amount: Double,
    isSelected: Boolean,
    selectedBgColor: Color,
    unselectedBgColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlight: Boolean
) {
    // 动画切换背景色
    val backgroundColor by animateColorAsState(
        if (isSelected) selectedBgColor else unselectedBgColor,
        label = "bgColor"
    )
    val elevation = if (isSelected) 0.dp else 2.dp // 选中时平面化(因有色块)，未选中时悬浮

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = elevation,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = if(isHighlight) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("%.2f", amount),
                style = if(isHighlight) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

// --- ChartPage 内容逻辑 (使用 Column 修复闪退) ---

@Composable
fun ChartPageContent(
    data: List<Expense>,
    chartMode: ChartMode,
    transactionType: TransactionType,
    navController: NavHostController,
    dateRange: Pair<Long, Long>
) {
    val categorySums = remember(data) {
        data.groupBy { it.category }.mapValues { (_, expenses) ->
            expenses.sumOf { abs(it.amount.toDouble()).toLong() }
        }
    }

    val sortedEntries = remember(categorySums) {
        categorySums.entries.sortedByDescending { it.value }
    }

    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    val colors = getChartColors()
    val lineData = remember(data, chartMode, transactionType) {
        prepareLineChartData(data, chartMode, transactionType)
    }

    // 【关键修复】使用 Column 而不是 LazyColumn
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 趋势图卡片
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
                        lineColor = MaterialTheme.colorScheme.primary, // 统一使用主题色
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

        if (transactionType == TransactionType.BALANCE) {
            BalanceReportSection(data, chartMode)
        } else {
            // 2. 分类统计卡片
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

                    val pieTotal = sortedEntries.sumOf { it.value }.toFloat()
                    if (pieTotal > 0f) {
                        Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                            PieChart(categorySums, title = if(transactionType == TransactionType.EXPENSE) "总支出" else "总收入")
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("没有数据", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    sortedEntries.forEachIndexed { index, entry ->
                        val amount = entry.value.toFloat()
                        val percentage = if (pieTotal > 0) amount / pieTotal * 100f else 0f
                        val color = colors[index % colors.size]
                        val barRatio = if (sortedEntries.isNotEmpty()) amount / sortedEntries.first().value.toFloat() else 0f

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

// --- 辅助组件 ---

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("本周期没有记录", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun CategoryRankItem(
    name: String,
    amount: Long,
    percentage: Float,
    color: Color,
    ratio: Float,
    icon: ImageVector?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
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

// ---------------------- 绘图逻辑 ----------------------

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
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 10f * density
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val tooltipTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12f * density
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val tooltipDateFormat = remember { SimpleDateFormat("MM-dd", Locale.CHINA) }
    val gradientColors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f))
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    val width = size.width.toFloat()
                    val leftPadding = 32.dp.toPx()
                    val chartWidth = width - leftPadding - 16.dp.toPx()
                    val spacing = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
                    if (selectedIndex != -1 && selectedIndex in dataPoints.indices) {
                        val point = dataPoints[selectedIndex]
                        val chartHeight = size.height.toFloat() - 24.dp.toPx()
                        val rangeTop = if (actualMax > 0) actualMax * 1.2f else 100f
                        val rangeBottom = if (actualMin < 0) actualMin * 1.2f else 0f
                        val drawingRange = (rangeTop - rangeBottom).coerceAtLeast(1f)
                        val x = leftPadding + selectedIndex * spacing
                        val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)
                        val clickRadius = 40.dp.toPx()
                        if (kotlin.math.abs(offset.x - x) < clickRadius && kotlin.math.abs(offset.y - y) < clickRadius) {
                            onPointClick(point)
                            return@detectTapGestures
                        }
                    }
                    val index = ((offset.x - leftPadding) / spacing).MathRound().coerceIn(0, dataPoints.lastIndex)
                    selectedIndex = index
                })
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val width = size.width.toFloat()
                        val leftPadding = 32.dp.toPx()
                        val spacing = (width - leftPadding - 16.dp.toPx()) / (dataPoints.size - 1).coerceAtLeast(1)
                        selectedIndex = ((offset.x - leftPadding) / spacing).MathRound().coerceIn(0, dataPoints.lastIndex)
                    },
                    onDrag = { change, _ ->
                        val width = size.width.toFloat()
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
            if (dataPoints.size <= 7 || index % (dataPoints.size / 5) == 0) {
                drawContext.canvas.nativeCanvas.drawText(point.label, x, height - 5.dp.toPx(), textPaint)
            }
        }
        fillPath.lineTo(leftPadding + (dataPoints.size - 1) * spacing, chartHeight)
        fillPath.close()
        drawPath(path = fillPath, brush = Brush.verticalGradient(colors = gradientColors, startY = 0f, endY = chartHeight))
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

        dataPoints.forEachIndexed { index, point ->
            val x = leftPadding + index * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Fill)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y), style = Stroke(width = 2.dp.toPx()))
        }

        if (selectedIndex != -1 && selectedIndex in dataPoints.indices) {
            val point = dataPoints[selectedIndex]
            val x = leftPadding + selectedIndex * spacing
            val y = chartHeight - ((point.value - rangeBottom) / drawingRange * chartHeight)
            drawLine(color = lineColor.copy(alpha = 0.5f), start = Offset(x, 0f), end = Offset(x, chartHeight), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, y))

            val dateText = tooltipDateFormat.format(Date(point.timeMillis))
            val amountText = String.format("%.0f", point.value)
            val textWidth = tooltipTextPaint.measureText(dateText).coerceAtLeast(tooltipTextPaint.measureText(amountText)) + 24.dp.toPx()
            val textHeight = 44.dp.toPx()
            var tooltipX = x - textWidth / 2
            if (tooltipX < 0) tooltipX = 0f
            if (tooltipX + textWidth > width) tooltipX = width - textWidth
            val tooltipY = if (y - textHeight - 12.dp.toPx() < 0) y + 12.dp.toPx() else y - textHeight - 12.dp.toPx()
            drawRoundRect(color = lineColor, topLeft = Offset(tooltipX, tooltipY), size = Size(textWidth, textHeight), cornerRadius = CornerRadius(8.dp.toPx()))
            drawContext.canvas.nativeCanvas.drawText(dateText, tooltipX + textWidth / 2, tooltipY + 16.dp.toPx(), tooltipTextPaint)
            drawContext.canvas.nativeCanvas.drawText(amountText, tooltipX + textWidth / 2, tooltipY + 34.dp.toPx(), tooltipTextPaint)
        }
    }
}

@Composable
fun PieChart(data: Map<String, Long>, title: String) {
    if (data.isEmpty()) return

    // 1. 数据准备 (保持最大9个分类 + 其他)
    val chartData = remember(data) {
        val sorted = data.entries.sortedByDescending { it.value }
        val allColors = getChartColors()
        val maxCategories = 9

        if (sorted.size <= maxCategories) {
            sorted.mapIndexed { index, entry ->
                ChartData(entry.key, entry.value, allColors[index % allColors.size])
            }
        } else {
            val topList = sorted.take(maxCategories).mapIndexed { index, entry ->
                ChartData(entry.key, entry.value, allColors[index % allColors.size])
            }
            val otherSum = sorted.drop(maxCategories).sumOf { it.value }
            topList + ChartData("其他", otherSum, Color(0xFFBDBDBD))
        }
    }

    val total = chartData.sumOf { it.value }.toFloat()
    if (total <= 0f) return

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current.density

    // 【修改】文字画笔颜色改为黑色/深灰色，不再跟随分类色
    val labelTextPaint = remember {
        android.graphics.Paint().apply {
            textSize = 11f * density
            color = android.graphics.Color.parseColor("#333333") // 深黑色，方便看
            typeface = Typeface.DEFAULT
        }
    }

    val centerTitlePaint = remember {
        android.graphics.Paint().apply {
            textSize = 14f * density
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.GRAY
        }
    }

    val centerValuePaint = remember {
        android.graphics.Paint().apply {
            textSize = 20f * density
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            color = android.graphics.Color.BLACK
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val center = Offset(canvasWidth / 2, canvasHeight / 2)
                        val vec = offset - center
                        val dist = sqrt(vec.x.pow(2) + vec.y.pow(2))
                        val minDimension = kotlin.math.min(canvasWidth, canvasHeight)
                        val radius = minDimension / 2 * 0.55f
                        val strokeWidth = radius * 0.5f
                        val innerRadiusThreshold = radius - strokeWidth / 2 - 20
                        val outerRadiusThreshold = radius + strokeWidth / 2 + 40

                        if (dist in innerRadiusThreshold..outerRadiusThreshold) {
                            var angle = Math.toDegrees(atan2(vec.y.toDouble(), vec.x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            var touchAngleRelativeToStart = angle + 90f
                            if (touchAngleRelativeToStart >= 360f) touchAngleRelativeToStart -= 360f
                            var currentStartAngle = 0f
                            var foundIndex = -1
                            for (i in chartData.indices) {
                                val sweep = 360f * (chartData[i].value / total)
                                if (touchAngleRelativeToStart >= currentStartAngle && touchAngleRelativeToStart < currentStartAngle + sweep) {
                                    foundIndex = i
                                    break
                                }
                                currentStartAngle += sweep
                            }
                            selectedIndex = if (selectedIndex == foundIndex) -1 else foundIndex
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.55f
            val strokeWidth = radius * 0.5f
            var startAngle = -90f

            chartData.forEachIndexed { index, slice ->
                val sweepAngle = 360f * (slice.value / total)
                val isSelected = index == selectedIndex
                val currentStrokeWidth = if (isSelected) strokeWidth * 1.15f else strokeWidth

                // 1. 绘制扇形
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Butt)
                )

                // 2. 绘制指示线和标签
                if (sweepAngle > 8f) { // 角度太小不绘制，避免重叠
                    val midAngle = startAngle + sweepAngle / 2
                    val midRad = Math.toRadians(midAngle.toDouble())
                    val outerRadius = radius + strokeWidth / 2
                    val lineStart = Offset(
                        (center.x + outerRadius * cos(midRad)).toFloat(),
                        (center.y + outerRadius * sin(midRad)).toFloat()
                    )
                    val lineOffset = 20.dp.toPx()
                    val elbow = Offset(
                        (center.x + (outerRadius + lineOffset) * cos(midRad)).toFloat(),
                        (center.y + (outerRadius + lineOffset) * sin(midRad)).toFloat()
                    )
                    val isRightSide = cos(midRad) > 0
                    val endLineLength = 25.dp.toPx()
                    val endX = if (isRightSide) elbow.x + endLineLength else elbow.x - endLineLength
                    val lineEnd = Offset(endX, elbow.y)

                    val path = Path().apply {
                        moveTo(lineStart.x, lineStart.y)
                        lineTo(elbow.x, elbow.y)
                        lineTo(lineEnd.x, lineEnd.y)
                    }

                    // 【关键】折线保持分类颜色
                    drawPath(path = path, color = slice.color, style = Stroke(width = 1.dp.toPx()))

                    // 【关键】文字颜色改为黑色 (上面 labelTextPaint 已定义为黑色)
                    // labelTextPaint.color = slice.color.toArgb() // <--- 删掉这行，不再跟随颜色

                    labelTextPaint.textAlign = if (isRightSide) android.graphics.Paint.Align.LEFT else android.graphics.Paint.Align.RIGHT
                    val textOffset = if (isRightSide) 5.dp.toPx() else -5.dp.toPx()

                    drawContext.canvas.nativeCanvas.drawText(
                        slice.name,
                        endX + textOffset,
                        elbow.y + labelTextPaint.textSize / 3,
                        labelTextPaint
                    )
                }
                startAngle += sweepAngle
            }

            // 绘制中心文字
            drawIntoCanvas { canvas ->
                if (selectedIndex != -1) {
                    val selectedSlice = chartData[selectedIndex]
                    // 选中时，标题可以用分类色，也可以用黑色
                    centerTitlePaint.color = selectedSlice.color.toArgb()
                    centerValuePaint.color = android.graphics.Color.BLACK
                    canvas.nativeCanvas.drawText(selectedSlice.name, center.x, center.y - 10.dp.toPx(), centerTitlePaint)
                    canvas.nativeCanvas.drawText(selectedSlice.value.toString(), center.x, center.y + 16.dp.toPx(), centerValuePaint)
                } else {
                    centerTitlePaint.color = android.graphics.Color.GRAY
                    centerValuePaint.color = android.graphics.Color.BLACK
                    canvas.nativeCanvas.drawText(title, center.x, center.y - 10.dp.toPx(), centerTitlePaint)
                    canvas.nativeCanvas.drawText(String.format("%.0f", total), center.x, center.y + 16.dp.toPx(), centerValuePaint)
                }
            }
        }
    }
}
// --- 数据处理函数 ---

fun calculateDateRange(calendar: Calendar, mode: ChartMode): Pair<Long, Long> {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val startMillis: Long
    val endMillis: Long

    when (mode) {
        ChartMode.WEEK -> {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (cal.firstDayOfWeek == Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            startMillis = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            endMillis = cal.timeInMillis
        }
        ChartMode.MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            startMillis = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            endMillis = cal.timeInMillis
        }
        ChartMode.YEAR -> {
            cal.set(Calendar.DAY_OF_YEAR, 1)
            startMillis = cal.timeInMillis
            cal.set(Calendar.MONTH, 11)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            endMillis = cal.timeInMillis
        }
    }
    return startMillis to endMillis
}

private fun Float.MathRound(): Int = kotlin.math.round(this).toInt()

fun prepareLineChartData(expenses: List<Expense>, chartMode: ChartMode, transactionType: TransactionType): List<LineChartPoint> {
    if (expenses.isEmpty()) return emptyList()
    val sampleDate = expenses.first().date
    val calendar = Calendar.getInstance()
    calendar.time = sampleDate
    val points = mutableListOf<LineChartPoint>()
    val sumFunc: (List<Expense>) -> Float = { list ->
        if (transactionType == TransactionType.BALANCE) list.sumOf { it.amount }.toFloat() else list.sumOf { abs(it.amount) }.toFloat()
    }
    when (chartMode) {
        ChartMode.WEEK -> {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (calendar.firstDayOfWeek == Calendar.SUNDAY) calendar.add(Calendar.DAY_OF_MONTH, 1)
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

// 莫兰迪配色
private fun getChartColors(): List<Color> {
    return listOf(
        Color(0xFF9FA8DA), // 1. 雾霾蓝
        Color(0xFFFFB74D), // 2. 杏橙色
        Color(0xFF80CBC4), // 3. 薄荷青
        Color(0xFFF48FB1), // 4. 干燥玫瑰
        Color(0xFFCE93D8), // 5. 香芋紫
        Color(0xFFFFE082), // 6. 暖黄色 (加深，替代了原来的奶油黄)
        Color(0xFFA5D6A7), // 7. 抹茶绿
        Color(0xFF90CAF9), // 8. 天空蓝 (稍微加深一点点)
        Color(0xFFB0BEC5)  // 9. 蓝灰色
    )
}

data class LineChartPoint(val label: String, val value: Float, val timeMillis: Long)
data class BalanceReportItem(val timeLabel: String, val income: Double, val expense: Double, val balance: Double)
private data class ChartData(val name: String, val value: Long, val color: Color)