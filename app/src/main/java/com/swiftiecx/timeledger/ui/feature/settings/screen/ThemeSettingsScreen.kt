package com.swiftiecx.timeledger.ui.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    val currentThemeColor by themeViewModel.themeColor.collectAsState()
    val themeOptions = themeViewModel.themeOptions

    // 创建一个临时的 ColorScheme 用于预览
    val previewColorScheme = MaterialTheme.colorScheme.copy(
        primary = currentThemeColor,
        onPrimary = Color.White,
        primaryContainer = currentThemeColor.copy(alpha = 0.2f),
        onPrimaryContainer = currentThemeColor
    )

    Scaffold(
        // [修改 1] 页面背景设为纯白
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.opt_theme)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- 即时预览区域 ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    // [修改 2] 将背景改为浅灰色，去除原本的紫色 (surfaceContainerLow)
                    containerColor = Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.theme_preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 使用自定义的配色方案包裹预览组件
                    MaterialTheme(colorScheme = previewColorScheme) {
                        PreviewMockUI()
                    }
                }
            }

            Text(
                stringResource(R.string.theme_selection_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 颜色选择网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(themeOptions) { option ->
                    ThemeOptionItem(
                        color = option.color,
                        name = stringResource(option.nameResId),
                        isSelected = (option.color.toArgb() == currentThemeColor.toArgb()),
                        onClick = { themeViewModel.updateThemeColor(option.color) }
                    )
                }
            }
        }
    }
}

// --- 模拟 UI 组件 ---
@Composable
fun PreviewMockUI() {
    Column(
        modifier = Modifier
            .width(240.dp) // 限制宽度，模拟手机屏幕
            .clip(RoundedCornerShape(12.dp))
            // 模拟屏幕背景为白色 (或默认背景色)，这样能在浅灰卡片上凸显出来
            .background(Color.White)
    ) {
        // 1. 模拟顶部标题栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 2. 模拟内容区域
        Column(modifier = Modifier.padding(16.dp)) {
            // 模拟一个卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                // 这里的 Surface 会默认是白色（在浅色模式下），和 MockUI 背景一致，靠 Elevation 区分
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.month_expense_mock),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.amount_mock),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary // 使用主色强调
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 模拟一个按钮
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.add_transaction_mock), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun ThemeOptionItem(
    color: Color,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected_label),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}