package com.example.myapplication.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.example.myapplication.data.Budget
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    year: Int,
    month: Int,
    onDateChange: (Int, Int) -> Unit,
    defaultCurrency: String
) {
    var showMonthPicker by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, year, month) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncBudgetsFor(year, month)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val budgets by viewModel.getBudgetsForMonth(year, month).collectAsState(initial = emptyList())
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val monthlyExpenses = remember(expenses, year, month) {
        expenses.filter {
            val expenseCalendar = Calendar.getInstance().apply { time = it.date }
            (expenseCalendar.get(Calendar.YEAR) == year &&
                    expenseCalendar.get(Calendar.MONTH) + 1 == month &&
                    it.amount < 0 &&
                    !it.category.startsWith("转账") &&
                    // 【关键修改】过滤掉不计入预算的条目
                    !it.excludeFromBudget)
        }
    }

    // 计算各分类支出
    val expenseMap = remember(monthlyExpenses, defaultCurrency, accountMap) {
        monthlyExpenses.groupBy { it.category }.mapValues { (_, expenses) ->
            expenses.sumOf { exp ->
                val account = accountMap[exp.accountId]
                if (account != null) {
                    ExchangeRates.convert(abs(exp.amount), account.currency, defaultCurrency)
                } else {
                    0.0
                }
            }
        }
    }

    // 计算总支出
    val totalSpent = remember(monthlyExpenses, defaultCurrency, accountMap) {
        monthlyExpenses.sumOf { exp ->
            val account = accountMap[exp.accountId]
            if (account != null) {
                ExchangeRates.convert(abs(exp.amount), account.currency, defaultCurrency)
            } else {
                0.0
            }
        }
    }

    val (totalBudgetList, categoryBudgets) = remember(budgets) {
        budgets.partition { it.category == "总预算" }
    }
    val totalBudget = totalBudgetList.firstOrNull()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TextButton(onClick = { showMonthPicker = true }) {
                        Text(
                            "${year}年${month}月",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        navController.navigate(Routes.budgetSettingsRoute(year, month))
                    }) {
                        Text("预算设置")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. 总预算卡片
            item {
                val displayBudget = if (totalBudget != null) {
                    val converted = ExchangeRates.convert(totalBudget.amount, "CNY", defaultCurrency)
                    totalBudget.copy(amount = converted)
                } else {
                    // 如果没有总预算，临时计算一个总和用于展示
                    val sumCategoryBudgets = categoryBudgets.sumOf {
                        ExchangeRates.convert(it.amount, "CNY", defaultCurrency)
                    }
                    Budget(category = "总预算", amount = sumCategoryBudgets, year = year, month = month)
                }

                BudgetSummaryCard(
                    budget = displayBudget,
                    spent = totalSpent,
                    currency = defaultCurrency
                )
            }

            // 2. 分类预算列表
            items(categoryBudgets) { budget ->
                val spent = expenseMap[budget.category] ?: 0.0
                val convertedBudgetAmount = ExchangeRates.convert(budget.amount, "CNY", defaultCurrency)
                BudgetCategoryItem(
                    budget = budget.copy(amount = convertedBudgetAmount),
                    spent = spent,
                    currency = defaultCurrency
                )
            }
        }
    }

    if (showMonthPicker) {
        YearMonthPicker(
            year = year,
            month = month,
            onConfirm = { newYear, newMonth ->
                onDateChange(newYear, newMonth)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

// --- 顶部总览卡片 ---
@Composable
fun BudgetSummaryCard(budget: Budget, spent: Double, currency: String) {
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "progress"
    )
    val isOverBudget = spent > budget.amount

    // 颜色定义
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = if (isOverBudget) errorColor else primaryColor

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：环形进度条
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 轨道
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // 进度
                    drawArc(
                        color = indicatorColor,
                        startAngle = -90f,
                        sweepAngle = 360 * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 右侧：详细数据
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "总预算",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("已用", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.0f", spent),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if(isOverBudget) errorColor else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("剩余", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.0f", (budget.amount - spent).coerceAtLeast(0.0)),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "总额: $currency ${String.format(Locale.US, "%.0f", budget.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// --- 分类预算卡片 ---
@Composable
fun BudgetCategoryItem(budget: Budget, spent: Double, currency: String) {
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "progress"
    )
    val isOverBudget = spent > budget.amount

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val barColor = if (isOverBudget) errorColor else primaryColor

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.5.dp) // 轻微阴影
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：圆形百分比 + 名称
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if(isOverBudget) errorColor.copy(alpha = 0.1f) else trackColor,
                                CircleShape
                            )
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if(isOverBudget) errorColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budget.category,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 右侧：已用 / 预算
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "已用 ${String.format(Locale.US, "%.0f", spent)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if(isOverBudget) errorColor else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "预算 ${String.format(Locale.US, "%.0f", budget.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部：线性进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }
        }
    }
}