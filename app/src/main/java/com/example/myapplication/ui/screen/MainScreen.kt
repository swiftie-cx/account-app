package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.navigation.BottomNavItem
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(expenseViewModel: ExpenseViewModel) {
    val navController = rememberNavController()

    val calendar = Calendar.getInstance()
    var budgetScreenYear by rememberSaveable { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var budgetScreenMonth by rememberSaveable { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var defaultCurrency by rememberSaveable { mutableStateOf("CNY") }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        // (核心修改)
        // 告诉 MainScreen 的 Scaffold：不要处理顶部的状态栏(statusBars)高度。
        // 只处理底部的导航栏(navigationBars)高度。
        // 这样，子页面(NavigationGraph)就会从屏幕最顶端开始绘制，从而让色块覆盖摄像头区域。
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars),

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

@Composable
fun AppBottomBar(navController: NavHostController, onBudgetTabClick: () -> Unit) {
    val items = listOf(
        BottomNavItem.Details,
        BottomNavItem.Chart,
        BottomNavItem.Budget,
        BottomNavItem.Assets
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
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        // 明细页不恢复状态，其他页恢复
                        restoreState = (item.route != BottomNavItem.Details.route)
                    }
                }
            )
        }
    }
}

// --- NavigationGraph 保持不变 ---
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
            DetailsScreen(viewModel = expenseViewModel, navController = navController, defaultCurrency = defaultCurrency)
        }

        composable(BottomNavItem.Chart.route) {
            ChartScreen(viewModel = expenseViewModel)
        }

        composable(BottomNavItem.Budget.route) {
            BudgetScreen(viewModel = expenseViewModel, navController = navController, year = budgetScreenYear, month = budgetScreenMonth, onDateChange = onBudgetScreenDateChange, defaultCurrency = defaultCurrency)
        }

        composable(BottomNavItem.Assets.route) {
            AssetsScreen(viewModel = expenseViewModel, navController = navController, defaultCurrency = defaultCurrency)
        }

        // --- 功能页面 ---
        composable(
            route = "${Routes.ADD_TRANSACTION}?expenseId={expenseId}&dateMillis={dateMillis}",
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("dateMillis") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId")
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis")
            AddTransactionScreen(navController = navController, viewModel = expenseViewModel, expenseId = if (expenseId == -1L) null else expenseId, dateMillis = if (dateMillis == -1L) null else dateMillis)
        }

        composable(
            route = Routes.BUDGET_SETTINGS,
            arguments = listOf(navArgument("year") { type = NavType.IntType }, navArgument("month") { type = NavType.IntType })
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: 0
            val month = backStackEntry.arguments?.getInt("month") ?: 0
            BudgetSettingsScreen(viewModel = expenseViewModel, navController = navController, year = year, month = month)
        }

        composable(Routes.ACCOUNT_MANAGEMENT) {
            AccountManagementScreen(viewModel = expenseViewModel, navController = navController)
        }

        composable(
            route = Routes.ADD_ACCOUNT,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId")
            AddAccountScreen(viewModel = expenseViewModel, navController = navController, accountId = if (accountId == -1L) null else accountId)
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(viewModel = expenseViewModel, navController = navController, defaultCurrency = defaultCurrency)
        }

        composable(
            route = Routes.DAILY_DETAILS,
            arguments = listOf(navArgument("dateMillis") { type = NavType.LongType })
        ) { backStackEntry ->
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis")
            if (dateMillis != null) {
                DailyDetailsScreen(viewModel = expenseViewModel, navController = navController, dateMillis = dateMillis)
            }
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController,
                defaultCurrency = defaultCurrency,
                viewModel = expenseViewModel)
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

        composable(
            route = Routes.TRANSACTION_DETAIL,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId")
            TransactionDetailScreen(viewModel = expenseViewModel, navController = navController, expenseId = expenseId, defaultCurrency = defaultCurrency)
        }

        composable(Routes.SEARCH) {
            SearchScreen(viewModel = expenseViewModel, navController = navController)
        }
    }
}

@Composable
fun PlaceholderScreen(screenName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$screenName Screen")
    }
}