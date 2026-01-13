package com.swiftiecx.timeledger.ui.feature.transaction.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailsScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    dateMillis: Long
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = dateMillis } }
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // 实时分类数据（包含用户新增）
    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    // ✅ 关键修复：用 sub.key 作为 map key（而不是 sub.title）
    val categoryStyleMap = remember(expenseMainCategories, incomeMainCategories) {
        val map = mutableMapOf<String, Pair<ImageVector, Color>>()
        (expenseMainCategories + incomeMainCategories).forEach { main ->
            main.subCategories.forEach { sub ->
                map[sub.key] = sub.icon to main.color
            }
        }
        map
    }

    val dailyExpenses = remember(allExpenses, calendar) {
        allExpenses.filter {
            val expenseCal = Calendar.getInstance().apply { time = it.date }
            expenseCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    expenseCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
        }.sortedByDescending { it.date.time }
    }

    val totalIncome = remember(dailyExpenses) { dailyExpenses.filter { it.amount > 0 }.sumOf { it.amount } }
    val totalExpense = remember(dailyExpenses) { dailyExpenses.filter { it.amount < 0 }.sumOf { it.amount } }

    val topBarFormatter = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = { Text(topBarFormatter.format(calendar.time)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("${Routes.ADD_TRANSACTION}?dateMillis=$dateMillis") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            DailySummaryHeader(calendar, totalExpense, totalIncome)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(dailyExpenses) { expense ->
                        // ✅ 统一：先转 stableKey，再取 icon/color
                        val stableKey = CategoryData.getStableKey(expense.category, context)

                        // 兼容旧数据：如果以前存的是 title，尝试用原值兜底
                        val stylePair = categoryStyleMap[stableKey] ?: categoryStyleMap[expense.category]

                        // [修改] 如果在 map 里找不到（比如借贷记录），尝试用 IconMapper 获取统一图标
                        val icon = stylePair?.first ?: IconMapper.getIcon(expense.category)

                        // [修改] 颜色兜底逻辑：如果没有配置颜色（借贷），按收支显示绿/红
                        val color = stylePair?.second ?: if (expense.amount < 0) Color(0xFFE53935) else Color(0xFF4CAF50)

                        DailyTransactionItem(
                            expense = expense,
                            displayCategory = CategoryData.getDisplayName(stableKey, context),
                            icon = icon,
                            categoryColor = color,
                            onClick = { navController.navigate(Routes.transactionDetailRoute(expense.id)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DailySummaryHeader(calendar: Calendar, totalExpense: Double, totalIncome: Double) {
    val formatter = remember { SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatter.format(calendar.time),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))

        if (totalExpense != 0.0) {
            Text(
                text = "支 ${String.format(Locale.US, "%.2f", abs(totalExpense))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (totalIncome > 0 && totalExpense != 0.0) Spacer(Modifier.width(12.dp))
        if (totalIncome > 0) {
            Text(
                text = "收 ${String.format(Locale.US, "%.2f", totalIncome)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DailyTransactionItem(
    expense: Expense,
    displayCategory: String,
    icon: ImageVector,
    categoryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = displayCategory,
                tint = categoryColor
            )
        }

        Text(
            text = displayCategory,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = String.format(Locale.US, "%.2f", expense.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (expense.amount < 0) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
        )
    }
}