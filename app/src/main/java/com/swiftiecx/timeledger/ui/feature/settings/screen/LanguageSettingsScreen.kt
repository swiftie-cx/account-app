package com.swiftiecx.timeledger.ui.feature.settings.screen

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
import com.swiftiecx.timeledger.ui.localization.createLocalizedContext
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel,
    expenseViewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentCode by themeViewModel.language.collectAsState()

    val languages = listOf(
        LanguageOption(stringResource(R.string.lang_follow_system), ""),
        LanguageOption(stringResource(R.string.lang_en), "en"),
        LanguageOption(stringResource(R.string.lang_zh), "zh"),
        LanguageOption(stringResource(R.string.lang_ja), "ja"),
        LanguageOption(stringResource(R.string.lang_ko), "ko"),
        LanguageOption(stringResource(R.string.lang_pt_br), "pt-BR"), // 葡萄牙语（巴西）
        LanguageOption(stringResource(R.string.lang_de), "de"),
        LanguageOption(stringResource(R.string.lang_es), "es"),
        LanguageOption(stringResource(R.string.lang_fr), "fr"),
        LanguageOption(stringResource(R.string.lang_id), "id"),
        LanguageOption(stringResource(R.string.lang_in), "hi"),    // 印地语
        LanguageOption(stringResource(R.string.lang_it), "it"),
        LanguageOption(stringResource(R.string.lang_es_mx), "es-MX"), // 对应西班牙语（墨西哥）
        LanguageOption(stringResource(R.string.lang_pl), "pl"),
        LanguageOption(stringResource(R.string.lang_ru), "ru"),
        LanguageOption(stringResource(R.string.lang_th), "th"),
        LanguageOption(stringResource(R.string.lang_tr), "tr"),
        LanguageOption(stringResource(R.string.lang_vi), "vi")     // 对应越南语
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
                        val isSelected = when {
                            option.code.isEmpty() -> currentCode.isEmpty()                 // Follow system
                            option.code.contains("-") -> currentCode.equals(option.code, ignoreCase = true) // 精确匹配 es-MX
                            else -> currentCode.equals(option.code, ignoreCase = true)      // 精确匹配 es / vi / ja ...
                        }
                        LanguageItem(
                            name = option.name,
                            isSelected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    scope.launch {
                                        // 1) 更新语言 Tag（不重建 Activity，避免闪屏）
                                        themeViewModel.setLanguage(option.code)

                                        // 2) 如果你的分类名称存进了数据库，这里用“新语言”的 context 重新写入
                                        val localizedContext = createLocalizedContext(context, option.code)
                                        expenseViewModel.refreshCategories(localizedContext)
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