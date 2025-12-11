package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet // (修改) Import new icon
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Person
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Details : BottomNavItem(
        route = "details",
        title = "明细",
        icon = Icons.Default.List
    )

    data object Chart : BottomNavItem(
        route = "chart",
        title = "图表",
        icon = Icons.Default.PieChart
    )

    // (修改) Report -> Budget
    data object Budget : BottomNavItem(
        route = "budget", // (修改) route
        title = "预算",    // (修改) title
        icon = Icons.Default.Assessment // Icon can stay the same or change
    )

    // (修改) Profile -> Assets
    data object Assets : BottomNavItem(
        route = "assets", // (修改) route
        title = "资产",    // (修改) title
        icon = Icons.Default.AccountBalanceWallet // (修改) icon
    )

    data object Mine : BottomNavItem(
        route = "mine",
        title = "我的",
        icon = Icons.Default.Person
    )
}