package com.example.myapplication.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // (新) Import
import androidx.compose.foundation.layout.* // Use * import
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.* // Use * import
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController // (新) Import
import com.example.myapplication.data.Account
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// (保持 private)
private data class DisplayTransferItem(
    val date: Date,
    val fromAccount: Account,
    val toAccount: Account,
    val fromAmount: Double, // 负数
    val toAmount: Double   // 正数
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
// (新) 接收 NavController
fun DetailsScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    // --- 日期状态 ---
    val calendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var showMonthPicker by remember { mutableStateOf(false) }

    // --- 获取数据 ---
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    val accountMap = remember(allAccounts) {
        allAccounts.associateBy { it.id }
    }

    // --- 1. 过滤数据 ---
    val monthlyExpenses = remember(allExpenses, selectedYear, selectedMonth) {
        allExpenses.filter {
            val expenseCalendar = Calendar.getInstance().apply { time = it.date }
            expenseCalendar.get(Calendar.YEAR) == selectedYear &&
                    expenseCalendar.get(Calendar.MONTH) + 1 == selectedMonth
        }
    }

    // --- 2. 预处理列表 ---
    val displayItems = remember(monthlyExpenses, accountMap) {
        val transferExpenses = monthlyExpenses.filter { it.category.startsWith("转账") }
        val regularExpenses = monthlyExpenses.filter { !it.category.startsWith("转账") }

        val processedTransfers = transferExpenses
            .groupBy { it.date }
            .mapNotNull { (_, pair) ->
                val outTx = pair.find { it.amount < 0 }
                val inTx = pair.find { it.amount > 0 }

                if (outTx == null || inTx == null) return@mapNotNull null
                val fromAccount = accountMap[outTx.accountId]
                val toAccount = accountMap[inTx.accountId]
                if (fromAccount == null || toAccount == null) return@mapNotNull null

                DisplayTransferItem(
                    date = outTx.date,
                    fromAccount = fromAccount,
                    toAccount = toAccount,
                    fromAmount = outTx.amount,
                    toAmount = inTx.amount
                )
            }

        val allItems: List<Any> = regularExpenses + processedTransfers

        allItems.sortedByDescending {
            when (it) {
                is Expense -> it.date.time
                is DisplayTransferItem -> it.date.time
                else -> 0L
            }
        }
    }

    // --- 3. 计算总额 ---
    val (incomeList, expenseList) = remember(displayItems) {
        displayItems.filterIsInstance<Expense>().partition { it.amount > 0 }
    }
    val totalIncome = remember(incomeList) { incomeList.sumOf { it.amount } }
    val totalExpense = remember(expenseList) { expenseList.sumOf { it.amount } }
    val balance = totalIncome + totalExpense

    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    // --- 4. 分组 ---
    val groupedItems = remember(displayItems) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        displayItems.groupBy { item ->
            val date = when (item) {
                is Expense -> item.date
                is DisplayTransferItem -> item.date
                else -> Date()
            }
            dateFormat.format(date)
        }
    }

    // --- 5. 月份选择器 ---
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

    // --- 6. UI 结构 ---
    Scaffold(
        topBar = {
            DetailsTopAppBar(
                onMenuClick = { /* TODO */ },
                // (新) Navigate to Search screen
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onCalendarClick = { /* TODO */ }
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            SummaryHeader(
                year = selectedYear,
                month = selectedMonth,
                expense = totalExpense,
                income = totalIncome,
                balance = balance,
                onMonthClick = { showMonthPicker = true }
            )

            LazyColumn {
                @OptIn(ExperimentalFoundationApi::class)
                groupedItems.forEach { (dateStr, itemsOnDate) ->
                    stickyHeader {
                        // (新) Add daily summary
                        val dailyTotalExpense = itemsOnDate.filterIsInstance<Expense>().filter { it.amount < 0 && !it.category.startsWith("转账") }.sumOf { abs(it.amount) }
                        val dailyTotalIncome = itemsOnDate.filterIsInstance<Expense>().filter { it.amount > 0 && !it.category.startsWith("转账") }.sumOf { it.amount }
                        DateHeader(dateStr = dateStr, dailyExpense = dailyTotalExpense, dailyIncome = dailyTotalIncome)
                    }

                    items(itemsOnDate) { item ->
                        when (item) {
                            is Expense -> {
                                val account = accountMap[item.accountId]
                                ExpenseItem(
                                    expense = item,
                                    icon = categoryIconMap[item.category],
                                    account = account,
                                    // (新) Add onClick navigation
                                    onClick = { navController.navigate(Routes.transactionDetailRoute(item.id)) }
                                )
                            }
                            is DisplayTransferItem -> TransferItem(
                                item = item,
                                onClick = { /* TODO: Decide how to handle transfer details */ }
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- (修改) DateHeader to include daily summary ---
@Composable
fun DateHeader(dateStr: String, dailyExpense: Double, dailyIncome: Double) {
    val displayFormat = remember { SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault()) }
    val originalFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDate = originalFormat.parse(dateStr)?.let { displayFormat.format(it) } ?: dateStr

    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)) { // Slightly different background
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = displayDate, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (dailyExpense > 0) {
                Text(
                    text = "支出: ${String.format("%.2f", dailyExpense)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (dailyIncome > 0) {
                Text(
                    text = "收入: ${String.format("%.2f", dailyIncome)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Green.copy(alpha= 0.7f), // Or your income color
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// --- (修改) TransferItem signature and make clickable ---
@Composable
private fun TransferItem(
    item: DisplayTransferItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
            contentDescription = "转账",
            tint = MaterialTheme.colorScheme.primary
        )

        // 左侧文案
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.fromAccount.name} → ${item.toAccount.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(item.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 右侧金额
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "-${String.format("%.2f", kotlin.math.abs(item.fromAmount))}",
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "+${String.format("%.2f", item.toAmount)}",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
// --- (修改) ExpenseItem signature, make clickable, show remark ---
@Composable
private fun ExpenseItem(
    expense: Expense,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    account: Account?,
    onClick: () -> Unit // (新) Add onClick
) {
    val amountColor = if (expense.amount < 0) MaterialTheme.colorScheme.error else Color.Unspecified
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp) // Add horizontal padding
            .clickable(onClick = onClick), // (新) Make clickable
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp) // Increase spacing
    ) {
        if (icon != null) {
            // (新) Add background circle to icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = expense.category,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        } else {
            Spacer(Modifier.size(40.dp))
        }

        // (修改) Show Remark or Category
        val displayText = if (!expense.remark.isNullOrBlank()) {
            expense.remark
        } else {
            expense.category
        }
        Text(text = displayText, modifier = Modifier.weight(1f)) // Use displayText

        val amountText = "${account?.currency ?: ""} ${abs(expense.amount)}"
        Text(
            text = amountText,
            color = amountColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// --- TopAppBar, SummaryHeader, YearMonthPicker remain unchanged ---
@OptIn(ExperimentalMaterial3Api::class) @Composable private fun DetailsTopAppBar(onMenuClick: () -> Unit, onSearchClick: () -> Unit, onCalendarClick: () -> Unit) { /* ... */ }
@Composable private fun SummaryHeader(year: Int, month: Int, expense: Double, income: Double, balance: Double, onMonthClick: () -> Unit) { /* ... */ }
