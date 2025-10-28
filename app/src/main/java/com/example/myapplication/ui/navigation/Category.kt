package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val title: String,
    val icon: ImageVector
)

// 支出分类列表
val expenseCategories = listOf(
    Category("购物", Icons.Default.ShoppingCart),
    Category("食物", Icons.Default.Restaurant),
    Category("手机", Icons.Default.PhoneAndroid),
    Category("娱乐", Icons.Default.SportsEsports),
    Category("教育", Icons.Default.School),
    Category("美容", Icons.Default.Face),
    Category("运动", Icons.Default.FitnessCenter),
    Category("社交", Icons.Default.People),
    Category("交通", Icons.Default.Commute),
    Category("衣服", Icons.Default.Checkroom),
    Category("汽车", Icons.Default.DirectionsCar),
    Category("酒", Icons.Default.LocalBar),
    Category("烟", Icons.Default.SmokingRooms),
    Category("电子", Icons.Default.Laptop),
    Category("旅行", Icons.Default.Flight),
    Category("医疗", Icons.Default.LocalHospital),
    Category("宠物", Icons.Default.Pets),
    Category("维修", Icons.Default.Build),
    Category("住房", Icons.Default.Home),
    Category("居家", Icons.Default.Deck),
    Category("礼物", Icons.Default.CardGiftcard),
    Category("捐款", Icons.Default.Favorite),
    Category("零食", Icons.Default.Icecream),
    Category("孩子", Icons.Default.ChildFriendly),
    Category("设置", Icons.Default.Settings)
)

// 收入分类列表
val incomeCategories = listOf(
    Category("薪水", Icons.Default.AccountBalanceWallet),
    Category("理财", Icons.Default.TrendingUp),
    Category("兼职", Icons.Default.Work),
    Category("礼金", Icons.Default.Redeem),
    Category("其他", Icons.Default.MoreHoriz),
    Category("设置", Icons.Default.Settings)
)
