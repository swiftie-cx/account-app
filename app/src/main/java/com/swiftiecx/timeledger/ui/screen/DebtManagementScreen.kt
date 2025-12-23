package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.data.DebtRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtManagementScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    // 这里我们获取所有 DEBT 类型的账户并展示其记录
    val debtAccounts = remember(allAccounts) { allAccounts.filter { it.category == "DEBT" } }

    // 汇总所有债务账户的记录
    var allDebtRecords by remember { mutableStateOf(emptyList<DebtRecord>()) }

    LaunchedEffect(debtAccounts) {
        // 实际开发中建议在 ViewModel 中写一个 getAllDebtRecords 的 Flow
        // 这里为了演示逻辑，通过 accountId 列表获取
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("债务管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 这里可以直接复用你之前写的 DebtRecordItem
            LazyColumn {
                // items(allDebtRecords) { record -> ... }
                item {
                    Text("借入/借出明细汇总", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(16.dp))
                }
                // 建议：展示统计卡片（总借入/总借出）后再跟列表
            }
        }
    }
}