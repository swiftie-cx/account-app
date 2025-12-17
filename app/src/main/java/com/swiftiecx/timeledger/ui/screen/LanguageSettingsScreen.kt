package com.swiftiecx.timeledger.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel,
    expenseViewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    var isRestarting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var currentCode by remember { mutableStateOf(themeViewModel.getCurrentLanguageCode()) }

    val languages = listOf(
        LanguageOption(stringResource(R.string.lang_follow_system), ""),
        LanguageOption(stringResource(R.string.lang_zh), "zh"),
        LanguageOption(stringResource(R.string.lang_en), "en"),
        LanguageOption(stringResource(R.string.lang_ja), "ja"),
        LanguageOption(stringResource(R.string.lang_ko), "ko")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.language_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isRestarting) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.language_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        languages.forEachIndexed { index, option ->
                            val isSelected = if (option.code.isEmpty()) currentCode.isEmpty()
                            else currentCode.startsWith(option.code.split("-")[0])

                            LanguageItem(
                                name = option.name,
                                isSelected = isSelected,
                                onClick = {
                                    if (!isSelected && !isRestarting) {
                                        isRestarting = true
                                        currentCode = option.code

                                        scope.launch {
                                            // 1. 设置全局语言配置
                                            themeViewModel.setLanguage(option.code)

                                            // 2. [核心修复] 手动创建一个对应新语言的 Context
                                            val newLocale = if (option.code.isEmpty()) {
                                                Locale.getDefault() // 跟随系统
                                            } else {
                                                Locale.forLanguageTag(option.code)
                                            }

                                            // 创建一个新的配置对象
                                            val config = Configuration(context.resources.configuration)
                                            config.setLocale(newLocale)
                                            // 生成持有新语言资源的 Context
                                            val localizedContext = context.createConfigurationContext(config)

                                            // 3. 传入这个新 Context 去刷新数据
                                            // 这样拿到的就是"未来"的语言字符串，而不是"现在"的
                                            expenseViewModel.refreshCategories(localizedContext)

                                            delay(500) // 稍微多给一点时间让数据库写入完成
                                            restartApp(context)
                                        }
                                    }
                                }
                            )
                            if (index < languages.size - 1) {
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.lang_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (isSelected) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

data class LanguageOption(val name: String, val code: String)

fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    if (intent != null) {
        val componentName = intent.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(mainIntent)
        if (context is Activity) {
            context.finish()
        }
        Runtime.getRuntime().exit(0)
    }
}