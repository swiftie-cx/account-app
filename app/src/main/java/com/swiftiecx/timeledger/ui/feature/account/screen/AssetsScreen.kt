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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource // [新增]
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增]
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(viewModel: ExpenseViewModel, navController: NavHostController, defaultCurrency: String) {
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val globalDebt by viewModel.getGlobalDebtSummary().collectAsState(initial = 0.0 to 0.0)

    var showAddAccountSheet by remember { mutableStateOf(false) }

    // --- 余额计算逻辑 ---
    val expenseSumsByAccount = remember(expenses) {
        expenses.groupBy { it.accountId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    val accountsWithBalance = remember(accounts, expenseSumsByAccount) {
        accounts.map { account ->
            val txSum = expenseSumsByAccount[account.id] ?: 0.0
            val initialBalance = if (account.category == "CREDIT" && account.initialBalance > 0) {
                -account.initialBalance
            } else {
                account.initialBalance
            }

            val currentBalance = initialBalance + txSum
            account to currentBalance
        }
    }

    // --- 资产/负债汇总统计逻辑 ---
    val assets = remember(accountsWithBalance, globalDebt, defaultCurrency) {
        val accountAssetsSum = accountsWithBalance.sumOf { (account, balance) ->
            val converted = ExchangeRates.convert(balance, account.currency, defaultCurrency)
            if (account.category == "FUNDS" && converted > 0) converted else 0.0
        }
        accountAssetsSum + globalDebt.first
    }

    val liabilities = remember(accountsWithBalance, globalDebt, defaultCurrency) {
        val accountLiabilitiesSum = accountsWithBalance.sumOf { (account, balance) ->
            val converted = ExchangeRates.convert(balance, account.currency, defaultCurrency)
            if (account.category == "CREDIT" || converted < 0) abs(converted) else 0.0
        }
        accountLiabilitiesSum + globalDebt.second
    }

    val netAssets = assets - liabilities

    // --- 添加账户/记录的选择弹窗 ---
    if (showAddAccountSheet) {
        AddAccountTypeBottomSheet(
            onDismiss = { showAddAccountSheet = false },
            onSelectFunds = {
                showAddAccountSheet = false
                navController.navigate(Routes.addAccountRoute(accountId = -1L, category = "FUNDS"))
            },
            onSelectCredit = {
                showAddAccountSheet = false
                navController.navigate(Routes.addAccountRoute(accountId = -1L, category = "CREDIT"))
            },
            onSelectBorrow = {
                showAddAccountSheet = false
                navController.navigate(Routes.addBorrowRoute(-1L))
            },
            onSelectLend = {
                showAddAccountSheet = false
                navController.navigate(Routes.addLendRoute(-1L))
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 资产汇总卡片
                AssetHeaderSection(
                    netAssets = netAssets,
                    assets = assets,
                    liabilities = liabilities,
                    currency = defaultCurrency
                )

                // 债务管理入口卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { navController.navigate(Routes.DEBT_MANAGEMENT) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(0.5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.title_debt_management),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.subtitle_debt_management_entry), // [新增资源]
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 右侧概览
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.tag_payable)}: ${String.format("%.2f", globalDebt.second)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stringResource(R.string.tag_receivable)}: ${String.format("%.2f", globalDebt.first)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // 账户列表
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accountsWithBalance, key = { it.first.id }) { (account, currentBalance) ->
                        AssetAccountItem(
                            account = account,
                            currentBalance = currentBalance,
                            onClick = { navController.navigate(Routes.accountDetailRoute(account.id)) }
                        )
                    }
                }
            }

            // 底部悬浮按钮组
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showAddAccountSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.add_account), style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = { navController.navigate(Routes.ACCOUNT_MANAGEMENT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
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
            .padding(horizontal = 16.dp)
            .padding(top = 0.dp, bottom = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.assets_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        val themeColor = MaterialTheme.colorScheme.primary

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    4.dp,
                    RoundedCornerShape(24.dp),
                    ambientColor = themeColor.copy(alpha = 0.3f),
                    spotColor = themeColor.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.net_assets),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.currency_amount_format, currency, netAssets),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.currency_amount_format, currency, assets),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
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
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.currency_amount_format, currency, liabilities),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
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
    val isCredit = account.category == "CREDIT"
    val creditLimit = (account.creditLimit ?: 0.0)

    val debt = if (isCredit) abs(currentBalance.coerceAtMost(0.0)) else 0.0
    val available = if (isCredit) max(creditLimit - debt, 0.0) else 0.0

    val progress = remember(isCredit, creditLimit, available) {
        if (!isCredit || creditLimit <= 0.0) 0f
        else (available / creditLimit).toFloat().coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Progress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
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
                        color = if (currentBalance < 0) Color.Red else MaterialTheme.colorScheme.onSurface
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

            if (isCredit && creditLimit > 0.0) {
                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stringResource(R.string.label_available_quota)}：${String.format("%.0f", available)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stringResource(R.string.label_total_quota)}：${String.format("%.0f", creditLimit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountTypeBottomSheet(
    onDismiss: () -> Unit,
    onSelectFunds: () -> Unit,
    onSelectCredit: () -> Unit,
    onSelectBorrow: () -> Unit,
    onSelectLend: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            )

            Text(
                text = stringResource(R.string.sheet_title_add_account_record),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AddAccountSheetItem(
                icon = Icons.Default.AccountBalance,
                title = stringResource(R.string.type_funds_account),
                subtitle = stringResource(R.string.desc_funds_account),
                onClick = onSelectFunds
            )
            Spacer(Modifier.height(10.dp))

            AddAccountSheetItem(
                icon = Icons.Default.CreditCard,
                title = stringResource(R.string.type_credit_account),
                subtitle = stringResource(R.string.desc_credit_account),
                onClick = onSelectCredit
            )
            Spacer(Modifier.height(10.dp))

            AddAccountSheetItem(
                icon = Icons.Default.SwapHoriz,
                title = stringResource(R.string.action_add_borrow),
                subtitle = stringResource(R.string.desc_add_borrow_record),
                onClick = onSelectBorrow
            )
            Spacer(Modifier.height(10.dp))

            AddAccountSheetItem(
                icon = Icons.Default.SwapHoriz,
                title = stringResource(R.string.action_add_lend),
                subtitle = stringResource(R.string.desc_add_lend_record),
                onClick = onSelectLend
            )

            Spacer(Modifier.height(14.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun AddAccountSheetItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}