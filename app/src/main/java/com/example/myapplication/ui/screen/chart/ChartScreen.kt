package com.example.myapplication.ui.screen.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
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
import com.example.myapplication.ui.navigation.MainCategory
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.screen.CustomDateRangePicker
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Calendar
import kotlin.math.abs

@Composable
fun ChartScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val allTransactions by viewModel.allExpenses.collectAsState(initial = emptyList())

    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    // --- [状态管理] 直接监听 ViewModel ---
    val chartMode by viewModel.chartModeState.collectAsState()
    val transactionType by viewModel.chartTransactionTypeState.collectAsState()
    val currentMillis by viewModel.chartDateMillisState.collectAsState()
    val customDateRange by viewModel.chartCustomDateRangeState.collectAsState()

    // 辅助状态
    val currentDate = remember(currentMillis) {
        Calendar.getInstance().apply { timeInMillis = currentMillis }
    }

    // 是否处于自定义模式
    val isCustomRange = customDateRange != null

    // 弹窗状态
    var showDateRangeDialog by remember { mutableStateOf(false) }

    // 1. 计算当前的时间范围
    // [修复报错点] 在 remember 内部处理 nullable 解构问题
    val (rangeStart, rangeEnd) = remember(chartMode, currentDate, customDateRange) {
        val range = customDateRange // 使用局部变量，确保智能转换生效
        if (range != null) {
            val start = range.first
            val endRaw = range.second

            // 确保结束日期包含当天的最后一秒
            val endCal = Calendar.getInstance().apply {
                timeInMillis = endRaw
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            start to endCal.timeInMillis
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

    // 4. 筛选图表数据 (收支类型)
    val filteredExpenses = remember(currentPeriodExpenses, transactionType) {
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
            // 顶部背景
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
                    onModeChange = { newMode ->
                        viewModel.setChartMode(newMode)
                        viewModel.setChartCustomDateRange(null, null)
                    },
                    onDateChange = { offset ->
                        val newCal = currentDate.clone() as Calendar
                        when (chartMode) {
                            ChartMode.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, offset)
                            ChartMode.MONTH -> newCal.add(Calendar.MONTH, offset)
                            ChartMode.YEAR -> newCal.add(Calendar.YEAR, offset)
                        }
                        viewModel.setChartDate(newCal.timeInMillis)
                    },
                    onTypeChange = { newType ->
                        viewModel.setChartTransactionType(newType)
                    },
                    onCustomRangeClick = { showDateRangeDialog = true },
                    onBackFromCustom = {
                        viewModel.setChartCustomDateRange(null, null)
                    }
                )

                if (filteredExpenses.isNotEmpty()) {
                    ChartPageContent(
                        data = filteredExpenses,
                        chartMode = if (isCustomRange) ChartMode.MONTH else chartMode,
                        transactionType = transactionType,
                        navController = navController,
                        dateRange = rangeStart to rangeEnd,
                        mainCategories = if (transactionType == TransactionType.INCOME) incomeMainCategories else expenseMainCategories,
                        isCustomRange = isCustomRange
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
            initialStartDate = customDateRange?.first,
            initialEndDate = customDateRange?.second,
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { start, end ->
                viewModel.setChartCustomDateRange(start, end)
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
    dateRange: Pair<Long, Long>,
    mainCategories: List<MainCategory>,
    isCustomRange: Boolean
) {
    val lineData = remember(data, chartMode, transactionType, dateRange, isCustomRange) {
        if (isCustomRange) {
            prepareCustomLineChartData(data, dateRange.first, dateRange.second, transactionType)
        } else {
            prepareLineChartData(data, chartMode, transactionType)
        }
    }

    val nestedStats = remember(data, mainCategories) {
        val totalAmount = data.sumOf { abs(it.amount) }.toFloat()

        mainCategories.mapNotNull { mainCat ->
            val subCategoryNames = mainCat.subCategories.map { it.title }.toSet()
            val relevantExpenses = data.filter { it.category in subCategoryNames }

            if (relevantExpenses.isNotEmpty()) {
                val mainAmount = relevantExpenses.sumOf { abs(it.amount) }

                MainCategoryStat(
                    name = mainCat.title,
                    amount = mainAmount,
                    percentageOfTotal = if (totalAmount > 0) (mainAmount / totalAmount * 100).toFloat() else 0f,
                    color = mainCat.color,
                    icon = mainCat.icon,
                    subCategories = emptyList()
                )
            } else {
                null
            }
        }.sortedByDescending { it.amount }
    }

    val pieChartData = remember(nestedStats) {
        nestedStats.associate { it.name to it.amount.toLong() }
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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
                                val end = when {
                                    isCustomRange -> {
                                        val label = point.label
                                        if (label.contains("-") && label.length > 5) {
                                            val c = calendar.clone() as Calendar
                                            c.add(Calendar.MONTH, 1)
                                            c.timeInMillis - 1
                                        } else {
                                            val c = calendar.clone() as Calendar
                                            c.add(Calendar.DAY_OF_MONTH, 1)
                                            c.timeInMillis - 1
                                        }
                                    }
                                    chartMode == ChartMode.YEAR -> {
                                        calendar.add(Calendar.MONTH, 1)
                                        calendar.timeInMillis - 1
                                    }
                                    else -> {
                                        calendar.add(Calendar.DAY_OF_MONTH, 1)
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

                // 全屏按钮
                val daysDiff = (dateRange.second - dateRange.first) / (1000 * 60 * 60 * 24)
                if (daysDiff > 31) {
                    IconButton(
                        onClick = {
                            val typeInt = when(transactionType) {
                                TransactionType.EXPENSE -> 0
                                TransactionType.INCOME -> 1
                                TransactionType.BALANCE -> 2
                            }
                            navController.navigate("fullscreen_chart/$typeInt/${dateRange.first}/${dateRange.second}")
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "全屏查看",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (transactionType == TransactionType.BALANCE) {
            BalanceReportSection(data, chartMode)
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if(transactionType == TransactionType.EXPENSE) "支出构成 (按大类)" else "收入构成 (按大类)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val pieTotal = pieChartData.values.sum().toFloat()
                    if (pieTotal > 0f) {
                        Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                            PieChart(pieChartData, title = if(transactionType == TransactionType.EXPENSE) "总支出" else "总收入")
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("没有数据", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val maxAmount = nestedStats.firstOrNull()?.amount ?: 1.0

                    nestedStats.forEach { mainStat ->
                        CategoryRankItem(
                            name = mainStat.name,
                            amount = mainStat.amount.toLong(),
                            percentage = mainStat.percentageOfTotal,
                            color = mainStat.color,
                            ratio = (mainStat.amount / maxAmount).toFloat(),
                            icon = mainStat.icon,
                            onClick = {
                                val typeInt = if (transactionType == TransactionType.INCOME) 1 else 0
                                navController.navigate(
                                    Routes.categoryChartDetailRoute(
                                        category = mainStat.name,
                                        type = typeInt,
                                        start = dateRange.first,
                                        end = dateRange.second
                                    )
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}