package com.example.myapplication.ui.screen.chart

import androidx.compose.ui.graphics.Color
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
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
    val sampleDate = expenses.first().date
    val calendar = Calendar.getInstance()
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