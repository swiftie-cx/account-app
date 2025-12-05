package com.example.myapplication.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 1. 用户信息主菜单
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val context = LocalContext.current

    Scaffold(
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
            if (isLoggedIn) {
                // --- 已登录视图 ---

                // 头像/邮箱展示
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(text = email, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(32.dp))

                // 功能列表
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

                // 退出登录
                Button(
                    onClick = {
                        viewModel.logout()
                        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("退出登录")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 注销账号
                Button(
                    onClick = {
                        viewModel.deleteUserAccount()
                        Toast.makeText(context, "账号已注销", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Text("注销账号")
                }

            } else {
                // --- 未登录视图 ---
                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text("尚未登录", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.weight(1f))

                // 底部两个大按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { navController.navigate(Routes.LOGIN) },
                        modifier = Modifier.weight(1f).height(50.dp)
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
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// 2. 注册界面
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

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("注册账号") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            // 邮箱
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

            // 密码
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("设置密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            // 确认密码
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            // 验证码
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
                        if (email.isBlank()) {
                            Toast.makeText(context, "请输入邮箱", Toast.LENGTH_SHORT).show()
                        } else if (viewModel.isEmailRegistered(email)) {
                            emailError = "该邮箱已注册，请直接登录"
                        } else {
                            isCountingDown = true
                            Toast.makeText(context, "验证码已发送: 123456", Toast.LENGTH_LONG).show()
                            scope.launch {
                                countdown = 60
                                while (countdown > 0) { delay(1000); countdown-- }
                                isCountingDown = false
                            }
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
                        Toast.makeText(context, "请设置密码", Toast.LENGTH_SHORT).show()
                    } else if (password != confirmPassword) {
                        Toast.makeText(context, "两次密码不一致", Toast.LENGTH_SHORT).show()
                    } else if (code != "123456") {
                        Toast.makeText(context, "验证码错误", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.register(email, password)
                        Toast.makeText(context, "注册成功并已登录", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
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

// 3. 登录界面 (新增)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
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

            // 忘记密码链接
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { navController.navigate(Routes.FORGOT_PASSWORD) }) {
                    Text("忘记密码？")
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (viewModel.login(email, password)) {
                        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "账号或密码错误", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = email.isNotBlank() && password.isNotBlank()
            ) {
                Text("登录")
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
    val context = LocalContext.current

    Scaffold(
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
                            Toast.makeText(context, "旧密码错误", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "密码修改成功", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "两次密码不一致或为空", Toast.LENGTH_SHORT).show()
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

// 5. 忘记密码 (重设密码)
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 如果是登录页面点进来的，email为空需手动输入；如果是登录后点进来的，自动填充
    // 自动填充已绑定的邮箱（如果已登录）
    val boundEmail by viewModel.userEmail.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    LaunchedEffect(isLoggedIn, boundEmail) {
        if (isLoggedIn && boundEmail.isNotBlank()) {
            email = boundEmail
        }
    }

    Scaffold(
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
                    enabled = !isLoggedIn // 如果已登录，通常锁定邮箱框，或者允许修改看业务需求，这里简单处理
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
                                    Toast.makeText(context, "验证码已发送: 123456", Toast.LENGTH_LONG).show()
                                    scope.launch {
                                        countdown = 60
                                        while (countdown > 0) { delay(1000); countdown-- }
                                        isCountingDown = false
                                    }
                                } else {
                                    Toast.makeText(context, "该邮箱未注册", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "请输入邮箱", Toast.LENGTH_SHORT).show()
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
                        if (code == "123456") {
                            step = 2
                        } else {
                            Toast.makeText(context, "验证码错误", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "密码重置成功", Toast.LENGTH_SHORT).show()
                            // 如果是从登录页进来的，退回登录页；如果是从信息页进来的，退回信息页
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "两次密码不一致或为空", Toast.LENGTH_SHORT).show()
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

// UserInfoItem 辅助组件
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