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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Budget
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
// --- (修复) 确保 Imports 在这里 ---
import com.example.myapplication.ui.screen.Routes // 导入 Routes
import com.example.myapplication.ui.screen.YearMonthPicker // 导入 YearMonthPicker
// --- 修复结束 ---
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val calendar = Calendar.getInstance()
    // (新) 状态提升到这里
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var showMonthPicker by remember { mutableStateOf(false) }

    // (新) 从 BudgetSettingsScreen 移过来的副作用
    LaunchedEffect(selectedYear, selectedMonth) {
        viewModel.syncBudgetsFor(selectedYear, selectedMonth)
    }

    // (修改) 使用状态变量来获取数据
    val budgets by viewModel.getBudgetsForMonth(selectedYear, selectedMonth).collectAsState(initial = emptyList())
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // (修改) 使用状态变量来过滤支出
    val monthlyExpenses = remember(expenses, selectedYear, selectedMonth) {
        expenses.filter {
            val expenseCalendar = Calendar.getInstance().apply { time = it.date }
            (expenseCalendar.get(Calendar.YEAR) == selectedYear &&
                    expenseCalendar.get(Calendar.MONTH) + 1 == selectedMonth &&
                    it.amount < 0) // (新) 只统计支出
        }
    }

    val expenseMap = remember(monthlyExpenses) {
        monthlyExpenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> abs(exp.amount) } }
    }

    val totalSpent = remember(monthlyExpenses) {
        monthlyExpenses.sumOf { abs(it.amount) }
    }

    // (这是我们上次的修改，保持不变)
    val (totalBudgetList, categoryBudgets) = remember(budgets) {
        budgets.partition { it.category == "总预算" }
    }
    val totalBudget = totalBudgetList.firstOrNull()

    Scaffold(
        floatingActionButton = {
            Button(onClick = {
                // (修改) 导航时传递当前选择的年和月
                navController.navigate(Routes.budgetSettingsRoute(selectedYear, selectedMonth))
            }) {
                Text("+ 预算设置")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // (新) 添加月份选择按钮
            TextButton(onClick = { showMonthPicker = true }) {
                Text(
                    "${selectedYear}年${selectedMonth}月",
                    style = MaterialTheme.typography.titleLarge // 让它显眼一点
                )
            }

            // (这是我们上次的修改，保持不变)
            if (totalBudget != null) {
                BudgetCard(budget = totalBudget, spent = totalSpent)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categoryBudgets) { budget ->
                    val spent = expenseMap[budget.category] ?: 0.0
                    BudgetCard(budget = budget, spent = spent)
                }
            }
        }
    }

    // (新) 添加从 BudgetSettingsScreen 移过来的弹窗
    if (showMonthPicker) {
        YearMonthPicker(
            year = selectedYear,
            month = selectedMonth,
            onConfirm = { year, month ->
                selectedYear = year
                selectedMonth = month
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

@Composable
fun BudgetCard(budget: Budget, spent: Double) {
    // ... (此函数保持不变)
    val percentage = if (budget.amount > 0) (spent / budget.amount) else 0.0
    val remaining = budget.amount - spent

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                CircularProgress(percentage = percentage.toFloat())
                Text(text = "${String.format(Locale.US, "%.1f", (1 - percentage) * 100)}%", style = MaterialTheme.typography.bodyLarge)
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = budget.category, style = MaterialTheme.typography.titleMedium)
                Text(text = "剩余: ¥${remaining}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "预算: ¥${budget.amount}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "支出: ¥${spent}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CircularProgress(percentage: Float) {
    // ... (此函数保持不变)
    Canvas(modifier = Modifier.size(80.dp)) {
        drawArc(
            color = Color.LightGray,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 15f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.Blue,
            startAngle = -90f,
            sweepAngle = 360 * percentage,
            useCenter = false,
            style = Stroke(width = 15f, cap = StrokeCap.Round)
        )
    }
}
