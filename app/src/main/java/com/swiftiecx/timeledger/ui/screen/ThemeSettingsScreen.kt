package com.swiftiecx.timeledger.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
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
    // 这里只简单模拟了 Primary 和相关的 OnPrimary，其他保持默认
    // 实际效果可能会因为您 App 的具体 Theme 设置而略有不同，但足够预览主色调了
    val previewColorScheme = MaterialTheme.colorScheme.copy(
        primary = currentThemeColor,
        onPrimary = Color.White, // 假设主色上文字为白色
        primaryContainer = currentThemeColor.copy(alpha = 0.2f), // 浅色容器
        onPrimaryContainer = currentThemeColor // 容器上文字颜色
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("主题风格") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- 【关键修改】全新的即时预览区域 ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("即时预览", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                    // 使用自定义的配色方案包裹预览组件
                    MaterialTheme(colorScheme = previewColorScheme) {
                        PreviewMockUI()
                    }
                }
            }

            Text(
                text = "选择配色",
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
                        name = option.name,
                        isSelected = (option.color.toArgb() == currentThemeColor.toArgb()),
                        onClick = { themeViewModel.updateThemeColor(option.color) }
                    )
                }
            }
        }
    }
}

// --- 【新增】模拟 UI 组件，用于展示主题效果 ---
@Composable
fun PreviewMockUI() {
    Column(
        modifier = Modifier
            .width(240.dp) // 限制宽度，模拟手机屏幕
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background) // 使用背景色
    ) {
        // 1. 模拟顶部标题栏 (使用 Primary 色)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "拾光账本",
                color = MaterialTheme.colorScheme.onPrimary, // 主色上的文字颜色
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 2. 模拟内容区域
        Column(modifier = Modifier.padding(16.dp)) {
            // 模拟一个卡片 (Surface 色)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "本月支出",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥ 1,234.56",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary // 使用主色强调
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 模拟一个按钮 (Primary 色)
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("记一笔", color = MaterialTheme.colorScheme.onPrimary)
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
                    contentDescription = "已选中",
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