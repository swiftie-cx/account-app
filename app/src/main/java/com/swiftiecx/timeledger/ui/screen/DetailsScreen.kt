package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.RecordType
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.text.startsWith

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailsScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    defaultCurrency: String
) {
    // --- 资源获取 ---
    val transferTypeString = stringResource(R.string.type_transfer)
    val appName = stringResource(R.string.app_name)

    // --- 日期状态 ---
    val calendar = Calendar.getInstance()
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var showMonthPicker by remember { mutableStateOf(false) }

    // --- 获取数据 (监听 ViewModel 的实时数据) ---
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    // 获取最新的大类列表
    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    val accountMap = remember(allAccounts) {
        allAccounts.associateBy { it.id }
    }

    // 构建 "分类名 -> (图标, 颜色)" 的查找表
    val categoryStyleMap = remember(expenseMainCategories, incomeMainCategories) {
        val map = mutableMapOf<String, Pair<ImageVector, Color>>()
        (expenseMainCategories + incomeMainCategories).forEach { main ->
            main.subCategories.forEach { sub ->
                map[sub.key] = sub.icon to main.color
            }
        }
        map
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
    // ✅ 基于 recordType/transferId 识别并合并转账（不再依赖 category 文本前缀）
    val displayItems = remember(monthlyExpenses, accountMap) {
        val transferExpenses = monthlyExpenses.filter { it.recordType == RecordType.TRANSFER }
        val regularExpenses = monthlyExpenses.filter { it.recordType == RecordType.INCOME_EXPENSE }

        val processedTransfers = transferExpenses
            .groupBy { it.transferId ?: it.date.time } // 优先用 transferId，旧数据回退到时间戳
            .mapNotNull { (_, pair) ->
                val outTx = pair.find { it.amount < 0 }
                val inTx = pair.find { it.amount > 0 }

                if (outTx == null || inTx == null) return@mapNotNull null
                val fromAccount = accountMap[outTx.accountId]
                val toAccount = accountMap[inTx.accountId]
                if (fromAccount == null || toAccount == null) return@mapNotNull null

                val feeRaw = abs(outTx.amount) - abs(inTx.amount)
                val fee = if (feeRaw < 0.01) 0.0 else feeRaw

                DisplayTransferItem(
                    expenseId = outTx.id, // 用转出那条作为详情入口
                    date = outTx.date,
                    fromAccount = fromAccount,
                    toAccount = toAccount,
                    fromAmount = outTx.amount,
                    toAmount = inTx.amount,
                    fee = fee
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

    val allExpensesForSum = remember(monthlyExpenses, accountMap, defaultCurrency) {
        monthlyExpenses
            .filter { it.recordType == com.swiftiecx.timeledger.data.RecordType.INCOME_EXPENSE }
            .mapNotNull { expense ->
                val account = accountMap[expense.accountId]
                if (account != null) {
                    val convertedAmount = ExchangeRates.convert(expense.amount, account.currency, defaultCurrency)
                    convertedAmount to expense.amount
                } else {
                    null
                }
            }
    }

    val totalIncome = remember(allExpensesForSum) {
        allExpensesForSum.filter { it.first > 0 }.sumOf { it.first }
    }
    val totalExpense = remember(allExpensesForSum) {
        allExpensesForSum.filter { it.first < 0 }.sumOf { it.first }
    }
    val balance = totalIncome + totalExpense

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DetailsTopAppBar(
                appName = appName,
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
            item {
                // 使用优化后的紧凑版 SummaryHeader
                SummaryHeader(
                    year = selectedYear,
                    month = selectedMonth,
                    expense = totalExpense,
                    income = totalIncome,
                    balance = balance,
                    defaultCurrency = defaultCurrency,
                    onMonthClick = { showMonthPicker = true }
                )
            }

            groupedItems.forEach { (dateStr, itemsOnDate) ->
                stickyHeader {
                    val dailyTotalExpense = itemsOnDate.filterIsInstance<Expense>()
                        .filter { it.amount < 0 && !it.category.startsWith(transferTypeString) }
                        .sumOf { expense ->
                            val account = accountMap[expense.accountId]
                            if (account != null) ExchangeRates.convert(abs(expense.amount), account.currency, defaultCurrency) else 0.0
                        }
                    val dailyTotalIncome = itemsOnDate.filterIsInstance<Expense>()
                        .filter { it.amount > 0 && !it.category.startsWith(transferTypeString) }
                        .sumOf { expense ->
                            val account = accountMap[expense.accountId]
                            if (account != null) ExchangeRates.convert(expense.amount, account.currency, defaultCurrency) else 0.0
                        }
                    DateHeader(
                        dateStr = dateStr,
                        dailyExpense = dailyTotalExpense,
                        dailyIncome = dailyTotalIncome,
                        defaultCurrency = defaultCurrency
                    )
                }

                items(itemsOnDate) { item ->
                    when (item) {
                        is Expense -> {
                            val account = accountMap[item.accountId]
                            val stylePair = categoryStyleMap[item.category]
                            val icon = stylePair?.first ?: IconMapper.getIcon(item.category)
                            val color = stylePair?.second ?: if(item.amount < 0) Color(0xFFE53935) else Color(0xFF4CAF50)

                            ExpenseItem(
                                expense = item,
                                icon = icon,
                                categoryColor = color,
                                account = account,
                                defaultCurrency = defaultCurrency,
                                onClick = { navController.navigate(Routes.transactionDetailRoute(item.id)) }
                            )
                        }
                        is DisplayTransferItem -> TransferItem(
                            item = item,
                            defaultCurrency = defaultCurrency,
                            onClick = {
                                navController.navigate(Routes.transactionDetailRoute(item.expenseId))
                            }
                        )
                    }
                }
            }
        }
    }
}

// ✅ [优化后] 极简紧凑版 SummaryHeader
@Composable
private fun SummaryHeader(
    year: Int, month: Int, expense: Double, income: Double, balance: Double, defaultCurrency: String, onMonthClick: () -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = themeColor.copy(alpha = 0.3f), spotColor = themeColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        // 使用 Row 实现左右布局
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // 左右两端对齐
            verticalAlignment = Alignment.CenterVertically // 垂直居中
        ) {
            // --- 左侧：月份选择 + 结余 ---
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 月份选择胶囊
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .clickable(onClick = onMonthClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.month_format, year, month),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.select_month),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // 结余显示
                Column {
                    Text(
                        text = stringResource(R.string.month_balance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.currency_amount_format, defaultCurrency, balance),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // --- 右侧：收入与支出 (垂直排列) ---
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp) // 收入和支出之间的间距
            ) {
                // 收入
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.income_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.amount_format, income),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 支出
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFE53935), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.expense_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.amount_format, abs(expense)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DateHeader(dateStr: String, dailyExpense: Double, dailyIncome: Double, defaultCurrency: String) {
    val displayFormat = remember { SimpleDateFormat("dd日", Locale.getDefault()) }
    val weekFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val originalFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateObj = originalFormat.parse(dateStr)

    val dayStr = dateObj?.let { displayFormat.format(it) } ?: dateStr
    val weekStr = dateObj?.let { weekFormat.format(it) } ?: ""
    val currencyFormat = stringResource(R.string.amount_no_decimal_format)


    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
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
                    text = stringResource(R.string.daily_income_format, String.format(currencyFormat, dailyIncome)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
            if (dailyExpense > 0) {
                Text(
                    text = stringResource(R.string.daily_expense_format, String.format(currencyFormat, dailyExpense)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransferItem(item: DisplayTransferItem, defaultCurrency: String, onClick: () -> Unit) {
    val transferColor = MaterialTheme.colorScheme.primary
    val feeAmount = abs(item.fee)
    val convertedFee = ExchangeRates.convert(feeAmount, item.fromAccount.currency, defaultCurrency)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(transferColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = stringResource(R.string.type_transfer),
                modifier = Modifier.size(22.dp),
                tint = transferColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.transfer_in_app),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (item.fee > 0.01) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.fee_format, String.format("%.2f", convertedFee)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.fromAccount.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item.toAccount.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val convertedToAmount = ExchangeRates.convert(item.toAmount, item.toAccount.currency, defaultCurrency)
        Text(
            text = stringResource(R.string.amount_format, convertedToAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopAppBar(
    appName: String,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                appName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, stringResource(R.string.search), tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onCalendarClick) {
                Icon(Icons.Default.DateRange, stringResource(R.string.calendar_nav_title), tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    )
}

@Composable
private fun ExpenseItem(
    expense: Expense,
    icon: ImageVector?,
    categoryColor: Color,
    account: Account?,
    defaultCurrency: String,
    onClick: () -> Unit
) {
    val isExpense = expense.amount < 0
    val amountColor = if (isExpense) Color(0xFFE53935) else Color(0xFF4CAF50)

    val iconContainerColor = categoryColor.copy(alpha = 0.15f)
    val iconTintColor = categoryColor

    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }

    val displayAmount = if (account != null) {
        ExchangeRates.convert(expense.amount, account.currency, defaultCurrency)
    } else {
        expense.amount
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (icon != null) iconContainerColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = expense.category,
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.size(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = CategoryData.getDisplayName(expense.category, LocalContext.current),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            val subText = buildString {
                append(dateFormat.format(expense.date))
                if (!expense.remark.isNullOrBlank()) {
                    append(stringResource(R.string.transaction_remark_format, expense.remark!!))
                } else if (account != null) {
                    append(stringResource(R.string.transaction_account_format, account.name))
                }
            }
            Text(
                text = subText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.amount_format, displayAmount),
                color = amountColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            if (account != null && account.currency != defaultCurrency) {
                val absOriginalAmount = abs(expense.amount)
                Text(
                    text = stringResource(R.string.original_amount_approx_format, account.currency, String.format(Locale.US, "%.2f", absOriginalAmount)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}