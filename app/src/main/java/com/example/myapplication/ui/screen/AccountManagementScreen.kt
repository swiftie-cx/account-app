package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button // (新) 导入
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
// (删除) FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.ui.navigation.IconMapper // (新) 导入
import com.example.myapplication.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    Scaffold(
        // (修改) 移除 FAB
        /*
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.ADD_ACCOUNT) }) {
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        }
        */
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("账户管理", style = MaterialTheme.typography.headlineLarge)

            // (新) 添加矩形按钮
            Button(
                onClick = { navController.navigate(Routes.ADD_ACCOUNT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp) // 增加一些垂直间距
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账户", modifier = Modifier.padding(end = 8.dp))
                Text("添加账户")
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) { // 调整内边距
                items(accounts) { account ->
                    AccountItem(account = account)
                }
            }
        }
    }
}

@Composable
fun AccountItem(account: Account) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = IconMapper.getIcon(account.iconName)
            Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "${account.currency} - ${account.type}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "¥${account.initialBalance}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}