package com.swiftiecx.timeledger.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 封装一个居中显示的 SnackbarHost ---
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

// --- 通用的时尚背景布局 ---
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
            // 1. 顶部带颜色的背景
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

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
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
                    .padding(top = 200.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
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

// --- 登录界面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AuthScreenWrapper(
        title = "欢迎回来",
        subtitle = "登录以同步您的账单数据",
        onBackClick = { navController.popBackStack() },
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) }
    ) {
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

        Button(
            onClick = {
                isLoading = true
                viewModel.login(
                    email = email,
                    password = password,
                    onSuccess = {
                        isLoading = false
                        navController.popBackStack()
                    },
                    onError = { msg ->
                        isLoading = false
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("立即登录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

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

// --- 注册界面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AuthScreenWrapper(
        title = "创建账号",
        subtitle = "注册即刻开启云端同步",
        onBackClick = { navController.popBackStack() },
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) }
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("电子邮箱") },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

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

        Button(
            onClick = {
                if (password.length < 6) {
                    scope.launch { snackbarHostState.showSnackbar("密码长度至少为 6 位") }
                } else if (password != confirmPassword) {
                    scope.launch { snackbarHostState.showSnackbar("两次密码不一致") }
                } else {
                    isLoading = true
                    viewModel.register(
                        email = email,
                        password = password,
                        onSuccess = {
                            isLoading = false
                            scope.launch {
                                snackbarHostState.showSnackbar("注册成功，验证邮件已发送")
                                delay(1000)
                                navController.popBackStack(Routes.LOGIN, inclusive = true)
                            }
                        },
                        onError = { msg ->
                            isLoading = false
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("注册并登录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- 用户信息主菜单 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val email by viewModel.userEmail.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- 状态管理：注销账号双重确认 ---
    var showDeleteAccountDialog by remember { mutableStateOf(false) } // 第一步：风险警告
    var showDeleteFinalDialog by remember { mutableStateOf(false) }   // 第二步：输入确认
    var deleteInput by remember { mutableStateOf("") }                // 输入框内容

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

            // 【修改】注销账号按钮，触发第一步弹窗
            TextButton(
                onClick = { showDeleteAccountDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("注销账号", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    // --- 弹窗逻辑：注销账号 ---

    // 1. 第一步：风险警告
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("确认注销账号?") },
            text = {
                Column {
                    Text("请注意：此操作将永久删除您的账号。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "我们会清除云端所有数据，无法找回。",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("本地数据将保留，但无法再进行云端同步。")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        // 进入第二步
                        deleteInput = ""
                        showDeleteFinalDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("下一步")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 2. 第二步：最终输入确认
    if (showDeleteFinalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFinalDialog = false },
            title = { Text("最终安全确认") },
            text = {
                Column {
                    Text("请输入以下文字以确认注销账号：", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "确认注销账号",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deleteInput,
                        onValueChange = { deleteInput = it },
                        placeholder = { Text("确认注销账号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteFinalDialog = false
                        // 执行注销
                        viewModel.deleteUserAccount(
                            onSuccess = {
                                Toast.makeText(context, "账号已注销", Toast.LENGTH_SHORT).show()
                                // 注销成功后回到欢迎页或登录页，并清空栈
                                navController.navigate(Routes.WELCOME) {
                                    popUpTo(0)
                                }
                            },
                            onError = { msg ->
                                // 自动处理“安全验证过期”的情况
                                if (msg.contains("安全验证过期") || msg.contains("recent login")) {
                                    Toast.makeText(context, "为了安全，请重新登录后再操作", Toast.LENGTH_LONG).show()
                                    navController.navigate(Routes.LOGIN)
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            }
                        )
                    },
                    // 必须完全匹配中文
                    enabled = deleteInput == "确认注销账号",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认注销")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFinalDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// --- 【重写】修改密码界面 (输入旧密码+新密码) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("修改密码") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 旧密码
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("当前密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                }
            )

            // 新密码
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("新密码 (至少6位)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                }
            )

            // 确认新密码
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认新密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                supportingText = {
                    if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                        Text("两次输入的密码不一致")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (oldPassword.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("请输入当前密码") }
                    } else if (newPassword.length < 6) {
                        scope.launch { snackbarHostState.showSnackbar("新密码长度不能少于6位") }
                    } else if (newPassword != confirmPassword) {
                        scope.launch { snackbarHostState.showSnackbar("两次新密码不一致") }
                    } else if (oldPassword == newPassword) {
                        scope.launch { snackbarHostState.showSnackbar("新密码不能与旧密码相同") }
                    } else {
                        isLoading = true
                        viewModel.changePassword(
                            oldPass = oldPassword,
                            newPass = newPassword,
                            onSuccess = {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("密码修改成功")
                                    delay(1000)
                                    navController.popBackStack()
                                }
                            },
                            onError = { error ->
                                isLoading = false
                                scope.launch { snackbarHostState.showSnackbar(error) }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && oldPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword == newPassword
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("确认修改", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- 忘记密码界面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    // 1. 获取当前登录状态和邮箱
    val userEmail by viewModel.userEmail.collectAsState()

    // 2. 初始化邮箱输入框：如果有登录邮箱则自动填充，否则为空
    var email by remember(userEmail) { mutableStateOf(userEmail) }

    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = { CenterAlignedTopAppBar(title = { Text("重置密码") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(
                "请输入您的注册邮箱，我们将向您发送重置密码的链接。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                // 即使自动填入，也允许用户修改
                enabled = !isLoading
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        isLoading = true
                        viewModel.sendPasswordResetEmail(
                            email = email,
                            onSuccess = {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("重置邮件已发送，请查收")
                                    delay(1500)
                                    navController.popBackStack()
                                }
                            },
                            onError = { msg ->
                                isLoading = false
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("请输入邮箱") }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("发送重置链接")
                }
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