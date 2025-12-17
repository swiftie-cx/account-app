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
import androidx.compose.ui.res.stringResource // [新增] 引入资源引用
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增] 引入 R 类
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.ui.navigation.CategoryData
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

    // [关键修改] 获取最新的大类列表 (包含用户新增的子类)
    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    val accountMap = remember(allAccounts) {
        allAccounts.associateBy { it.id }
    }

    // [关键修改] 构建 "分类名 -> (图标, 颜色)" 的查找表
    val categoryStyleMap = remember(expenseMainCategories, incomeMainCategories) {
        val map = mutableMapOf<String, Pair<ImageVector, Color>>()
        (expenseMainCategories + incomeMainCategories).forEach { main ->
            main.subCategories.forEach { sub ->
                // 将子类名映射到：(子类图标, 大类颜色)
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

    // --- 2. 预处理列表 (转账逻辑保持不变) ---
    val displayItems = remember(monthlyExpenses, accountMap, transferTypeString) {
        val transferExpenses = monthlyExpenses.filter { it.category.startsWith(transferTypeString) }
        val regularExpenses = monthlyExpenses.filter { !it.category.startsWith(transferTypeString) }

        val processedTransfers = transferExpenses
            .groupBy { it.date }
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
                    expenseId = outTx.id,
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

    // --- 3. 计算总额 (BUG 修复区域) ---
    val allExpensesForSum = remember(monthlyExpenses, accountMap, defaultCurrency) {
        monthlyExpenses.mapNotNull { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                // BUG FIX: 转换金额到 defaultCurrency
                val convertedAmount = ExchangeRates.convert(expense.amount, account.currency, defaultCurrency)
                convertedAmount to expense.amount // 返回 (兑换后金额, 原始金额)
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

    // --- UI 结构 ---
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
                SummaryHeader(
                    year = selectedYear,
                    month = selectedMonth,
                    expense = totalExpense,
                    income = totalIncome,
                    balance = balance,
                    defaultCurrency = defaultCurrency, // [新增]
                    onMonthClick = { showMonthPicker = true }
                )
            }

            groupedItems.forEach { (dateStr, itemsOnDate) ->
                stickyHeader {
                    // BUG FIX: Daily total 同样需要进行汇率转换
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
                        defaultCurrency = defaultCurrency // [新增]
                    )
                }

                items(itemsOnDate) { item ->
                    when (item) {
                        is Expense -> {
                            val account = accountMap[item.accountId]

                            // [关键修改] 从实时 Map 中获取样式
                            val stylePair = categoryStyleMap[item.category]
                            val icon = stylePair?.first
                            // 如果找不到颜色（极少情况），回退到红/绿默认色
                            val color = stylePair?.second ?: if(item.amount < 0) Color(0xFFE53935) else Color(0xFF4CAF50)

                            ExpenseItem(
                                expense = item,
                                icon = icon,
                                categoryColor = color, // 传入颜色
                                account = account,
                                defaultCurrency = defaultCurrency,
                                onClick = { navController.navigate(Routes.transactionDetailRoute(item.id)) }
                            )
                        }
                        is DisplayTransferItem -> TransferItem(
                            item = item,
                            defaultCurrency = defaultCurrency, // [新增]
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


@Composable
private fun SummaryHeader(
    year: Int, month: Int, expense: Double, income: Double, balance: Double, defaultCurrency: String, onMonthClick: () -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = themeColor.copy(alpha = 0.3f), spotColor = themeColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .clickable(onClick = onMonthClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.month_format, year, month),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.select_month),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.month_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        // 直接传入 balance (Double 类型)，让 strings.xml 里的 %.2f 去格式化它
                        text = stringResource(R.string.currency_amount_format, defaultCurrency, balance),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.income_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = stringResource(R.string.amount_format, income),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFFE53935), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.expense_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = stringResource(R.string.amount_format, abs(expense)),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
            // 【修改】标题栏：增加手续费标签
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
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), // 浅色背景
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.fee_format, String.format("%.2f", convertedFee)),
                            style = MaterialTheme.typography.labelSmall, // 小字体
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

        // 显示的是到账金额
        // BUG FIX: 到账金额也需要转换到默认货币进行显示
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

// [关键修改] ExpenseItem: 接收外部传入的 categoryColor，不再使用静态的 CategoryHelper
@Composable
private fun ExpenseItem(
    expense: Expense,
    icon: ImageVector?,
    categoryColor: Color, // 新增参数
    account: Account?,
    defaultCurrency: String,
    onClick: () -> Unit
) {
    val isExpense = expense.amount < 0
    // 金额颜色保持 红/绿
    val amountColor = if (isExpense) Color(0xFFE53935) else Color(0xFF4CAF50)

    // 图标背景使用大类颜色的浅色版，图标使用大类颜色
    val iconContainerColor = categoryColor.copy(alpha = 0.15f)
    val iconTintColor = categoryColor

    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }

    // BUG FIX: 修正显示金额，使其始终显示 defaultCurrency
    val displayAmount = if (account != null) {
        ExchangeRates.convert(expense.amount, account.currency, defaultCurrency)
    } else {
        expense.amount // Fallback
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
            // BUG FIX: 显示已经转换的金额`
            Text(
                text = stringResource(R.string.amount_format, displayAmount),
                color = amountColor, // 金额颜色
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // 兑换提示 (如果当前交易的账户货币不是默认货币)
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