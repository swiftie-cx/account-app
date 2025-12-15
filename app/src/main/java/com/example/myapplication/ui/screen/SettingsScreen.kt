package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    defaultCurrency: String,
    viewModel: ExpenseViewModel
) {
    val scrollState = rememberScrollState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. 用户信息卡片 ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 如果已登录，去用户信息页；如果未登录，去登录页
                        if (isLoggedIn) {
                            navController.navigate(Routes.USER_INFO)
                        } else {
                            navController.navigate(Routes.LOGIN)
                        }
                    },
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 用户名/邮箱
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) userEmail else "点击登录/注册",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLoggedIn) {
                            Text(
                                text = "查看账号信息",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "登录以同步数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. 常规设置 ---
            SettingsGroupTitle("常规")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Category,
                        title = "类别设置",
                        onClick = { navController.navigate(Routes.CATEGORY_SETTINGS) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Default.Repeat,
                        title = "周期记账",
                        onClick = { navController.navigate(Routes.PERIODIC_BOOKKEEPING) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "默认货币",
                        value = defaultCurrency,
                        onClick = { navController.navigate(Routes.CURRENCY_SELECTION) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "主题风格",
                        onClick = { navController.navigate(Routes.THEME_SETTINGS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. 安全设置 ---
            SettingsGroupTitle("安全")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "隐私密码",
                        onClick = { navController.navigate(Routes.PRIVACY_SETTINGS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 4. 数据设置 (【新增】同步入口) ---
            SettingsGroupTitle("数据")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // 【新增】云端同步按钮
                    SettingsItem(
                        icon = Icons.Default.CloudSync, // 需要 import androidx.compose.material.icons.filled.CloudSync
                        title = "云端同步",
                        onClick = {
                            // 如果已登录直接去同步页，未登录则先去登录页
                            if (isLoggedIn) {
                                navController.navigate(Routes.SYNC)
                            } else {
                                navController.navigate(Routes.LOGIN)
                            }
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // 清除数据按钮
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "清除所有数据",
                        textColor = MaterialTheme.colorScheme.error,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = { showClearDataDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 清除数据确认弹窗
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("确认清除?") },
            text = { Text("此操作将删除本地所有账单、账户和设置数据，且无法恢复。\n(不会删除云端备份)") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// 辅助组件：分组标题
@Composable
fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp)
    )
}

// 辅助组件：设置项
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}