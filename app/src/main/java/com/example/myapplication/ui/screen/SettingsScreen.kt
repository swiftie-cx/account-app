package com.example.myapplication.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val context = LocalContext.current

    var showWarningDialog by remember { mutableStateOf(false) }
    var showFinalConfirmDialog by remember { mutableStateOf(false) }
    var confirmationInput by remember { mutableStateOf("") }
    val targetPhrase = "确认清除全部数据"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) },
                // navigationIcon 移除
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                UserStatusHeader(
                    isLoggedIn = isLoggedIn,
                    email = userEmail,
                    onClick = {
                        if (isLoggedIn) {
                            navController.navigate(Routes.USER_INFO)
                        } else {
                            navController.navigate(Routes.LOGIN)
                        }
                    }
                )
            }

            SettingsGroup(title = "常规") {
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = "类别设置",
                    onClick = { navController.navigate(Routes.CATEGORY_SETTINGS) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Repeat, // 使用 Repeat 图标
                    title = "周期记账",
                    onClick = { navController.navigate(Routes.PERIODIC_BOOKKEEPING) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Paid,
                    title = "默认货币",
                    value = defaultCurrency,
                    onClick = { navController.navigate(Routes.CURRENCY_SELECTION) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "主题风格",
                    onClick = { navController.navigate(Routes.THEME_SETTINGS) }
                )
            }

            SettingsGroup(title = "安全") {
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
            }

            SettingsGroup(title = "数据") {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "清除所有数据",
                    titleColor = MaterialTheme.colorScheme.error,
                    iconColor = MaterialTheme.colorScheme.error,
                    showArrow = false,
                    onClick = { showWarningDialog = true }
                )
            }

            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

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

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

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
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (isLoggedIn) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "开启云同步与高级功能",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showArrow) {
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(20.dp))
        }
    }
}