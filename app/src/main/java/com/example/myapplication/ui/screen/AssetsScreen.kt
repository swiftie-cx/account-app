package com.example.myapplication.ui.screen

import androidx.compose.foundation.background // 确保导入
import androidx.compose.foundation.clickable // 确保导入
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // 确保导入
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape // 确保导入
import androidx.compose.material.icons.Icons // 确保导入
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight // 确保导入
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.Expense // (新) 导入 Expense 用于计算
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlin.math.abs

@Composable
fun AssetsScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
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
            account to currentBalance // 创建 Pair<Account, Double>
        }
    }

    // 3. 计算资产、负债、净资产汇总 (基于当前余额)
    val assets = remember(accountsWithBalance) {
        accountsWithBalance.filter { !it.first.isLiability }.sumOf { it.second }
    }
    val liabilities = remember(accountsWithBalance) {
        // 负债账户的余额通常是负数或零，我们需要取绝对值来表示负债总额
        accountsWithBalance.filter { it.first.isLiability }.sumOf { abs(it.second) }
    }
    val netAssets = assets - liabilities
    // --- 计算结束 ---

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 黄色摘要卡片
        SummaryCard(
            netAssets = netAssets,
            assets = assets,
            liabilities = liabilities
        )

        // 账户列表
        LazyColumn(
            modifier = Modifier
                .weight(1f) // 占据剩余空间
                .padding(horizontal = 16.dp)
        ) {
            items(accountsWithBalance, key = { it.first.id }) { (account, currentBalance) ->
                AssetAccountItem(
                    account = account,
                    currentBalance = currentBalance, // 传递当前余额
                    onClick = {
                        // TODO: 导航到账户详情/编辑页面，可以传递 account.id
                        // navController.navigate("account_detail/${account.id}")
                    }
                )
            }
        }

        // 底部按钮行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { navController.navigate(Routes.ADD_ACCOUNT) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("添加账户", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = {
                    // 可以导航到 AccountManagementScreen，如果那个屏幕用于编辑
                    navController.navigate(Routes.ACCOUNT_MANAGEMENT)
                    // 或者在这里触发编辑模式
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("管理账户", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// 黄色摘要卡片 Composable
@Composable
private fun SummaryCard(netAssets: Double, assets: Double, liabilities: Double) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer, // 使用主题颜色
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
                    text = String.format("%.2f", netAssets),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("资产", style = MaterialTheme.typography.labelMedium)
                        Text(String.format("%.2f", assets), style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("负债", style = MaterialTheme.typography.labelMedium)
                        Text(String.format("%.2f", liabilities), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            // 这里可以添加图片
            // Icon(painterResource(id = R.drawable.your_money_bag_icon), ...)
        }
    }
}

// 资产屏幕的账户列表项 Composable
@Composable
fun AssetAccountItem(
    account: Account,
    currentBalance: Double, // 接收当前余额
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick), // 使列表项可点击
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 背景色
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = IconMapper.getIcon(account.iconName)
            // 图标背景圆圈
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

            Spacer(Modifier.padding(start = 16.dp)) // 图标和文字间距

            // 账户名称
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f) // 占据空间
            )

            // 显示当前余额
            Text(
                text = "${account.currency} ${String.format("%.2f", currentBalance)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant // 稍暗的颜色
            )
            // ">" 图标
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "详情",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}