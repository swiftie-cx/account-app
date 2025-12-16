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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.swiftiecx.timeledger.R
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

    // 【修改 1】在 Composable 上下文获取资源字符串
    val unknownTimeText = stringResource(R.string.sync_unknown_time)

    // 监听冲突状态，控制弹窗显示
    var showConflictDialog by remember { mutableStateOf(false) }
    var cloudTimeStr by remember { mutableStateOf("") }

    LaunchedEffect(syncState) {
        if (syncState is SyncUiState.Conflict) {
            val time = (syncState as SyncUiState.Conflict).cloudTime
            // 【修改 2】使用普通变量 unknownTimeText
            cloudTimeStr = if (time > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(time)) else unknownTimeText
            showConflictDialog = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.sync_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                text = if (userEmail.isNotEmpty()) stringResource(R.string.sync_current_account_label, userEmail) else stringResource(R.string.sync_not_logged_in),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.sync_description),
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
                        Text(stringResource(R.string.finish))
                    }
                }
                is SyncUiState.Error -> {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(state.err, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.startSync() }) {
                        Text(stringResource(R.string.sync_retry))
                    }
                }
                else -> {
                    // Idle 状态或 Conflict 状态(Conflict时弹窗，背景不动)
                    Button(
                        onClick = { viewModel.startSync() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = userEmail.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.sync_start), fontSize = 18.sp)
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
            title = { Text(stringResource(R.string.sync_conflict_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.sync_conflict_msg1))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.sync_cloud_time_label, cloudTimeStr), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.sync_conflict_msg2), fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConflictDialog = false
                        viewModel.performSync(SyncStrategy.MERGE) // 智能合并
                    }
                ) {
                    Text(stringResource(R.string.sync_merge_strategy))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showConflictDialog = false
                        viewModel.performSync(SyncStrategy.OVERWRITE_LOCAL)
                    }) {
                        Text(stringResource(R.string.sync_overwrite_local_strategy))
                    }
                    TextButton(onClick = {
                        showConflictDialog = false
                        viewModel.performSync(SyncStrategy.OVERWRITE_CLOUD)
                    }) {
                        Text(stringResource(R.string.sync_overwrite_cloud_strategy))
                    }
                }
            }
        )
    }
}