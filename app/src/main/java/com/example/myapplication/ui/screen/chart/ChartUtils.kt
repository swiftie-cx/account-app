package com.example.myapplication.ui.screen.chart

import androidx.compose.ui.graphics.Color
import com.example.myapplication.data.Expense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// --- 枚举定义 ---
enum class ChartMode { WEEK, MONTH, YEAR }
enum class TransactionType { INCOME, EXPENSE, BALANCE }

// --- 数据类 ---
data class LineChartPoint(val label: String, val value: Float, val timeMillis: Long)
data class BalanceReportItem(val timeLabel: String, val income: Double, val expense: Double, val balance: Double)
data class ChartData(val name: String, val value: Long, val color: Color)

// [新增] 嵌套统计数据类
data class SubCategoryStat(
    val name: String,
    val amount: Double,
    val percentageOfParent: Float // 占父大类的百分比
)

data class MainCategoryStat(
    val name: String,
    val amount: Double,
    val percentageOfTotal: Float, // 占总支出的百分比
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val subCategories: List<SubCategoryStat>
)

// --- 工具函数 ---

fun calculateDateRange(calendar: Calendar, mode: ChartMode): Pair<Long, Long> {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val startMillis: Long
    val endMillis: Long

    when (mode) {
        ChartMode.WEEK -> {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (cal.firstDayOfWeek == Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            startMillis = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            endMillis = cal.timeInMillis
        }
        ChartMode.MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            startMillis = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            endMillis = cal.timeInMillis
        }
        ChartMode.YEAR -> {
            cal.set(Calendar.DAY_OF_YEAR, 1)
            startMillis = cal.timeInMillis
            cal.set(Calendar.MONTH, 11)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            endMillis = cal.timeInMillis
        }
    }
    return startMillis to endMillis
}

fun Float.MathRound(): Int = kotlin.math.round(this).toInt()

