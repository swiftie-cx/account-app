package com.example.myapplication.ui.screen

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    // 获取账户列表和默认ID
    val accounts by viewModel.allAccounts.collectAsState()
    val defaultAccountId by viewModel.defaultAccountId.collectAsState()

    // 本地状态用于拖拽排序
    var listData by remember(accounts) { mutableStateOf(accounts) }

    // 拖拽状态管理
    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            listData = listData.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { _, _ ->
            viewModel.reorderAccounts(listData) // 拖拽结束保存顺序到数据库
        }
    )

    // 当数据库数据更新时（例如新增了账户），同步更新列表
    LaunchedEffect(accounts) {
        if (listData.size != accounts.size || listData.toSet() != accounts.toSet()) {
            listData = accounts
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("账户管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ADD_ACCOUNT) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                "长按拖拽排序，点击圆圈设为默认",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(state)
                    .detectReorderAfterLongPress(state),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listData, key = { it.id }) { account ->
                    ReorderableItem(state, key = account.id) { isDragging ->
                        val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)

                        AccountItem(
                            account = account,
                            isDefault = (account.id == defaultAccountId),
                            onSetDefault = { viewModel.setDefaultAccount(account.id) },
                            onClick = {
                                navController.navigate("${Routes.ADD_ACCOUNT}?accountId=${account.id}")
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .shadow(elevation.value, RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: Account,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 单选框 (设为默认)
            IconButton(onClick = onSetDefault) {
                Icon(
                    imageVector = if (isDefault) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "设为默认",
                    tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            val icon = IconMapper.getIcon(account.iconName)
            Icon(
                imageVector = icon,
                contentDescription = account.name,
                modifier = Modifier.size(24.dp),
                tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.width(16.dp))

            // 账户信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 【关键修复】删除了 initialAmount 和 currency，改用 type
                Text(
                    text = account.type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 拖拽把手图标
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "排序",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        }
    }
}