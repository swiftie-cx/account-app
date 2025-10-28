package com.example.myapplication.ui.screen

// (新) 添加 import
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// (新) 添加 import
import androidx.navigation.NavHostController
import com.example.myapplication.data.Budget
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
// (移除了不再需要的 import)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController, // (修复) 重新添加 navController
    year: Int,
    month: Int
) {

    LaunchedEffect(year, month) {
        viewModel.syncBudgetsFor(year, month)
    }

    val budgets by viewModel.getBudgetsForMonth(year, month).collectAsState(initial = emptyList())

    // (修复) 确保 budgetMap 存储的是 Budget 对象，而不只是 Double
    val budgetMap = remember(budgets) {
        budgets.associateBy { it.category }
    }

    val expenseCategoryTitles = remember {
        expenseCategories.map { it.title }
    }

    val allBudgetableCategories = remember {
        listOf("总预算") + expenseCategoryTitles
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "${year}年${month}月预算设置",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
        )

        LazyColumn {
            items(allBudgetableCategories) { categoryTitle ->
                BudgetItemRow( // <-- 这个函数现在会在下面定义
                    title = categoryTitle,
                    // (修复) 从 budget.amount 获取金额
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
            // (修复) 从 budget.amount 获取金额
            var amount by remember { mutableStateOf(budgetMap[editingCategory]?.amount?.toString() ?: "0") }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "设置 ${editingCategory} 的预算",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
                Text(
                    amount,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                NumericKeyboard(
                    onNumberClick = { if (amount == "0") amount = it else amount += it },
                    onBackspaceClick = { amount = if (amount.length > 1) amount.dropLast(1) else "0" },
                    onDateClick = { /* Not used here */ },
                    onDoneClick = {
                        val newAmount = amount.toDoubleOrNull() ?: 0.0

                        // (修复) 确保我们传递了正确的 id 来进行更新
                        val existingBudget = budgetMap[editingCategory!!]
                        val budget = Budget(
                            id = existingBudget?.id ?: 0, // <-- 修复 id=0 的 bug
                            category = editingCategory!!,
                            amount = newAmount,
                            year = year,
                            month = month
                        )
                        viewModel.saveBudget(budget, expenseCategoryTitles)

                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }
                )
            }
        }
    }
}

// (新) 添加缺失的 BudgetItemRow Composable
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