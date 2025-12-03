package com.example.myapplication.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.navigation.Routes
// (新增) 导入 VM
import com.example.myapplication.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    defaultCurrency: String,
    viewModel: ExpenseViewModel // (修改) 接收 ViewModel
) {
    // 状态：控制两个弹窗
    var showWarningDialog by remember { mutableStateOf(false) }
    var showFinalConfirmDialog by remember { mutableStateOf(false) }

    // 状态：第二次弹窗的输入内容
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
                .padding(16.dp)
        ) {
            // 1. 个人信息
            SettingsItem(
                icon = Icons.Default.Person,
                title = "个人信息",
                onClick = { /* TODO */ }
            )

            // 2. 类别设置
            SettingsItem(
                icon = Icons.Default.Category,
                title = "类别设置",
                onClick = { navController.navigate(Routes.CATEGORY_SETTINGS) }
            )

            // 3. 默认货币
            SettingsItem(
                icon = Icons.Default.Paid,
                title = "默认货币",
                value = defaultCurrency,
                onClick = { navController.navigate(Routes.CURRENCY_SELECTION) }
            )

            // 4. 主题
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "主题",
                onClick = { /* TODO */ }
            )

            // 5. 隐私密码
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "隐私密码",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. 清除数据 (点击触发第一步警告)
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

    // --- 第一步：警告弹窗 ---
    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("警告") },
            text = { Text("您即将清除所有的记账记录、账户信息和预算设置。\n\n此操作不可恢复！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWarningDialog = false
                        showFinalConfirmDialog = true // 进入第二步
                        confirmationInput = "" // 重置输入框
                    }
                ) {
                    Text("下一步", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // --- 第二步：输入验证弹窗 ---
    if (showFinalConfirmDialog) {
        val isMatch = confirmationInput == targetPhrase

        AlertDialog(
            onDismissRequest = { showFinalConfirmDialog = false },
            title = { Text("最终确认") },
            text = {
                Column {
                    Text("为了防止误操作，请在下方输入：")
                    Text(
                        text = targetPhrase,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = confirmationInput,
                        onValueChange = { confirmationInput = it },
                        placeholder = { Text(targetPhrase) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            cursorColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // (修改) 真正调用清除数据逻辑
                        viewModel.clearAllData()
                        showFinalConfirmDialog = false
                    },
                    enabled = isMatch, // 只有输入匹配时按钮才可用
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("彻底删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalConfirmDialog = false }) {
                    Text("取消")
                }
            }
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
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
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
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}