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
import androidx.compose.ui.res.stringResource // [新增] 引入资源引用
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增] 引入 R 类
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.screen.CustomDateRangePicker
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.util.Calendar
import kotlin.math.abs
import kotlin.text.startsWith

@Composable
fun ChartScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    // [新增] 跨币种所需数据
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val defaultCurrency by viewModel.defaultCurrency.collectAsState(initial = "CNY")
    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }

    val transferTypeString = stringResource(R.string.type_transfer) // [i18n]

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

    // 3. 计算统计数据 (BUG 修复区域 - 引入汇率兑换)
    val expensesForSum = remember(currentPeriodExpenses, accountMap, defaultCurrency, transferTypeString) {
        currentPeriodExpenses
            .filter { !it.category.startsWith(transferTypeString) }
            .mapNotNull { expense ->
                val account = accountMap[expense.accountId]
                if (account != null) {
                    // 兑换到默认货币
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
    val filteredExpenses = remember(currentPeriodExpenses, transactionType, transferTypeString) {
        currentPeriodExpenses.filter { expense ->
            when (transactionType) {
                TransactionType.EXPENSE -> expense.amount < 0 && !expense.category.startsWith(transferTypeString)
                TransactionType.INCOME -> expense.amount > 0 && !expense.category.startsWith(transferTypeString)
                TransactionType.BALANCE -> !expense.category.startsWith(transferTypeString)
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
                    defaultCurrency = defaultCurrency, // [传入]
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
                        accountMap = accountMap, // [传入]
                        defaultCurrency = defaultCurrency // [传入]
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
    accountMap: Map<Long, Account>, // [新增]
    defaultCurrency: String // [新增]
) {
    val transferTypeString = stringResource(R.string.type_transfer) // [i18n]

    // [修改调用] 传入 accountMap 和 defaultCurrency
    val lineData = remember(data, chartMode, transactionType, dateRange, isCustomRange, accountMap, defaultCurrency) {
        if (isCustomRange) {
            prepareCustomLineChartData(data, dateRange.first, dateRange.second, transactionType, accountMap, defaultCurrency)
        } else {
            prepareLineChartData(data, chartMode, transactionType, accountMap, defaultCurrency)
        }
    }

    // [BUG 修复] 计算总金额时，必须使用兑换后的金额进行百分比计算
    val expensesForStats = remember(data, accountMap, defaultCurrency, transferTypeString) {
        data.filter { !it.category.startsWith(transferTypeString) }.mapNotNull { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                // 将交易金额兑换成默认货币的绝对值
                ExchangeRates.convert(abs(expense.amount), account.currency, defaultCurrency)
            } else {
                null
            }
        }
    }

    val totalAmount = remember(expensesForStats) { expensesForStats.sumOf { it } }

    val nestedStats = remember(data, mainCategories, totalAmount, accountMap, defaultCurrency, transferTypeString) {
        mainCategories.mapNotNull { mainCat ->
            val subCategoryNames = mainCat.subCategories.map { it.title }.toSet()

            // 筛选出属于该大类的，且不属于转账的交易
            val relevantExpenses = data.filter { it.category in subCategoryNames && !it.category.startsWith(transferTypeString) }

            if (relevantExpenses.isNotEmpty()) {
                // [BUG 修复] 计算 mainAmount 时进行兑换
                val mainAmount = relevantExpenses.sumOf { expense ->
                    val account = accountMap[expense.accountId]
                    if (account != null) {
                        ExchangeRates.convert(abs(expense.amount), account.currency, defaultCurrency)
                    } else {
                        0.0
                    }
                }

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
        // 由于 nestedStats.amount 已经是 Double，这里需要将其转换为 Long (或保持 Double 用于 PieChart)
        // 假设 PieChart 需要 Long，且数据量不会溢出 Long
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
                                        if (label.contains("-") && label.length > 5) { // 简单判断是否是月 (MM-dd) 模式，但这里可能更复杂
                                            // 假设 ChartUtils 中的月格式是 yyyy-MM
                                            // 如果是月统计模式，则 end 是下个月初
                                            val c = calendar.clone() as Calendar
                                            c.add(Calendar.MONTH, 1)
                                            c.timeInMillis - 1
                                        } else {
                                            // 否则按日计算
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
            // [修改调用] 传入 accountMap 和 defaultCurrency
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
                                currency = defaultCurrency // [新增]
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
                            amount = mainStat.amount, // [修正] 传递 Double
                            percentage = mainStat.percentageOfTotal,
                            color = mainStat.color,
                            ratio = (mainStat.amount / maxAmount).toFloat(),
                            icon = mainStat.icon,
                            currency = defaultCurrency, // [传入]
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