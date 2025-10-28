package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem // 确保导入
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.navigation.NavHostController
import androidx.navigation.NavType // 确保导入
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument // 确保导入
import com.example.myapplication.ui.navigation.BottomNavItem
import com.example.myapplication.ui.viewmodel.ExpenseViewModel


// --- 定义 1: Routes 对象 (移到顶层) ---
object Routes {
    const val ADD_TRANSACTION = "add_transaction"
    const val BUDGET_SETTINGS = "budget_settings/{year}/{month}"
    fun budgetSettingsRoute(year: Int, month: Int) = "budget_settings/$year/$month"

    const val ACCOUNT_MANAGEMENT = "account_management"
    const val ADD_ACCOUNT = "add_account"

    // 搜索页面路由
    const val SEARCH = "search"

    // 交易详情页面路由
    const val TRANSACTION_DETAIL = "transaction_detail/{expenseId}"
    fun transactionDetailRoute(expenseId: Long) = "transaction_detail/$expenseId"
}

// --- 定义 2: MainScreen 可组合函数 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(expenseViewModel: ExpenseViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.ADD_TRANSACTION) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        NavigationGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            expenseViewModel = expenseViewModel
        )
    }
}

// --- 定义 3: AppBottomBar 可组合函数 ---
@Composable
fun AppBottomBar(navController: NavHostController) {
    // 使用来自 BottomNavItem.kt 的更新后的项目
    val items = listOf(
        BottomNavItem.Details,
        BottomNavItem.Chart,
        BottomNavItem.Budget, // 使用 "预算"
        BottomNavItem.Assets  // 使用 "资产"
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomAppBar() {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// --- 定义 4: NavigationGraph 可组合函数 ---
@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseViewModel
) {
    NavHost(navController, startDestination = BottomNavItem.Details.route, modifier = modifier) {
        composable(BottomNavItem.Details.route) {
            // 传递 NavController
            DetailsScreen(viewModel = expenseViewModel, navController = navController)
        }
        composable(BottomNavItem.Chart.route) {
            ChartScreen(viewModel = expenseViewModel)
        }
        composable(BottomNavItem.Budget.route) { // 使用 "budget" 路由
            // 仍然指向 ReportScreen 的内容，可以稍后重命名 ReportScreen.kt
            // 或者如果你已经有了 BudgetScreen.kt，则指向 BudgetScreen
            BudgetScreen(viewModel = expenseViewModel, navController = navController)
            // ReportScreen(viewModel = expenseViewModel, navController = navController) // 如果预算页内容在 ReportScreen
        }
        composable(BottomNavItem.Assets.route) { // 使用 "assets" 路由
            // 指向新的 AssetsScreen
            AssetsScreen(viewModel = expenseViewModel, navController = navController)
        }
        composable(Routes.ADD_TRANSACTION) {
            AddTransactionScreen(navController = navController, viewModel = expenseViewModel)
        }
        composable(
            route = Routes.BUDGET_SETTINGS,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: 0
            val month = backStackEntry.arguments?.getInt("month") ?: 0
            BudgetSettingsScreen(
                viewModel = expenseViewModel,
                navController = navController,
                year = year,
                month = month
            )
        }

        composable(Routes.ACCOUNT_MANAGEMENT) {
            AccountManagementScreen(viewModel = expenseViewModel, navController = navController)
        }

        composable(Routes.ADD_ACCOUNT) {
            AddAccountScreen(viewModel = expenseViewModel, navController = navController)
        }



        // 添加交易详情页面的目标
        composable(
            route = Routes.TRANSACTION_DETAIL,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId")
            TransactionDetailScreen(
                viewModel = expenseViewModel,
                navController = navController,
                expenseId = expenseId
            )
        }
    }
}

// --- 定义 5: PlaceholderScreen 可组合函数 ---
@Composable
fun PlaceholderScreen(screenName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$screenName Screen")
    }
}

// --- 文件结束 ---