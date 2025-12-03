package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
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
// (修复) 导入正确的 Routes 包路径
import com.example.myapplication.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.ADD_ACCOUNT) }) {
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

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = IconMapper.getIcon(account.iconName)
            Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "${account.currency} - ${account.type}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "${account.currency} ${String.format("%.2f", account.initialBalance)}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}