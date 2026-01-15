package com.swiftiecx.timeledger.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.swiftiecx.timeledger.R

object IconMapper {
    fun getIcon(iconName: String?): ImageVector {
        return when (iconName) {
            // --- 核心账户/资产图标 ---
            "Wallet" -> Icons.Default.AccountBalanceWallet
            "Bank" -> Icons.Default.AccountBalance
            "CreditCard" -> Icons.Default.CreditCard
            "Savings" -> Icons.Default.Savings
            "Payment" -> Icons.Default.Payment
            "AttachMoney" -> Icons.Default.AttachMoney
            "Calculate" -> Icons.Default.Calculate
            "TrendingUp" -> Icons.AutoMirrored.Filled.TrendingUp
            "Smartphone" -> Icons.Default.Smartphone
            "CurrencyExchange" -> Icons.Default.CurrencyExchange
            "Euro" -> Icons.Default.Euro
            "ShowChart" -> Icons.Default.ShowChart
            "PieChart" -> Icons.Default.PieChart

            // --- 新增：生活消费类图标 (解决图标重复问题) ---
            "ShoppingCart" -> Icons.Default.ShoppingCart      // 购物
            "Home" -> Icons.Default.Home                      // 住房/家庭
            "DirectionsCar" -> Icons.Default.DirectionsCar    // 汽车/交通
            "Flight" -> Icons.Default.Flight                  // 旅行
            "Restaurant" -> Icons.Default.Restaurant          // 餐饮
            "MedicalServices" -> Icons.Default.MedicalServices // 医疗
            "School" -> Icons.Default.School                  // 教育
            "Pets" -> Icons.Default.Pets                      // 宠物
            "Redeem" -> Icons.Default.Redeem                  // 礼物/人情
            "MoreHoriz" -> Icons.Default.MoreHoriz            // 其他/杂项

            // --- 其他常用分类图标补充 ---
            "Fastfood" -> Icons.Default.Fastfood
            "DirectionsBus" -> Icons.Default.DirectionsBus
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "Movie" -> Icons.Default.Movie
            "LocalHospital" -> Icons.Default.LocalHospital
            "LocalLibrary" -> Icons.Default.LocalLibrary
            "Work" -> Icons.Default.Work
            "FamilyRestroom" -> Icons.Default.FamilyRestroom
            "DirectionsRun" -> Icons.AutoMirrored.Filled.DirectionsRun
            "MoreVert" -> Icons.Default.MoreVert

            // --- 借贷相关图标映射 ---
            // 兼容中文 "借出" 和英文 "Lend"
            "Lend", "借出" -> Icons.Default.ArrowCircleUp // 或者 Icons.Default.Upload
            // 兼容中文 "借入" 和英文 "Borrow"
            "Borrow", "借入" -> Icons.Default.ArrowCircleDown // 或者 Icons.Default.Download

            // --- 默认兜底 ---
            else -> Icons.Default.AccountBalanceWallet
        }
    }

    // 反向映射：用于将图标对象转回字符串名称 (主要用于调试或存储)
    fun getIconName(icon: ImageVector): String {
        return when (icon) {
            Icons.Default.AccountBalanceWallet -> "Wallet"
            Icons.Default.AccountBalance -> "Bank"
            Icons.Default.CreditCard -> "CreditCard"
            Icons.Default.Savings -> "Savings"
            Icons.Default.Payment -> "Payment"
            Icons.Default.AttachMoney -> "AttachMoney"
            Icons.Default.Calculate -> "Calculate"
            Icons.AutoMirrored.Filled.TrendingUp -> "TrendingUp"
            Icons.Default.Smartphone -> "Smartphone"

            // 新增生活类反向映射
            Icons.Default.ShoppingCart -> "ShoppingCart"
            Icons.Default.Home -> "Home"
            Icons.Default.DirectionsCar -> "DirectionsCar"
            Icons.Default.Flight -> "Flight"
            Icons.Default.Restaurant -> "Restaurant"
            Icons.Default.MedicalServices -> "MedicalServices"
            Icons.Default.School -> "School"
            Icons.Default.Pets -> "Pets"
            Icons.Default.Redeem -> "Redeem"
            Icons.Default.MoreHoriz -> "MoreHoriz"

            // 借贷
            Icons.Default.ArrowCircleUp -> "Lend"
            Icons.Default.ArrowCircleDown -> "Borrow"

            else -> "Wallet"
        }
    }
}

// [保持原样] 账户类型管理器：统一管理 Key 到多语言的映射
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