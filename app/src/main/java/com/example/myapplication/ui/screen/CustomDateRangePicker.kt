package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CustomDateRangePicker(
    initialStartDate: Long?,
    initialEndDate: Long?,
    onConfirm: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // 内部状态：当前选中的范围
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }

    // 内部状态：当前展示的年月（默认显示开始时间所在的月份，如果没有则显示当前月）
    val calendar = remember { Calendar.getInstance() }

    // 初始化显示的年月
    var displayYear by remember {
        mutableIntStateOf(
            if (initialStartDate != null) {
                calendar.timeInMillis = initialStartDate
                calendar.get(Calendar.YEAR)
            } else {
                Calendar.getInstance().get(Calendar.YEAR)
            }
        )
    }
    var displayMonth by remember {
        mutableIntStateOf(
            if (initialStartDate != null) {
                calendar.timeInMillis = initialStartDate
                calendar.get(Calendar.MONTH) + 1
            } else {
                Calendar.getInstance().get(Calendar.MONTH) + 1
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 顶部标题栏：显示当前选中的范围
                RangeHeader(startDate, endDate)

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 月份切换控制栏
                MonthController(
                    year = displayYear,
                    month = displayMonth,
                    onPrevClick = {
                        if (displayMonth == 1) {
                            displayMonth = 12
                            displayYear--
                        } else {
                            displayMonth--
                        }
                    },
                    onNextClick = {
                        if (displayMonth == 12) {
                            displayMonth = 1
                            displayYear++
                        } else {
                            displayMonth++
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. 星期表头
                WeekHeader()

                Spacer(modifier = Modifier.height(8.dp))

                // 4. 日历网格
                CalendarGrid(
                    year = displayYear,
                    month = displayMonth,
                    startDate = startDate,
                    endDate = endDate,
                    onDateClick = { dateMillis ->
                        val clickedDate = dateMillis

                        if (startDate == null) {
                            // 情况1：还没选，直接设为开始
                            startDate = clickedDate
                        } else if (endDate == null) {
                            // 情况2：已选开始，未选结束
                            if (clickedDate < startDate!!) {
                                // 如果点的比开始早，修正为新的开始
                                startDate = clickedDate
                            } else {
                                // 设为结束
                                endDate = clickedDate
                            }
                        } else {
                            // 情况3：都选了，重新开始选
                            startDate = clickedDate
                            endDate = null
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(startDate, endDate) },
                        enabled = startDate != null // 至少选一个才能确认
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun RangeHeader(startDate: Long?, endDate: Long?) {
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.CHINA) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "选择日期范围",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = if (startDate != null) dateFormat.format(Date(startDate)) else "开始日期",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (startDate != null) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    text = " - ",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (endDate != null) dateFormat.format(Date(endDate)) else "结束日期",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (endDate != null) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

@Composable
fun MonthController(year: Int, month: Int, onPrevClick: () -> Unit, onNextClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
        }

        Text(
            text = "${year}年${month}月",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
        }
    }
}

@Composable
fun WeekHeader() {
    val weeks = listOf("日", "一", "二", "三", "四", "五", "六")
    Row(modifier = Modifier.fillMaxWidth()) {
        weeks.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun CalendarGrid(
    year: Int,
    month: Int,
    startDate: Long?,
    endDate: Long?,
    onDateClick: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    // 设置为当月1号
    calendar.set(year, month - 1, 1, 0, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday...

    // 前面的空白格数量 (日历第一排的空位)
    val emptySlots = firstDayOfWeek - 1

    val totalSlots = emptySlots + daysInMonth
    val list = (0 until totalSlots).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp) // 紧凑排列以便做范围背景
    ) {
        items(list) { index ->
            if (index < emptySlots) {
                // 空白格
                Box(modifier = Modifier.size(40.dp))
            } else {
                val day = index - emptySlots + 1
                calendar.set(year, month - 1, day)
                val currentMillis = calendar.timeInMillis

                // 判断状态
                val isStart = startDate != null && isSameDay(currentMillis, startDate!!)
                val isEnd = endDate != null && isSameDay(currentMillis, endDate!!)
                val isInRange = startDate != null && endDate != null &&
                        currentMillis > startDate!! && currentMillis < endDate!!

                // 渲染日期单元格
                DayCell(
                    day = day,
                    isStart = isStart,
                    isEnd = isEnd,
                    isInRange = isInRange,
                    onClick = { onDateClick(currentMillis) }
                )
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    isStart: Boolean,
    isEnd: Boolean,
    isInRange: Boolean,
    onClick: () -> Unit
) {
    // 范围背景色 (使用我们调整过的 PrimaryContainer)
    val rangeColor = MaterialTheme.colorScheme.primaryContainer
    // 选中背景色 (使用 Primary)
    val selectedColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(40.dp) // 单元格大小
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 1. 范围背景 (长条)
        if (isInRange || isStart || isEnd) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp) // 上下留一点白，只在水平方向连贯
                    .background(
                        color = if (isInRange) rangeColor else Color.Transparent,
                        // 如果是起点，左边要是圆的；如果是终点，右边是圆的；中间是直的
                        shape = when {
                            isStart && isEnd -> CircleShape // 单选一天
                            isStart -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                            isEnd -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
                            else -> androidx.compose.ui.graphics.RectangleShape
                        }
                    )
            )

            // 为了解决 rangeColor 覆盖在 start/end 圆圈下的问题，
            // 如果是 Start 或 End，我们需要额外画一个半截的背景来连接中间
            if ((isStart || isEnd) && !(isStart && isEnd)) {
                // 这里的逻辑有点复杂，简单做法：
                // Start/End 也是 Range 的一部分，只是上面多盖了一个圆圈
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .background(rangeColor,
                            if(isStart) RoundedCornerShape(topStart=50.dp, bottomStart=50.dp)
                            else RoundedCornerShape(topEnd=50.dp, bottomEnd=50.dp)
                        )
                )
            }
        }

        // 2. 选中圆圈 (Start/End)
        if (isStart || isEnd) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
            )
        }

        // 3. 文字
        Text(
            text = day.toString(),
            color = if (isStart || isEnd) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 辅助函数：忽略时分秒比较日期
fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}