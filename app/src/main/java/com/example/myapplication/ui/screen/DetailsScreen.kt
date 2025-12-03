package com.example.myapplication.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailsScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    defaultCurrency: String
) {
    // --- 日期状态 ---
    val calendar = Calendar.getInstance()
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
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
                onMenuClick = { /* TODO: 实现菜单点击逻辑 */ },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onCalendarClick = { navController.navigate(Routes.CALENDAR) }
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
                groupedItems.forEach { (dateStr, itemsOnDate) ->
                    stickyHeader {
                        // 添加日总收支
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
                                    defaultCurrency = defaultCurrency,
                                    onClick = { navController.navigate(Routes.transactionDetailRoute(item.id)) }
                                )
                            }
                            is DisplayTransferItem -> TransferItem(
                                item = item,
                                onClick = { /* TODO: 决定转账记录的点击行为 */ }
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- 顶部应用栏 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopAppBar(
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text("魔法记账", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "菜单") }
        },
        actions = {
            IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, "搜索") }
            IconButton(onClick = onCalendarClick) { Icon(Icons.Default.DateRange, "日历") }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

// --- 黄色摘要卡片 ---
@Composable
private fun SummaryHeader(
    year: Int, month: Int, expense: Double, income: Double, balance: Double, onMonthClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "${year}年", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(text = "支出", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Start)
                Text(text = "收入", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Start)
                Text(text = "结余", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Start)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onMonthClick, modifier = Modifier.weight(1f).padding(start = 0.dp)) {
                    Text(
                        text = "${month}月",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "选择月份",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(text = String.format("%.2f", abs(expense)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Start)
                Text(text = String.format("%.2f", income), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Start)
                Text(text = String.format("%.2f", balance), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Start)
            }
        }
    }
}


// --- 日期和日汇总 Header ---
@Composable
fun DateHeader(dateStr: String, dailyExpense: Double, dailyIncome: Double) {
    val displayFormat = remember { SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault()) }
    val originalFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDate = originalFormat.parse(dateStr)?.let { displayFormat.format(it) } ?: dateStr

    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)) {
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
                    color = Color.Green.copy(alpha= 0.7f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// --- 转账列表项 ---
@Composable
private fun TransferItem(item: DisplayTransferItem, onClick: () -> Unit) {
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
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(8.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = "${item.fromAccount.name} → ${item.toAccount.name}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Column(horizontalAlignment = Alignment.End) {
            val fromText = "${item.fromAccount.currency} ${String.format("%.2f", abs(item.fromAmount))}"
            val toText = "→ ${item.toAccount.currency} ${String.format("%.2f", item.toAmount)}"
            Text(
                text = fromText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = toText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// --- 支出/收入列表项 ---
@Composable
private fun ExpenseItem(
    expense: Expense,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    account: Account?,
    defaultCurrency: String,
    onClick: () -> Unit
) {
    val amountColor = if (expense.amount < 0) MaterialTheme.colorScheme.error else Color.Unspecified

    val needsConversion = account != null && account.currency != defaultCurrency

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
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

        // 显示备注或类别
        val displayText = if (!expense.remark.isNullOrBlank()) expense.remark else expense.category
        Text(text = displayText ?: expense.category, modifier = Modifier.weight(1f))

        // 右侧金额显示区域
        Column(horizontalAlignment = Alignment.End) {
            // 原金额
            val amountText = "${account?.currency ?: ""} ${String.format("%.2f", abs(expense.amount))}"
            Text(
                text = amountText,
                color = amountColor,
                style = MaterialTheme.typography.bodyLarge
            )

            // 如果币种不同，显示折算金额
            if (needsConversion) {
                val convertedAmount = ExchangeRates.convert(abs(expense.amount), account!!.currency, defaultCurrency)
                Text(
                    text = "≈ $defaultCurrency ${String.format(Locale.US, "%.2f", convertedAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}