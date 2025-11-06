package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // 使用 * 导入
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.* // 使用 * 导入
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.data.Expense // 确保导入 Expense
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import com.example.myapplication.ui.screen.Routes // 确保导入 Routes
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(viewModel: ExpenseViewModel, navController: NavHostController, defaultCurrency: String) {
    // 获取账户和交易记录
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // --- 计算当前余额和汇总 ---
    // 1. 按 accountId 分组计算每个账户的交易总额
    val expenseSumsByAccount = remember(expenses) {
        expenses.groupBy { it.accountId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    // 2. 计算每个账户的当前余额 (Account 和 Balance 的 Pair 列表)
    val accountsWithBalance = remember(accounts, expenseSumsByAccount) {
        accounts.map { account ->
            val currentBalance = account.initialBalance + (expenseSumsByAccount[account.id] ?: 0.0)
            account to currentBalance
        }
    }

    // 3. 计算资产、负债、净资产汇总 (基于当前余额)
    val assets = remember(accountsWithBalance, defaultCurrency) {
        accountsWithBalance.filter { !it.first.isLiability }.sumOf { (account, balance) ->
            ExchangeRates.convert(balance, account.currency, defaultCurrency)
        }
    }
    val liabilities = remember(accountsWithBalance, defaultCurrency) {
        accountsWithBalance.filter { it.first.isLiability }.sumOf { (account, balance) ->
            abs(ExchangeRates.convert(balance, account.currency, defaultCurrency))
        }
    }
    val netAssets = assets - liabilities
    // --- 计算结束 ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资产") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            SummaryCard(
                netAssets = netAssets,
                assets = assets,
                liabilities = liabilities,
                currency = defaultCurrency
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(accountsWithBalance, key = { it.first.id }) { (account, currentBalance) ->
                    AssetAccountItem(
                        account = account,
                        currentBalance = currentBalance,
                        onClick = {
                            // TODO: 导航到账户详情/编辑页面
                            // navController.navigate("account_detail/${account.id}")
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Routes.ADD_ACCOUNT) }, // 使用 Routes
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) { Text("添加账户", color = MaterialTheme.colorScheme.onSurfaceVariant) }

                Button(
                    onClick = { navController.navigate(Routes.ACCOUNT_MANAGEMENT) }, // 使用 Routes
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) { Text("管理账户", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

// 黄色摘要卡片 Composable
@Composable
private fun SummaryCard(netAssets: Double, assets: Double, liabilities: Double, currency: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("净资产", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$currency ${String.format(Locale.US, "%.2f", netAssets)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("资产", style = MaterialTheme.typography.labelMedium)
                        Text("$currency ${String.format(Locale.US, "%.2f", assets)}", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("负债", style = MaterialTheme.typography.labelMedium)
                        Text("$currency ${String.format(Locale.US, "%.2f", liabilities)}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            // Icon(painterResource(id = R.drawable.your_money_bag_icon), ...)
        }
    }
}

// 资产屏幕的账户列表项 Composable
@Composable
fun AssetAccountItem(
    account: Account,
    currentBalance: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = IconMapper.getIcon(account.iconName)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = account.name,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.padding(start = 16.dp))

            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${account.currency} ${String.format(Locale.US, "%.2f", currentBalance)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "详情",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
