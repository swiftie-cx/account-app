package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource // [新增] 引入资源引用
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增] 引入 R 类
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.find

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    accountId: Long,
    defaultCurrency: String
) {
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    // [新增] 1. 获取实时分类数据
    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    // [新增] 2. 构建样式查找表
    val categoryStyleMap = remember(expenseMainCategories, incomeMainCategories) {
        val map = mutableMapOf<String, Pair<ImageVector, Color>>()
        (expenseMainCategories + incomeMainCategories).forEach { main ->
            main.subCategories.forEach { sub ->
                map[sub.title] = sub.icon to main.color
            }
        }
        map
    }

    // 获取当前账户
    val account = remember(allAccounts, accountId) {
        allAccounts.find { it.id == accountId }
    }

    // 筛选该账户的流水 (收支 + 转账)
    val accountTransactions = remember(allExpenses, accountId) {
        allExpenses
            .filter { it.accountId == accountId }
            .sortedByDescending { it.date.time } // 按日期倒序
    }

    // 计算当前余额
    val currentBalance = remember(account, accountTransactions) {
        if (account != null) {
            val transactionsSum = accountTransactions.sumOf { it.amount }
            account.initialBalance + transactionsSum
        } else {
            0.0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: stringResource(R.string.account_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // 右上角提供编辑入口
                    IconButton(onClick = {
                        if (account != null) {
                            navController.navigate(Routes.addAccountRoute(account.id))
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_account))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (account == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.account_not_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // 1. 顶部余额卡片
                AccountHeaderCard(
                    account = account,
                    currentBalance = currentBalance
                )

                // 2. 交易列表标题
                Text(
                    text = stringResource(R.string.transaction_records_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )

                // 3. 交易列表
                if (accountTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_transaction_records), color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(accountTransactions) { expense ->

                            // [修改] 3. 动态获取样式
                            val stylePair = categoryStyleMap[expense.category]
                            val icon = stylePair?.first
                            val color = stylePair?.second ?: if(expense.amount < 0) Color(0xFFE53935) else Color(0xFF4CAF50)

                            AccountTransactionItem(
                                expense = expense,
                                icon = icon,
                                categoryColor = color, // 传入颜色
                                onClick = {
                                    navController.navigate(Routes.transactionDetailRoute(expense.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountHeaderCard(account: Account, currentBalance: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconMapper.getIcon(account.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.current_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.currency_amount_format, account.currency, String.format(Locale.US, "%.2f", currentBalance)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// [修改] 组件升级：接收 categoryColor
@Composable
fun AccountTransactionItem(
    expense: Expense,
    icon: ImageVector?,
    categoryColor: Color, // 新增参数
    onClick: () -> Unit
) {
    val isExpense = expense.amount < 0
    // 金额颜色保持 红/绿
    val amountColor = if (isExpense) Color(0xFFE53935) else Color(0xFF4CAF50)

    // [修改] 使用传入的颜色
    val iconBgColor = categoryColor.copy(alpha = 0.15f)
    val iconTint = categoryColor

    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) } // 已应用 i18n 修复

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor), // 应用背景色
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = expense.category,
                        tint = iconTint, // 应用图标色
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    // 默认图标 (比如转账没有特定 category 图标时)
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中间信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(expense.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (!expense.remark.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.transaction_remark_format, expense.remark!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 金额
            val amountText = if (isExpense) String.format("%.2f", expense.amount) else "+${String.format("%.2f", expense.amount)}"
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp), // 留出图标宽度，增强层级感
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    }
}