fun prepareLineChartData(expenses: List<Expense>, chartMode: ChartMode, transactionType: TransactionType): List<LineChartPoint> {
    if (expenses.isEmpty()) return emptyList()
    val calendar = Calendar.getInstance()
    // 注意：这里不需要取 expenses.first().date，而是应该根据 chartMode 生成完整的坐标轴
    // 但为了简单兼容，原有逻辑是基于数据存在的日期。
    // 如果想要更严谨的图表（即使某天没数据也显示0），需要重写这里。
    // 目前保持原有逻辑，但建议确保日期范围正确。

    // 为了简化，这里我们复用下面的 prepareCustomLineChartData 的逻辑核心，或者保持您原有的逻辑。
    // 这里我保持您原有的逻辑，不做大改，以免引入新Bug。
    val sampleDate = expenses.first().date
    calendar.time = sampleDate

    val points = mutableListOf<LineChartPoint>()
    val sumFunc: (List<Expense>) -> Float = { list ->
        if (transactionType == TransactionType.BALANCE) list.sumOf { it.amount }.toFloat() else list.sumOf { abs(it.amount) }.toFloat()
    }
    when (chartMode) {
        ChartMode.WEEK -> {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (calendar.firstDayOfWeek == Calendar.SUNDAY) calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            for (i in 0 until 7) {
                val dayStart = calendar.time
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time
                val sum = sumFunc(expenses.filter { it.date >= dayStart && it.date < dayEnd })
                val label = SimpleDateFormat("dd", Locale.CHINA).format(dayStart)
                points.add(LineChartPoint(label, sum, dayStart.time))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        ChartMode.MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in 1..maxDays) {
                val dayStart = calendar.time
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time
                val sum = sumFunc(expenses.filter { it.date >= dayStart && it.date < dayEnd })
                points.add(LineChartPoint(i.toString(), sum, dayStart.time))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        ChartMode.YEAR -> {
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            for (i in 0 until 12) {
                val monthStart = calendar.time
                val nextMonthCal = Calendar.getInstance().apply { time = monthStart; add(Calendar.MONTH, 1) }
                val monthEnd = nextMonthCal.time
                val sum = sumFunc(expenses.filter { it.date >= monthStart && it.date < monthEnd })
                points.add(LineChartPoint("${i + 1}月", sum, monthStart.time))
                calendar.add(Calendar.MONTH, 1)
            }
        }
    }
    return points
}

// [新增] 专门处理自定义范围的折线图数据生成 (修复报错的关键)
fun prepareCustomLineChartData(
    data: List<Expense>,
    startDate: Long,
    endDate: Long,
    transactionType: TransactionType
): List<LineChartPoint> {
    if (data.isEmpty()) return emptyList()

    // 1. 计算天数跨度
    val diffMillis = endDate - startDate
    val daysDiff = diffMillis / (1000 * 60 * 60 * 24)

    // 2. 决定粒度：如果跨度超过 90 天，按月统计；否则按日统计
    val isMonthly = daysDiff > 90

    val points = mutableListOf<LineChartPoint>()
    val calendar = Calendar.getInstance()

    // 设置循环的起始时间 (重置为当天的 00:00:00)
    calendar.timeInMillis = startDate
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val endCalendar = Calendar.getInstance()
    endCalendar.timeInMillis = endDate
    // 确保结束时间包含当天
    endCalendar.set(Calendar.HOUR_OF_DAY, 23)
    endCalendar.set(Calendar.MINUTE, 59)
    endCalendar.set(Calendar.SECOND, 59)

    // 定义金额汇总函数
    val sumFunc: (List<Expense>) -> Float = { list ->
        if (transactionType == TransactionType.BALANCE)
            list.sumOf { it.amount }.toFloat()
        else
            list.sumOf { abs(it.amount) }.toFloat()
    }

    val dateFormat = SimpleDateFormat("MM-dd", Locale.CHINA)
    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.CHINA)

    // 3. 循环生成点
    // 防止死循环，加一个安全计数
    var safeCount = 0
    val maxLoops = 366 * 5 // 最多5年

    while (calendar.timeInMillis <= endCalendar.timeInMillis && safeCount < maxLoops) {
        val intervalStart = calendar.timeInMillis

        // 计算当前区间的结束时间
        val intervalEnd = if (isMonthly) {
            // 如果是按月，结束时间是下个月初
            val c = calendar.clone() as Calendar
            c.add(Calendar.MONTH, 1)
            c.timeInMillis
        } else {
            // 如果是按日，结束时间是明天
            val c = calendar.clone() as Calendar
            c.add(Calendar.DATE, 1)
            c.timeInMillis
        }

        // 筛选区间内的数据
        val sum = sumFunc(data.filter { it.date.time in intervalStart until intervalEnd })

        // 生成标签
        val label = if (isMonthly) {
            monthFormat.format(Date(intervalStart))
        } else {
            dateFormat.format(Date(intervalStart))
        }

        points.add(LineChartPoint(label, sum, intervalStart))

        // 步进
        if (isMonthly) {
            calendar.add(Calendar.MONTH, 1)
        } else {
            calendar.add(Calendar.DATE, 1)
        }
        safeCount++
    }

    return points
}

fun generateBalanceReportItems(data: List<Expense>, chartMode: ChartMode): List<BalanceReportItem> {
    val calendar = Calendar.getInstance()
    val groupedByDate = data.groupBy { expense ->
        calendar.time = expense.date
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        if (chartMode == ChartMode.YEAR) calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.timeInMillis
    }
    return groupedByDate.entries.sortedByDescending { it.key }.map { (timeMillis, expenses) ->
        calendar.timeInMillis = timeMillis
        val timeLabel = when (chartMode) {
            ChartMode.WEEK, ChartMode.MONTH -> calendar.get(Calendar.DAY_OF_MONTH).toString() + "日"
            ChartMode.YEAR -> (calendar.get(Calendar.MONTH) + 1).toString() + "月"
        }
        val income = expenses.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = expenses.filter { it.amount < 0 }.sumOf { abs(it.amount) }
        val balance = income - expense
        BalanceReportItem(timeLabel, income, expense, balance)
    }
}

// 莫兰迪配色
fun getChartColors(): List<Color> {
    return listOf(
        Color(0xFF9FA8DA), Color(0xFFFFB74D), Color(0xFF80CBC4),
        Color(0xFFF48FB1), Color(0xFFCE93D8), Color(0xFFFFE082),
        Color(0xFFA5D6A7), Color(0xFF90CAF9), Color(0xFFB0BEC5)
    )
}