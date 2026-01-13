package com.swiftiecx.timeledger.ui.feature.transaction.screen

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.RecordType
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.common.YearMonthPicker
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.find
import kotlin.math.abs

data class DailySummary(val income: Double, val expense: Double, val currency: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    defaultCurrency: String
) {
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val budgets by viewModel
        .getBudgetsForMonth(currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH) + 1)
        .collectAsState(initial = emptyList())

    val totalBudget = budgets.find { it.category == "总预算" }

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    // --- 数据计算逻辑 ---
    val dailySummaries = remember(allExpenses, currentMonth, defaultCurrency, accountMap) {
        val month = currentMonth.get(Calendar.MONTH)
        val year = currentMonth.get(Calendar.YEAR)

        allExpenses
            .filter { expense ->
                val cal = Calendar.getInstance().apply { time = expense.date }
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }
            .filter { it.recordType == RecordType.INCOME_EXPENSE }
            .groupBy {
                val cal = Calendar.getInstance().apply { time = it.date }
                cal.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { (_, expenses) ->
                val income = expenses.filter { it.amount > 0 }.sumOf {
                    val account = accountMap[it.accountId]
                    if (account != null) ExchangeRates.convert(it.amount, account.currency, defaultCurrency) else 0.0
                }
                val expense = expenses.filter { it.amount < 0 }.sumOf {
                    val account = accountMap[it.accountId]
                    if (account != null) ExchangeRates.convert(abs(it.amount), account.currency, defaultCurrency) else 0.0
                }
                DailySummary(income, expense, defaultCurrency)
            }
    }

    val monthlySummary = remember(dailySummaries) {
        var totalInc = 0.0
        var totalExp = 0.0
        dailySummaries.values.forEach {
            totalInc += it.income
            totalExp += it.expense
        }
        totalInc to totalExp
    }

    if (showMonthPicker) {
        YearMonthPicker(
            year = currentMonth.get(Calendar.YEAR),
            month = currentMonth.get(Calendar.MONTH) + 1,
            onConfirm = { year, month ->
                val newCal = Calendar.getInstance()
                newCal.set(Calendar.YEAR, year)
                newCal.set(Calendar.MONTH, month - 1)
                newCal.set(Calendar.DAY_OF_MONTH, 1)
                currentMonth = newCal
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CalendarTopAppBar(
                calendar = currentMonth,
                onPrevMonth = { currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } },
                onNextMonth = { currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } },
                onMonthClick = { showMonthPicker = true },
                onBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ADD_TRANSACTION) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. 月度汇总卡片（已改：跟明细页一致的“surface + primaryContainer 0.75f + 黑字”）
            MonthlyOverviewCard(
                income = monthlySummary.first,
                expense = monthlySummary.second,
                currency = defaultCurrency
            )

            // 2. 日历网格
            val convertedTotalBudget = totalBudget?.let {
                ExchangeRates.convert(it.amount, "CNY", defaultCurrency)
            } ?: 0.0

            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    CalendarGrid(
                        calendar = currentMonth,
                        summaries = dailySummaries,
                        monthlyBudget = convertedTotalBudget,
                        navController = navController
                    )
                }
            }

            // 3. 底部预算信息
            BudgetInfoCard(
                monthlyBudget = convertedTotalBudget,
                daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH),
                currency = defaultCurrency
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- 组件：顶部导航（已改：跟明细页统一的“浅黄胶囊 + 黑字”） ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopAppBar(
    calendar: Calendar,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    onBack: () -> Unit
) {
    val locale = Locale.getDefault()
    val monthPattern = remember(locale) {
        DateFormat.getBestDateTimePattern(locale, "yMMM")
    }
    val monthFormat = remember(locale, monthPattern) {
        SimpleDateFormat(monthPattern, locale)
    }

    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            // 外层用 surface，内层用 primaryContainer 0.75f，黑字
            val themeColor = MaterialTheme.colorScheme.primary
            Card(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .shadow(
                        6.dp,
                        RoundedCornerShape(50),
                        ambientColor = themeColor.copy(alpha = 0.18f),
                        spotColor = themeColor.copy(alpha = 0.18f)
                    ),
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.prev_month),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clickable(onClick = onMonthClick)
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monthFormat.format(calendar.time),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onNextMonth, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.next_month),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    )
}

