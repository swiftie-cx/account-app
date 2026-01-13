package com.swiftiecx.timeledger.ui.feature.budget.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Budget
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.RecordType
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.common.YearMonthPicker
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.util.Calendar
import kotlin.math.abs

data class BudgetDisplayItem(
    val stableKey: String,
    val budget: Double,
    val spent: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    year: Int,
    month: Int,
    onDateChange: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var showMonthPicker by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, year, month) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncBudgetsFor(year, month)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val defaultCurrency by viewModel.defaultCurrency.collectAsState(initial = "CNY")
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val budgets by viewModel.getBudgetsForMonth(year, month).collectAsState(initial = emptyList())
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    val budgetBaseCurrency = defaultCurrency
    val transferTypeString = stringResource(R.string.type_transfer)
    val totalBudgetKey = "总预算"
// [修正] 获取 Key
    val expenseCategoryKeys = remember(context) {
        CategoryData.getExpenseCategories(context).flatMap { it.subCategories }.map { it.key }
    }

    val budgetMap = remember(budgets, totalBudgetKey, expenseCategoryKeys) {
        budgets.associateBy { it.category }
            .mapKeys { (key, _) ->
                if (key == "总预算" || key == "Total Budget") totalBudgetKey else key
            }
            .filter { (key, _) -> key == totalBudgetKey || key in expenseCategoryKeys }
    }

    val totalBudget = budgetMap[totalBudgetKey]

    val monthlyExpenses = remember(expenses, year, month) {
        expenses.filter {
            val expenseCalendar = Calendar.getInstance().apply { time = it.date }
            val isTargetMonth = (expenseCalendar.get(Calendar.YEAR) == year && expenseCalendar.get(Calendar.MONTH) + 1 == month)
            val isExpense = it.amount < 0
            val isRegular = it.recordType == RecordType.INCOME_EXPENSE
            isTargetMonth && isExpense && isRegular
        }
    }

    val expenseMap = remember(monthlyExpenses, defaultCurrency, accountMap, expenseCategoryKeys, context) {
        monthlyExpenses
            .groupBy { exp -> CategoryData.getStableKey(exp.category, context) }
            .mapValues { (_, expenses) ->
                expenses.sumOf { exp ->
                    val account = accountMap[exp.accountId]
                    if (account != null) {
                        ExchangeRates.convert(abs(exp.amount), account.currency, defaultCurrency)
                    } else {
                        0.0
                    }
                }
            }
    }

    val totalSpent = remember(expenseMap) { expenseMap.values.sum() }

    val categoryBudgetsList = remember(budgetMap, expenseCategoryKeys, expenseMap, context) {
        expenseCategoryKeys.mapNotNull { stableKey ->
            val budgetAmount = budgetMap[stableKey]?.amount ?: 0.0
            if (budgetAmount > 0.0) {
                // [修正] 调用 CategoryData.getDisplayName
                val displayName = CategoryData.getDisplayName(stableKey, context)
                val spent = expenseMap[stableKey] ?: expenseMap[displayName] ?: 0.0
                BudgetDisplayItem(stableKey, budgetAmount, spent)
            } else {
                null
            }
        }.sortedByDescending { it.budget }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TextButton(onClick = { showMonthPicker = true }) {
                        Text(
                            stringResource(R.string.date_format_year_month_format, year, month),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        navController.navigate(Routes.budgetSettingsRoute(year, month))
                    }) {
                        Text(stringResource(R.string.budget_settings_button))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                val displayBudget = if (totalBudget != null) {
                    val converted = ExchangeRates.convert(totalBudget.amount, budgetBaseCurrency, defaultCurrency)
                    totalBudget.copy(amount = converted)
                } else {
                    val sumCategoryBudgets = categoryBudgetsList.sumOf { it.budget }
                    Budget(category = totalBudgetKey, amount = sumCategoryBudgets, year = year, month = month)
                }
                BudgetSummaryCard(displayBudget, totalSpent, defaultCurrency)
            }

            items(categoryBudgetsList, key = { it.stableKey }) { item ->
                // [修正] 全部调用 CategoryData 的方法
                val displayName = CategoryData.getDisplayName(item.stableKey, context)
                val color = CategoryData.getColor(item.stableKey, 0, context)
                val icon = CategoryData.getIcon(item.stableKey, context)
                val convertedBudget = ExchangeRates.convert(item.budget, budgetBaseCurrency, defaultCurrency)

                BudgetCategoryItem(displayName, convertedBudget, item.spent, defaultCurrency, color, icon)
            }
        }
    }

    if (showMonthPicker) {
        YearMonthPicker(
            year = year,
            month = month,
            onConfirm = { newYear, newMonth ->
                onDateChange(newYear, newMonth)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

@Composable
fun BudgetSummaryCard(budget: Budget, spent: Double, currency: String) {
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(1000), label = "progress")
    val isOverBudget = spent > budget.amount
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = if (isOverBudget) errorColor else primaryColor

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(color = indicatorColor, startAngle = -90f, sweepAngle = 360 * animatedProgress, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                }
                Text(text = stringResource(R.string.budget_percent_format, (progress * 100).toInt()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = indicatorColor)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.total_budget_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.budget_spent), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = stringResource(R.string.currency_amount_no_decimal_format, currency, spent), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.budget_remaining), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = stringResource(R.string.currency_amount_no_decimal_format, currency, (budget.amount - spent).coerceAtLeast(0.0)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (isOverBudget) errorColor else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.budget_total_amount_format, currency, budget.amount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun BudgetCategoryItem(title: String, budgetAmount: Double, spent: Double, currency: String, color: Color, icon: ImageVector) {
    val remaining = budgetAmount - spent
    val percentage = if (budgetAmount > 0) (spent / budgetAmount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = percentage.coerceIn(0f, 1f), animationSpec = tween(1000), label = "progress")
    val isOverBudget = spent > budgetAmount
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val barColor = if (isOverBudget) errorColor else color

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 如果需要显示左侧小圆环可以解除注释，目前设为空实现避免占位
                // BudgetCategoryProgressRing(percentage, barColor)
                // Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(barColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = barColor, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.spent_label_short), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.currency_amount_no_decimal_format, currency, spent), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.budget_label_short), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.currency_amount_no_decimal_format, currency, budgetAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // [修改] 替换 LinearProgressIndicator 为自定义 Box 实现，确保样式为连续实心条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(barColor)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(text = if (remaining >= 0) stringResource(R.string.remaining_amount_format, currency, remaining) else stringResource(R.string.overspent_amount_format, currency, abs(remaining)), style = MaterialTheme.typography.bodySmall, color = if (remaining < 0) errorColor else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// 保留空实现或根据需要移除
@Composable
fun BudgetCategoryProgressRing(percentage: Float, color: Color) {}