package com.swiftiecx.timeledger.ui.screen.chart

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
import androidx.compose.ui.platform.LocalContext // [新增]
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.ui.navigation.CategoryData // [新增]
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.screen.CustomDateRangePicker
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.util.Calendar
import kotlin.math.abs

@Composable
fun ChartScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val context = LocalContext.current // [新增] 获取 Context
    // [新增] 跨币种所需数据
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val defaultCurrency by viewModel.defaultCurrency.collectAsState(initial = "CNY")
    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }

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
    val (rangeStart, rangeEnd) = remember(chartMode, currentDate, customDateRange) {
        val range = customDateRange
        if (range != null) {
            val start = range.first
            val endRaw = range.second
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

    // [关键修改] 使用 Stable Key 判断转账
    fun isTransfer(expense: Expense): Boolean {
        val key = CategoryData.getStableKey(expense.category, context)
        return key.startsWith("Transfer")
    }

    // 3. 计算统计数据
    val expensesForSum = remember(currentPeriodExpenses, accountMap, defaultCurrency, context) {
        currentPeriodExpenses
            .filter { !isTransfer(it) } // [修改] 使用新的转账判断
            .mapNotNull { expense ->
                val account = accountMap[expense.accountId]
                if (account != null) {
                    ExchangeRates.convert(expense.amount, account.currency, defaultCurrency)
                } else {
                    null
                }
            }
    }

    val totalExpense = remember(expensesForSum) {
        expensesForSum.filter { it < 0 }.sumOf { abs(it) }
    }
    val totalIncome = remember(expensesForSum) {
        expensesForSum.filter { it > 0 }.sumOf { it }
    }
    val totalBalance = totalIncome - totalExpense

    // 4. 筛选图表数据 (收支类型)
    val filteredExpenses = remember(currentPeriodExpenses, transactionType, context) {
        currentPeriodExpenses.filter { expense ->
            val isTransfer = isTransfer(expense)
            when (transactionType) {
                TransactionType.EXPENSE -> expense.amount < 0 && !isTransfer
                TransactionType.INCOME -> expense.amount > 0 && !isTransfer
                TransactionType.BALANCE -> !isTransfer
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
                    defaultCurrency = defaultCurrency,
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
                        isCustomRange = isCustomRange,
                        accountMap = accountMap,
                        defaultCurrency = defaultCurrency
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
    isCustomRange: Boolean,
    accountMap: Map<Long, Account>,
    defaultCurrency: String
) {
    val context = LocalContext.current // [新增]

    // [关键修改] 使用 Stable Key 判断转账
    fun isTransfer(expense: Expense): Boolean {
        val key = CategoryData.getStableKey(expense.category, context)
        return key.startsWith("Transfer")
    }

    val lineData = remember(data, chartMode, transactionType, dateRange, isCustomRange, accountMap, defaultCurrency) {
        if (isCustomRange) {
            prepareCustomLineChartData(data, dateRange.first, dateRange.second, transactionType, accountMap, defaultCurrency)
        } else {
            prepareLineChartData(data, chartMode, transactionType, accountMap, defaultCurrency)
        }
    }

    val expensesForStats = remember(data, accountMap, defaultCurrency, context) {
        data.filter { !isTransfer(it) }.mapNotNull { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                ExchangeRates.convert(abs(expense.amount), account.currency, defaultCurrency)
            } else {
                null
            }
        }
    }

    val totalAmount = remember(expensesForStats) { expensesForStats.sumOf { it } }

    // [关键修复] 嵌套统计使用 Key 匹配
    val nestedStats = remember(data, mainCategories, totalAmount, accountMap, defaultCurrency, context) {
        mainCategories.mapNotNull { mainCat ->
            // [修复] 获取该大类下所有子分类的 Stable Keys
            val subCategoryKeys = mainCat.subCategories.map { it.key }.toSet()

            // [修复] 使用 Stable Key 进行匹配
            val relevantExpenses = data.filter {
                val expenseKey = CategoryData.getStableKey(it.category, context)
                expenseKey in subCategoryKeys && !expenseKey.startsWith("Transfer")
            }

            if (relevantExpenses.isNotEmpty()) {
                val mainAmount = relevantExpenses.sumOf { expense ->
                    val account = accountMap[expense.accountId]
                    if (account != null) {
                        ExchangeRates.convert(abs(expense.amount), account.currency, defaultCurrency)
                    } else {
                        0.0
                    }
                }

                MainCategoryStat(
                    name = mainCat.title, // 显示使用本地化 Title
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
                        text = if (transactionType == TransactionType.BALANCE) stringResource(R.string.chart_trend_balance) else stringResource(R.string.chart_trend_general),
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
                            contentDescription = stringResource(R.string.chart_fullscreen),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (transactionType == TransactionType.BALANCE) {
            BalanceReportSection(data, chartMode, defaultCurrency, accountMap)
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if(transactionType == TransactionType.EXPENSE) stringResource(R.string.chart_composition_expense) else stringResource(R.string.chart_composition_income),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val pieTotal = pieChartData.values.sum().toFloat()
                    if (pieTotal > 0f) {
                        Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                            PieChart(
                                data = pieChartData,
                                title = if(transactionType == TransactionType.EXPENSE) stringResource(R.string.chart_total_expense) else stringResource(R.string.chart_total_income),
                                currency = defaultCurrency
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.chart_no_data), color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val maxAmount = nestedStats.firstOrNull()?.amount ?: 1.0

                    nestedStats.forEach { mainStat ->
                        CategoryRankItem(
                            name = mainStat.name,
                            amount = mainStat.amount,
                            percentage = mainStat.percentageOfTotal,
                            color = mainStat.color,
                            ratio = (mainStat.amount / maxAmount).toFloat(),
                            icon = mainStat.icon,
                            currency = defaultCurrency,
                            onClick = {
                                val typeInt = if (transactionType == TransactionType.INCOME) 1 else 0
                                navController.navigate(
                                    Routes.categoryChartDetailRoute(
                                        category = mainStat.name, // 传递的是 Title
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