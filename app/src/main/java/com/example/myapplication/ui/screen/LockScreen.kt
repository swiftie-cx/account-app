package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    viewModel: ExpenseViewModel,
    onUnlockSuccess: () -> Unit
) {
    val privacyType = viewModel.getPrivacyType()
    var inputPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    // 控制弹窗显示
    var showEmailUnlockDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.8f))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "请验证密码",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 错误提示区
            Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if (errorMsg.isNotEmpty()) {
                    Text(text = errorMsg, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- 锁屏逻辑 (PIN / Pattern) ---
            if (privacyType == "PIN") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    repeat(4) { index ->
                        val filled = index < inputPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        )
                    }
                }

                Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.BottomCenter) {
                    InteractivePinPad(
                        onNumberClick = { num ->
                            if (inputPin.length < 4) {
                                inputPin += num
                                errorMsg = ""
                                if (inputPin.length == 4) {
                                    if (viewModel.verifyPin(inputPin)) {
                                        onUnlockSuccess()
                                    } else {
                                        errorMsg = "密码错误"
                                        inputPin = ""
                                    }
                                }
                            }
                        },
                        onDeleteClick = { if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1) }
                    )
                }

            } else if (privacyType == "PATTERN") {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    InteractivePatternLock(
                        onPatternComplete = { pattern ->
                            if (viewModel.verifyPattern(pattern)) {
                                onUnlockSuccess()
                            } else {
                                errorMsg = "图案错误"
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }

            // --- 底部忘记密码按钮 ---
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = { showEmailUnlockDialog = true },
                border = null
            ) {
                Text("忘记密码？使用账号验证")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // --- 弹窗调用 ---
    if (showEmailUnlockDialog) {
        UnlockByEmailDialog(
            viewModel = viewModel,
            onDismiss = { showEmailUnlockDialog = false },
            onUnlockSuccess = {
                // 验证成功：重置锁并进入 App
                viewModel.setPrivacyType("NONE")
                viewModel.setBiometricEnabled(false)
                onUnlockSuccess()
            }
        )
    }
}

/**
 * 现代化的验证弹窗
 */
@Composable
fun UnlockByEmailDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    val savedEmail by viewModel.userEmail.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(Icons.Default.Security to "账号密码", Icons.Outlined.MarkEmailRead to "验证码")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf(savedEmail) }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var isCountingDown by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(60) }
    var passwordVisible by remember { mutableStateOf(false) }

    // 用状态来控制显示的错误信息，替代 Toast
    var uiMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头部图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "安全验证",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Tab栏
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    divider = {},
                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                ) {
                    tabs.forEachIndexed { index, (icon, title) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                uiMessage = null // 切换 Tab 清除错误
                            },
                            text = { Text(title) },
                            icon = { Icon(imageVector = icon, contentDescription = null) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 表单内容
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 邮箱输入框
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            uiMessage = null
                        },
                        label = { Text("注册邮箱") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    if (selectedTab == 0) {
                        // 密码模式
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                uiMessage = null
                            },
                            label = { Text("登录密码") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                    } else {
                        // 验证码模式
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = {
                                    code = it
                                    uiMessage = null
                                },
                                label = { Text("验证码") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.medium
                            )
                            // 获取验证码按钮
                            Button(
                                onClick = {
                                    if (email.isBlank()) {
                                        uiMessage = "请输入邮箱"; isError = true
                                    } else if (!viewModel.isEmailRegistered(email)) {
                                        uiMessage = "该邮箱未注册"; isError = true
                                    } else {
                                        // 立即倒计时
                                        isCountingDown = true
                                        uiMessage = null // 清除旧错误
                                        scope.launch {
                                            countdown = 60
                                            while (countdown > 0 && isCountingDown) {
                                                delay(1000)
                                                countdown--
                                            }
                                            isCountingDown = false
                                            countdown = 60
                                        }

                                        // 发送邮件
                                        viewModel.sendCodeToEmail(
                                            email = email,
                                            onSuccess = {
                                                uiMessage = "验证码已发送"; isError = false
                                            },
                                            onError = { msg ->
                                                isCountingDown = false
                                                uiMessage = msg; isError = true
                                            }
                                        )
                                    }
                                },
                                enabled = !isCountingDown && email.isNotBlank(),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.height(OutlinedTextFieldDefaults.MinHeight)
                            ) {
                                Text(if (isCountingDown) "${countdown}s" else "获取")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- 消息提示区域 (替代 Toast) ---
                // 这里用一个 Text 占位，显示错误或成功信息
                Box(
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiMessage != null) {
                        Text(
                            text = uiMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 提示文案
                Text(
                    text = "为保障账户安全，验证通过后将重置应用锁设置。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                // 操作按钮
                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            // 账号密码验证
                            if (viewModel.login(email, password)) {
                                onUnlockSuccess()
                            } else {
                                uiMessage = "账号或密码错误"; isError = true
                            }
                        } else {
                            // 验证码验证
                            if (viewModel.verifyCode(email, code)) {
                                onUnlockSuccess()
                            } else {
                                uiMessage = "验证码错误或邮箱不匹配"; isError = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("验证并解锁", modifier = Modifier.padding(vertical = 4.dp))
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("取消")
                }
            }
        }
    }
}