package com.swiftiecx.timeledger.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.swiftiecx.timeledger.R // 【注意】请确认这里自动导入的是你的包名

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int, // [修改] 改为 Int 类型，用于接收 R.string.xxx
    val icon: ImageVector
) {
    data object Details : BottomNavItem(
        route = "details",
        titleResId = R.string.nav_details, // [修改] 使用资源ID
        icon = Icons.Default.List
    )

    data object Chart : BottomNavItem(
        route = "chart",
        titleResId = R.string.nav_chart, // [修改] 使用资源ID
        icon = Icons.Default.PieChart
    )

    data object Budget : BottomNavItem(
        route = "budget",
        titleResId = R.string.nav_budget, // [修改] 使用资源ID
        icon = Icons.Default.Assessment
    )

    data object Assets : BottomNavItem(
        route = "assets",
        titleResId = R.string.nav_assets, // [修改] 使用资源ID
        icon = Icons.Default.AccountBalanceWallet
    )

    data object Mine : BottomNavItem(
        route = "mine",
        titleResId = R.string.nav_me, // [修改] 使用资源ID
        icon = Icons.Default.Person
    )
}