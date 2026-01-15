package com.swiftiecx.timeledger.ui.feature.chart.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.ui.common.CategoryData
import com.swiftiecx.timeledger.ui.common.MainCategory
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.feature.chart.component.CategoryRankItem
import com.swiftiecx.timeledger.ui.feature.chart.util.ChartMode
import com.swiftiecx.timeledger.ui.feature.chart.visual.LineChart
import com.swiftiecx.timeledger.ui.feature.chart.visual.PieChart
import com.swiftiecx.timeledger.ui.feature.chart.util.TransactionType
import com.swiftiecx.timeledger.ui.feature.chart.util.prepareCustomLineChartData
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import kotlin.collections.find
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChartDetailScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    categoryName: String, // 这里传入的是 Title (e.g. "餐饮")
    transactionType: Int,
    startDate: Long,
    endDate: Long
) {
    val context = LocalContext.current
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val defaultCurrency by viewModel.defaultCurrency.collectAsState(initial = "CNY")
    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }

    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    val mainCategory = remember(categoryName, expenseMainCategories, incomeMainCategories) {
        val list = if (transactionType == 1) incomeMainCategories else expenseMainCategories
        list.find { it.title == categoryName }
    }

    val categoryColor = mainCategory?.color ?: MaterialTheme.colorScheme.primary

    // [关键修复] 获取该大类下所有子分类的 Stable Keys
    val subCategoryKeys = remember(mainCategory) {
        mainCategory?.subCategories?.map { it.key }?.toSet() ?: emptySet()
    }

    // [关键修复] 使用 Stable Key 过滤交易
    val filteredExpenses = remember(allExpenses, subCategoryKeys, startDate, endDate, transactionType, context) {
        if (subCategoryKeys.isEmpty()) {
            emptyList()
        } else {
            allExpenses.filter { expense ->
                val isTypeMatch = if (transactionType == 1) expense.amount > 0 else expense.amount < 0
                // 把交易记录的 key 转为 stable key 后进行匹配
                val expenseKey = CategoryData.getStableKey(expense.category, context)
                val isCategoryMatch = expenseKey in subCategoryKeys
                val isDateMatch = expense.date.time in startDate..endDate
                isTypeMatch && isCategoryMatch && isDateMatch
            }
        }
    }

    val totalAmount = remember(filteredExpenses, accountMap, defaultCurrency) {
        filteredExpenses.sumOf { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                ExchangeRates.convert(abs(expense.amount), account.currency, defaultCurrency)
            } else {
                0.0
            }
        }.toFloat()
    }

    val chartMode = remember(startDate, endDate) {
        val days = (endDate - startDate) / (1000 * 60 * 60 * 24)
        if (days > 90) ChartMode.YEAR else ChartMode.MONTH
    }

    val lineData = remember(filteredExpenses, chartMode, accountMap, defaultCurrency) {
        val typeEnum = if (transactionType == 1) TransactionType.INCOME else TransactionType.EXPENSE
        prepareCustomLineChartData(
            data = filteredExpenses,
            startDate = startDate,
            endDate = endDate,
            transactionType = typeEnum,
            accountMap = accountMap,
            defaultCurrency = defaultCurrency
        )
    }

    // [关键修复] 聚合时使用 DisplayName (Title)
    val subCategorySums = remember(filteredExpenses, accountMap, defaultCurrency, context) {
        filteredExpenses.groupBy { CategoryData.getDisplayName(it.category, context) }
            .mapValues { (_, list) ->
                list.sumOf { expense ->
                    val account = accountMap[expense.accountId]
                    if (account != null) {
                        ExchangeRates.convert(abs(expense.amount), account.currency, defaultCurrency)
                    } else {
                        0.0
                    }
                }.toLong()
            }
            .entries.sortedByDescending { it.value }
    }

    val pieChartData = remember(subCategorySums) {
        subCategorySums.associate { it.key to it.value }
    }

    val pieChartTitle = stringResource(
        if (transactionType == 1) R.string.type_income else R.string.type_expense
    )

    // 构建 Icon Map (Key -> Icon 和 Title -> Icon 都存一份，保险)
    val subCategoryIconMap = remember(context) {
        val expenseMains = CategoryData.getExpenseCategories(context)
        val incomeMains = CategoryData.getIncomeCategories(context)

        fun buildIconMap(mains: List<MainCategory>): Map<String, ImageVector> {
            val map = mutableMapOf<String, ImageVector>()
            mains.forEach { main ->
                main.subCategories.forEach { sub ->
                    map[sub.title] = sub.icon
                    map[sub.key] = sub.icon
                }
            }
            return map
        }

        buildIconMap(expenseMains) + buildIconMap(incomeMains)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.total_amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.currency_amount_format, defaultCurrency, totalAmount),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                }
            }

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.chart_no_data), color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.trend_chart), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            LineChart(
                                dataPoints = lineData,
                                modifier = Modifier.fillMaxSize(),
                                lineColor = categoryColor,
                                onPointClick = {}
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.category_composition), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))

                        if (pieChartData.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                                PieChart(
                                    data = pieChartData,
                                    title = pieChartTitle,
                                    currency = defaultCurrency
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        val maxAmount = subCategorySums.firstOrNull()?.value ?: 1L

                        subCategorySums.forEachIndexed { index, entry ->
                            val amount = entry.value.toDouble()
                            val percentage = if (totalAmount > 0) (amount.toFloat() / totalAmount * 100f) else 0f
                            val barRatio = if (maxAmount > 0) (amount.toFloat() / maxAmount.toFloat()) else 0f

                            // entry.key 是 DisplayName
                            val icon = subCategoryIconMap[entry.key] ?: Icons.Default.HelpOutline

                            CategoryRankItem(
                                name = entry.key,
                                amount = amount,
                                percentage = percentage,
                                color = categoryColor,
                                ratio = barRatio,
                                icon = icon,
                                currency = defaultCurrency,
                                onClick = {
                                    val searchType = if (transactionType == 0) 1 else 2
                                    navController.navigate(
                                        Routes.searchRoute(
                                            category = entry.key, // 这里可以传 DisplayName，因为 SearchScreen 已经修复支持 Title 搜索
                                            startDate = startDate,
                                            endDate = endDate,
                                            type = searchType
                                        )
                                    )
                                }
                            )
                            if (index < subCategorySums.size - 1) {
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}