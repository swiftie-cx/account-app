package com.swiftiecx.timeledger.ui.feature.auth.screen

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
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
                        contentDescription = stringResource(R.string.back),
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
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AuthScreenWrapper(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle),
        onBackClick = { navController.popBackStack() },
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) }
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.label_email)) },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.label_password)) },
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
                text = stringResource(R.string.action_forgot_password),
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
                Text(stringResource(R.string.action_login), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.text_no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.action_go_register),
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
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var waitingForVerification by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AuthScreenWrapper(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        onBackClick = { navController.popBackStack() },
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) }
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.label_email)) },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.label_set_password)) },
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
            label = { Text(stringResource(R.string.label_confirm_password)) },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        // 注册后：停留在当前页面，等待用户点击邮件链接完成激活。
        LaunchedEffect(waitingForVerification) {
            if (!waitingForVerification) return@LaunchedEffect
            while (waitingForVerification) {
                val verified = viewModel.refreshEmailVerification()
                if (verified) {
                    waitingForVerification = false
                    // 回到设置页（注册页是从设置页进入）
                    navController.popBackStack()
                    break
                }
                delay(1500)
            }
        }

        Button(
            onClick = {
                if (password.length < 6) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_password_min_6)) }
                } else if (password != confirmPassword) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_password_mismatch)) }
                } else {
                    isLoading = true
                    viewModel.register(
                        email = email,
                        password = password,
                        onSuccess = {
                            isLoading = false
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.msg_register_success_verification_sent))
                                // 开始轮询邮箱验证状态
                                waitingForVerification = true
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
                Text(stringResource(R.string.action_register_and_login), fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

    val confirmPhrase = stringResource(R.string.account_delete_confirm_phrase)

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.user_info_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                        Text(
                            text = if (email.isNotBlank()) stringResource(R.string.status_logged_in) else stringResource(R.string.status_not_logged_in),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        title = stringResource(R.string.menu_change_password),
                        onClick = { navController.navigate(Routes.CHANGE_PASSWORD) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    UserInfoItem(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(R.string.menu_forgot_password),
                        onClick = { navController.navigate(Routes.FORGOT_PASSWORD) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 仅已登录时才显示「登出 / 注销账号」
            if (email.isNotBlank()) {
                Button(
                    onClick = {
                        viewModel.logout()
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_logged_out)) }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.action_logout))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 注销账号按钮
                TextButton(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_delete_account), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // --- 弹窗逻辑：注销账号 ---

    // 1. 风险警告
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_account_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_delete_account_warning1))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.dialog_delete_account_warning2),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.dialog_delete_account_warning3))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        deleteInput = ""
                        showDeleteFinalDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_next_step))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 2. 最终输入确认
    if (showDeleteFinalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFinalDialog = false },
            title = { Text(stringResource(R.string.dialog_final_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_enter_text_to_confirm), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = confirmPhrase,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deleteInput,
                        onValueChange = { deleteInput = it },
                        placeholder = { Text(confirmPhrase) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteFinalDialog = false
                        viewModel.deleteUserAccount(
                            onSuccess = {
                                Toast.makeText(context, context.getString(R.string.toast_account_deleted), Toast.LENGTH_SHORT).show()
                                // 注销后按「登出」同样的体验：回到设置页，避免跳转到 Welcome 造成重复建账
                                viewModel.logout()
                                navController.popBackStack()
                            },
                            onError = { msg ->
                                if (msg.contains("安全验证过期") || msg.contains("recent login")) {
                                    Toast.makeText(context, context.getString(R.string.toast_reauth_required), Toast.LENGTH_LONG).show()
                                    navController.navigate(Routes.LOGIN)
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            }
                        )
                    },
                    enabled = deleteInput == confirmPhrase,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFinalDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// --- 修改密码界面 (输入旧密码+新密码) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    val context = LocalContext.current

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
                title = { Text(stringResource(R.string.change_password_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                label = { Text(stringResource(R.string.label_current_password)) },
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
                label = { Text(stringResource(R.string.label_new_password_min_6)) },
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
                label = { Text(stringResource(R.string.label_confirm_new_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                supportingText = {
                    if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                        Text(stringResource(R.string.error_new_password_mismatch))
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (oldPassword.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_enter_current_password)) }
                    } else if (newPassword.length < 6) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_new_password_too_short)) }
                    } else if (newPassword != confirmPassword) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_new_password_mismatch)) }
                    } else if (oldPassword == newPassword) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_new_password_same_as_old)) }
                    } else {
                        isLoading = true
                        viewModel.changePassword(
                            oldPass = oldPassword,
                            newPass = newPassword,
                            onSuccess = {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.msg_password_changed))
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
                    Text(stringResource(R.string.action_confirm_change), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- 忘记密码界面 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    val context = LocalContext.current

    val userEmail by viewModel.userEmail.collectAsState()
    var email by remember(userEmail) { mutableStateOf(userEmail) }

    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { CenteredSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.reset_password_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(
                stringResource(R.string.reset_password_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.label_email_short)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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
                                    snackbarHostState.showSnackbar(context.getString(R.string.msg_reset_email_sent))
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
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_enter_email)) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.action_send_reset_link))
                }
            }
        }
    }
}

@Composable
fun UserInfoItem(icon: ImageVector, title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
