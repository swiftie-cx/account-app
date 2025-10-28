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
// --- (修复) 添加 Imports ---
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
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var showMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedYear, selectedMonth) {
        viewModel.syncBudgetsFor(selectedYear, selectedMonth)
    }

    val budgets by viewModel.getBudgetsForMonth(selectedYear, selectedMonth).collectAsState(initial = emptyList())
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val monthlyExpenses = remember(expenses, selectedYear, selectedMonth) {
        expenses.filter {
            val expenseCalendar = Calendar.getInstance().apply { time = it.date }
            (expenseCalendar.get(Calendar.YEAR) == selectedYear &&
                    expenseCalendar.get(Calendar.MONTH) + 1 == selectedMonth &&
                    it.amount < 0)
        }
    }
    val expenseMap = remember(monthlyExpenses) {
        monthlyExpenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> abs(exp.amount) } }
    }
    val totalSpent = remember(monthlyExpenses) {
        monthlyExpenses.sumOf { abs(it.amount) }
    }
    val (totalBudgetList, categoryBudgets) = remember(budgets) {
        budgets.partition { it.category == "总预算" }
    }
    val totalBudget = totalBudgetList.firstOrNull()

    Scaffold(
        floatingActionButton = {
            Button(onClick = {
                // 现在 Routes.budgetSettingsRoute 可以被正确解析
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

            TextButton(onClick = { showMonthPicker = true }) {
                Text(
                    "${selectedYear}年${selectedMonth}月",
                    style = MaterialTheme.typography.titleLarge
                )
            }

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

    if (showMonthPicker) {
        // 现在 YearMonthPicker 可以被正确解析
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

// ... (BudgetCard 和 CircularProgress 保持不变) ...
@Composable
fun BudgetCard(budget: Budget, spent: Double) { /* ... */ }

@Composable
fun CircularProgress(percentage: Float) { /* ... */ }

// --- YearMonthPicker 函数定义已移到 YearMonthPicker.kt ---