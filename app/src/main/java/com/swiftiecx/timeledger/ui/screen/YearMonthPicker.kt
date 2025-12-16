package com.swiftiecx.timeledger.ui.screen

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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .width(300.dp)
                .wrapContentHeight()
        ) {
            Column(
                // 底部 padding 收紧到 8.dp，顶部保持 16.dp 呼吸感
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 顶部大标题
                Text(
                    text = "${currentYear}年${currentMonth}月",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    // 进一步减小标题下方的留白
                    modifier = Modifier.padding(bottom = 4.dp)
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
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$currentYear",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (isYearSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "切换模式",
                            tint = if (isYearSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 右侧：翻页箭头
                    Row {
                        IconButton(
                            onClick = {
                                if (isYearSelectionMode) currentYear -= 12 else currentYear--
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (isYearSelectionMode) currentYear += 12 else currentYear++
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 间距：控制栏与网格之间
                Spacer(modifier = Modifier.height(8.dp))

                // 3. 网格内容区域
                // 高度精确计算：4行 * 36dp + 3个间隙 * 8dp = 144 + 24 = 168dp
                // 这样刚好容纳内容，没有任何多余垂直空白
                Box(modifier = Modifier.height(168.dp)) {
                    if (isYearSelectionMode) {
                        YearGrid(
                            selectedYear = currentYear,
                            displayYearBase = currentYear,
                            onYearSelected = {
                                currentYear = it
                                isYearSelectionMode = false
                            }
                        )
                    } else {
                        MonthGrid(
                            selectedMonth = currentMonth,
                            onMonthSelected = { currentMonth = it }
                        )
                    }
                }

                // 间距：网格与按钮之间 (极度收紧)
                Spacer(modifier = Modifier.height(4.dp))

                // 4. 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("取消", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { onConfirm(currentYear, currentMonth) },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        Text("确定", style = MaterialTheme.typography.labelLarge)
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months) { month ->
            val isSelected = month == selectedMonth
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(backgroundColor)
                    .clickable { onMonthSelected(month) }
            ) {
                Text(
                    text = "${month}月",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun YearGrid(selectedYear: Int, displayYearBase: Int, onYearSelected: (Int) -> Unit) {
    val base = displayYearBase - 4
    val years = (base until base + 12).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(years) { year ->
            val isSelected = year == selectedYear
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(backgroundColor)
                    .clickable { onYearSelected(year) }
            ) {
                Text(
                    text = "$year",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}