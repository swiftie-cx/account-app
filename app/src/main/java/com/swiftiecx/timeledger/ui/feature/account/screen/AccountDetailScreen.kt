package com.swiftiecx.timeledger.ui.feature.account.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
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
import com.swiftiecx.timeledger.data.RecordType
import com.swiftiecx.timeledger.ui.common.CategoryData
import com.swiftiecx.timeledger.ui.common.IconMapper
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    accountId: Long,
    defaultCurrency: String
) {
    val context = LocalContext.current
    var showAddMenu by remember { mutableStateOf(false) }

    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }

    val account = remember(allAccounts, accountId) {
        allAccounts.find { it.id == accountId }
    }

    val accountTransactions = remember(allExpenses, accountId) {
        allExpenses
            .filter { it.accountId == accountId }
            .sortedByDescending { it.date.time }
    }

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
        floatingActionButton = {
            if (account?.category == "DEBT") {
                FloatingActionButton(
                    onClick = { showAddMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_record))
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
                    currentBalance = currentBalance,
                    onRepayClick = {
                        // ✅ 修复：NavGraph 中没有 "transfer" 这个目的地，会直接崩溃。
                        // 这里直接跳到「记一笔」页面的【转账】Tab。
                        // type=2 -> 转账 (与 AddTransactionScreen 的 tabs 索引一致)
                        navController.navigate(Routes.creditRepayRoute(accountId = account!!.id, maxAmount = currentBalance))
                    }
                )

                Text(
                    text = if (account.category == "DEBT") stringResource(R.string.title_debt_records) else stringResource(R.string.transaction_records_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (account.category == "DEBT") {
                        items(debtRecords) { record ->
                            DebtRecordItem(
                                record = record,
                                allAccounts = allAccounts,
                                currency = account.currency,
                                onClick = {
                                    // 未来详情入口
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        }
                    }
                    else {
                        items(accountTransactions) { expense ->
                            val isTransfer = expense.recordType == RecordType.TRANSFER
                            val otherAccount = expense.relatedAccountId?.let { accountMap[it] }

                            val isDebtRelated = expense.category in setOf("借出", "Lend", "债务还款", "借入", "Borrow", "债务收款")

                            val isRepayment = isTransfer && otherAccount != null && (
                                    (account?.category == "FUNDS" && otherAccount.category == "CREDIT") ||
                                            (account?.category == "CREDIT" && otherAccount.category == "FUNDS")
                                    )

                            val transferColor = MaterialTheme.colorScheme.primary

                            // ✅ 最小改动：调整 when 顺序，债务优先于普通转账；还款仍最高优先
                            val icon = when {
                                isTransfer && isRepayment -> Icons.Default.Payment

                                // ✅ 借入/借出（含不计入收支）优先于 isTransfer
                                isDebtRelated -> {
                                    if (expense.category in setOf("借出", "Lend", "债务还款")) {
                                        IconMapper.getIcon("Lend")
                                    } else {
                                        IconMapper.getIcon("Borrow")
                                    }
                                }

                                isTransfer -> Icons.AutoMirrored.Filled.CompareArrows
                                else -> CategoryData.getIcon(expense.category, context)
                            }

                            // ✅ 最小改动：调整 when 顺序，债务优先于普通转账；还款仍最高优先
                            val displayName = when {
                                isTransfer && isRepayment && otherAccount != null && account?.category == "CREDIT" ->
                                    stringResource(R.string.repayment_from, otherAccount.name)

                                isTransfer && isRepayment && otherAccount != null && account?.category == "FUNDS" ->
                                    stringResource(R.string.repayment_to, otherAccount.name)

                                // ✅ 借入/借出（含不计入收支）优先于 isTransfer
                                isDebtRelated -> {
                                    when (expense.category) {
                                        "借出", "Lend" -> stringResource(R.string.type_lend_out)
                                        "借入", "Borrow" -> stringResource(R.string.type_borrow_in)
                                        "债务还款" -> stringResource(R.string.type_repayment)
                                        "债务收款" -> stringResource(R.string.type_collection)
                                        else -> expense.category
                                    }
                                }

                                isTransfer ->
                                    stringResource(R.string.transfer_in_app)

                                else ->
                                    CategoryData.getDisplayName(expense.category, context)
                            }

                            // ✅ 最小改动：调整 when 顺序，债务优先于普通转账
                            val color = when {
                                // ✅ 借入/借出优先于 isTransfer（否则会被标成转账色）
                                isDebtRelated -> if (expense.amount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)

                                isTransfer -> transferColor
                                else -> CategoryData.getColor(
                                    expense.category,
                                    if (expense.amount < 0) 0 else 1,
                                    context
                                )
                            }

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
                        text = stringResource(R.string.sheet_title_select_type),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_add_borrow_account)) },
                        supportingContent = { Text(stringResource(R.string.desc_add_borrow_account)) },
                        leadingContent = {
                            Icon(Icons.Default.CallReceived, null, tint = Color(0xFFE53935))
                        },
                        modifier = Modifier.clickable {
                            showAddMenu = false
                            navController.navigate(Routes.addBorrowRoute(accountId))
                        }
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_add_lend_account)) },
                        supportingContent = { Text(stringResource(R.string.desc_add_lend_account)) },
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

@Composable
fun DebtRecordItem(
    record: DebtRecord,
    allAccounts: List<Account>,
    currency: String,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

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

        if (!record.note.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.label_note)}: ${record.note}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_date),
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
                    text = if(record.settleTime != null) stringResource(R.string.label_settle_date) else stringResource(R.string.label_expect_date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (record.settleTime != null) dateFormat.format(record.settleTime) else stringResource(R.string.status_unsettled),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (record.settleTime != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }
        }

        if (inAccountName != null || outAccountName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                if (inAccountName != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_target_account),
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
                            text = stringResource(R.string.label_source_account),
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
    currentBalance: Double,
    onRepayClick: () -> Unit = {} // [新增] 回调
) {
    val isCredit = account.category == "CREDIT"
    val creditLimit = account.creditLimit ?: 0.0
    val debt = if (isCredit) max(currentBalance, 0.0) else 0.0
    val available = if (isCredit) max(creditLimit - debt, 0.0) else 0.0

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
                    text = stringResource(R.string.label_current_arrears),
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

                val animatedProgress by animateFloatAsState(targetValue = progress, label = "Progress")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.label_total_quota), style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
                        Text(
                            text = stringResource(R.string.currency_amount_format, account.currency, creditLimit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.label_available_quota), style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
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
                    val billingText = account.billingDay?.takeIf { it > 0 }?.toString() ?: stringResource(R.string.not_set)
                    val repayText = account.repaymentDay?.takeIf { it > 0 }?.toString() ?: stringResource(R.string.not_set)
                    Text(
                        text = stringResource(R.string.label_bill_day, billingText),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.label_repay_day, repayText),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }

                // [新增] 还款按钮 (仅信贷账户显示)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRepayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_repay),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
fun main () {
    val string ="string"
    println(string[0])
}