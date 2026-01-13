package com.swiftiecx.timeledger.ui.feature.auth.screen

import androidx.compose.ui.res.stringResource
import com.swiftiecx.timeledger.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.swiftiecx.timeledger.ui.feature.settings.component.InteractivePatternLock
import com.swiftiecx.timeledger.ui.feature.settings.component.InteractivePinPad
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel

@Composable
fun LockScreen(
    viewModel: ExpenseViewModel,
    onUnlockSuccess: () -> Unit
) {
    val privacyType = viewModel.getPrivacyType()
    val biometricEnabled = viewModel.isBiometricEnabled()
    var inputPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    // i18n texts
    val wrongPinText = stringResource(R.string.lock_error_wrong_pin)
    val wrongPatternText = stringResource(R.string.lock_error_wrong_pattern)
    val forgotButtonText = stringResource(R.string.lock_forgot_password)

    // 指纹解锁：进入锁屏后自动弹出一次（用户可取消后继续用 PIN/手势）
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var hasPromptedBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(privacyType, biometricEnabled) {
        if (
            privacyType != "NONE" &&
            biometricEnabled &&
            activity != null &&
            !hasPromptedBiometric
        ) {
            hasPromptedBiometric = true
            showBiometricPrompt(
                activity = activity,
                onSuccess = onUnlockSuccess,
                onError = { msg ->
                    // 只提示可读的错误，不阻断手动解锁
                    errorMsg = msg
                }
            )
        }
    }

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
                text = stringResource(R.string.lock_verify_password),
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
                                        errorMsg = wrongPinText
                                        inputPin = ""
                                    }
                                }
                            }
                        },
                        onDeleteClick = {
                            if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
                        }
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
                                errorMsg = wrongPatternText
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }

            // --- 底部忘记密码按钮 ---
            Spacer(modifier = Modifier.height(20.dp))
            // 这里是第一层救命稻草：点击弹出账号登录框
            OutlinedButton(
                onClick = { showEmailUnlockDialog = true },
                border = null
            ) {
                Text(forgotButtonText)
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

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val canAuthenticate = BiometricManager.from(activity).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    )

    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
        onError(activity.getString(R.string.lock_biometric_not_supported))
        return
    }

    val executor = ContextCompat.getMainExecutor(activity)

    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // 用户取消不算错误，不提示
                if (
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    return
                }
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                // 指纹不匹配：系统会提示，这里不强制追加文案
            }
        }
    )

    // 系统指纹弹窗样式不可自定义，这里尽量“少文字”以降低视觉干扰
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.bio_prompt_title))
        .setNegativeButtonText(activity.getString(R.string.cancel))
        .setConfirmationRequired(false)
        .build()

    prompt.authenticate(promptInfo)
}

/**
 * 现代化的验证弹窗 (适配 Firebase 异步登录 + 找回密码)
 */
@Composable
fun UnlockByEmailDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current  // ✅ 新增：用于回调里取字符串

    val savedEmail by viewModel.userEmail.collectAsState()

    var email by remember { mutableStateOf(savedEmail) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // 用状态来控制显示的错误/提示信息
    var uiMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // 用于控制倒计时 (防止狂点发送重置邮件)
    var isResetSent by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

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
                    text = stringResource(R.string.unlock_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                // 表单内容
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 邮箱输入框
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            uiMessage = null
                        },
                        label = { Text(stringResource(R.string.unlock_email_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // 密码输入框
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            uiMessage = null
                        },
                        label = { Text(stringResource(R.string.unlock_password_label)) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )

                    // 【新增】第二层救命稻草：如果连账号密码也忘了
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = if (isResetSent) stringResource(R.string.unlock_reset_sent) else stringResource(R.string.unlock_send_reset),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isResetSent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(enabled = !isResetSent && !isLoading) {
                                if (email.isBlank()) {
                                    uiMessage = context.getString(R.string.unlock_enter_email_first) // ✅ 改
                                    isError = true
                                } else {
                                    isLoading = true
                                    viewModel.sendPasswordResetEmail(
                                        email = email,
                                        onSuccess = {
                                            isLoading = false
                                            isResetSent = true
                                            uiMessage = context.getString(R.string.unlock_reset_link_sent) // ✅ 改
                                            isError = false
                                        },
                                        onError = { msg ->
                                            isLoading = false
                                            uiMessage = msg
                                            isError = true
                                        }
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- 消息提示区域 ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
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

                Spacer(Modifier.height(8.dp))

                // 提示文案
                Text(
                    text = stringResource(R.string.unlock_reset_lock_tip),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                // 验证按钮
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            uiMessage = context.getString(R.string.unlock_enter_account_password) // ✅ 改
                            isError = true
                        } else {
                            isLoading = true
                            uiMessage = null
                            // 异步调用 Firebase 登录
                            viewModel.login(
                                email = email,
                                password = password,
                                onSuccess = {
                                    isLoading = false
                                    onUnlockSuccess()
                                },
                                onError = { msg ->
                                    isLoading = false
                                    uiMessage = msg
                                    isError = true
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.unlock_verify_and_unlock),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
