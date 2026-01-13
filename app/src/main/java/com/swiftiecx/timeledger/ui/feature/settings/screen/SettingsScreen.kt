package com.swiftiecx.timeledger.ui.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    // [修改 1] 删除 defaultCurrency 参数，不再依赖外部传参
    viewModel: ExpenseViewModel,
    themeViewModel: ThemeViewModel
) {
    val scrollState = rememberScrollState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // [修改 2] 核心修复：直接在内部监听 ViewModel 的实时数据
    // 这样能确保显示的一定是当前生效的货币设置
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    // 语言设置监听逻辑
    val currentLanguageCode by themeViewModel.language.collectAsState()
    val currentLanguageLabel = when (currentLanguageCode) {
        "zh" -> stringResource(R.string.lang_zh)
        "en" -> stringResource(R.string.lang_en)
        "ja" -> stringResource(R.string.lang_ja)
        "ko" -> stringResource(R.string.lang_ko)
        "de" -> stringResource(R.string.lang_de)
        "fr" -> stringResource(R.string.lang_fr)
        "es" -> stringResource(R.string.lang_es)
        "it" -> stringResource(R.string.lang_it)
        "br" -> stringResource(R.string.lang_pt_br) // 对应葡萄牙语（巴西）
        "es-MX" -> stringResource(R.string.lang_es_mx) // 对应西班牙语（墨西哥）
        "pl" -> stringResource(R.string.lang_pl)
        "ru" -> stringResource(R.string.lang_ru)
        "id" -> stringResource(R.string.lang_id)
        "vi" -> stringResource(R.string.lang_vi)
        "tr" -> stringResource(R.string.lang_tr)
        "in" -> stringResource(R.string.lang_in)    // 对应印地语
        else -> stringResource(R.string.lang_follow_system)
    }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showClearDataFinalDialog by remember { mutableStateOf(false) }
    var clearDataInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_me), fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. 用户信息卡片 ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isLoggedIn) {
                            navController.navigate(Routes.USER_INFO)
                        } else {
                            navController.navigate(Routes.LOGIN)
                        }
                    },
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) userEmail else stringResource(R.string.click_to_login),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isLoggedIn) stringResource(R.string.view_profile) else stringResource(R.string.login_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. 常规设置 ---
            SettingsGroupTitle(stringResource(R.string.group_general))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Category,
                        title = stringResource(R.string.opt_category),
                        onClick = { navController.navigate(Routes.CATEGORY_SETTINGS) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Default.Repeat,
                        title = stringResource(R.string.opt_periodic),
                        onClick = { navController.navigate(Routes.PERIODIC_BOOKKEEPING) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // [注意] 这里的 defaultCurrency 已经是上面的实时变量了
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = stringResource(R.string.opt_currency),
                        value = defaultCurrency,
                        onClick = { navController.navigate(Routes.CURRENCY_SELECTION) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    SettingsItem(
                        icon = Icons.Default.Public,
                        title = stringResource(R.string.opt_language),
                        value = currentLanguageLabel,
                        onClick = { navController.navigate(Routes.LANGUAGE_SETTINGS) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.opt_theme),
                        onClick = { navController.navigate(Routes.THEME_SETTINGS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. 安全设置 ---
            SettingsGroupTitle(stringResource(R.string.group_security))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.opt_privacy),
                        onClick = { navController.navigate(Routes.PRIVACY_SETTINGS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 4. 数据设置 ---
            SettingsGroupTitle(stringResource(R.string.group_data))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.CloudSync,
                        title = stringResource(R.string.opt_sync),
                        onClick = {
                            if (isLoggedIn) {
                                navController.navigate(Routes.SYNC)
                            } else {
                                navController.navigate(Routes.LOGIN)
                            }
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = stringResource(R.string.opt_clear_data),
                        textColor = MaterialTheme.colorScheme.error,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = { showClearDataDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "${stringResource(R.string.version)} 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- 弹窗逻辑 ---
    val confirmCode = stringResource(R.string.confirm_clear_code)

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_title)) },
            text = { Text(stringResource(R.string.dialog_clear_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        clearDataInput = ""
                        showClearDataFinalDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.next_step))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showClearDataFinalDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataFinalDialog = false },
            title = { Text(stringResource(R.string.dialog_final_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_final_msg), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = confirmCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = clearDataInput,
                        onValueChange = { clearDataInput = it },
                        placeholder = { Text(confirmCode) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataFinalDialog = false
                    },
                    enabled = clearDataInput == confirmCode,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataFinalDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
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
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}