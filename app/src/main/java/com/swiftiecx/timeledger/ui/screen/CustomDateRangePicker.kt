package com.swiftiecx.timeledger.ui.screen

import android.text.format.DateFormat
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

    // 初始化显示的年月（从 startDate 推断，否则当前月）
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
                // 1) 顶部标题（单选/范围）
                if (isSingleSelection) {
                    SingleDateHeader(startDate)
                } else {
                    RangeHeader(startDate, endDate)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2) 月份切换控制栏
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

                // 3) 星期表头（跟随 Locale 的一周起始日）
                WeekHeader()

                Spacer(modifier = Modifier.height(8.dp))

                // 4) 日历网格
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
                            when {
                                startDate == null -> {
                                    startDate = clickedDate
                                    endDate = null
                                }
                                endDate == null -> {
                                    if (clickedDate < startDate!!) {
                                        startDate = clickedDate
                                    } else if (clickedDate == startDate!!) {
                                        // 同一天：视为只选了 start
                                        endDate = null
                                    } else {
                                        endDate = clickedDate
                                    }
                                }
                                else -> {
                                    startDate = clickedDate
                                    endDate = null
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5) 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(startDate, endDate) },
                        enabled = startDate != null
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun SingleDateHeader(date: Long?) {
    val locale = Locale.getDefault()
    // ✅ 不写死中文格式，完全跟随系统语言
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "yMMMd") }
    val dateFormat = remember(locale, pattern) { SimpleDateFormat(pattern, locale) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.select_date),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (date != null) dateFormat.format(Date(date)) else stringResource(R.string.select_date),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun RangeHeader(startDate: Long?, endDate: Long?) {
    val locale = Locale.getDefault()
    // ✅ 用 "MMMd"（如 Dec 16 / 12月16日 / 12.16 等），跟随系统语言
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "MMMd") }
    val dateFormat = remember(locale, pattern) { SimpleDateFormat(pattern, locale) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.select_date_range),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = if (startDate != null) dateFormat.format(Date(startDate)) else stringResource(R.string.start_date),
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
                    text = if (endDate != null) dateFormat.format(Date(endDate)) else stringResource(R.string.end_date),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (endDate != null) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

@Composable
fun MonthController(
    year: Int,
    month: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val locale = Locale.getDefault()
    // ✅ 用 "yMMM"（如 Dec 2025 / 2025년 12월 / 2025年12月），跟随系统语言
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "yMMM") }
    val monthFormat = remember(locale, pattern) { SimpleDateFormat(pattern, locale) }

    val cal = remember { Calendar.getInstance() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevClick) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.prev_month)
            )
        }

        Text(
            text = monthFormat.format(
                cal.apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }.time
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextClick) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_month)
            )
        }
    }
}

@Composable
fun WeekHeader() {
    val locale = Locale.getDefault()
    val cal = remember { Calendar.getInstance(locale) }

    // ✅ 单字母/窄格式（S M T… / 日 一 二… / 월 화…）
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "EEEEE") }
    val dayFormat = remember(locale, pattern) { SimpleDateFormat(pattern, locale) }

    val days = remember(locale) {
        val list = mutableListOf<String>()
        val tmp = cal.clone() as Calendar
        // ✅ 从本地的一周起始日开始（有的国家周一开始）
        tmp.set(Calendar.DAY_OF_WEEK, tmp.firstDayOfWeek)
        repeat(7) {
            list.add(dayFormat.format(tmp.time))
            tmp.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
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
    val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)

    // ✅ 修复：避免负数，保证 0..6
    val emptySlots = (firstDayOfMonth - calendar.firstDayOfWeek + 7) % 7

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
                calendar.set(year, month - 1, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val currentMillis = calendar.timeInMillis

                val isStart = startDate != null && isSameDay(currentMillis, startDate)
                val isEnd = !isSingleSelection && endDate != null && isSameDay(currentMillis, endDate)
                val isInRange = !isSingleSelection &&
                        startDate != null &&
                        endDate != null &&
                        currentMillis > startDate &&
                        currentMillis < endDate

                DayCell(
                    day = day,
                    isStart = isStart,
                    isEnd = isEnd,
                    isInRange = isInRange,
                    isSingleSelection = isSingleSelection,
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
    isSingleSelection: Boolean,
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
                        .background(
                            rangeColor,
                            if (isStart) RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                            else RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
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

        val isSelected = isStart || (!isSingleSelection && isEnd)
        Text(
            text = day.toString(),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
