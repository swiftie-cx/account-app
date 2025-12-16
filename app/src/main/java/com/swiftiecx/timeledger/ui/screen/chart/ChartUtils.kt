package com.swiftiecx.timeledger.ui.screen.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

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
    val icon: ImageVector,
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

fun Float.MathRound(): Int = round(this).toInt()

// [修改] 新增 AccountMap 和 defaultCurrency 参数
fun prepareLineChartData(
    expenses: List<Expense>,
    chartMode: ChartMode,
    transactionType: TransactionType,
    accountMap: Map<Long, Account>, // [新增]
    defaultCurrency: String // [新增]
): List<LineChartPoint> {
    if (expenses.isEmpty()) return emptyList()
    val calendar = Calendar.getInstance()

    val points = mutableListOf<LineChartPoint>()

    // [BUG 修复] 定义金额兑换和汇总函数
    val sumFunc: (List<Expense>) -> Float = { list ->
        list.sumOf { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                val amountToConvert = if (transactionType == TransactionType.INCOME) expense.amount else abs(expense.amount)
                // 兑换到默认货币
                ExchangeRates.convert(amountToConvert, account.currency, defaultCurrency)
            } else {
                0.0
            }
        }.toFloat()
    }

    // 如果是余额，需要特殊处理，因为余额是正负相加
    val balanceSumFunc: (List<Expense>) -> Float = { list ->
        list.sumOf { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                ExchangeRates.convert(expense.amount, account.currency, defaultCurrency)
            } else {
                0.0
            }
        }.toFloat()
    }

    val finalSumFunc = if (transactionType == TransactionType.BALANCE) balanceSumFunc else sumFunc

    // 假设日期是从第一个 expense 中获取的，用于初始化循环
    val sampleDate = expenses.first().date
    calendar.time = sampleDate

    // [i18n] 日期格式
    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    val monthLabelFormat = SimpleDateFormat("M月", Locale.getDefault())


    when (chartMode) {
        ChartMode.WEEK -> {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (calendar.firstDayOfWeek == Calendar.SUNDAY) calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            for (i in 0 until 7) {
                val dayStart = calendar.time
                val nextDayCal = Calendar.getInstance().apply { time = dayStart; add(Calendar.DAY_OF_MONTH, 1) }
                val dayEnd = nextDayCal.time
                val sum = finalSumFunc(expenses.filter { it.date >= dayStart && it.date < dayEnd })
                val label = dayFormat.format(dayStart) // [i18n]
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
                val sum = finalSumFunc(expenses.filter { it.date >= dayStart && it.date < dayEnd })
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
                val sum = finalSumFunc(expenses.filter { it.date >= monthStart && it.date < monthEnd })
                val label = monthLabelFormat.format(monthStart) // [i18n]
                points.add(LineChartPoint(label, sum, monthStart.time))
                calendar.add(Calendar.MONTH, 1)
            }
        }
    }
    return points
}

// [修改] 新增 AccountMap 和 defaultCurrency 参数
fun prepareCustomLineChartData(
    data: List<Expense>,
    startDate: Long,
    endDate: Long,
    transactionType: TransactionType,
    accountMap: Map<Long, Account>, // [新增]
    defaultCurrency: String // [新增]
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

    // [BUG 修复] 定义金额兑换和汇总函数
    val sumFunc: (List<Expense>) -> Float = { list ->
        list.sumOf { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                val amountToConvert = if (transactionType == TransactionType.INCOME) expense.amount else abs(expense.amount)
                // 兑换到默认货币
                ExchangeRates.convert(amountToConvert, account.currency, defaultCurrency)
            } else {
                0.0
            }
        }.toFloat()
    }

    // 如果是余额，需要特殊处理，因为余额是正负相加
    val balanceSumFunc: (List<Expense>) -> Float = { list ->
        list.sumOf { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                ExchangeRates.convert(expense.amount, account.currency, defaultCurrency)
            } else {
                0.0
            }
        }.toFloat()
    }

    val finalSumFunc = if (transactionType == TransactionType.BALANCE) balanceSumFunc else sumFunc

    // [i18n] 日期格式
    val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

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
        val sum = finalSumFunc(data.filter { it.date.time in intervalStart until intervalEnd })

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

// [修改] 新增 AccountMap 和 defaultCurrency 参数
fun generateBalanceReportItems(
    data: List<Expense>,
    chartMode: ChartMode,
    accountMap: Map<Long, Account>, // [新增]
    defaultCurrency: String // [新增]
): List<BalanceReportItem> {
    val calendar = Calendar.getInstance()
    val groupedByDate = data.groupBy { expense ->
        calendar.time = expense.date
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        if (chartMode == ChartMode.YEAR) calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.timeInMillis
    }

    // [BUG 修复] 定义金额兑换函数
    fun convertAndSum(expenses: List<Expense>, isIncome: Boolean): Double {
        return expenses.sumOf { expense ->
            val account = accountMap[expense.accountId]
            if (account != null) {
                // 收入或支出绝对值
                val amountToConvert = if (isIncome) expense.amount else abs(expense.amount)
                // 兑换到默认货币
                ExchangeRates.convert(amountToConvert, account.currency, defaultCurrency)
            } else {
                0.0
            }
        }
    }

    // [i18n] 日期格式
    val dayLabelFormat = SimpleDateFormat("dd日", Locale.getDefault())
    val monthLabelFormat = SimpleDateFormat("M月", Locale.getDefault())


    return groupedByDate.entries.sortedByDescending { it.key }.map { (timeMillis, expenses) ->
        calendar.timeInMillis = timeMillis
        val timeLabel = when (chartMode) {
            ChartMode.WEEK, ChartMode.MONTH -> dayLabelFormat.format(calendar.time) // [i18n]
            ChartMode.YEAR -> monthLabelFormat.format(calendar.time) // [i18n]
        }

        // [BUG 修复] 使用兑换后的金额进行计算
        val income = convertAndSum(expenses.filter { it.amount > 0 }, true)
        val expense = convertAndSum(expenses.filter { it.amount < 0 }, false)
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