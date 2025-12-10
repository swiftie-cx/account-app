package com.example.myapplication.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 封装一个居中显示的 SnackbarHost ---
@Composable
fun CenteredSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState) { data ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface, // 默认深色背景
            contentColor = MaterialTheme.colorScheme.inverseOnSurface, // 默认浅色文字
            shape = RoundedCornerShape(8.dp)
        ) {
            // 关键：fillMaxWidth + TextAlign.Center 实现文字居中
            Text(
                text = data.visuals.message,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// 1. 用户信息主菜单
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    val email by viewModel.userEmail.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        // 使用自定义的居中 SnackbarHost
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("用户信息") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(text = email, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(32.dp))

            UserInfoItem(
                icon = Icons.Default.LockReset,
                title = "修改密码",
                onClick = { navController.navigate(Routes.CHANGE_PASSWORD) }
            )

            UserInfoItem(
                icon = Icons.Default.HelpOutline,
                title = "忘记密码",
                onClick = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.logout()
                    scope.launch { snackbarHostState.showSnackbar("已退出登录") }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("退出登录")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.deleteUserAccount()
                    scope.launch { snackbarHostState.showSnackbar("账号已注销") }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("注销账号")
            }
        }
    }
}

// 2. 登录界面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("登录") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { navController.navigate(Routes.FORGOT_PASSWORD) }) {
                    Text("忘记密码？")
                }
            }

            Spacer(Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (viewModel.login(email, password)) {
                            // 登录成功，直接返回
                            navController.popBackStack()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("账号或密码错误") }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    enabled = email.isNotBlank() && password.isNotBlank()
                ) {
                    Text("登录")
                }

                OutlinedButton(
                    onClick = { navController.navigate(Routes.REGISTER) },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("注册")
                }
            }
        }
    }
}

// 3. 注册界面 (修复导航 + 即时反馈 + 居中提示)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    var isCountingDown by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(60) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        // 使用居中显示的 Snackbar
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("注册账号") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text("邮箱") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = emailError != null,
                supportingText = { if (emailError != null) Text(emailError!!) }
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("设置密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("验证码") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))

                // 获取验证码按钮
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("请输入邮箱") }
                        } else if (viewModel.isEmailRegistered(email)) {
                            emailError = "该邮箱已注册，请直接登录"
                        } else {
                            // 1. 立即开始倒计时 (UI 优先)
                            isCountingDown = true
                            scope.launch {
                                countdown = 60
                                while (countdown > 0 && isCountingDown) {
                                    delay(1000)
                                    countdown--
                                }
                                isCountingDown = false
                                countdown = 60
                            }

                            // 2. 发送邮件
                            viewModel.sendCodeToEmail(
                                email = email,
                                onSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar("验证码已发送至邮箱") }
                                },
                                onError = { msg ->
                                    // 3. 失败回滚
                                    isCountingDown = false
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    },
                    enabled = !isCountingDown && email.isNotBlank() && emailError == null
                ) {
                    Text(if (isCountingDown) "${countdown}s" else "获取验证码")
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (password.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("请设置密码") }
                    } else if (password != confirmPassword) {
                        scope.launch { snackbarHostState.showSnackbar("两次密码不一致") }
                    } else if (code.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("请输入验证码") }
                    } else {
                        if (viewModel.verifyCode(email, code)) {
                            viewModel.register(email, password)
                            // 注册成功后，直接回到设置页，跳过登录页
                            navController.popBackStack(Routes.LOGIN, inclusive = true)
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("验证码错误") }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && code.isNotBlank()
            ) {
                Text("注册并登录")
            }
        }
    }
}

// 4. 修改密码
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var step by remember { mutableIntStateOf(1) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("修改密码") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (step == 1) {
                Text("第一步：验证身份", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("输入旧密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (viewModel.verifyUserPassword(oldPassword)) {
                            step = 2
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("旧密码错误") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("下一步")
                }
            } else {
                Text("第二步：设置新密码", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("输入新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (newPassword.isNotBlank() && newPassword == confirmPassword) {
                            viewModel.saveUserPassword(newPassword)
                            scope.launch { snackbarHostState.showSnackbar("密码修改成功") }
                            navController.popBackStack()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("两次密码不一致或为空") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认")
                }
            }
        }
    }
}

// 5. 忘记密码
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var step by remember { mutableIntStateOf(1) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isCountingDown by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(60) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val boundEmail by viewModel.userEmail.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    LaunchedEffect(isLoggedIn, boundEmail) {
        if (isLoggedIn && boundEmail.isNotBlank()) {
            email = boundEmail
        }
    }

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("重置密码") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (step == 1) {
                Text("第一步：验证邮箱", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoggedIn
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("验证码") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (email.isNotBlank()) {
                                if (viewModel.isEmailRegistered(email)) {
                                    isCountingDown = true
                                    scope.launch {
                                        countdown = 60
                                        while (countdown > 0 && isCountingDown) {
                                            delay(1000)
                                            countdown--
                                        }
                                        isCountingDown = false
                                        countdown = 60
                                    }

                                    viewModel.sendCodeToEmail(
                                        email = email,
                                        onSuccess = {
                                            scope.launch { snackbarHostState.showSnackbar("验证码已发送") }
                                        },
                                        onError = { msg ->
                                            isCountingDown = false
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("该邮箱未注册") }
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("请输入邮箱") }
                            }
                        },
                        enabled = !isCountingDown && email.isNotBlank()
                    ) {
                        Text(if (isCountingDown) "${countdown}s" else "获取验证码")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (viewModel.verifyCode(email, code)) {
                            step = 2
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("验证码错误") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.isNotBlank()
                ) {
                    Text("下一步")
                }
            } else {
                Text("第二步：重置密码", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("输入新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (newPassword.isNotBlank() && newPassword == confirmPassword) {
                            viewModel.saveUserPassword(newPassword)
                            scope.launch { snackbarHostState.showSnackbar("密码重置成功") }
                            navController.popBackStack()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("两次密码不一致或为空") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认")
                }
            }
        }
    }
}

@Composable
fun UserInfoItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}