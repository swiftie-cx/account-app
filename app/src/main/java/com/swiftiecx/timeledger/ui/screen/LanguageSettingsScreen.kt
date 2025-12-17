package com.swiftiecx.timeledger.ui.screen

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.swiftiecx.timeledger.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {
    // 获取 Context 用于重启 App
    val context = LocalContext.current

    // 【关键优化】使用 remember 记录当前选中的语言，实现点击即时刷新 UI
    var currentCode by remember { mutableStateOf(themeViewModel.getCurrentLanguageCode()) }

    val languages = listOf(
        LanguageOption(stringResource(R.string.lang_follow_system), ""),
        LanguageOption(stringResource(R.string.lang_zh), "zh-CN"),
        LanguageOption(stringResource(R.string.lang_en), "en")
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.language_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    languages.forEachIndexed { index, option ->
                        // 这里的判断逻辑兼容了 zh-CN, zh-TW 等情况
                        val isSelected = if (option.code.isEmpty()) currentCode.isEmpty()
                        else currentCode.startsWith(option.code.split("-")[0])

                        LanguageItem(
                            name = option.name,
                            isSelected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    // 1. 立即更新 UI 状态 (打钩)
                                    currentCode = option.code
                                    // 2. 调用 ViewModel 切换语言
                                    themeViewModel.setLanguage(option.code)
                                    // 3. 【核心】强制重启 App 以彻底应用新语言（清除内存缓存）
                                    restartApp(context)
                                }
                            }
                        )
                        if (index < languages.size - 1) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
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

@Composable
fun LanguageItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

data class LanguageOption(val name: String, val code: String)

/**
 * 强制重启 App 的工具函数
 * 通过清除任务栈并杀死当前进程，让系统重新启动 App，
 * 从而彻底清除单例（如 Repository）中缓存的旧语言资源。
 */
fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    // 启动新任务
    context.startActivity(mainIntent)
    // 杀死当前进程
    Process.killProcess(Process.myPid())
}