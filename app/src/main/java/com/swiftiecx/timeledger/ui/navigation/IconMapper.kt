package com.swiftiecx.timeledger.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.swiftiecx.timeledger.R

object IconMapper {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "Wallet" -> Icons.Default.AccountBalanceWallet
            "Bank" -> Icons.Default.AccountBalance
            "CreditCard" -> Icons.Default.CreditCard
            "TrendingUp" -> Icons.Default.TrendingUp
            "Smartphone" -> Icons.Default.Smartphone
            "AttachMoney" -> Icons.Default.AttachMoney
            "Savings" -> Icons.Default.Savings
            "Payment" -> Icons.Default.Payment
            "CurrencyExchange" -> Icons.Default.CurrencyExchange
            "Euro" -> Icons.Default.Euro
            "ShowChart" -> Icons.Default.ShowChart
            "PieChart" -> Icons.Default.PieChart
            else -> Icons.Default.AccountBalanceWallet // Default icon
        }
    }

    fun getIconName(icon: ImageVector): String {
        return when (icon) {
            Icons.Default.AccountBalanceWallet -> "Wallet"
            Icons.Default.AccountBalance -> "Bank"
            Icons.Default.CreditCard -> "CreditCard"
            Icons.Default.TrendingUp -> "TrendingUp"
            Icons.Default.Smartphone -> "Smartphone"
            Icons.Default.AttachMoney -> "AttachMoney"
            Icons.Default.Savings -> "Savings"
            Icons.Default.Payment -> "Payment"
            Icons.Default.CurrencyExchange -> "CurrencyExchange"
            Icons.Default.Euro -> "Euro"
            Icons.Default.ShowChart -> "ShowChart"
            Icons.Default.PieChart -> "PieChart"
            else -> "Wallet"
        }
    }
}

// [新增] 账户类型管理器：统一管理 Key 到多语言的映射
object AccountTypeManager {

    // 定义 Key 到 资源ID 的映射
    private val typeMap = mapOf(
        "account_cash" to R.string.account_cash,
        "account_card" to R.string.account_card,
        "account_credit" to R.string.account_credit,
        "account_investment" to R.string.account_investment,
        "account_ewallet" to R.string.account_ewallet,
        "account_default" to R.string.account_default
    )

    // 定义旧数据（中文/英文）到 Key 的兼容映射
    private val legacyToKeyMap = mapOf(
        "现金" to "account_cash", "Cash" to "account_cash",
        "银行卡" to "account_card", "Bank card" to "account_card", "Bank Card" to "account_card",
        "信用卡" to "account_credit", "Credit card" to "account_credit", "Credit Card" to "account_credit",
        "投资" to "account_investment", "Investment" to "account_investment",
        "电子钱包" to "account_ewallet", "E-wallet" to "account_ewallet",
        "默认" to "account_default", "Default" to "account_default"
    )

    // 获取所有可用类型 (Key, ResId)
    fun getAllTypes(): List<Pair<String, Int>> {
        return typeMap.entries.map { it.key to it.value }
    }

    // 根据 Key 获取资源 ID，如果 Key 不存在则尝试兼容旧数据
    fun getTypeResId(keyOrName: String): Int? {
        // 1. 直接匹配 Key
        if (typeMap.containsKey(keyOrName)) {
            return typeMap[keyOrName]
        }
        // 2. 尝试匹配旧数据
        val mappedKey = legacyToKeyMap[keyOrName]
        if (mappedKey != null) {
            return typeMap[mappedKey]
        }
        return null
    }

    // [Helper] 在 Composable 中直接获取显示名称
    @Composable
    fun getDisplayName(keyOrName: String): String {
        val resId = getTypeResId(keyOrName)
        return if (resId != null) stringResource(resId) else keyOrName
    }

    // 获取标准的 Key (用于保存)
    fun getStableKey(keyOrName: String): String {
        return if (typeMap.containsKey(keyOrName)) keyOrName
        else legacyToKeyMap[keyOrName] ?: keyOrName
    }
}