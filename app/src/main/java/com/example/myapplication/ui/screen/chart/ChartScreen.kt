package com.example.myapplication.ui.screen.chart // 包名变了

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.CategoryHelper
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.screen.CustomDateRangePicker
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Calendar
import kotlin.math.abs

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
            // 顶部紫色弧形背景
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部仪表盘
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

                // 图表内容区域
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

    val colors = getChartColors()
    val lineData = remember(data, chartMode, transactionType) {
        prepareLineChartData(data, chartMode, transactionType)
    }

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
                        lineColor = MaterialTheme.colorScheme.primary,
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

                        // 【修改】使用 CategoryHelper 获取大类颜色 (为后续嵌套做准备)
                        // 注意：这里暂时还是用小类名查大类颜色
                        val typeInt = if(transactionType == TransactionType.INCOME) 1 else 0
                        val color = CategoryHelper.getCategoryColor(entry.key, typeInt)
                        val icon = CategoryHelper.getIcon(entry.key)

                        val barRatio = if (sortedEntries.isNotEmpty()) amount / sortedEntries.first().value.toFloat() else 0f

                        CategoryRankItem(
                            name = entry.key,
                            amount = entry.value,
                            percentage = percentage,
                            color = color,
                            ratio = barRatio,
                            icon = icon,
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