package com.example.myapplication.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import com.example.myapplication.ui.navigation.Routes
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    // 获取账户列表和默认ID
    val accounts by viewModel.allAccounts.collectAsState()
    val defaultAccountId by viewModel.defaultAccountId.collectAsState()

    var listData by remember(accounts) { mutableStateOf(accounts) }

    // 拖拽状态管理
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        listData = listData.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }, onDragEnd = { _, _ ->
        viewModel.reorderAccounts(listData) // 拖拽结束保存顺序
    })

    LaunchedEffect(accounts) {
        listData = accounts
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
            // (修改) 确保 FAB 样式正确，并添加 padding 防止贴边过近被系统手势或圆角遮挡
            FloatingActionButton(
                onClick = { navController.navigate(Routes.addAccountRoute()) },
                containerColor = MaterialTheme.colorScheme.primary, // 显式设置颜色
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(16.dp) // 增加额外的 padding 确保不贴边
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        },
        // (可选) 显式指定 FAB 位置，通常默认为 End，这里再次确认
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "长按拖拽排序，点击圆圈设为默认",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(state)
                    .detectReorderAfterLongPress(state),
                // (关键) 给底部留出空间，防止 FAB 遮挡最后一个列表项
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(listData, key = { it.id }) { account ->
                    ReorderableItem(state, key = account.id) { isDragging ->
                        val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)

                        AccountItem(
                            account = account,
                            isDefault = (account.id == defaultAccountId),
                            onSetDefault = { viewModel.setDefaultAccount(account.id) },
                            onClick = { navController.navigate(Routes.addAccountRoute(account.id)) },
                            modifier = Modifier
                                .shadow(elevation.value)
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
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
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
            Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "${account.currency} - ${account.type}", style = MaterialTheme.typography.bodyMedium)
            }

            // 拖拽把手图标
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}