package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun YearMonthPicker(
    year: Int,
    month: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 内部状态
    var currentYear by remember { mutableIntStateOf(year) }
    var currentMonth by remember { mutableIntStateOf(month) }
    var isYearSelectionMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp), // 增加圆角，更符合 Material3
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), // 使用稍微高层级的背景色
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 顶部大标题
                Text(
                    text = "${currentYear}年${currentMonth}月",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 2. 控制栏：年份下拉 + 左右箭头
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：年份切换按钮
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isYearSelectionMode = !isYearSelectionMode }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$currentYear",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "切换模式",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 右侧：翻页箭头
                    Row {
                        IconButton(onClick = {
                            if (isYearSelectionMode) currentYear -= 12 else currentYear--
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            if (isYearSelectionMode) currentYear += 12 else currentYear++
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. 网格内容区域
                Box(modifier = Modifier.height(240.dp)) {
                    if (isYearSelectionMode) {
                        YearGrid(
                            selectedYear = currentYear,
                            displayYearBase = currentYear,
                            onYearSelected = {
                                currentYear = it
                                isYearSelectionMode = false // 选完年自动跳回选月
                            }
                        )
                    } else {
                        MonthGrid(
                            selectedMonth = currentMonth,
                            onMonthSelected = { currentMonth = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // 按钮靠右
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(currentYear, currentMonth) }) {
                        Text("确定", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun MonthGrid(selectedMonth: Int, onMonthSelected: (Int) -> Unit) {
    val months = (1..12).toList()
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months) { month ->
            val isSelected = month == selectedMonth
            // 使用主题色：选中为主色(Purple)，未选中为透明
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            // 文字颜色：选中为OnPrimary(White)，未选中为OnSurface(Black/DarkGrey)
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp)) // 胶囊/圆形
                    .background(backgroundColor)
                    .clickable { onMonthSelected(month) }
            ) {
                Text(
                    text = "${month}月",
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun YearGrid(selectedYear: Int, displayYearBase: Int, onYearSelected: (Int) -> Unit) {
    // 动态计算年份范围，以当前年份为中心
    val base = displayYearBase - 4
    val years = (base until base + 12).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(years) { year ->
            val isSelected = year == selectedYear
            // 同上，使用主题色
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .clickable { onYearSelected(year) }
            ) {
                Text(
                    text = "$year",
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}