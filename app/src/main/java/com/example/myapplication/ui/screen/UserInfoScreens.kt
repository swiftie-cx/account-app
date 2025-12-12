package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 封装一个居中显示的 SnackbarHost (保持不变) ---
@Composable
fun CenteredSnackbarHost(hostState: SnackbarHostState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SnackbarHost(hostState) { data ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = data.visuals.message,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// --- 【新增】通用的时尚背景布局 ---
@Composable
fun AuthScreenWrapper(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Scaffold(
        snackbarHost = snackbarHost,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. 顶部带颜色的背景 (高度占屏幕约 35%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                // 返回按钮
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = 16.dp, start = 8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }

                // 标题区域
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet, // App Logo
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // 2. 中间悬浮卡片内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 200.dp) // 让卡片下移，叠加在背景上
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // 宽度占屏幕 90%
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        content()
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- 【美化】登录界面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AuthScreenWrapper(
        title = "欢迎回来",
        subtitle = "登录以同步您的账单数据",
        onBackClick = { navController.popBackStack() },
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) }
    ) {
        // 邮箱输入
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("电子邮箱") },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        // 密码输入
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )

        // 忘记密码链接
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "忘记密码？",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }

        Spacer(Modifier.height(8.dp))

        // 登录按钮
        Button(
            onClick = {
                if (viewModel.login(email, password)) {
                    navController.popBackStack()
                } else {
                    scope.launch { snackbarHostState.showSnackbar("账号或密码错误") }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = email.isNotBlank() && password.isNotBlank()
        ) {
            Text("立即登录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // 注册引导
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("还没有账号？", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "去注册",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { navController.navigate(Routes.REGISTER) }
                    .padding(8.dp)
            )
        }
    }
}

// --- 【美化】注册界面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isCountingDown by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(60) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AuthScreenWrapper(
        title = "创建账号",
        subtitle = "注册即刻开启云端同步",
        onBackClick = { navController.popBackStack() },
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) }
    ) {
        // 邮箱
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text("电子邮箱") },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError!!) }
        )

        // 验证码行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("验证码") },
                leadingIcon = { Icon(Icons.Outlined.MarkEmailRead, null) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    if (email.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("请输入邮箱") }
                    } else if (viewModel.isEmailRegistered(email)) {
                        emailError = "该邮箱已注册，请直接登录"
                    } else {
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
                                scope.launch { snackbarHostState.showSnackbar("验证码已发送至邮箱") }
                            },
                            onError = { msg ->
                                isCountingDown = false
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }
                },
                enabled = !isCountingDown && email.isNotBlank() && emailError == null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(OutlinedTextFieldDefaults.MinHeight),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text(if (isCountingDown) "${countdown}s" else "获取")
            }
        }

        // 密码
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("设置密码") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // 确认密码
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("确认密码") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        // 注册按钮
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
                        navController.popBackStack(Routes.LOGIN, inclusive = true)
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("验证码错误") }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && code.isNotBlank()
        ) {
            Text("注册并登录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- 用户信息主菜单 (基本保持原有结构，微调样式) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    val email by viewModel.userEmail.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("用户信息", fontWeight = FontWeight.Bold) },
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
            // 用户头像卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = if(email.isNotBlank()) "已登录" else "未登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // 菜单项
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    UserInfoItem(
                        icon = Icons.Default.LockReset,
                        title = "修改密码",
                        onClick = { navController.navigate(Routes.CHANGE_PASSWORD) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    UserInfoItem(
                        icon = Icons.Default.HelpOutline,
                        title = "忘记密码",
                        onClick = { navController.navigate(Routes.FORGOT_PASSWORD) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部按钮
            Button(
                onClick = {
                    viewModel.logout()
                    scope.launch { snackbarHostState.showSnackbar("已退出登录") }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("退出登录")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    viewModel.deleteUserAccount()
                    scope.launch { snackbarHostState.showSnackbar("账号已注销") }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("注销账号", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// --- 保持原有逻辑的辅助界面 (修改密码、忘记密码) ---
// 您之前的代码逻辑是正确的，为了节省篇幅，这里我保持它们的结构不变，
// 但为了保持文件完整性，我还是把它们放进来，您可以根据需要应用同样的 UI 美化

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    // 复用之前的逻辑，建议您可以参照 AuthScreenWrapper 风格进行改造
    // 这里保持功能完整性
    var step by remember { mutableIntStateOf(1) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = { CenterAlignedTopAppBar(title = { Text("修改密码") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (step == 1) {
                OutlinedTextField(value = oldPassword, onValueChange = { oldPassword = it }, label = { Text("输入旧密码") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { if (viewModel.verifyUserPassword(oldPassword)) step = 2 else scope.launch { snackbarHostState.showSnackbar("旧密码错误") } }, modifier = Modifier.fillMaxWidth()) { Text("下一步") }
            } else {
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("新密码") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("确认新密码") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { if (newPassword.isNotBlank() && newPassword == confirmPassword) { viewModel.saveUserPassword(newPassword); scope.launch { snackbarHostState.showSnackbar("修改成功") }; navController.popBackStack() } else scope.launch { snackbarHostState.showSnackbar("密码不一致") } }, modifier = Modifier.fillMaxWidth()) { Text("确认") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    // 保持原有逻辑
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
    LaunchedEffect(isLoggedIn, boundEmail) { if (isLoggedIn && boundEmail.isNotBlank()) email = boundEmail }

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = { CenterAlignedTopAppBar(title = { Text("重置密码") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (step == 1) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("邮箱") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !isLoggedIn)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("验证码") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (email.isNotBlank()) {
                            if (viewModel.isEmailRegistered(email)) {
                                isCountingDown = true
                                scope.launch { countdown = 60; while (countdown > 0 && isCountingDown) { delay(1000); countdown-- }; isCountingDown = false; countdown = 60 }
                                viewModel.sendCodeToEmail(email, { scope.launch { snackbarHostState.showSnackbar("验证码已发送") } }, { msg -> isCountingDown = false; scope.launch { snackbarHostState.showSnackbar(msg) } })
                            } else scope.launch { snackbarHostState.showSnackbar("该邮箱未注册") }
                        } else scope.launch { snackbarHostState.showSnackbar("请输入邮箱") }
                    }, enabled = !isCountingDown && email.isNotBlank()) { Text(if (isCountingDown) "${countdown}s" else "获取") }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { if (viewModel.verifyCode(email, code)) step = 2 else scope.launch { snackbarHostState.showSnackbar("验证码错误") } }, modifier = Modifier.fillMaxWidth(), enabled = code.isNotBlank()) { Text("下一步") }
            } else {
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("新密码") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("确认新密码") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { if (newPassword.isNotBlank() && newPassword == confirmPassword) { viewModel.saveUserPassword(newPassword); scope.launch { snackbarHostState.showSnackbar("密码重置成功") }; navController.popBackStack() } else scope.launch { snackbarHostState.showSnackbar("密码不一致") } }, modifier = Modifier.fillMaxWidth()) { Text("确认") }
            }
        }
    }
}

@Composable
fun UserInfoItem(icon: ImageVector, title: String, value: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) { Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(8.dp)) }
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}