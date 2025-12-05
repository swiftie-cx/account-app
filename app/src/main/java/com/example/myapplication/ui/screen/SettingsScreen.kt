package com.example.myapplication.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    defaultCurrency: String,
    viewModel: ExpenseViewModel
) {
    // 监听登录状态和用户信息
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val context = LocalContext.current

    // 状态：控制警告弹窗
    var showWarningDialog by remember { mutableStateOf(false) }
    var showFinalConfirmDialog by remember { mutableStateOf(false) }
    var confirmationInput by remember { mutableStateOf("") }
    val targetPhrase = "确认清除全部数据"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // --- 顶部：用户状态卡片 ---
            UserStatusHeader(
                isLoggedIn = isLoggedIn,
                email = userEmail,
                onClick = {
                    if (isLoggedIn) {
                        // 已登录 -> 跳转详细信息页
                        navController.navigate(Routes.USER_INFO)
                    } else {
                        // 未登录 -> 跳转登录页
                        navController.navigate(Routes.LOGIN)
                    }
                }
            )

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            Column(modifier = Modifier.padding(16.dp)) {
                // 1. 类别设置
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = "类别设置",
                    onClick = { navController.navigate(Routes.CATEGORY_SETTINGS) }
                )

                // 2. 默认货币
                SettingsItem(
                    icon = Icons.Default.Paid,
                    title = "默认货币",
                    value = defaultCurrency,
                    onClick = { navController.navigate(Routes.CURRENCY_SELECTION) }
                )

                // 3. 主题
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "主题",
                    onClick = { navController.navigate(Routes.THEME_SETTINGS) }
                )

                // 4. 隐私密码 (增加未登录拦截)
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "隐私密码",
                    onClick = {
                        if (isLoggedIn) {
                            navController.navigate(Routes.PRIVACY_SETTINGS)
                        } else {
                            Toast.makeText(context, "请先登录账号", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. 清除数据
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "清除数据",
                    titleColor = MaterialTheme.colorScheme.error,
                    iconColor = MaterialTheme.colorScheme.error,
                    showArrow = false,
                    onClick = { showWarningDialog = true }
                )
            }
        }
    }

    // --- 警告弹窗 ---
    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("警告") },
            text = { Text("您即将清除所有的记账记录、账户信息和预算设置。\n\n此操作不可恢复！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWarningDialog = false
                        showFinalConfirmDialog = true
                        confirmationInput = ""
                    }
                ) { Text("下一步", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showWarningDialog = false }) { Text("取消") } }
        )
    }

    // --- 最终确认弹窗 ---
    if (showFinalConfirmDialog) {
        val isMatch = confirmationInput == targetPhrase
        AlertDialog(
            onDismissRequest = { showFinalConfirmDialog = false },
            title = { Text("最终确认") },
            text = {
                Column {
                    Text("为了防止误操作，请在下方输入：")
                    Text(text = targetPhrase, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedTextField(
                        value = confirmationInput,
                        onValueChange = { confirmationInput = it },
                        placeholder = { Text(targetPhrase) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.error, cursorColor = MaterialTheme.colorScheme.error)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAllData(); showFinalConfirmDialog = false },
                    enabled = isMatch,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("彻底删除") }
            },
            dismissButton = { TextButton(onClick = { showFinalConfirmDialog = false }) { Text("取消") } }
        )
    }
}

// 顶部用户状态组件
@Composable
fun UserStatusHeader(
    isLoggedIn: Boolean,
    email: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 文字信息
        Column(modifier = Modifier.weight(1f)) {
            if (isLoggedIn) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "查看账号信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "登录 / 注册",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "登录后使用云同步和隐私功能",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showArrow) {
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}