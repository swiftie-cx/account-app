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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.DebtRecord
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    accountId: Long,
    defaultCurrency: String
) {
    val context = LocalContext.current
    var showAddMenu by remember { mutableStateOf(false) } // 控制操作菜单弹窗

    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    val account = remember(allAccounts, accountId) {
        allAccounts.find { it.id == accountId }
    }

    // 针对普通/信贷账户的流水记录
    val accountTransactions = remember(allExpenses, accountId) {
        allExpenses
            .filter { it.accountId == accountId }
            .sortedByDescending { it.date.time }
    }

    // 针对债务账户的记录 (借贷列表)
    val debtRecords = if (account?.category == "DEBT") {
        viewModel.getDebtRecords(accountId).collectAsState(initial = emptyList()).value
    } else {
        emptyList()
    }

    val currentBalance = remember(account, accountTransactions) {
        if (account != null) {
            val txSum = accountTransactions.sumOf { it.amount }
            if (account.category == "CREDIT") account.initialBalance - txSum
            else account.initialBalance + txSum
        } else 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: stringResource(R.string.account_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (account != null) {
                        IconButton(onClick = {
                            navController.navigate(Routes.addAccountRoute(account.id))
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_account)
                            )
                        }
                    }
                }
            )
        },
        // [入口对接]：如果是债务账户，显示悬浮按钮以新增记录
        floatingActionButton = {
            if (account?.category == "DEBT") {
                FloatingActionButton(
                    onClick = { showAddMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增记录")
                }
            }
        }
    ) { innerPadding ->
        if (account == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.account_not_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                AccountHeaderCard(
                    account = account,
                    currentBalance = currentBalance
                )

                Text(
                    text = if (account.category == "DEBT") "借贷记录" else stringResource(R.string.transaction_records_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    // === 分支逻辑：如果是债务账户，显示 DebtRecord 列表 ===
                    if (account.category == "DEBT") {
                        items(debtRecords) { record ->
                            DebtRecordItem(
                                record = record,
                                allAccounts = allAccounts,
                                currency = account.currency,
                                onClick = {
                                    // 未来可在此处添加查看/编辑借贷记录详情的导航
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        }
                    }
                    // === 否则显示普通账户流水列表 ===
                    else {
                        items(accountTransactions) { expense ->
                            val displayName = CategoryData.getDisplayName(expense.category, context)
                            val icon = CategoryData.getIcon(expense.category, context)
                            val color = CategoryData.getColor(
                                expense.category,
                                if (expense.amount < 0) 0 else 1,
                                context
                            )

                            AccountTransactionItem(
                                displayName = displayName,
                                icon = icon,
                                categoryColor = color,
                                expense = expense,
                                onClick = {
                                    navController.navigate(
                                        Routes.transactionDetailRoute(expense.id)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // [入口对接]：选择操作类型的底部滑块
        if (showAddMenu) {
            ModalBottomSheet(
                onDismissRequest = { showAddMenu = false },
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = "选择记录类型",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 新增借入
                    ListItem(
                        headlineContent = { Text("新增借入 (欠款)") },
                        supportingContent = { Text("借入资金并增加到选定账户") },
                        leadingContent = {
                            Icon(Icons.Default.CallReceived, null, tint = Color(0xFFE53935))
                        },
                        modifier = Modifier.clickable {
                            showAddMenu = false
                            navController.navigate(Routes.addBorrowRoute(accountId))
                        }
                    )

                    // 新增借出
                    ListItem(
                        headlineContent = { Text("新增借出 (应收)") },
                        supportingContent = { Text("从选定账户转出资金外借") },
                        leadingContent = {
                            Icon(Icons.Default.CallMade, null, tint = Color(0xFF4CAF50))
                        },
                        modifier = Modifier.clickable {
                            showAddMenu = false
                            navController.navigate(Routes.addLendRoute(accountId))
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
//  借贷记录列表项组件 (DebtRecordItem)
// ==========================================
@Composable
fun DebtRecordItem(
    record: DebtRecord,
    allAccounts: List<Account>,
    currency: String,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // 获取关联账户名称（用于显示资金从哪来/到哪去）
    val inAccountName = remember(record.inAccountId, allAccounts) {
        allAccounts.find { it.id == record.inAccountId }?.name
    }
    val outAccountName = remember(record.outAccountId, allAccounts) {
        allAccounts.find { it.id == record.outAccountId }?.name
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // 第一行：姓名 + 金额
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = record.personName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$currency ${String.format("%.2f", record.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 第二行：备注 (如果有)
        if (!record.note.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "备注: ${record.note}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第三行：时间详情 (发生时间 + 结算状态)
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "发生时间",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = dateFormat.format(record.borrowTime),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if(record.settleTime != null) "结算时间" else "预计还款/收款",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (record.settleTime != null) dateFormat.format(record.settleTime) else "未结算",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (record.settleTime != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }
        }

        // 第四行：资金账户关联详情
        if (inAccountName != null || outAccountName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                if (inAccountName != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "到账账户",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = inAccountName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (outAccountName != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "支出账户",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = outAccountName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountHeaderCard(
    account: Account,
    currentBalance: Double
) {
    val isCredit = account.category == "CREDIT"
    val creditLimit = account.creditLimit ?: 0.0
    val debt = if (isCredit) kotlin.math.max(currentBalance, 0.0) else 0.0
    val available = if (isCredit) kotlin.math.max(creditLimit - debt, 0.0) else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconMapper.getIcon(account.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isCredit) {
                Text(
                    text = stringResource(R.string.current_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black.copy(alpha = 0.7f)
                )

                Text(
                    text = stringResource(
                        R.string.currency_amount_format,
                        account.currency,
                        currentBalance
                    ),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            } else {
                Text(
                    text = "当前欠款",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black.copy(alpha = 0.7f)
                )
                Text(
                    text = stringResource(R.string.currency_amount_format, account.currency, debt),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                val progress = if (creditLimit > 0) {
                    (available / creditLimit).toFloat().coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    trackColor = Color.Black.copy(alpha = 0.08f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("总额度", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
                        Text(
                            text = stringResource(R.string.currency_amount_format, account.currency, creditLimit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("可用额度", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
                        Text(
                            text = stringResource(R.string.currency_amount_format, account.currency, available),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val billingText = account.billingDay?.takeIf { it > 0 }?.toString() ?: "未设置"
                    val repayText = account.repaymentDay?.takeIf { it > 0 }?.toString() ?: "未设置"
                    Text(
                        text = "出账日：$billingText",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "还款日：$repayText",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountTransactionItem(
    displayName: String,
    icon: ImageVector,
    categoryColor: Color,
    expense: Expense,
    onClick: () -> Unit
) {
    val dateFormat = remember {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    }
    val amountColor = if (expense.amount < 0) {
        Color(0xFFE53935)
    } else {
        Color(0xFF4CAF50)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = displayName,
                tint = categoryColor
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateFormat.format(expense.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Text(
            text = String.format("%.2f", expense.amount),
            color = amountColor,
            fontWeight = FontWeight.Bold
        )
    }
}