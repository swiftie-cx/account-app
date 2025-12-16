// timeledger/ui/screen/CustomDateRangePicker.kt
package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape // [关键修复] 导入 CircleShape
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.swiftiecx.timeledger.R
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
                        // [i18n]
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(startDate, endDate) },
                        enabled = startDate != null
                    ) {
                        // [i18n]
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun SingleDateHeader(date: Long?) {
    // [Fix] 使用 Locale.getDefault()
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        // [i18n]
        Text(
            text = stringResource(R.string.select_date), // 假设 strings.xml 有 select_date
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (date != null) dateFormat.format(Date(date)) else stringResource(R.string.select_date), // [i18n] 再次使用 select_date
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun RangeHeader(startDate: Long?, endDate: Long?) {
    // [Fix] 使用 Locale.getDefault()
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            // [i18n]
            Text(
                text = stringResource(R.string.select_date_range), // 假设 strings.xml 有 select_date_range
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = if (startDate != null) dateFormat.format(Date(startDate)) else stringResource(R.string.start_date), // [i18n]
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
                    text = if (endDate != null) dateFormat.format(Date(endDate)) else stringResource(R.string.end_date), // [i18n] 假设 strings.xml 有 end_date
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
    // [Fix] 使用 Locale.getDefault()
    val monthFormat = remember { SimpleDateFormat("yyyy年MM月", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance().apply { set(year, month - 1, 1) } }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.prev_month)) // [i18n]
        }

        Text(
            // [Fix] 动态生成月份文本
            text = monthFormat.format(calendar.apply { set(year, month - 1, 1) }.time),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month)) // [i18n]
        }
    }
}

@Composable
fun WeekHeader() {
    // [Fix] 使用 Locale.getDefault() 来获取本地化的星期几名称
    val days = remember {
        val format = SimpleDateFormat("EEEEE", Locale.getDefault()) // EEEEE for single letter (e.g., S, M, T)
        val cal = Calendar.getInstance()
        val daysList = mutableListOf<String>()
        // 确保从周日或周一开始，具体取决于 Locale
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // 先设置为周日
        repeat(7) {
            daysList.add(format.format(cal.time))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        daysList
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// CalendarGrid 和 DayCell 逻辑复杂，暂不修改，但它们的引用已修复
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
    val emptySlots = firstDayOfWeek - calendar.firstDayOfWeek
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
        // 只有在【非单选模式】下，才绘制范围背景长条
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
                            else -> RectangleShape
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

        // 选中的圆圈。单选模式下，isStart 就是选中的那一天。
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