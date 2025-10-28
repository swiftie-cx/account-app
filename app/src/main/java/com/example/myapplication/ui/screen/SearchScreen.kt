package com.example.myapplication.ui.screen

import androidx.compose.animation.AnimatedVisibility // <-- (修复) 添加导入
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows // (新) 导入
import androidx.compose.material.icons.filled.Add // For category add button
import androidx.compose.material.icons.filled.Clear // For search clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseTypeFilter
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val searchText by viewModel.searchText.collectAsState()
    val selectedType by viewModel.selectedTypeFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val allCategories = remember { listOf("全部") + expenseCategories.map { it.title } + incomeCategories.map { it.title } }

    var showCategorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp)) {
            // Search Bar
            OutlinedTextField(/* ... */)

            Spacer(Modifier.height(8.dp))

            // Type Filters
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { /* ... FilterChips ... */ }

            Spacer(Modifier.height(8.dp))

            // Category Filter Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = selectedCategory != "全部",
                    onClick = { showCategorySheet = true },
                    label = { Text(selectedCategory ?: "选择类别") }
                )
            }


            Spacer(Modifier.height(16.dp))

            // Results List
            LazyColumn {
                val groupedResults = remember(filteredExpenses) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    filteredExpenses.groupBy { dateFormat.format(it.date) }
                }

                @OptIn(ExperimentalFoundationApi::class)
                groupedResults.forEach { (dateStr, expensesOnDate) ->
                    stickyHeader {
                        // (修改) 简化 DateHeader 调用
                        DateHeader(dateStr = dateStr, dailyExpense = 0.0, dailyIncome = 0.0)
                    }
                    items(expensesOnDate, key = { it.id }) { expense ->
                        val account = accountMap[expense.accountId]
                        SearchResultItem(
                            expense = expense,
                            account = account,
                            onClick = { navController.navigate(Routes.transactionDetailRoute(expense.id)) },
                            onEditClick = { /* TODO: Navigate to Edit */ },
                            onDeleteClick = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }

    // Category Selection Bottom Sheet
    if (showCategorySheet) {
        ModalBottomSheet(/* ... */) { /* ... LazyColumn with categories ... */ }
    }
}

// Composable for displaying each search result item
@Composable
fun SearchResultItem(
    expense: Expense,
    account: Account?,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }
    val isTransfer = expense.category.startsWith("转账")
    val icon = if(isTransfer) Icons.AutoMirrored.Filled.CompareArrows else categoryIconMap[expense.category]
    val displayText = if (!expense.remark.isNullOrBlank()) expense.remark else expense.category
    // (修复) 确保显示货币
    val amountText = "${account?.currency ?: ""} ${abs(expense.amount)}"
    val amountColor = if (expense.amount < 0 && !isTransfer) MaterialTheme.colorScheme.error
    else if (expense.amount > 0 && !isTransfer) Color.Unspecified // Or income color
    else MaterialTheme.colorScheme.onSurface

    var showActions by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showActions = !showActions }
                .padding(vertical = 8.dp, horizontal = 16.dp), // Add horizontal padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = expense.category,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))
            Text(displayText ?: expense.category, modifier = Modifier.weight(1f))

            Text(
                text = amountText, // 使用修正后的 amountText
                color = amountColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Action Buttons
        AnimatedVisibility(visible = showActions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onEditClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("编辑") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onDeleteClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("删除") }
            }
        }
        Divider()
    }
}