package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// --- 基础数据结构 ---

// [兼容] 保持原有的 Category 类定义不变，它现在充当“小类”的角色
data class Category(
    val title: String,
    val icon: ImageVector
)

// 为了语义清晰，我们给它一个别名 SubCategory，新代码可以用这个名字
typealias SubCategory = Category

// [新增] 大类数据结构
data class MainCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val subCategories: List<Category> // 列表里存的是 Category
)

// --- 数据定义 ---

// 支出大类定义 (带颜色)
val expenseMainCategories = listOf(
    MainCategory(
        title = "餐饮美食",
        icon = Icons.Default.Restaurant,
        color = Color(0xFFFFB74D), // 暖橙色
        subCategories = listOf(
            Category("食物", Icons.Default.Restaurant),
            Category("零食", Icons.Default.Icecream),
            Category("酒", Icons.Default.LocalBar),
            Category("烟", Icons.Default.SmokingRooms)
        )
    ),
    MainCategory(
        title = "购物消费",
        icon = Icons.Default.ShoppingCart,
        color = Color(0xFFF06292), // 桃红色
        subCategories = listOf(
            Category("购物", Icons.Default.ShoppingCart),
            Category("衣服", Icons.Default.Checkroom),
            Category("电子", Icons.Default.Laptop),
            Category("礼物", Icons.Default.CardGiftcard),
            Category("美容", Icons.Default.Face)
        )
    ),
    MainCategory(
        title = "交通出行",
        icon = Icons.Default.Commute,
        color = Color(0xFF4DB6AC), // 青绿色
        subCategories = listOf(
            Category("交通", Icons.Default.Commute),
            Category("汽车", Icons.Default.DirectionsCar),
            Category("维修", Icons.Default.Build),
            Category("旅行", Icons.Default.Flight)
        )
    ),
    MainCategory(
        title = "居家生活",
        icon = Icons.Default.Home,
        color = Color(0xFF7986CB), // 靛青色
        subCategories = listOf(
            Category("住房", Icons.Default.Home),
            Category("居家", Icons.Default.Deck),
            Category("手机", Icons.Default.PhoneAndroid),
            Category("宠物", Icons.Default.Pets),
            Category("服务", Icons.Default.CleaningServices) // 对应之前的"其他"或新增
        )
    ),
    MainCategory(
        title = "文化娱乐",
        icon = Icons.Default.SportsEsports,
        color = Color(0xFF9575CD), // 紫色
        subCategories = listOf(
            Category("娱乐", Icons.Default.SportsEsports),
            Category("运动", Icons.Default.FitnessCenter),
            Category("社交", Icons.Default.People),
            Category("电影", Icons.Default.Movie),
            Category("演出", Icons.Default.TheaterComedy)
        )
    ),
    MainCategory(
        title = "医疗教育",
        icon = Icons.Default.School,
        color = Color(0xFF4FC3F7), // 浅蓝色
        subCategories = listOf(
            Category("教育", Icons.Default.School),
            Category("医疗", Icons.Default.LocalHospital),
            Category("孩子", Icons.Default.ChildFriendly),
            Category("书籍", Icons.Default.MenuBook)
        )
    ),
    MainCategory(
        title = "金融转账",
        icon = Icons.Default.AttachMoney,
        color = Color(0xFFA1887F), // 棕色
        subCategories = listOf(
            Category("捐款", Icons.Default.Favorite),
            Category("其他", Icons.Default.MoreHoriz)
        )
    )
)

// 收入大类定义
val incomeMainCategories = listOf(
    MainCategory(
        title = "职业收入",
        icon = Icons.Default.Work,
        color = Color(0xFF81C784), // 绿色
        subCategories = listOf(
            Category("薪水", Icons.Default.AccountBalanceWallet),
            Category("奖金", Icons.Default.StackedBarChart),
            Category("报销", Icons.Default.ReceiptLong),
            Category("兼职", Icons.Default.Work)
        )
    ),
    MainCategory(
        title = "其他收入",
        icon = Icons.Default.Savings,
        color = Color(0xFFFFD54F), // 金黄色
        subCategories = listOf(
            Category("理财", Icons.Default.TrendingUp),
            Category("礼金", Icons.Default.Redeem),
            Category("红包", Icons.Default.CardGiftcard),
            Category("二手", Icons.Default.Storefront),
            Category("其他", Icons.Default.MoreHoriz)
        )
    )
)

// --- [关键修复] 兼容层：重新导出扁平化列表 ---
// 这样 DetailsScreen 和其他页面引用的 `expenseCategories` 依然有效
val expenseCategories: List<Category> = expenseMainCategories.flatMap { it.subCategories }
val incomeCategories: List<Category> = incomeMainCategories.flatMap { it.subCategories }

// --- 辅助工具类：用于快速查找颜色 ---
object CategoryHelper {
    // 查找某个小类名称对应的大类颜色
    fun getCategoryColor(categoryName: String, type: Int): Color {
        // type: 0=支出, 1=收入
        val list = if (type == 1) incomeMainCategories else expenseMainCategories
        return list.find { main -> main.subCategories.any { sub -> sub.title == categoryName } }?.color
        // 如果找不到（比如旧数据里的分类），返回默认红/绿
            ?: if(type == 1) Color(0xFF4CAF50) else Color(0xFFE53935)
    }

    // 查找某个小类名称对应的大类对象
    fun getMainCategory(categoryName: String): MainCategory? {
        return (expenseMainCategories + incomeMainCategories).find { main ->
            main.subCategories.any { sub -> sub.title == categoryName }
        }
    }

    // [新增] 查找某个小类名称对应的图标
    fun getIcon(categoryName: String): ImageVector {
        val allSubCategories = (expenseMainCategories + incomeMainCategories).flatMap { it.subCategories }
        return allSubCategories.find { it.title == categoryName }?.icon ?: Icons.Default.HelpOutline
    }
}