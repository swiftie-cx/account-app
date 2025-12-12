package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
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
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val currentExpense = remember(expenses, expenseId) {
        expenses.find { it.id == expenseId }
    }

    val relatedTransferExpense = remember(currentExpense, expenses) {
        if (currentExpense?.category?.startsWith("转账") == true) {
            expenses.find {
                it.id != currentExpense.id &&
                        it.date.time == currentExpense.date.time &&
                        it.category.startsWith("转账")
            }
        } else {
            null
        }
    }

    val (transferOut, transferIn) = remember(currentExpense, relatedTransferExpense) {
        if (currentExpense != null && relatedTransferExpense != null) {
            if (currentExpense.amount < 0) {
                currentExpense to relatedTransferExpense
            } else {
                relatedTransferExpense to currentExpense
            }
        } else {
            null to null
        }
    }

    val transactionFee = remember(transferOut, transferIn) {
        if (transferOut != null && transferIn != null) {
            val accOut = accountMap[transferOut.accountId]
            val accIn = accountMap[transferIn.accountId]
            if (accOut?.currency == accIn?.currency) {
                val fee = abs(transferOut.amount) - abs(transferIn.amount)
                if (fee > 0.01) fee else 0.0
            } else {
                0.0
            }
        } else {
            0.0
        }
    }

    val outAccountBalance = remember(expenses, transferOut) {
        if (transferOut != null) {
            val account = accountMap[transferOut.accountId]
            val sum = expenses.filter { it.accountId == transferOut.accountId }.sumOf { it.amount }
            (account?.initialBalance ?: 0.0) + sum
        } else 0.0
    }

    val inAccountBalance = remember(expenses, transferIn) {
        if (transferIn != null) {
            val account = accountMap[transferIn.accountId]
            val sum = expenses.filter { it.accountId == transferIn.accountId }.sumOf { it.amount }
            (account?.initialBalance ?: 0.0) + sum
        } else 0.0
    }

    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    val icon = if (transferOut != null) {
        Icons.AutoMirrored.Filled.CompareArrows
    } else {
        currentExpense?.let { categoryIconMap[it.category] }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault()) }
    val shortDateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                    Text("编辑")
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
                    Text("删除")
                }
            }
        }
    ) { innerPadding ->
        if (currentExpense == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("未找到记录")
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
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (transferOut != null) "内部转账" else currentExpense.category,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(24.dp))
                }

                Column(modifier = Modifier.fillMaxWidth()) {

                    if (transferOut != null && transferIn != null) {
                        val accountOut = accountMap[transferOut.accountId]
                        val accountIn = accountMap[transferIn.accountId]

                        DetailRow(label = "类型", value = "转账")

                        // 转出详情 (统一风格)
                        DetailRow(
                            label = "转出",
                            value = "${accountOut?.currency ?: ""} ${String.format("%.2f", abs(transferOut.amount))}",
                            valueColor = Color(0xFFE53935) // 红色
                        )
                        // 余额小字
                        DetailRow(
                            label = "",
                            value = "${accountOut?.name} (余额: ${String.format("%.2f", outAccountBalance)})",
                            isSubText = true
                        )

                        // 转入详情 (统一风格)
                        DetailRow(
                            label = "转入",
                            value = "${accountIn?.currency ?: ""} ${String.format("%.2f", abs(transferIn.amount))}",
                            valueColor = Color(0xFF4CAF50) // 绿色
                        )
                        // 余额小字
                        DetailRow(
                            label = "",
                            value = "${accountIn?.name} (余额: ${String.format("%.2f", inAccountBalance)})",
                            isSubText = true
                        )

                        if (transactionFee > 0) {
                            DetailRow(
                                label = "手续费",
                                value = "${accountOut?.currency ?: ""} ${String.format("%.2f", transactionFee)}",
                                valueColor = MaterialTheme.colorScheme.error
                            )
                        }

                    } else {
                        val account = accountMap[currentExpense.accountId]
                        DetailRow(label = "类型", value = if (currentExpense.amount < 0) "支出" else "收入")

                        DetailRow(
                            label = "金额",
                            value = "${account?.currency ?: ""} ${abs(currentExpense.amount)}",
                            valueColor = if (currentExpense.amount > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                        )

                        if (account != null && account.currency != defaultCurrency) {
                            val converted = ExchangeRates.convert(abs(currentExpense.amount), account.currency, defaultCurrency)
                            DetailRow(
                                label = "折合",
                                value = "≈ $defaultCurrency ${String.format(Locale.US, "%.2f", converted)}"
                            )
                        }

                        DetailRow(label = "账户", value = account?.name ?: "未知账户")
                    }

                    DetailRow(label = "日期", value = shortDateFormat.format(currentExpense.date))

                    val remark = if (transferOut != null) transferOut.remark else currentExpense.remark
                    if (!remark.isNullOrBlank()) {
                        DetailRow(label = "备注", value = remark)
                    }

                    Text(
                        text = "(添加于 ${dateFormat.format(currentExpense.date)})",
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
    isSubText: Boolean = false // 新增参数，用于显示从属信息（如余额）
) {
    // 如果是 SubText，垂直间距缩小，标签不占位，内容颜色变淡
    val verticalPadding = if (isSubText) 0.dp else 12.dp
    val labelText = if (isSubText) "" else label
    val finalValueColor = if (isSubText) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else valueColor
    val valueStyle = if (isSubText) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.Top
    ) {
        // 标签列固定宽度，保持对齐
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