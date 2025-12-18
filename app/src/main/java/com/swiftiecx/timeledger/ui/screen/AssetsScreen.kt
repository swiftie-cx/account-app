package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.ui.navigation.Routes
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(viewModel: ExpenseViewModel, navController: NavHostController, defaultCurrency: String) {
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // --- 计算逻辑 ---
    val expenseSumsByAccount = remember(expenses) {
        expenses.groupBy { it.accountId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    val accountsWithBalance = remember(accounts, expenseSumsByAccount) {
        accounts.map { account ->
            val currentBalance = account.initialBalance + (expenseSumsByAccount[account.id] ?: 0.0)
            account to currentBalance
        }
    }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AssetHeaderSection(
                netAssets = netAssets,
                assets = assets,
                liabilities = liabilities,
                currency = defaultCurrency
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accountsWithBalance, key = { it.first.id }) { (account, currentBalance) ->
                    AssetAccountItem(
                        account = account,
                        currentBalance = currentBalance,
                        onClick = {
                            navController.navigate(Routes.accountDetailRoute(account.id))
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
                    onClick = { navController.navigate(Routes.addAccountRoute()) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        // 改：用 primary，更显色
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(stringResource(R.string.add_account), style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = { navController.navigate(Routes.ACCOUNT_MANAGEMENT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        // 改：用 primary，更显色
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(stringResource(R.string.manage_account), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun AssetHeaderSection(
    netAssets: Double,
    assets: Double,
    liabilities: Double,
    currency: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.assets_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        val themeColor = MaterialTheme.colorScheme.primary

        Card(
            modifier = Modifier
                .fillMaxWidth()
                // 明细页同款：有一圈柔和外阴影（如果你不想要，就删掉这一行）
                .shadow(
                    8.dp,
                    RoundedCornerShape(24.dp),
                    ambientColor = themeColor.copy(alpha = 0.3f),
                    spotColor = themeColor.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    // 明细页同款：primaryContainer + 0.75 透明度
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.net_assets),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.currency_amount_format, currency, netAssets),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface   // 黑字
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.total_assets),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.currency_amount_format, currency, assets),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface // 黑字
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stringResource(R.string.total_liabilities),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.currency_amount_format, currency, liabilities),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface // 黑字
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun AssetAccountItem(
    account: Account,
    currentBalance: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    // 图标底也别用太淡的 container，可保持一致但更清晰
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconMapper.getIcon(account.iconName),
                    contentDescription = account.name,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.currency_amount_format, account.currency, currentBalance),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
