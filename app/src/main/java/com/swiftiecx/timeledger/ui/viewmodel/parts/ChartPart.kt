package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.ui.feature.chart.util.ChartMode
import com.swiftiecx.timeledger.ui.feature.chart.util.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChartPart {
    private val _chartMode = MutableStateFlow(ChartMode.MONTH)
    val chartModeState: StateFlow<ChartMode> = _chartMode.asStateFlow()
    fun setChartMode(mode: ChartMode) { _chartMode.value = mode }

    private val _chartTransactionType = MutableStateFlow(TransactionType.EXPENSE)
    val chartTransactionTypeState: StateFlow<TransactionType> = _chartTransactionType.asStateFlow()
    fun setChartTransactionType(type: TransactionType) { _chartTransactionType.value = type }

    private val _chartDateMillis = MutableStateFlow(System.currentTimeMillis())
    val chartDateMillisState: StateFlow<Long> = _chartDateMillis.asStateFlow()
    fun setChartDate(millis: Long) { _chartDateMillis.value = millis }

    private val _chartCustomDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val chartCustomDateRangeState: StateFlow<Pair<Long, Long>?> = _chartCustomDateRange.asStateFlow()
    fun setChartCustomDateRange(start: Long?, end: Long?) {
        _chartCustomDateRange.value = if (start != null && end != null) start to end else null
    }
}
