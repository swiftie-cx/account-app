package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import com.example.myapplication.ui.navigation.Routes
data class DailySummary(val income: Double, val expense: Double, val currency: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: ExpenseViewModel, navController: NavHostController, defaultCurrency: String) {
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }

    val budgets by viewModel.getBudgetsForMonth(currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH) + 1).collectAsState(initial = emptyList())
    val totalBudget = budgets.find { it.category == "总预算" }
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val dailySummaries = remember(allExpenses, currentMonth, defaultCurrency, accountMap) {
        val month = currentMonth.get(Calendar.MONTH)
        val year = currentMonth.get(Calendar.YEAR)

        allExpenses
            .filter { expense ->
                val cal = Calendar.getInstance().apply { time = expense.date }
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }
            .groupBy {
                val cal = Calendar.getInstance().apply { time = it.date }
                cal.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { (_, expenses) ->
                val income = expenses.filter { it.amount > 0 }.sumOf { 
                    val account = accountMap[it.accountId]
                    if (account != null) {
                        ExchangeRates.convert(it.amount, account.currency, defaultCurrency)
                    } else { 0.0 }
                }
                val expense = expenses.filter { it.amount < 0 }.sumOf { 
                    val account = accountMap[it.accountId]
                    if (account != null) {
                        ExchangeRates.convert(abs(it.amount), account.currency, defaultCurrency)
                    } else { 0.0 }
                 }
                DailySummary(income, expense, defaultCurrency)
            }
    }

    Scaffold(
        topBar = {
            CalendarTopAppBar(
                calendar = currentMonth,
                onPrevMonth = {
                    currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                },
                onNextMonth = {
                    currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                },
                onBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.ADD_TRANSACTION) }) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val convertedTotalBudget = totalBudget?.let { 
                ExchangeRates.convert(it.amount, "CNY", defaultCurrency)
            } ?: 0.0
            CalendarGrid(currentMonth, dailySummaries, convertedTotalBudget, navController)
            BudgetSummary(convertedTotalBudget, currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH), defaultCurrency)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopAppBar(calendar: Calendar, onPrevMonth: () -> Unit, onNextMonth: () -> Unit, onBack: () -> Unit) {
    val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())

    TopAppBar(
        title = { Text("日历") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, "上个月") }
                Text(monthFormat.format(calendar.time), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, "下个月") }
            }
        }
    )
}

@Composable
private fun CalendarGrid(
    calendar: Calendar,
    summaries: Map<Int, DailySummary>,
    monthlyBudget: Double,
    navController: NavHostController
) {
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    val emptyDays = (firstDayOfMonth - calendar.firstDayOfWeek + 7) % 7
    val dailyBudget = if (daysInMonth > 0) monthlyBudget / daysInMonth else 0.0

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
            val days = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            for (day in days) {
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(emptyDays) { Spacer(Modifier) }

            items(daysInMonth) { day ->
                val summary = summaries[day + 1]
                DayCell(day = day + 1, summary = summary, dailyBudget = dailyBudget, calendar = calendar, navController = navController)
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    summary: DailySummary?,
    dailyBudget: Double,
    calendar: Calendar,
    navController: NavHostController
) {
    val isOverBudget = summary?.expense ?: 0.0 > dailyBudget && dailyBudget > 0
    val backgroundColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface

    val dayCalendar = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable { navController.navigate(Routes.dailyDetailsRoute(dayCalendar.timeInMillis)) },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = day.toString(), style = MaterialTheme.typography.bodyMedium)
            if (summary != null) {
                if (summary.income > 0) {
                    Text(
                        text = String.format(Locale.US, "%.0f", summary.income),
                        color = Color.Green.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                if (summary.expense > 0) {
                    Text(
                        text = String.format(Locale.US, "%.0f", summary.expense),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetSummary(monthlyBudget: Double, daysInMonth: Int, currency: String) {
    val dailyBudget = if (daysInMonth > 0) monthlyBudget / daysInMonth else 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "日均预算: $currency ${String.format("%.2f", dailyBudget)}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape))
            Spacer(Modifier.width(4.dp))
            Text("超出预算", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.surface, CircleShape).border(BorderStroke(1.dp, Color.Gray)))
            Spacer(Modifier.width(4.dp))
            Text("未超预算", style = MaterialTheme.typography.labelSmall)
        }
    }
}
