package com.example.myapplication.ui.screen.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.navigation.CategoryHelper
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChartDetailScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    categoryName: String, // 这里传入的是大类名称 (如 "餐饮美食")
    transactionType: Int, // 0: 支出, 1: 收入
    startDate: Long,
    endDate: Long
) {
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // [关键修复] 获取实时的大类列表，以便正确查找到当前大类的信息
    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    // 1. 查找当前大类对象
    val mainCategory = remember(categoryName, expenseMainCategories, incomeMainCategories) {
        val list = if (transactionType == 1) incomeMainCategories else expenseMainCategories
        list.find { it.title == categoryName }
    }

    val categoryColor = mainCategory?.color ?: MaterialTheme.colorScheme.primary

    // 获取该大类下的所有小类名称集合
    val subCategoryNames = remember(mainCategory) {
        mainCategory?.subCategories?.map { it.title }?.toSet() ?: emptySet()
    }

    // 2. 筛选数据：属于该大类（即其 category 在小类集合中）的所有账单
    val filteredExpenses = remember(allExpenses, subCategoryNames, startDate, endDate, transactionType) {
        if (subCategoryNames.isEmpty()) {
            emptyList()
        } else {
            allExpenses.filter { expense ->
                val isTypeMatch = if (transactionType == 1) expense.amount > 0 else expense.amount < 0
                // 关键：判断账单的分类是否属于当前大类的子类列表
                val isCategoryMatch = expense.category in subCategoryNames
                val isDateMatch = expense.date.time in startDate..endDate
                isTypeMatch && isCategoryMatch && isDateMatch
            }
        }
    }

    // 3. 统计总金额
    val totalAmount = remember(filteredExpenses) {
        filteredExpenses.sumOf { abs(it.amount) }.toFloat()
    }

    // 4. 准备折线图数据
    val chartMode = remember(startDate, endDate) {
        val days = (endDate - startDate) / (1000 * 60 * 60 * 24)
        if (days > 32) ChartMode.YEAR else ChartMode.MONTH
    }

    val lineData = remember(filteredExpenses, chartMode) {
        val typeEnum = if (transactionType == 1) TransactionType.INCOME else TransactionType.EXPENSE
        prepareLineChartData(filteredExpenses, chartMode, typeEnum)
    }

    // 5. 准备饼图和列表数据 (按子类聚合)
    val subCategorySums = remember(filteredExpenses) {
        filteredExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { abs(it.amount).toLong() } }
            .entries.sortedByDescending { it.value }
    }

    val pieChartData = remember(subCategorySums) {
        subCategorySums.associate { it.key to it.value }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) }, // 显示大类名称
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // --- 头部总览 ---
            Card(
                colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("本期总额", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = String.format("%.2f", totalAmount),
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
                    Text("暂无详细数据", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                // --- 1. 趋势折线图 ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("趋势变化", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

                // --- 2. 子类构成 (环形图 + 列表) ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("子类构成", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))

                        // 环形图
                        if (pieChartData.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                PieChart(pieChartData, title = "合计")
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // 子类列表
                        subCategorySums.forEachIndexed { index, entry ->
                            val amount = entry.value.toFloat()
                            val percentage = if (totalAmount > 0) amount / totalAmount * 100f else 0f
                            val ratio = if (subCategorySums.isNotEmpty()) amount / subCategorySums.first().value.toFloat() else 0f

                            // 尝试查找子类图标
                            val icon = CategoryHelper.getIcon(entry.key)

                            CategoryRankItem(
                                name = entry.key,
                                amount = entry.value,
                                percentage = percentage,
                                color = categoryColor, // 统一使用大类主题色
                                ratio = ratio,
                                icon = icon,
                                onClick = {
                                    // 点击子类 -> 跳转到 SearchScreen
                                    val searchType = if (transactionType == 0) 1 else 2
                                    navController.navigate(
                                        Routes.searchRoute(
                                            category = entry.key,
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