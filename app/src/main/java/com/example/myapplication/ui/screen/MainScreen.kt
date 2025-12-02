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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.util.Calendar


// --- 定义 1: Routes 对象 (移到顶层) ---
object Routes {
    const val ADD_TRANSACTION = "add_transaction"
    const val BUDGET_SETTINGS = "budget_settings/{year}/{month}"
    fun budgetSettingsRoute(year: Int, month: Int) = "budget_settings/$year/$month"

    const val ACCOUNT_MANAGEMENT = "account_management"
    const val ADD_ACCOUNT = "add_account"

    // 搜索页面路由
    const val SEARCH = "search"

    // (新) 日历页面路由
    const val CALENDAR = "calendar"

    // (新) 每日详情页面路由
    const val DAILY_DETAILS = "daily_details/{dateMillis}"
    fun dailyDetailsRoute(dateMillis: Long) = "daily_details/$dateMillis"

    // (新) 设置页面路由
    const val SETTINGS = "settings"
    const val CURRENCY_SELECTION = "currency_selection"
    const val CATEGORY_SETTINGS = "category_settings"
    const val ADD_CATEGORY = "add_category"

    // 交易详情页面路由
    const val TRANSACTION_DETAIL = "transaction_detail/{expenseId}"
    fun transactionDetailRoute(expenseId: Long) = "transaction_detail/$expenseId"
}

// --- 定义 2: MainScreen 可组合函数 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(expenseViewModel: ExpenseViewModel) {
    val navController = rememberNavController()

    val calendar = Calendar.getInstance()
    var budgetScreenYear by rememberSaveable { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var budgetScreenMonth by rememberSaveable { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var defaultCurrency by rememberSaveable { mutableStateOf("CNY") } // 初始默认货币

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            AppBottomBar(navController = navController) {
                budgetScreenYear = calendar.get(Calendar.YEAR)
                budgetScreenMonth = calendar.get(Calendar.MONTH) + 1
            }
        },
        floatingActionButton = {
            if (currentRoute == BottomNavItem.Details.route) {
                FloatingActionButton(onClick = { navController.navigate(Routes.ADD_TRANSACTION) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        NavigationGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            expenseViewModel = expenseViewModel,
            budgetScreenYear = budgetScreenYear,
            budgetScreenMonth = budgetScreenMonth,
            onBudgetScreenDateChange = { year, month ->
                budgetScreenYear = year
                budgetScreenMonth = month
            },
            defaultCurrency = defaultCurrency,
            onDefaultCurrencyChange = { currency ->
                defaultCurrency = currency
            }
        )
    }
}

// --- 定义 3: AppBottomBar 可组合函数 ---
@Composable
fun AppBottomBar(navController: NavHostController, onBudgetTabClick: () -> Unit) {
    // 使用来自 BottomNavItem.kt 的更新后的项目
    val items = listOf(
        BottomNavItem.Details,
        BottomNavItem.Chart,
        BottomNavItem.Budget, // 使用 "预算"
        BottomNavItem.Assets  // 使用 "资产"
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomAppBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (item.route == BottomNavItem.Budget.route) {
                        onBudgetTabClick()
                    }
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
    expenseViewModel: ExpenseViewModel,
    budgetScreenYear: Int,
    budgetScreenMonth: Int,
    onBudgetScreenDateChange: (Int, Int) -> Unit,
    defaultCurrency: String,
    onDefaultCurrencyChange: (String) -> Unit
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
            BudgetScreen(
                viewModel = expenseViewModel,
                navController = navController,
                year = budgetScreenYear,
                month = budgetScreenMonth,
                onDateChange = onBudgetScreenDateChange,
                defaultCurrency = defaultCurrency
            )
        }
        composable(BottomNavItem.Assets.route) { // 使用 "assets" 路由
            // 指向新的 AssetsScreen
            AssetsScreen(viewModel = expenseViewModel, navController = navController, defaultCurrency = defaultCurrency)
        }
        composable(
            route = "${Routes.ADD_TRANSACTION}?expenseId={expenseId}&dateMillis={dateMillis}",
            arguments = listOf(
                navArgument("expenseId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("dateMillis") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId")
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis")
            AddTransactionScreen(
                navController = navController,
                viewModel = expenseViewModel,
                expenseId = if (expenseId == -1L) null else expenseId,
                dateMillis = if (dateMillis == -1L) null else dateMillis
            )
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

        // (新) 添加日历屏幕的路由
        composable(Routes.CALENDAR) {
            CalendarScreen(viewModel = expenseViewModel, navController = navController, defaultCurrency = defaultCurrency)
        }

        // (新) 添加每日详情页面的路由
        composable(
            route = Routes.DAILY_DETAILS,
            arguments = listOf(navArgument("dateMillis") { type = NavType.LongType })
        ) { backStackEntry ->
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis")
            if (dateMillis != null) {
                DailyDetailsScreen(
                    viewModel = expenseViewModel,
                    navController = navController,
                    dateMillis = dateMillis
                )
            }
        }

        // (新) 添加设置和货币选择页面的路由
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController, defaultCurrency = defaultCurrency)
        }
        composable(Routes.CURRENCY_SELECTION) {
            CurrencySelectionScreen(navController = navController, onCurrencySelected = onDefaultCurrencyChange)
        }
        composable(Routes.CATEGORY_SETTINGS) {
            CategorySettingsScreen(navController = navController, viewModel = expenseViewModel)
        }
        composable(Routes.ADD_CATEGORY) {
            AddCategoryScreen(navController = navController, viewModel = expenseViewModel)
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
        // 添加搜索页面的目标
        composable(Routes.SEARCH) {
            SearchScreen(viewModel = expenseViewModel, navController = navController)
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