package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsCurrencyPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope
) {
    private fun detectAutoCurrency(): String {
        return try {
            val locale = Locale.getDefault()
            val language = locale.language

            // 只做“默认值猜测”，不影响用户手动设置
            when (language) {
                "zh" -> "CNY"
                "en" -> "USD"
                "ja" -> "JPY"
                "ko" -> "KRW"
                "de", "fr", "it", "es" -> "EUR"
                "pl" -> "PLN"
                "ru" -> "RUB"
                "id" -> "IDR"
                "vn", "vi" -> "VND"
                "tr" -> "TRY"
                "th" -> "THB"
                "in" -> "INR"
                else -> "USD"
            }
        } catch (_: Exception) {
            "USD"
        }
    }

    private val _defaultCurrency = MutableStateFlow(
        repository.getSavedCurrency() ?: detectAutoCurrency()
    )
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    fun setDefaultCurrency(currencyCode: String) {
        _defaultCurrency.value = currencyCode
        scope.launch(Dispatchers.IO) {
            repository.saveDefaultCurrency(currencyCode)
        }
    }
}
