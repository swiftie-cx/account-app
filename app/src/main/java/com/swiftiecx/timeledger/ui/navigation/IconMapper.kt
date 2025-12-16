package com.swiftiecx.timeledger.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {

    // 1. 注册所有在 Category.kt 和 AddAccountScreen 中用到的图标
    private val iconMap: Map<String, ImageVector> = mapOf(
        // --- 账户图标 ---
        "AccountBalance" to Icons.Default.AccountBalance,
        "CreditCard" to Icons.Default.CreditCard,
        "AttachMoney" to Icons.Default.AttachMoney,
        "ShowChart" to Icons.Default.ShowChart,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,

        // --- 支出图标 (对应 Category.kt) ---
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "Restaurant" to Icons.Default.Restaurant,
        "PhoneAndroid" to Icons.Default.PhoneAndroid,
        "SportsEsports" to Icons.Default.SportsEsports,
        "School" to Icons.Default.School,
        "Face" to Icons.Default.Face,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "People" to Icons.Default.People,
        "Commute" to Icons.Default.Commute,
        "Checkroom" to Icons.Default.Checkroom,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "LocalBar" to Icons.Default.LocalBar,
        "SmokingRooms" to Icons.Default.SmokingRooms,
        "Laptop" to Icons.Default.Laptop,
        "Flight" to Icons.Default.Flight,
        "LocalHospital" to Icons.Default.LocalHospital,
        "Pets" to Icons.Default.Pets,
        "Build" to Icons.Default.Build,
        "Home" to Icons.Default.Home,
        "Deck" to Icons.Default.Deck,
        "CardGiftcard" to Icons.Default.CardGiftcard,
        "Favorite" to Icons.Default.Favorite,
        "Icecream" to Icons.Default.Icecream,
        "ChildFriendly" to Icons.Default.ChildFriendly,

        // --- 收入图标 (对应 Category.kt) ---
        "StackedBarChart" to Icons.Default.StackedBarChart,
        "TrendingUp" to Icons.Default.TrendingUp,
        "Work" to Icons.Default.Work,
        "Redeem" to Icons.Default.Redeem,
        "ReceiptLong" to Icons.Default.ReceiptLong,
        "Storefront" to Icons.Default.Storefront,
        "MoreHoriz" to Icons.Default.MoreHoriz
    )

    // 反向映射：通过图标对象查找名称 (用于保存)
    private val reverseIconMap: Map<ImageVector, String> = iconMap.entries.associate { (k, v) -> v to k }

    // 获取所有图标列表 (用于选择器)
    val allIcons: List<Pair<String, ImageVector>>
        get() = iconMap.toList()

    // 通过名称获取图标 (读取时)
    fun getIcon(name: String): ImageVector {
        return iconMap[name] ?: Icons.Default.HelpOutline // 找不到则返回问号
    }

    // 通过图标获取名称 (保存时)
    fun getIconName(icon: ImageVector): String {
        return reverseIconMap[icon] ?: "HelpOutline"
    }
}