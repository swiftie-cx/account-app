package com.example.myapplication.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // 使用 * 导入
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // 确保导入 items
import androidx.compose.material3.* // 使用 * 导入
import androidx.compose.runtime.* // 使用 * 导入
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Budget
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    year: Int,
    month: Int
) {

    LaunchedEffect(year, month) {
        viewModel.syncBudgetsFor(year, month)
    }

    val budgets by viewModel.getBudgetsForMonth(year, month).collectAsState(initial = emptyList())
    val budgetMap = remember(budgets) { budgets.associateBy { it.category } }
    val expenseCategoryTitles = remember { expenseCategories.map { it.title } }
    val allBudgetableCategories = remember { listOf("总预算") + expenseCategoryTitles }

    var showBottomSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "${year}年${month}月预算设置",
            style = MaterialTheme.typography.headlineLarge
        )

        LazyColumn {
            items(allBudgetableCategories) { categoryTitle ->
                BudgetItemRow(
                    title = categoryTitle,
                    budgetAmount = budgetMap[categoryTitle]?.amount,
                    onEditClick = {
                        editingCategory = categoryTitle
                        showBottomSheet = true
                    }
                )
            }
        }
    }


    if (showBottomSheet && editingCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            var amount by remember { mutableStateOf(budgetMap[editingCategory]?.amount?.toString() ?: "0") }
            var isCalculation by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "设置 ${editingCategory} 的预算",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    amount,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                NumericKeyboard(
                    onNumberClick = { if (amount == "0") amount = it else amount += it },
                    onOperatorClick = { operator ->
                        amount += " $operator "
                        isCalculation = true
                    },
                    onBackspaceClick = {
                        amount = if (amount.length > 1) amount.dropLast(1) else "0"
                        isCalculation = amount.contains("+") || amount.contains("-")
                    },
                    onDateClick = { /* Not used */ },
                    onDoneClick = {
                        val newAmount = amount.toDoubleOrNull() ?: 0.0
                        val existingBudget = budgetMap[editingCategory!!]
                        val budget = Budget(
                            id = existingBudget?.id ?: 0,
                            category = editingCategory!!,
                            amount = newAmount,
                            year = year,
                            month = month
                        )
                        viewModel.saveBudget(budget, expenseCategoryTitles)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showBottomSheet = false
                        }
                    },
                    onEqualsClick = {
                        try {
                            val result = evaluateExpression(amount)
                            amount = result.toBigDecimal().toPlainString()
                        } catch (e: Exception) {
                            amount = "Error"
                        }
                        isCalculation = false
                    },
                    isCalculation = isCalculation
                )
            }
        }
    }
}

@Composable
fun BudgetItemRow(title: String, budgetAmount: Double?, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title)
        Text(text = budgetAmount?.toString() ?: "编辑")
    }
}

// --- YearMonthPicker 函数定义已删除 ---