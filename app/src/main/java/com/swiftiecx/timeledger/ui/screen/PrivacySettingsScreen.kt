package com.swiftiecx.timeledger.ui.screen
import androidx.compose.ui.res.stringResource
import com.swiftiecx.timeledger.R

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel // (新增) 导入VM
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel // (新增) 接收 VM
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // --- 状态管理 (从 ViewModel 初始化) ---
    val currentType = viewModel.getPrivacyType()
    var isPinEnabled by remember { mutableStateOf(currentType == "PIN") }
    var isPatternEnabled by remember { mutableStateOf(currentType == "PATTERN") }
    var isBiometricEnabled by remember { mutableStateOf(viewModel.isBiometricEnabled()) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPatternSetupDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // 应用锁设置前必须先登录
    var showLoginRequireDialog by remember { mutableStateOf(false) }

    // 指纹解锁需要先设置数字/手势密码
    var showBiometricRequireDialog by remember { mutableStateOf(false) }

    // 兼容旧数据：如果没有设置隐私密码，却误开启了指纹开关，自动关闭
    LaunchedEffect(isPinEnabled, isPatternEnabled) {
        if (!isPinEnabled && !isPatternEnabled && isBiometricEnabled) {
            isBiometricEnabled = false
            viewModel.setBiometricEnabled(false)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. 数字密码
            PrivacySection(
                title = stringResource(R.string.privacy_pin_title),
                description = stringResource(R.string.privacy_pin_desc),
                isChecked = isPinEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (!isLoggedIn) {
                            // 未登录：不允许设置应用锁
                            isPinEnabled = false
                            showLoginRequireDialog = true
                        } else {
                            showPinSetupDialog = true
                        }
                    } else {
                        // 关闭
                        isPinEnabled = false
                        isBiometricEnabled = false
                        viewModel.setPrivacyType("NONE")
                        viewModel.setBiometricEnabled(false)
                    }
                }
            ) {
                // 静态预览图
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PreviewCard(modifier = Modifier.weight(1f)) { PinLockPreview(true) }
                    PreviewCard(modifier = Modifier.weight(1f)) { PinLockPreview(false) }
                }
            }

            // 2. 手势密码
            PrivacySection(
                title = stringResource(R.string.privacy_pattern_title),
                description = stringResource(R.string.privacy_pattern_desc),
                isChecked = isPatternEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (!isLoggedIn) {
                            isPatternEnabled = false
                            showLoginRequireDialog = true
                        } else {
                            showPatternSetupDialog = true
                        }
                    } else {
                        isPatternEnabled = false
                        isBiometricEnabled = false
                        viewModel.setPrivacyType("NONE")
                        viewModel.setBiometricEnabled(false)
                    }
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PreviewCard(modifier = Modifier.weight(1f)) { PatternLockPreview(false) }
                    PreviewCard(modifier = Modifier.weight(1f)) { PatternLockPreview(true) }
                }
            }

            // 3. 指纹
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.privacy_biometric_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.privacy_biometric_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!isLoggedIn) {
                                    isBiometricEnabled = false
                                    viewModel.setBiometricEnabled(false)
                                    showLoginRequireDialog = true
                                    return@Switch
                                }
                                if (!isPinEnabled && !isPatternEnabled) {
                                    // 没有设置数字/手势密码时，不允许开启
                                    isBiometricEnabled = false
                                    viewModel.setBiometricEnabled(false)
                                    showBiometricRequireDialog = true
                                } else {
                                    isBiometricEnabled = true
                                    viewModel.setBiometricEnabled(true)
                                }
                            } else {
                                isBiometricEnabled = false
                                viewModel.setBiometricEnabled(false)
                            }
                        },
                        enabled = true
                    )
                }
            }
        }
    }

    // --- 弹窗逻辑 ---

    if (showPinSetupDialog) {
        SetupPinDialog(
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                // 保存数据
                viewModel.savePin(pin)
                viewModel.setPrivacyType("PIN")

                // 更新UI状态
                isPinEnabled = true
                isPatternEnabled = false // 互斥
                showPinSetupDialog = false
                showSuccessDialog = true
            }
        )
    }

    if (showPatternSetupDialog) {
        SetupPatternDialog(
            onDismiss = { showPatternSetupDialog = false },
            onConfirm = { pattern ->
                // 保存数据
                viewModel.savePattern(pattern)
                viewModel.setPrivacyType("PATTERN")

                // 更新UI状态
                isPatternEnabled = true
                isPinEnabled = false
                showPatternSetupDialog = false
                showSuccessDialog = true
            }
        )
    }

    if (showSuccessDialog) {
        SuccessAnimationDialog(onDismiss = { showSuccessDialog = false })
    }

    if (showBiometricRequireDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricRequireDialog = false },
            title = { Text(stringResource(R.string.privacy_biometric_require_title)) },
            text = { Text(stringResource(R.string.privacy_biometric_require_text)) },
            confirmButton = {
                TextButton(onClick = { showBiometricRequireDialog = false }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }

    if (showLoginRequireDialog) {
        AlertDialog(
            onDismissRequest = { showLoginRequireDialog = false },
            title = { Text(stringResource(R.string.login_required_title)) },
            text = { Text(stringResource(R.string.login_required_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLoginRequireDialog = false
                        navController.navigate(Routes.LOGIN)
                    }
                ) { Text(stringResource(R.string.go_login)) }
            },
            dismissButton = {
                TextButton(onClick = { showLoginRequireDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun SuccessAnimationDialog(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onDismiss()
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AnimatedCheckmark()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.privacy_enabled_success), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AnimatedCheckmark() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, animationSpec = tween(500, easing = LinearEasing)) }
    Canvas(modifier = Modifier.size(80.dp)) {
        val radius = size.width / 2
        val strokeWidth = 5.dp.toPx()
        drawCircle(color = primaryColor, radius = radius, style = Stroke(width = strokeWidth), alpha = progress.value)
        val p1 = Offset(size.width * 0.25f, size.height * 0.5f)
        val p2 = Offset(size.width * 0.45f, size.height * 0.7f)
        val p3 = Offset(size.width * 0.75f, size.height * 0.35f)
        val checkPath = Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y) }
        val pathMeasure = PathMeasure().apply { setPath(checkPath, false) }
        val drawingPath = Path()
        pathMeasure.getSegment(0f, pathMeasure.length * progress.value, drawingPath, true)
        drawPath(path = drawingPath, color = primaryColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

@Composable
fun SetupPinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ✅ 回调里不能用 stringResource：提前取好
    val pinMismatchText = stringResource(R.string.pin_error_mismatch)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (step == 1) onDismiss() else { step = 1; currentPin = ""; errorMsg = null } }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
                Spacer(Modifier.height(40.dp))
                Text(text = if (step == 1) stringResource(R.string.pin_setup_title) else stringResource(R.string.pin_confirm_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(text = errorMsg ?: if (step == 1) stringResource(R.string.pin_prompt_input_4) else stringResource(R.string.pin_prompt_confirm), color = if (errorMsg != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(40.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { index -> Box(modifier = Modifier.size(16.dp).background(if (index < currentPin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)) }
                }
                Spacer(Modifier.weight(1f))
                InteractivePinPad(onNumberClick = { num ->
                    if (currentPin.length < 4) {
                        currentPin += num; errorMsg = null
                        if (currentPin.length == 4) {
                            if (step == 1) { firstPin = currentPin; currentPin = ""; step = 2 }
                            else {
                                if (currentPin == firstPin) onConfirm(currentPin)
                                else {
                                    errorMsg = pinMismatchText   // ✅ 改
                                    currentPin = ""
                                }
                            }
                        }
                    }
                }, onDeleteClick = { if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1) })
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SetupPatternDialog(onDismiss: () -> Unit, onConfirm: (List<Int>) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ✅ 回调里不能用 stringResource：提前取好
    val patternTooShortText = stringResource(R.string.pattern_error_too_short)
    val patternMismatchText = stringResource(R.string.pattern_error_mismatch)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (step == 1) onDismiss() else { step = 1; currentPattern = emptyList(); errorMsg = null } }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
                Spacer(Modifier.height(40.dp))
                Text(text = if (step == 1) stringResource(R.string.pattern_setup_title) else stringResource(R.string.pattern_confirm_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(text = errorMsg ?: stringResource(R.string.pattern_prompt_connect_4), color = if (errorMsg != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                    InteractivePatternLock(onPatternComplete = { pattern ->
                        if (pattern.size < 4) {
                            errorMsg = patternTooShortText    // ✅ 改
                            currentPattern = emptyList()
                        } else {
                            if (step == 1) { firstPattern = pattern; currentPattern = emptyList(); step = 2; errorMsg = null }
                            else {
                                if (pattern == firstPattern) onConfirm(pattern)
                                else {
                                    errorMsg = patternMismatchText // ✅ 改
                                    currentPattern = emptyList()
                                }
                            }
                        }
                    })
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// 静态预览组件 PreviewCard, PrivacySection, PinLockPreview, PatternLockPreview 保持不变，请确保它们存在
@Composable
fun PrivacySection(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = isChecked, onCheckedChange = onCheckedChange)
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
fun PreviewCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.aspectRatio(0.55f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Surface(modifier = Modifier.fillMaxWidth().padding(12.dp).aspectRatio(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Box(contentAlignment = Alignment.Center) { content() }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PinLockPreview(showDots: Boolean) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { Box(modifier = Modifier.size(10.dp).background(if (showDots) activeColor else inactiveColor, CircleShape)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", ""))
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { char -> Text(text = char, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor, modifier = Modifier.width(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun PatternLockPreview(drawPattern: Boolean) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val dotRadius = 3.dp.toPx()
        val spacing = size.width / 3
        val startX = spacing / 2
        val startY = spacing / 2
        val dots = mutableListOf<Offset>()
        for (row in 0..2) {
            for (col in 0..2) {
                val x = startX + col * spacing
                val y = startY + row * spacing
                dots.add(Offset(x, y))
                drawCircle(color = inactiveColor, radius = dotRadius, center = Offset(x, y))
            }
        }
        if (drawPattern) {
            val pathIndices = listOf(0, 1, 2, 4, 6, 7, 8)
            for (i in 0 until pathIndices.size - 1) {
                drawLine(color = activeColor, start = dots[pathIndices[i]], end = dots[pathIndices[i+1]], strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
            pathIndices.forEach { index -> drawCircle(color = activeColor, radius = dotRadius, center = dots[index]) }
        }
    }
}
