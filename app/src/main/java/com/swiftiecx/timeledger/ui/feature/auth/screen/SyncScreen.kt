package com.swiftiecx.timeledger.ui.feature.auth.screen

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.ui.viewmodel.model.SyncUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.swiftiecx.timeledger.data.repository.SyncStrategy
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    navController: NavController,
    viewModel: ExpenseViewModel
) {
    // ✅ 先把资源字符串取出来（不能在 LaunchedEffect 里取）
    val unknownTimeText = stringResource(R.string.sync_unknown_time)

    // ✅ TODO_RENAME_1：把 syncUiState 改成你 ViewModel 里真实存在的 StateFlow 名字
    val syncState by viewModel.syncState.collectAsState() // TODO_RENAME_1
    val userEmail by viewModel.userEmail.collectAsState()

    var showConflictDialog by remember { mutableStateOf(false) }
    var cloudTimeStr by remember { mutableStateOf(unknownTimeText) }

    LaunchedEffect(syncState) {
        if (syncState is SyncUiState.Conflict) {
            val cloudTime = (syncState as SyncUiState.Conflict).cloudTime
            cloudTimeStr = if (cloudTime != null && cloudTime > 0L) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                sdf.format(Date(cloudTime))
            } else {
                unknownTimeText
            }
            showConflictDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (userEmail.isNotEmpty())
                    stringResource(R.string.sync_current_account_label, userEmail)
                else
                    stringResource(R.string.sync_not_logged_in),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.sync_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            when (val state = syncState) {
                is SyncUiState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(state.msg, color = MaterialTheme.colorScheme.primary)
                }

                is SyncUiState.Success -> {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.msg,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(24.dp))

                    // ✅ TODO_RENAME_2：如果你 ViewModel 里不是 resetSyncState()，改成你真实的方法名
                    Button(onClick = { viewModel.resetSyncState() }) { // TODO_RENAME_2
                        Text(stringResource(R.string.finish))
                    }
                }

                is SyncUiState.Error -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(state.err, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.startSync() }) {
                        Text(stringResource(R.string.sync_retry))
                    }
                }

                else -> {
                    Button(
                        onClick = { viewModel.startSync() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = userEmail.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.sync_start), fontSize = 18.sp)
                    }
                }
            }
        }
    }

    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = {
                showConflictDialog = false
                viewModel.resetSyncState() // TODO_RENAME_2 同上
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.sync_conflict_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.sync_conflict_msg1))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.sync_cloud_time_label, cloudTimeStr),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.sync_conflict_msg2),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConflictDialog = false
                    viewModel.performSync(SyncStrategy.MERGE)
                }) { Text(stringResource(R.string.sync_merge_strategy)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showConflictDialog = false
                        viewModel.performSync(SyncStrategy.OVERWRITE_LOCAL)
                    }) { Text(stringResource(R.string.sync_overwrite_local_strategy)) }

                    TextButton(onClick = {
                        showConflictDialog = false
                        viewModel.performSync(SyncStrategy.OVERWRITE_CLOUD)
                    }) { Text(stringResource(R.string.sync_overwrite_cloud_strategy)) }
                }
            }
        )
    }
}
