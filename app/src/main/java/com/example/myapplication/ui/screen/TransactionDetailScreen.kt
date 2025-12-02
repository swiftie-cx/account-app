package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    expenseId: Long?
) {
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    // Find the specific expense based on ID
    val expense = remember(expenses, expenseId) {
        expenses.find { it.id == expenseId }
    }
    val account = remember(expense, accountMap) {
        expense?.let { accountMap[it.accountId] }
    }

    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }
    val icon = expense?.let { categoryIconMap[it.category] }

    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault()) }
    val shortDateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            // Bottom buttons for Edit/Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        expense?.let {
                            navController.navigate("add_transaction?expenseId=${it.id}")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("编辑")
                }
                Button(
                    onClick = {
                        expense?.let {
                            viewModel.deleteExpense(it)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            }
        }
    ) { innerPadding ->
        if (expense == null) {
            // Show loading or error state if expense not found
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("加载中或未找到记录...")
            }
        } else {
            // Display details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Center icon and category
            ) {
                // Icon and Category Name
                if (icon != null) {
                    Box( // Add background circle
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = expense.category,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxSize() // Fill the box
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(expense.category, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(24.dp))
                }

                // Details List
                Column(modifier = Modifier.fillMaxWidth()) { // Align details to start
                    DetailRow(label = "类型", value = if (expense.amount < 0) "支出" else "收入") // Simplified type
                    DetailRow(label = "金额", value = "${account?.currency ?: ""} ${abs(expense.amount)}")
                    DetailRow(label = "日期", value = shortDateFormat.format(expense.date))
                    DetailRow(label = "账户", value = account?.name ?: "未知账户") // Display account name
                    expense.remark?.let { DetailRow(label = "备注", value = it) }

                    // Add creation time if needed
                    Text(
                        text = "(添加于 ${dateFormat.format(expense.date)})", // Assuming date holds creation time for now
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), // Increase padding
        verticalAlignment = Alignment.Top // Align label top if value wraps
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp) // Fixed width for labels
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}