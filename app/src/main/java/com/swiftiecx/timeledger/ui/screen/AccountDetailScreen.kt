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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Account
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
    val context = androidx.compose.ui.platform.LocalContext.current

    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    val account = remember(allAccounts, accountId) {
        allAccounts.find { it.id == accountId }
    }

    val accountTransactions = remember(allExpenses, accountId) {
        allExpenses
            .filter { it.accountId == accountId }
            .sortedByDescending { it.date.time }
    }

    val currentBalance = remember(account, accountTransactions) {
        if (account != null) {
            account.initialBalance + accountTransactions.sumOf { it.amount }
        } else 0.0
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
                    if (account != null) {
                        IconButton(onClick = {
                            navController.navigate(Routes.addAccountRoute(account.id))
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_account))
                        }
                    }
                }
            )
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
                    text = stringResource(R.string.transaction_records_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn {
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
}

@Composable
fun AccountHeaderCard(account: Account, currentBalance: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        // [关键修复] 添加 fillMaxWidth() 确保 Column 占满卡片宽度，从而使居中生效
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = IconMapper.getIcon(account.iconName),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.current_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Text(
                text = stringResource(
                    R.string.currency_amount_format,
                    account.currency,
                    currentBalance // [保留之前的防闪退修复] 传入 Double
                ),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val amountColor = if (expense.amount < 0) Color(0xFFE53935) else Color(0xFF4CAF50)

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
                fontWeight = FontWeight.Medium
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