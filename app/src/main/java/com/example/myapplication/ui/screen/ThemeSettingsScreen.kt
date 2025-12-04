package com.example.myapplication.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.viewmodel.AppThemeOptions
import com.example.myapplication.ui.viewmodel.ThemeOption
import com.example.myapplication.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    val currentThemeColor by themeViewModel.themeColor.collectAsState()

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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. 顶部：沉浸式预览区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                LivePreviewCard(currentThemeColor)
            }

            // 2. 底部：颜色选择网格
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "选择配色",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(AppThemeOptions) { themeOption ->
                            ThemeOptionItem(
                                option = themeOption,
                                isSelected = themeOption.color == currentThemeColor,
                                onClick = { themeViewModel.setThemeColor(themeOption.color) }
                            )
                        }
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }
}

// --- 核心组件：实时预览卡片 (保持不变) ---
@Composable
fun LivePreviewCard(targetColor: Color) {
    val animatedPrimary by animateColorAsState(targetColor, animationSpec = tween(500), label = "primary")
    val animatedContainer by animateColorAsState(targetColor.copy(alpha = 0.1f), animationSpec = tween(500), label = "container")
    val animatedOnPrimary by animateColorAsState(Color.White, label = "onPrimary")

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(350.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.White)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.width(60.dp).height(8.dp).background(Color.LightGray, CircleShape))
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("本月", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = animatedPrimary)
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(animatedPrimary, CircleShape))
                }
                Spacer(Modifier.width(16.dp))
                Text("上月", fontSize = 10.sp, color = Color.Gray)
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedPrimary)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(modifier = Modifier.width(40.dp).height(6.dp).background(Color.White.copy(0.6f), CircleShape))
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.width(80.dp).height(14.dp).background(Color.White, CircleShape))
                    }
                }

                Spacer(Modifier.height(16.dp))

                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(animatedContainer)) {
                            Box(modifier = Modifier.size(12.dp).background(animatedPrimary, CircleShape).align(Alignment.Center))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.width(60.dp).height(8.dp).background(Color.LightGray.copy(0.4f), CircleShape))
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.width(40.dp).height(6.dp).background(Color.LightGray.copy(0.2f), CircleShape))
                        }
                        Box(modifier = Modifier.width(30.dp).height(8.dp).background(animatedPrimary.copy(alpha = 0.8f), CircleShape))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(animatedPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = animatedOnPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// --- 颜色选项组件 (重构：极简圆形) ---
@Composable
fun ThemeOptionItem(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 动画：选中时稍微放大，并增加阴影
    val size by animateDpAsState(if (isSelected) 56.dp else 48.dp, label = "size")
    val elevation by animateDpAsState(if (isSelected) 8.dp else 2.dp, label = "elevation")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // 纯粹的圆形色块
        Box(
            modifier = Modifier
                .size(size) // 动态大小
                .shadow(elevation, CircleShape) // 动态阴影
                .clip(CircleShape)
                .background(option.color),
            contentAlignment = Alignment.Center
        ) {
            // 选中时显示白色对号
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 颜色名称
        Text(
            text = option.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}