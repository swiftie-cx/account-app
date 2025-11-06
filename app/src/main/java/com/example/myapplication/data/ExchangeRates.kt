package com.example.myapplication.data

object ExchangeRates {

    // 以 CNY 为基础货币
    private val ratesToCny = mapOf(
        "CNY" to 1.0,      // 人民币
        "USD" to 7.25,    // 美元
        "JPY" to 0.046,   // 日元
        "HKD" to 0.93,    // 港币
        "EUR" to 7.85,    // 欧元
        "GBP" to 9.20,    // 英镑
        "CAD" to 5.30,
        "AUD" to 4.80,
        "KRW" to 0.0053,
        "SGD" to 5.35,
        "INR" to 0.087,
        "IDR" to 0.00045
    )

    /**
     * 将任意货币换算为目标货币
     * @param amount 金额
     * @param fromCurrency 原始货币代码
     * @param toCurrency 目标货币代码
     * @return 换算后的金额
     */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val fromRate = ratesToCny[fromCurrency.uppercase()] ?: return amount // 如果找不到汇率，返回原金额
        val toRate = ratesToCny[toCurrency.uppercase()] ?: return amount // 如果找不到汇率，返回原金额

        // 1. 先换算成基础货币 CNY
        val amountInCny = amount * fromRate

        // 2. 再从 CNY 换算成目标货币
        return amountInCny / toRate
    }
}