// --- 组件：月度汇总卡片（已改：跟明细页一致） ---
@Composable
fun MonthlyOverviewCard(income: Double, expense: Double, currency: String) {
    val balance = income - expense
    val themeColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                8.dp,
                RoundedCornerShape(20.dp),
                ambientColor = themeColor.copy(alpha = 0.22f),
                spotColor = themeColor.copy(alpha = 0.22f)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.calendar_month_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.US, "%.2f", balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${stringResource(R.string.income_short)}  ${String.format(Locale.US, "%.0f", income)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFE53935), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${stringResource(R.string.expense_short)}  ${String.format(Locale.US, "%.0f", expense)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// --- 组件：日历网格 ---
@Composable
private fun CalendarGrid(
    calendar: Calendar,
    summaries: Map<Int, DailySummary>,
    monthlyBudget: Double,
    navController: NavHostController
) {
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth =
        (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    val emptyDays = (firstDayOfMonth - calendar.firstDayOfWeek + 7) % 7
    val dailyBudget = if (daysInMonth > 0) monthlyBudget / daysInMonth else 0.0

    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
            today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
    val todayDay = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else -1

    val weekLabels = listOf(
        stringResource(R.string.weekday_sun),
        stringResource(R.string.weekday_mon),
        stringResource(R.string.weekday_tue),
        stringResource(R.string.weekday_wed),
        stringResource(R.string.weekday_thu),
        stringResource(R.string.weekday_fri),
        stringResource(R.string.weekday_sat)
    )

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            for (dayLabel in weekLabels) {
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val totalSlots = emptyDays + daysInMonth
        val rows = (totalSlots + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    if (index < emptyDays || index >= totalSlots) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(0.85f).padding(2.dp))
                    } else {
                        val day = index - emptyDays + 1
                        val summary = summaries[day]
                        Box(modifier = Modifier.weight(1f).padding(2.dp)) {
                            DayCell(
                                day = day,
                                summary = summary,
                                dailyBudget = dailyBudget,
                                calendar = calendar,
                                navController = navController,
                                isToday = (day == todayDay)
                            )
                        }
                    }
                }
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
    navController: NavHostController,
    isToday: Boolean
) {
    val expense = summary?.expense ?: 0.0
    val income = summary?.income ?: 0.0

    val isOverBudget = expense > dailyBudget && dailyBudget > 0
    val hasExpense = expense > 0

    val backgroundColor = when {
        isOverBudget -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        hasExpense -> Color(0xFFE8F5E9)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isToday) 2.dp else 0.dp
    val textColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    val dayCalendar = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }

    Column(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { navController.navigate(Routes.dailyDetailsRoute(dayCalendar.timeInMillis)) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )

        Spacer(Modifier.weight(1f))

        if (income > 0) {
            Text(
                text = String.format(Locale.US, "%.0f", income),
                color = Color(0xFF388E3C),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                maxLines = 1
            )
        }
        if (expense > 0) {
            Text(
                text = String.format(Locale.US, "%.0f", expense),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFFE53935),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

// --- 组件：底部预算信息卡片 ---
@Composable
private fun BudgetInfoCard(monthlyBudget: Double, daysInMonth: Int, currency: String) {
    val dailyBudget = if (daysInMonth > 0) monthlyBudget / daysInMonth else 0.0

    if (dailyBudget > 0) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.daily_budget_label)}  $currency ${String.format(Locale.US, "%.0f", dailyBudget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFE8F5E9), CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.budget_status_normal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.width(12.dp))

                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.budget_status_over),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
