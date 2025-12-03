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

// 定义截图中的黄色主题色
private val SelectionColor = Color(0xFF9C27B0) // 接近截图的黄色
private val TextSelectedColor = Color.Black
private val TextNormalColor = Color.Black

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
    var isYearSelectionMode by remember { mutableStateOf(false) } // true=选年份, false=选月份

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 顶部大标题 "2025年12月"
                Text(
                    text = "${currentYear}年${currentMonth}月",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextNormalColor
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
                            style = MaterialTheme.typography.titleMedium.copy(color = TextNormalColor)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "切换模式",
                            tint = TextNormalColor
                        )
                    }

                    // 右侧：翻页箭头
                    Row {
                        IconButton(onClick = {
                            if (isYearSelectionMode) currentYear -= 12 else currentYear--
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = TextNormalColor)
                        }
                        IconButton(onClick = {
                            if (isYearSelectionMode) currentYear += 12 else currentYear++
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextNormalColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. 网格内容区域
                Box(modifier = Modifier.height(240.dp)) {
                    if (isYearSelectionMode) {
                        YearGrid(
                            selectedYear = currentYear,
                            displayYearBase = currentYear, // 基于当前年份显示一页
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

                Spacer(modifier = Modifier.height(16.dp))

                // 4. 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", style = MaterialTheme.typography.titleMedium, color = SelectionColor)
                    }
                    TextButton(onClick = { onConfirm(currentYear, currentMonth) }) {
                        Text("确定", style = MaterialTheme.typography.titleMedium, color = SelectionColor)
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months) { month ->
            val isSelected = month == selectedMonth
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp)) // 胶囊形状
                    .background(if (isSelected) SelectionColor else Color.Transparent)
                    .clickable { onMonthSelected(month) }
            ) {
                Text(
                    text = "${month}月",
                    color = if (isSelected) TextSelectedColor else TextNormalColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun YearGrid(selectedYear: Int, displayYearBase: Int, onYearSelected: (Int) -> Unit) {
    // 计算当前页显示的12个年份 (例如 2017-2028)
    // 算法：找到当前年份所在的12年区间的起始年份
    val startYear = displayYearBase - (displayYearBase % 12) + (selectedYear % 12 % 4) // 简单的动态调整，或者固定逻辑
    // 简化逻辑：以传入的年份为中心，或者显示固定的12个
    // 这里为了复刻截图(2017-2028)，我们动态生成一个包含当前年份的范围
    val base = displayYearBase - 4 // 让当前年份稍微靠中间一点
    val years = (base until base + 12).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(years) { year ->
            val isSelected = year == selectedYear
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) SelectionColor else Color.Transparent)
                    .clickable { onYearSelected(year) }
            ) {
                Text(
                    text = "$year",
                    color = if (isSelected) TextSelectedColor else TextNormalColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}