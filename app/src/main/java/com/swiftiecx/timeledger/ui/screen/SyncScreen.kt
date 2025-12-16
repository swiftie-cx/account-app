package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.swiftiecx.timeledger.data.SyncStrategy
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.ui.viewmodel.SyncUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(navController: NavController, viewModel: ExpenseViewModel) {
    val syncState by viewModel.syncState.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState(initial = "")

    // 监听冲突状态，控制弹窗显示
    var showConflictDialog by remember { mutableStateOf(false) }
    var cloudTimeStr by remember { mutableStateOf("") }

    LaunchedEffect(syncState) {
        if (syncState is SyncUiState.Conflict) {
            val time = (syncState as SyncUiState.Conflict).cloudTime
            cloudTimeStr = if (time > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(time)) else "未知时间"
            showConflictDialog = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("云端同步") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // 顶部图标区
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = if (userEmail.isNotEmpty()) "当前账号: $userEmail" else "未登录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "点击同步将自动备份或恢复数据。\n如果两端都有数据，系统将询问处理方式。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // 状态显示与按钮区
            when (val state = syncState) {
                is SyncUiState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(state.msg, color = MaterialTheme.colorScheme.primary)
                }
                is SyncUiState.Success -> {
                    Icon(Icons.Default.Done, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(state.msg, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.resetSyncState() }) {
                        Text("完成")
                    }
                }
                is SyncUiState.Error -> {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(state.err, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.startSync() }) {
                        Text("重试")
                    }
                }
                else -> {
                    // Idle 状态或 Conflict 状态(Conflict时弹窗，背景不动)
                    Button(
                        onClick = { viewModel.startSync() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = userEmail.isNotEmpty()
                    ) {
                        Text("开始同步", fontSize = 18.sp)
                    }
                }
            }
        }
    }

    // --- 冲突处理弹窗 ---
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = {
                showConflictDialog = false
                viewModel.resetSyncState() // 点外部取消，回到初始状态
            },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("发现数据冲突") },
            text = {
                Column {
                    Text("本地和云端都存在数据。")
                    Spacer(Modifier.height(8.dp))
                    Text("云端备份时间: $cloudTimeStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("请选择操作：", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConflictDialog = false
                        viewModel.performSync(SyncStrategy.MERGE) // 智能合并
                    }
                ) {
                    Text("合并数据 (推荐)")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showConflictDialog = false
                        viewModel.performSync(SyncStrategy.OVERWRITE_LOCAL)
                    }) {
                        Text("以云端为准")
                    }
                    TextButton(onClick = {
                        showConflictDialog = false
                        viewModel.performSync(SyncStrategy.OVERWRITE_CLOUD)
                    }) {
                        Text("以本地为准")
                    }
                }
            }
        )
    }
}