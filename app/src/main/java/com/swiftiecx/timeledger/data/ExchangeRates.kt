package com.swiftiecx.timeledger.data

import android.util.Log
import com.swiftiecx.timeledger.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExchangeRates {

    // 1. 保留原本的写死汇率作为“离线默认值” (1 外币 = x CNY)
    private val defaultRates = mapOf(
        "CNY" to 1.0,
        "USD" to 7.25,
        "JPY" to 0.046,
        "HKD" to 0.93,
        "EUR" to 7.85,
        "GBP" to 9.20,
        "CAD" to 5.30,
        "AUD" to 4.80,
        "KRW" to 0.0053,
        "SGD" to 5.35,
        "INR" to 0.087,
        "IDR" to 0.00045
    )

    // 2. 当前使用的汇率表 (可变)
    private var currentRates: MutableMap<String, Double> = defaultRates.toMutableMap()

    /**
     * 从 Frankfurter API 获取最新汇率并更新缓存
     * API 返回的是：1 CNY = x Target (例如 1 CNY = 0.138 USD)
     * 我们的逻辑是：1 Target = x CNY (例如 1 USD = 7.25 CNY)
     * 所以需要取倒数： 1 / API_Rate
     */
    suspend fun updateRates() {
        withContext(Dispatchers.IO) {
            try {
                // 请求以 CNY 为基准的汇率
                val response = RetrofitClient.api.getLatestRates("CNY")

                val newRates = mutableMapOf<String, Double>()
                newRates["CNY"] = 1.0 // 基准

                // 遍历 API 返回的 map (key=货币代码, value=1 CNY能换多少该货币)
                response.rates.forEach { (currencyCode, rateToCny) ->
                    if (rateToCny != 0.0) {
                        // 转换为我们的格式: 1 外币 = (1/rate) CNY
                        newRates[currencyCode] = 1.0 / rateToCny
                    }
                }

                // 更新内存中的汇率表 (只更新 API 有的，API 没有的保留默认值)
                // 这里的 synchronized 确保线程安全
                synchronized(this@ExchangeRates) {
                    // 先把默认的 Put 进去，确保 API 没涵盖的货币（如 API 可能不包含某些小众货币）还有旧值
                    val mergedRates = defaultRates.toMutableMap()
                    // 覆盖新的
                    mergedRates.putAll(newRates)
                    currentRates = mergedRates
                }

                Log.d("ExchangeRates", "汇率更新成功: $currentRates")

            } catch (e: Exception) {
                Log.e("ExchangeRates", "汇率更新失败，使用默认值: ${e.message}")
                e.printStackTrace()
                // 失败时保持 currentRates 不变，继续使用默认值或上次成功的值
            }
        }
    }

    /**
     * 将任意货币换算为目标货币
     */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        // 使用 synchronized 读取，防止在更新时读取
        val rates = synchronized(this) { currentRates }

        val fromRate = rates[fromCurrency.uppercase()] ?: return amount
        val toRate = rates[toCurrency.uppercase()] ?: return amount

        // 1. 先换算成基础货币 CNY
        val amountInCny = amount * fromRate

        // 2. 再从 CNY 换算成目标货币
        return amountInCny / toRate
    }
}