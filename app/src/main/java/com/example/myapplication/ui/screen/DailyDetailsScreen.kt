package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import com.example.myapplication.ui.navigation.Routes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailsScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    dateMillis: Long
) {
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = dateMillis } }
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
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
        topBar = {
            TopAppBar(
                title = { Text(topBarFormatter.format(calendar.time)) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("${Routes.ADD_TRANSACTION}?dateMillis=$dateMillis") }) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            DailySummaryHeader(calendar, totalExpense, totalIncome)
            LazyColumn {
                items(dailyExpenses) { expense ->
                    val icon = categoryIconMap[expense.category]
                    if (icon != null) {
                        DailyTransactionItem(
                            expense = expense,
                            icon = icon,
                            onClick = { navController.navigate(Routes.transactionDetailRoute(expense.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySummaryHeader(calendar: Calendar, totalExpense: Double, totalIncome: Double) {
    val formatter = remember { SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(formatter.format(calendar.time), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text("支出: ${String.format(Locale.US, "%.2f", abs(totalExpense))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(8.dp))
        Text("收入: ${String.format(Locale.US, "%.2f", totalIncome)}", style = MaterialTheme.typography.bodyMedium, color = Color.Green.copy(alpha = 0.8f))
    }
}

@Composable
private fun DailyTransactionItem(
    expense: Expense,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = expense.category, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(expense.category, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = String.format(Locale.US, "%.2f", expense.amount),
            style = MaterialTheme.typography.bodyLarge,
            color = if (expense.amount < 0) MaterialTheme.colorScheme.error else Color.Green.copy(alpha = 0.8f)
        )
    }
}
