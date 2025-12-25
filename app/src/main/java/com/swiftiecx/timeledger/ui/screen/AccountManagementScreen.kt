package com.swiftiecx.timeledger.ui.screen

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
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
                    // ✅ 修复提示语：增加左滑删除指引
                    text = "提示：长按拖拽排序，点击星标设为默认，向左滑动可删除账户。",
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
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
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
            title = { Text("确认删除账户？") },
            text = { Text("删除账户“${accountToDelete?.name}”将无法恢复，与其关联的所有账单记录也会被永久删除。") },
            confirmButton = {
                TextButton(
                    onClick = { accountToDelete?.let { viewModel.deleteAccount(it) }; accountToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { accountToDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
fun AccountItemCard(
    account: Account,
    currentBalance: Double,
    isDefault: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
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

                // ✅ 调整数据项容器布局：使用 Alignment.Top 压缩空白
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // ✅ 修复标签颜色：使用更和谐的淡品牌色
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = AccountTypeManager.getDisplayName(account.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // ✅ 数据对齐：通过 Column(Alignment.Start) 确保欠款与额度完美对齐
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = if (isCredit) "欠款: ${account.currency} ${String.format(Locale.US, "%.2f", abs(currentBalance.coerceAtMost(0.0)))}"
                            else "${account.currency} ${String.format(Locale.US, "%.2f", currentBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCredit && currentBalance < 0) Color(0xFFE53935) else MaterialTheme.colorScheme.outline
                        )

                        if (isCredit) {
                            Text(
                                text = "总额度: ${account.currency} ${String.format(Locale.US, "%.0f", account.creditLimit ?: 0.0)}",
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
            Text(text = "添加记录 / 账户", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            AddAccountSheetItem(Icons.Default.AccountBalance, "资产账户", "现金 / 储蓄 / 银行卡", onSelectFunds)
            Spacer(Modifier.height(10.dp))
            AddAccountSheetItem(Icons.Default.CreditCard, "信贷账户", "信用卡 / 花呗等额度账户", onSelectCredit)
            Spacer(Modifier.height(10.dp))
            AddAccountSheetItem(Icons.Default.SwapHoriz, "新增借入", "我欠别人的：录入一笔借入记录", onSelectBorrow)
            Spacer(Modifier.height(10.dp))
            AddAccountSheetItem(Icons.Default.SwapHoriz, "新增借出", "别人欠我的：录入一笔外借记录", onSelectLend)
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("取消") }
        }
    }
}

@Composable
private fun AddAccountSheetItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        }
    }
}