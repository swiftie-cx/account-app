package com.example.myapplication.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.* // 使用 * 导入
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // 确保导入 items
import androidx.compose.material3.* // 使用 * 导入
import androidx.compose.runtime.* // 使用 * 导入
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.example.myapplication.data.Budget
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
// --- (修复) 添加 Imports ---
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.screen.YearMonthPicker // 导入 YearMonthPicker
// --- 修复结束 ---
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    year: Int,
    month: Int,
    onDateChange: (Int, Int) -> Unit,
    defaultCurrency: String
) {
    var showMonthPicker by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, year, month) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncBudgetsFor(year, month)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val budgets by viewModel.getBudgetsForMonth(year, month).collectAsState(initial = emptyList())
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val monthlyExpenses = remember(expenses, year, month) {
        expenses.filter {
            val expenseCalendar = Calendar.getInstance().apply { time = it.date }
            (expenseCalendar.get(Calendar.YEAR) == year &&
                    expenseCalendar.get(Calendar.MONTH) + 1 == month &&
                    it.amount < 0)
        }
    }
    val expenseMap = remember(monthlyExpenses, defaultCurrency, accountMap) {
        monthlyExpenses.groupBy { it.category }.mapValues { (_, expenses) ->
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
    val totalSpent = remember(monthlyExpenses, defaultCurrency, accountMap) {
        monthlyExpenses.sumOf { exp ->
            val account = accountMap[exp.accountId]
            if (account != null) {
                ExchangeRates.convert(abs(exp.amount), account.currency, defaultCurrency)
            } else {
                0.0
            }
        }
    }
    val (totalBudgetList, categoryBudgets) = remember(budgets) {
        budgets.partition { it.category == "总预算" }
    }
    val totalBudget = totalBudgetList.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showMonthPicker = true }) {
                            Text(
                                "${year}年${month}月",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = {
                        navController.navigate(Routes.budgetSettingsRoute(year, month))
                    }) {
                        Text("预算设置")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (totalBudget != null) {
                val convertedBudgetAmount = ExchangeRates.convert(totalBudget.amount, "CNY", defaultCurrency)
                BudgetCard(budget = totalBudget.copy(amount = convertedBudgetAmount), spent = totalSpent, currency = defaultCurrency)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categoryBudgets) { budget ->
                    val spent = expenseMap[budget.category] ?: 0.0
                    val convertedBudgetAmount = ExchangeRates.convert(budget.amount, "CNY", defaultCurrency)
                    BudgetCard(budget = budget.copy(amount = convertedBudgetAmount), spent = spent, currency = defaultCurrency)
                }
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
fun BudgetCard(budget: Budget, spent: Double, currency: String) {
    val percentage = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgress(percentage = percentage)
            Column(modifier = Modifier.weight(1f)) {
                Text(budget.category, style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = percentage,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(String.format(Locale.US, "已用: %s %.2f", currency, spent), style = MaterialTheme.typography.bodySmall)
                    Text(String.format(Locale.US, "预算: %s %.2f", currency, budget.amount), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun CircularProgress(percentage: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color.LightGray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
            drawArc(
                color = if (percentage > 1) Color.Red else Color.Blue,
                startAngle = -90f,
                sweepAngle = 360 * percentage,
                useCenter = false,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }
        Text(text = "${(percentage * 100).toInt()}%")
    }
}
