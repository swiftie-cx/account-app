package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.ExchangeRates
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

    // 1. 获取当前点击的记录
    val currentExpense = remember(expenses, expenseId) {
        expenses.find { it.id == expenseId }
    }

    // 2. 判断是否为转账，如果是，尝试寻找另一半记录 (同一时间、不同ID、也是转账类别)
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

    // 3. 确定转出方(Out)和转入方(In)
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

    // 4. 计算手续费 (转出绝对值 - 转入绝对值)
    val transactionFee = remember(transferOut, transferIn) {
        if (transferOut != null && transferIn != null) {
            val fee = abs(transferOut.amount) - abs(transferIn.amount)
            // 解决浮点数精度问题，若小于 0.01 则视为 0
            if (fee < 0.01) 0.0 else fee
        } else {
            0.0
        }
    }

    // UI 资源准备
    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    // 如果是转账，使用双向箭头图标；否则使用分类图标
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
            // 底部操作栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        currentExpense?.let {
                            // 编辑时，如果是转账，把 ID 传过去，AddTransactionScreen 会自动识别并进入转账模式
                            navController.navigate("add_transaction?expenseId=${it.id}")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("编辑")
                }
                Button(
                    onClick = {
                        // 删除逻辑：如果是转账，应该把两条都删了
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 顶部大图标 ---
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

                // --- 详细列表 ---
                Column(modifier = Modifier.fillMaxWidth()) {

                    if (transferOut != null && transferIn != null) {
                        // === 转账特定 UI ===
                        val accountOut = accountMap[transferOut.accountId]
                        val accountIn = accountMap[transferIn.accountId]

                        DetailRow(label = "类型", value = "转账")

                        // 显示账户流向
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("账户", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(accountOut?.name ?: "未知", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(accountIn?.name ?: "未知", style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        // 显示金额 (显示实际到账金额)
                        DetailRow(label = "到账金额", value = "${accountIn?.currency ?: ""} ${String.format("%.2f", transferIn.amount)}")

                        // 【关键】显示手续费 (如果有)
                        if (transactionFee > 0) {
                            DetailRow(
                                label = "手续费",
                                value = "${accountOut?.currency ?: ""} ${String.format("%.2f", transactionFee)}",
                                valueColor = MaterialTheme.colorScheme.error // 红色高亮手续费
                            )
                        }

                    } else {
                        // === 普通收支 UI ===
                        val account = accountMap[currentExpense.accountId]
                        DetailRow(label = "类型", value = if (currentExpense.amount < 0) "支出" else "收入")

                        DetailRow(
                            label = "金额",
                            value = "${account?.currency ?: ""} ${abs(currentExpense.amount)}",
                            valueColor = if(currentExpense.amount > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                        )

                        // 汇率折算
                        if (account != null && account.currency != defaultCurrency) {
                            val converted = ExchangeRates.convert(abs(currentExpense.amount), account.currency, defaultCurrency)
                            DetailRow(
                                label = "折合",
                                value = "≈ $defaultCurrency ${String.format(Locale.US, "%.2f", converted)}"
                            )
                        }

                        DetailRow(label = "账户", value = account?.name ?: "未知账户")
                    }

                    // --- 通用信息 ---
                    DetailRow(label = "日期", value = shortDateFormat.format(currentExpense.date))

                    // 备注：转账时优先显示转出方的备注
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
private fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor
        )
    }
}