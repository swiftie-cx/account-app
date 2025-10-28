package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.* // 使用 * 导入
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.* // 使用 * 导入
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
// --- (修复) 添加 Import ---
import com.example.myapplication.ui.screen.Routes
// --- 修复结束 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    Scaffold(
        // 将 FAB 移到这里，因为 AssetsScreen 现在是账户列表的主要入口
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.ADD_ACCOUNT) }) { // 使用 Routes
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("账户管理", style = MaterialTheme.typography.headlineLarge)

            // 这里的列表可以用于编辑/删除操作
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(accounts) { account ->
                    // 可以添加编辑/删除按钮或使条目可点击以进行编辑
                    AccountItem(account = account)
                }
            }
        }
    }
}

// AccountItem 显示账户信息，可以添加编辑/删除交互
@Composable
fun AccountItem(account: Account) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        // 可以添加 .clickable {} 来导航到编辑页面
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = IconMapper.getIcon(account.iconName)
            Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "${account.currency} - ${account.type}", style = MaterialTheme.typography.bodyMedium)
            }
            // 显示初始余额，因为这个页面可能主要用于管理，而不是看实时余额
            Text(
                text = "${account.currency} ${String.format("%.2f", account.initialBalance)}",
                style = MaterialTheme.typography.titleMedium
            )
            // 可以在这里添加编辑/删除图标按钮
        }
    }
}