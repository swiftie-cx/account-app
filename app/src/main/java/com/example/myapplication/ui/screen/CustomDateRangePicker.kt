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
    onDismiss: () -> Unit,
    isSingleSelection: Boolean = false
) {
    // 内部状态：当前选中的范围
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }

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
                // 1. 顶部标题栏
                if (isSingleSelection) {
                    SingleDateHeader(startDate)
                } else {
                    RangeHeader(startDate, endDate)
                }

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
                    isSingleSelection = isSingleSelection,
                    onDateClick = { dateMillis ->
                        if (isSingleSelection) {
                            startDate = dateMillis
                            endDate = null
                        } else {
                            val clickedDate = dateMillis
                            if (startDate == null) {
                                startDate = clickedDate
                            } else if (endDate == null) {
                                if (clickedDate < startDate!!) {
                                    startDate = clickedDate
                                } else {
                                    endDate = clickedDate
                                }
                            } else {
                                startDate = clickedDate
                                endDate = null
                            }
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
                        enabled = startDate != null
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun SingleDateHeader(date: Long?) {
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "选择日期",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (date != null) dateFormat.format(Date(date)) else "请选择",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
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
    isSingleSelection: Boolean,
    onDateClick: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, 1, 0, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val emptySlots = firstDayOfWeek - 1
    val totalSlots = emptySlots + daysInMonth
    val list = (0 until totalSlots).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(list) { index ->
            if (index < emptySlots) {
                Box(modifier = Modifier.size(40.dp))
            } else {
                val day = index - emptySlots + 1
                calendar.set(year, month - 1, day)
                val currentMillis = calendar.timeInMillis

                val isStart = startDate != null && isSameDay(currentMillis, startDate!!)
                val isEnd = !isSingleSelection && endDate != null && isSameDay(currentMillis, endDate!!)
                val isInRange = !isSingleSelection && startDate != null && endDate != null &&
                        currentMillis > startDate!! && currentMillis < endDate!!

                DayCell(
                    day = day,
                    isStart = isStart,
                    isEnd = isEnd,
                    isInRange = isInRange,
                    isSingleSelection = isSingleSelection, // 传入单选标志
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
    isSingleSelection: Boolean, // 新增参数
    onClick: () -> Unit
) {
    val rangeColor = MaterialTheme.colorScheme.primaryContainer
    val selectedColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 修改点：只有在【非单选模式】下，才绘制范围背景长条
        if (!isSingleSelection && (isInRange || isStart || isEnd)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(
                        color = if (isInRange) rangeColor else Color.Transparent,
                        shape = when {
                            isStart && isEnd -> CircleShape
                            isStart -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                            isEnd -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
                            else -> androidx.compose.ui.graphics.RectangleShape
                        }
                    )
            )
            // 补丁：解决圆角连接处的缝隙
            if ((isStart || isEnd) && !(isStart && isEnd)) {
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

        // 修改点：选中的圆圈。单选模式下，isStart 就是选中的那一天。
        if (isStart || (!isSingleSelection && isEnd)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
            )
        }

        // 文字颜色处理：选中为白色，未选中为主题文字色
        val isSelected = isStart || (!isSingleSelection && isEnd)
        Text(
            text = day.toString(),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}