package com.swiftiecx.timeledger.ui.screen

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    expenseId: Long?,
    defaultCurrency: String = "CNY"
) {
    val context = LocalContext.current // [新增] 获取 Context 用于 CategoryData

    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val currentExpense = remember(expenses, expenseId) {
        expenses.find { it.id == expenseId }
    }

    // 转账：基于 recordType/transferId 找另一条转账记录（进/出）
    val relatedTransferExpense = remember(currentExpense, expenses) {
        if (currentExpense?.recordType == com.swiftiecx.timeledger.data.RecordType.TRANSFER) {
            val tid = currentExpense.transferId
            if (tid != null) {
                expenses.find { it.id != currentExpense.id && it.recordType == com.swiftiecx.timeledger.data.RecordType.TRANSFER && it.transferId == tid }
            } else {
                // 旧数据兜底：同一时间戳 + recordType=TRANSFER
                expenses.find { it.id != currentExpense!!.id && it.recordType == com.swiftiecx.timeledger.data.RecordType.TRANSFER && it.date.time == currentExpense.date.time }
            }
        } else null
    }

    val (transferOut, transferIn) = remember(currentExpense, relatedTransferExpense) {
        if (currentExpense != null && relatedTransferExpense != null) {
            if (currentExpense.amount < 0) currentExpense to relatedTransferExpense
            else relatedTransferExpense to currentExpense
        } else null to null
    }

    val transactionFee = remember(transferOut, transferIn) {
        if (transferOut != null && transferIn != null) {
            val fee = abs(transferOut.amount) - abs(transferIn.amount)
            if (fee > 0.01) fee else 0.0
        } else 0.0
    }

    val outAccountBalance = remember(expenses, transferOut, accountMap) {
        if (transferOut != null) {
            val account = accountMap[transferOut.accountId]
            val sum = expenses.filter { it.accountId == transferOut.accountId }.sumOf { it.amount }
            (account?.initialBalance ?: 0.0) + sum
        } else 0.0
    }

    val inAccountBalance = remember(expenses, transferIn, accountMap) {
        if (transferIn != null) {
            val account = accountMap[transferIn.accountId]
            val sum = expenses.filter { it.accountId == transferIn.accountId }.sumOf { it.amount }
            (account?.initialBalance ?: 0.0) + sum
        } else 0.0
    }

    // [关键修改] 使用 CategoryData 获取图标和颜色 (不再手动构建 Map)
    var displayIcon: ImageVector = Icons.Default.HelpOutline
    var displayColor: Color = MaterialTheme.colorScheme.primary
    var displayTitle: String = ""

    if (transferOut != null) {
        displayIcon = Icons.AutoMirrored.Filled.CompareArrows
        displayColor = MaterialTheme.colorScheme.primary
        displayTitle = stringResource(R.string.internal_transfer)
    } else if (currentExpense != null) {
        // 使用 stable key 获取样式
        val stableKey = CategoryData.getStableKey(currentExpense.category, context)
        displayIcon = CategoryData.getIcon(stableKey, context)

        // 0=支出, 1=收入
        val typeInt = if (currentExpense.amount < 0) 0 else 1
        displayColor = CategoryData.getColor(stableKey, typeInt, context)

        // 获取本地化标题
        displayTitle = CategoryData.getDisplayName(stableKey, context)
    }

    // 4) 日期格式
    val locale = Locale.getDefault()
    val fullPattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "yMMMdHms") }
    val shortPattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "yMMMd") }
    val dateFormat = remember(locale, fullPattern) { SimpleDateFormat(fullPattern, locale) }
    val shortDateFormat = remember(locale, shortPattern) { SimpleDateFormat(shortPattern, locale) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        currentExpense?.let {
                            navController.navigate("add_transaction?expenseId=${it.id}")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.edit))
                }
                Button(
                    onClick = {
                        if (transferOut != null && transferIn != null) {
                            viewModel.deleteExpense(transferOut)
                            viewModel.deleteExpense(transferIn)
                        } else {
                            currentExpense?.let { viewModel.deleteExpense(it) }
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    ) { innerPadding ->
        if (currentExpense == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.record_not_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部图标+标题
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(displayColor.copy(alpha = 0.15f), CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = displayIcon,
                        contentDescription = null,
                        tint = displayColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(8.dp))

                // [关键修改] 显示本地化标题
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth()) {

                    if (transferOut != null && transferIn != null) {
                        val accountOut = accountMap[transferOut.accountId]
                        val accountIn = accountMap[transferIn.accountId]

                        DetailRow(label = stringResource(R.string.type_label), value = stringResource(R.string.type_transfer))

                        // 转出
                        DetailRow(
                            label = stringResource(R.string.transfer_out),
                            value = "${accountOut?.currency.orEmpty()} ${String.format(Locale.US, "%.2f", abs(transferOut.amount))}",
                            valueColor = Color(0xFFE53935)
                        )
                        DetailRow(
                            label = "",
                            value = stringResource(
                                R.string.account_balance_format,
                                accountOut?.name ?: "",
                                outAccountBalance
                            ),
                            isSubText = true
                        )

                        // 转入
                        DetailRow(
                            label = stringResource(R.string.transfer_in),
                            value = "${accountIn?.currency.orEmpty()} ${String.format(Locale.US, "%.2f", abs(transferIn.amount))}",
                            valueColor = Color(0xFF4CAF50)
                        )
                        DetailRow(
                            label = "",
                            value = stringResource(
                                R.string.account_balance_format,
                                accountIn?.name ?: "",
                                inAccountBalance
                            ),
                            isSubText = true
                        )

                        if (transactionFee > 0) {
                            DetailRow(
                                label = stringResource(R.string.fee_label),
                                value = "${accountOut?.currency.orEmpty()} ${String.format(Locale.US, "%.2f", transactionFee)}",
                                valueColor = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        val account = accountMap[currentExpense.accountId]

                        DetailRow(
                            label = stringResource(R.string.type_label),
                            value = if (currentExpense.amount < 0)
                                stringResource(R.string.type_expense)
                            else
                                stringResource(R.string.type_income)
                        )

                        DetailRow(
                            label = stringResource(R.string.amount_label),
                            value = "${account?.currency.orEmpty()} ${String.format(Locale.US, "%.2f", abs(currentExpense.amount))}",
                            valueColor = if (currentExpense.amount > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                        )

                        if (account != null && account.currency != defaultCurrency) {
                            val converted = ExchangeRates.convert(abs(currentExpense.amount), account.currency, defaultCurrency)
                            DetailRow(
                                label = stringResource(R.string.converted_label),
                                value = stringResource(
                                    R.string.converted_amount_format,
                                    defaultCurrency,
                                    converted
                                )
                            )
                        }

                        DetailRow(
                            label = stringResource(R.string.account_label),
                            value = account?.name ?: stringResource(R.string.unknown_account)
                        )
                    }

                    DetailRow(
                        label = stringResource(R.string.date_label),
                        value = shortDateFormat.format(currentExpense.date)
                    )

                    val remark = if (transferOut != null) transferOut.remark else currentExpense.remark
                    if (!remark.isNullOrBlank()) {
                        DetailRow(label = stringResource(R.string.remark_label), value = remark)
                    }

                    Text(
                        text = stringResource(R.string.added_at_format, dateFormat.format(currentExpense.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isSubText: Boolean = false
) {
    val verticalPadding = if (isSubText) 0.dp else 12.dp
    val labelText = if (isSubText) "" else label
    val finalValueColor =
        if (isSubText) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else valueColor
    val valueStyle = if (isSubText) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = valueStyle,
            color = finalValueColor,
            fontWeight = if (!isSubText && valueColor != MaterialTheme.colorScheme.onSurface) FontWeight.Bold else FontWeight.Normal
        )
    }
}