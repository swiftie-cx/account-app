package com.swiftiecx.timeledger.ui.feature.account.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增]
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.ui.navigation.AccountTypeManager
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val defaultAccountId by viewModel.defaultAccountId.collectAsState()

    var listData by remember(accounts) { mutableStateOf(accounts) }

    // --- 实时余额计算逻辑 ---
    val expenseSumsByAccount = remember(expenses) {
        expenses.groupBy { it.accountId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var showAddSelectionSheet by remember { mutableStateOf(false) }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            listData = listData.toMutableList().apply { add(to.index, removeAt(from.index)) }
        },
        onDragEnd = { _, _ -> viewModel.reorderAccounts(listData) }
    )

    LaunchedEffect(accounts) {
        if (listData.map { it.id } != accounts.map { it.id }) {
            listData = accounts
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.account_management_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSelectionSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_account)) }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.account_mgmt_tip), // [修改] 使用资源
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            LazyColumn(
                state = state.listState,
                modifier = Modifier.fillMaxSize().reorderable(state).detectReorderAfterLongPress(state),
                contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listData, key = { it.id }) { account ->
                    val txSum = expenseSumsByAccount[account.id] ?: 0.0
                    val currentBalance = account.initialBalance + txSum

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                accountToDelete = account
                                false
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                    else -> Color.Transparent
                                }, label = "bgColor"
                            )
                            Box(Modifier.fillMaxSize().background(color, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White)
                            }
                        }
                    ) {
                        ReorderableItem(state, key = account.id) { isDragging ->
                            val elevation = animateDpAsState(if (isDragging) 12.dp else 2.dp, label = "elevation")
                            AccountItemCard(
                                account = account,
                                currentBalance = currentBalance,
                                isDefault = (account.id == defaultAccountId),
                                elevation = elevation.value,
                                onSetDefault = { viewModel.setDefaultAccount(account.id) },
                                onClick = { navController.navigate(Routes.accountDetailRoute(account.id)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSelectionSheet) {
        AddAccountTypeBottomSheet(
            onDismiss = { showAddSelectionSheet = false },
            onSelectFunds = { showAddSelectionSheet = false; navController.navigate(Routes.addAccountRoute(accountId = -1L, category = "FUNDS")) },
            onSelectCredit = { showAddSelectionSheet = false; navController.navigate(Routes.addAccountRoute(accountId = -1L, category = "CREDIT")) },
            onSelectBorrow = { showAddSelectionSheet = false; navController.navigate(Routes.addBorrowRoute(-1L)) },
            onSelectLend = { showAddSelectionSheet = false; navController.navigate(Routes.addLendRoute(-1L)) }
        )
    }

    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_account_title)) },
            text = { Text(stringResource(R.string.dialog_delete_account_msg, accountToDelete?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = { accountToDelete?.let { viewModel.deleteAccount(it) }; accountToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { accountToDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
fun AccountItemCard(
    account: Account,
    currentBalance: Double,
    isDefault: Boolean,
    elevation: Dp,
    onSetDefault: () -> Unit,
    onClick: () -> Unit
) {
    val isCredit = account.category == "CREDIT"

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().shadow(elevation, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(IconMapper.getIcon(account.iconName), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        // 注意：AccountTypeManager 如果有硬编码字符串，也需要修改该类，目前暂保持原样调用
                        Text(
                            text = AccountTypeManager.getDisplayName(account.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.Start) {
                        // [修改] 欠款文本
                        val balanceText = if (isCredit) {
                            "${stringResource(R.string.label_arrears)} ${account.currency} ${String.format(Locale.US, "%.2f", abs(currentBalance.coerceAtMost(0.0)))}"
                        } else {
                            "${account.currency} ${String.format(Locale.US, "%.2f", currentBalance)}"
                        }

                        Text(
                            text = balanceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCredit && currentBalance < 0) Color(0xFFE53935) else MaterialTheme.colorScheme.outline
                        )

                        if (isCredit) {
                            Text(
                                text = "${stringResource(R.string.label_total_limit)} ${account.currency} ${String.format(Locale.US, "%.0f", account.creditLimit ?: 0.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSetDefault) {
                    Icon(
                        imageVector = if (isDefault) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(R.string.set_as_default),
                        tint = if (isDefault) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
                Icon(Icons.Default.DragIndicator, contentDescription = stringResource(R.string.account_reorder), tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 4.dp))
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
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Box(Modifier.padding(top = 8.dp, bottom = 10.dp).align(Alignment.CenterHorizontally).width(42.dp).height(5.dp).clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)))
            Text(text = stringResource(R.string.sheet_title_add_account_record), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

            AddAccountSheetItem(Icons.Default.AccountBalance, stringResource(R.string.type_funds_account), stringResource(R.string.desc_funds_account), onSelectFunds)
            Spacer(Modifier.height(10.dp))
            AddAccountSheetItem(Icons.Default.CreditCard, stringResource(R.string.type_credit_account), stringResource(R.string.desc_credit_account), onSelectCredit)
            Spacer(Modifier.height(10.dp))
            AddAccountSheetItem(Icons.Default.SwapHoriz, stringResource(R.string.action_add_borrow), stringResource(R.string.desc_add_borrow_record), onSelectBorrow)
            Spacer(Modifier.height(10.dp))
            AddAccountSheetItem(Icons.Default.SwapHoriz, stringResource(R.string.action_add_lend), stringResource(R.string.desc_add_lend_record), onSelectLend)
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.cancel)) }
        }
    }
}

@Composable
private fun AddAccountSheetItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        }
    }
}