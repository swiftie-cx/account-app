package com.example.myapplication.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = MaterialTheme.colorScheme.background, // 使用清爽背景
        topBar = {
            DetailsTopAppBar(
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onCalendarClick = { navController.navigate(Routes.CALENDAR) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 头部统计卡片作为列表的第一项
            item {
                SummaryHeader(
                    year = selectedYear,
                    month = selectedMonth,
                    expense = totalExpense,
                    income = totalIncome,
                    balance = balance,
                    onMonthClick = { showMonthPicker = true }
                )
            }

            groupedItems.forEach { (dateStr, itemsOnDate) ->
                stickyHeader {
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
                            onClick = { /* TODO */ }
                        )
                    }
                }
            }
        }
    }
}

// --- 顶部应用栏 (透明/极简) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopAppBar(
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "拾光账本",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, "设置", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onCalendarClick) {
                Icon(Icons.Default.DateRange, "日历", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent, // 透明背景，让界面更通透
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    )
}

// --- 顶部统计卡片 (现代设计：卡片悬浮 + 渐变) ---
@Composable
private fun SummaryHeader(
    year: Int, month: Int, expense: Double, income: Double, balance: Double, onMonthClick: () -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.primary

    // 创建一个柔和的渐变背景，基于主题色
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            themeColor.copy(alpha = 0.8f),
            themeColor.copy(alpha = 0.6f),
            themeColor.copy(alpha = 0.4f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = themeColor.copy(alpha = 0.3f), spotColor = themeColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 卡片底色白
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 背景装饰：可以用纯色或上面定义的渐变，这里使用更干净的纯色+Container，或者使用渐变作为装饰
            // 为了高级感，我们使用主题色的 Container 作为底色，文字用深色
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 第一行：月份切换 (胶囊样式)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .clickable(onClick = onMonthClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${year}年${month}月",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "选择月份",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 第二行：结余大字
                Column {
                    Text(
                        text = "本月结余",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = String.format("%.2f", balance),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 第三行：收支详情
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("收入", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = String.format("%.2f", income),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFFE53935), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("支出", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = String.format("%.2f", abs(expense)),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// --- 日期分割头 (更轻量) ---
@Composable
fun DateHeader(dateStr: String, dailyExpense: Double, dailyIncome: Double) {
    val displayFormat = remember { SimpleDateFormat("dd日", Locale.CHINESE) } // 只显示日期，更简洁
    val weekFormat = remember { SimpleDateFormat("EEE", Locale.CHINESE) }
    val originalFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateObj = originalFormat.parse(dateStr)

    val dayStr = dateObj?.let { displayFormat.format(it) } ?: dateStr
    val weekStr = dateObj?.let { weekFormat.format(it) } ?: ""

    // 透明背景，不干扰视线
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background // 与背景同色
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = dayStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = weekStr,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(Modifier.weight(1f))

            if (dailyIncome > 0) {
                Text(
                    text = "收 ${String.format("%.0f", dailyIncome)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
            if (dailyExpense > 0) {
                Text(
                    text = "支 ${String.format("%.0f", dailyExpense)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp), // 增加间距
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = "转账",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "转账",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${item.fromAccount.name} → ${item.toAccount.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = String.format("%.2f", item.toAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- 支出/收入列表项 (核心优化：图标容器 + 排版) ---
@Composable
private fun ExpenseItem(
    expense: Expense,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    account: Account?,
    defaultCurrency: String,
    onClick: () -> Unit
) {
    val isExpense = expense.amount < 0
    // 定义颜色：支出红，收入绿，或者跟随主题色
    // 这里使用高级灰+重点色
    val amountColor = if (isExpense) MaterialTheme.colorScheme.onSurface else Color(0xFF2E7D32) // 支出黑色，收入绿色

    // 图标背景色：使用主题色的极淡版本，或者根据收支区分
    val iconContainerColor = if (isExpense) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    } else {
        Color(0xFFE8F5E9) // 浅绿
    }

    val iconTintColor = if (isExpense) {
        MaterialTheme.colorScheme.error
    } else {
        Color(0xFF2E7D32)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp), // 舒适的间距
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 图标 (增加底色容器)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (icon != null) iconContainerColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = expense.category,
                    tint = iconTintColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.size(16.dp))

        // 2. 中间信息 (类别 + 备注/账户)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val subText = if (!expense.remark.isNullOrBlank()) expense.remark else account?.name ?: ""
            if (subText.isNotBlank()) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // 3. 金额
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format("%.2f", expense.amount), // 显示正负号
                color = amountColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 汇率换算显示
            if (account != null && account.currency != defaultCurrency) {
                val convertedAmount = ExchangeRates.convert(abs(expense.amount), account.currency, defaultCurrency)
                Text(
                    text = "≈ $defaultCurrency ${String.format(Locale.US, "%.2f", convertedAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}