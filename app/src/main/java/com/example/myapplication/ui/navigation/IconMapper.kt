package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline // 默认图标
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 一个用于在数据库存储的 iconName (String) 和 UI 显示的 (ImageVector) 之间映射的工具
 */
object IconMapper {

    // 1. 定义所有可用的账户图标
    private val iconMap: Map<String, ImageVector> = mapOf(
        "AccountBalance" to Icons.Default.AccountBalance,
        "CreditCard" to Icons.Default.CreditCard,
        "AttachMoney" to Icons.Default.AttachMoney,
        "ShowChart" to Icons.Default.ShowChart,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet
        // 你可以从 AddAccountScreen 的 icons 列表中添加更多
    )

    // 2. 一个列表，用于 AddAccountScreen 的图标选择器
    // 我们返回 List<Pair<String, ImageVector>> 以便同时获取名称和图标
    val allIcons: List<Pair<String, ImageVector>>
        get() = iconMap.toList()

    // 3. 一个函数，用于根据名称获取图标（用于所有其他屏幕）
    fun getIcon(name: String): ImageVector {
        // 如果找不到，返回一个默认的“问号”图标
        return iconMap[name] ?: Icons.Default.HelpOutline
    }
}