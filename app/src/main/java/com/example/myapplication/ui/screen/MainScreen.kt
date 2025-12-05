package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.myapplication.ui.viewmodel.ThemeViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    expenseViewModel: ExpenseViewModel,
    themeViewModel: ThemeViewModel
) {
    val navController = rememberNavController()

    val calendar = Calendar.getInstance()
    var budgetScreenYear by rememberSaveable { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var budgetScreenMonth by rememberSaveable { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var defaultCurrency by rememberSaveable { mutableStateOf("CNY") }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // (修改) 在此处添加 Routes.SEARCH 到隐藏列表
    // 当当前路由是 LOCK, ADD_TRANSACTION 或 SEARCH 时，不显示主 Scaffold (即不显示底部导航栏)
    val showScaffold = currentRoute != Routes.LOCK &&
            currentRoute?.startsWith(Routes.ADD_TRANSACTION) != true &&
            currentRoute != Routes.SEARCH

    if (showScaffold) {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars),
            bottomBar = {
                AppBottomBar(navController = navController) {
                    budgetScreenYear = calendar.get(Calendar.YEAR)
                    budgetScreenMonth = calendar.get(Calendar.MONTH) + 1
                }
            },
            floatingActionButton = {
                if (currentRoute == BottomNavItem.Details.route) {
                    FloatingActionButton(
                        onClick = { navController.navigate(Routes.ADD_TRANSACTION) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
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
                themeViewModel = themeViewModel,
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
    } else {
        NavigationGraph(
            navController = navController,
            modifier = Modifier,
            expenseViewModel = expenseViewModel,
            themeViewModel = themeViewModel,
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
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = {
                    if (item.route == BottomNavItem.Budget.route) {
                        onBudgetTabClick()
                    }
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = (item.route != BottomNavItem.Details.route)
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseViewModel,
    themeViewModel: ThemeViewModel,
    budgetScreenYear: Int,
    budgetScreenMonth: Int,
    onBudgetScreenDateChange: (Int, Int) -> Unit,
    defaultCurrency: String,
    onDefaultCurrencyChange: (String) -> Unit
) {
    val privacyType = expenseViewModel.getPrivacyType()
    val startDestination = if (privacyType != "NONE") Routes.LOCK else BottomNavItem.Details.route

    NavHost(navController, startDestination = startDestination, modifier = modifier) {

        composable(Routes.LOCK) {
            LockScreen(
                viewModel = expenseViewModel,
                onUnlockSuccess = {
                    navController.navigate(BottomNavItem.Details.route) {
                        popUpTo(Routes.LOCK) { inclusive = true }
                    }
                }
            )
        }

        composable(BottomNavItem.Details.route) {
            DetailsScreen(viewModel = expenseViewModel, navController = navController, defaultCurrency = defaultCurrency)
        }

        composable(BottomNavItem.Chart.route) {
            ChartScreen(viewModel = expenseViewModel, navController = navController)
        }

        composable(BottomNavItem.Budget.route) {
            BudgetScreen(viewModel = expenseViewModel, navController = navController, year = budgetScreenYear, month = budgetScreenMonth, onDateChange = onBudgetScreenDateChange, defaultCurrency = defaultCurrency)
        }

        composable(BottomNavItem.Assets.route) {
            AssetsScreen(viewModel = expenseViewModel, navController = navController, defaultCurrency = defaultCurrency)
        }

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
            SettingsScreen(
                navController = navController,
                defaultCurrency = defaultCurrency,
                viewModel = expenseViewModel
            )
        }

        composable(Routes.PRIVACY_SETTINGS) {
            PrivacySettingsScreen(
                navController = navController,
                viewModel = expenseViewModel
            )
        }

        composable(Routes.THEME_SETTINGS) {
            ThemeSettingsScreen(
                navController = navController,
                themeViewModel = themeViewModel
            )
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

        composable(
            route = Routes.SEARCH,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType; defaultValue = "" },
                navArgument("startDate") { type = NavType.LongType; defaultValue = -1L },
                navArgument("endDate") { type = NavType.LongType; defaultValue = -1L },
                navArgument("type") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category").takeIf { !it.isNullOrBlank() }
            val startDate = backStackEntry.arguments?.getLong("startDate")?.takeIf { it != -1L }
            val endDate = backStackEntry.arguments?.getLong("endDate")?.takeIf { it != -1L }
            val type = backStackEntry.arguments?.getInt("type") ?: 0

            SearchScreen(
                viewModel = expenseViewModel,
                navController = navController,
                initialCategory = category,
                initialStartDate = startDate,
                initialEndDate = endDate,
                initialType = type
            )
        }

        // --- 用户信息相关路由 ---
        composable(Routes.USER_INFO) {
            UserInfoScreen(navController = navController, viewModel = expenseViewModel)
        }
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController, viewModel = expenseViewModel)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController, viewModel = expenseViewModel)
        }
        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(navController = navController, viewModel = expenseViewModel)
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController = navController, viewModel = expenseViewModel)
        }
    }
}

@Composable
fun PlaceholderScreen(screenName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$screenName Screen")
    }
}