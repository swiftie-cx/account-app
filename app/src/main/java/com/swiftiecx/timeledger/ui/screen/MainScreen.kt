package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swiftiecx.timeledger.ui.navigation.BottomNavItem
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.ui.viewmodel.ThemeViewModel
import java.util.Calendar
import com.swiftiecx.timeledger.ui.screen.chart.ChartScreen
import com.swiftiecx.timeledger.ui.screen.chart.CategoryChartDetailScreen
import com.swiftiecx.timeledger.ui.screen.chart.FullScreenChartScreen

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

    val defaultCurrency by expenseViewModel.defaultCurrency.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showScaffold = currentRoute != Routes.LOCK &&
            currentRoute != Routes.WELCOME &&
            currentRoute != Routes.SYNC &&
            currentRoute != Routes.LANGUAGE_SETTINGS &&
            currentRoute?.startsWith(Routes.ADD_TRANSACTION) != true &&
            currentRoute?.startsWith("add_periodic_transaction") != true &&
            currentRoute != Routes.SEARCH &&
            currentRoute?.startsWith("category_chart_detail") != true &&
            currentRoute?.startsWith("fullscreen_chart") != true

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
                    expenseViewModel.setDefaultCurrency(currency)
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
                expenseViewModel.setDefaultCurrency(currency)
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
        BottomNavItem.Assets,
        BottomNavItem.Mine
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomAppBar(
        containerColor = androidx.compose.ui.graphics.Color.White,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val title = stringResource(item.titleResId)
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = title) },
                label = { Text(title) },
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
                        restoreState = true
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
    val isFirstLaunch = expenseViewModel.isFirstLaunch

    val startDestination = when {
        isFirstLaunch -> Routes.WELCOME
        privacyType != "NONE" -> Routes.LOCK
        else -> BottomNavItem.Details.route
    }

    NavHost(navController, startDestination = startDestination, modifier = modifier) {

        composable(Routes.WELCOME) {
            WelcomeScreen(navController = navController, viewModel = expenseViewModel)
        }

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

        composable(
            route = Routes.CATEGORY_CHART_DETAIL,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("type") { type = NavType.IntType },
                navArgument("start") { type = NavType.LongType },
                navArgument("end") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val type = backStackEntry.arguments?.getInt("type") ?: 0
            val start = backStackEntry.arguments?.getLong("start") ?: 0L
            val end = backStackEntry.arguments?.getLong("end") ?: 0L

            CategoryChartDetailScreen(
                navController = navController,
                viewModel = expenseViewModel,
                categoryName = category,
                transactionType = type,
                startDate = start,
                endDate = end
            )
        }

        composable(
            route = "fullscreen_chart/{type}/{start}/{end}",
            arguments = listOf(
                navArgument("type") { type = NavType.IntType },
                navArgument("start") { type = NavType.LongType },
                navArgument("end") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getInt("type") ?: 0
            val start = backStackEntry.arguments?.getLong("start") ?: 0L
            val end = backStackEntry.arguments?.getLong("end") ?: 0L

            FullScreenChartScreen(
                navController = navController,
                viewModel = expenseViewModel,
                startDate = start,
                endDate = end,
                transactionType = type
            )
        }

        composable(BottomNavItem.Budget.route) {
            BudgetScreen(
                viewModel = expenseViewModel,
                navController = navController,
                year = budgetScreenYear,
                month = budgetScreenMonth,
                onDateChange = onBudgetScreenDateChange
            )
        }

        composable(BottomNavItem.Assets.route) {
            AssetsScreen(viewModel = expenseViewModel, navController = navController, defaultCurrency = defaultCurrency)
        }

        composable(BottomNavItem.Mine.route) {
            SettingsScreen(
                navController = navController,
                viewModel = expenseViewModel,
                themeViewModel = themeViewModel
            )
        }

        composable(
            route = "${Routes.ADD_TRANSACTION}?expenseId={expenseId}&dateMillis={dateMillis}&type={type}",
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("dateMillis") { type = NavType.LongType; defaultValue = -1L },
                navArgument("type") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId")
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis")
            val type = backStackEntry.arguments?.getInt("type") ?: 0

            AddTransactionScreen(
                navController = navController,
                viewModel = expenseViewModel,
                expenseId = if (expenseId == -1L) null else expenseId,
                dateMillis = if (dateMillis == -1L) null else dateMillis,
                initialTab = type
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
            BudgetSettingsScreen(viewModel = expenseViewModel, navController = navController, year = year, month = month)
        }

        composable(Routes.ACCOUNT_MANAGEMENT) {
            AccountManagementScreen(viewModel = expenseViewModel, navController = navController)
        }

        composable(
            route = "add_account?accountId={accountId}&category={category}&debtType={debtType}",
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("category") { type = NavType.StringType; defaultValue = "FUNDS" },
                navArgument("debtType") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            val category = backStackEntry.arguments?.getString("category") ?: "FUNDS"
            val debtType = backStackEntry.arguments
                ?.getString("debtType")
                ?.takeIf { it.isNotBlank() }

            AddAccountScreen(
                viewModel = expenseViewModel,
                navController = navController,
                accountId = if (accountId == -1L) null else accountId,
                presetCategory = category,
                presetDebtType = debtType
            )
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
                viewModel = expenseViewModel,
                themeViewModel = themeViewModel
            )
        }

        composable(Routes.PRIVACY_SETTINGS) {
            PrivacySettingsScreen(navController = navController, viewModel = expenseViewModel)
        }

        composable(Routes.THEME_SETTINGS) {
            ThemeSettingsScreen(navController = navController, themeViewModel = themeViewModel)
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
            TransactionDetailScreen(
                viewModel = expenseViewModel,
                navController = navController,
                expenseId = expenseId,
                defaultCurrency = defaultCurrency
            )
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

        composable(Routes.PERIODIC_BOOKKEEPING) {
            PeriodicBookkeepingScreen(navController = navController, viewModel = expenseViewModel)
        }

        composable(
            route = "add_periodic_transaction?id={id}&type={type}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("type") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")
            val type = backStackEntry.arguments?.getInt("type") ?: 0

            AddPeriodicTransactionScreen(
                navController = navController,
                viewModel = expenseViewModel,
                periodicId = if (id == -1L) null else id,
                initialType = type
            )
        }

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

        composable(Routes.SYNC) {
            SyncScreen(navController = navController, viewModel = expenseViewModel)
        }

        composable(Routes.LANGUAGE_SETTINGS) {
            LanguageSettingsScreen(
                navController = navController,
                themeViewModel = themeViewModel,
                expenseViewModel = expenseViewModel
            )
        }

        composable(
            route = Routes.ACCOUNT_DETAIL,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            if (accountId != -1L) {
                AccountDetailScreen(
                    viewModel = expenseViewModel,
                    navController = navController,
                    accountId = accountId,
                    defaultCurrency = defaultCurrency
                )
            }
        }

        // === 债务管理核心路由注册 ===

        // 1. 债务总览管理页
        composable(Routes.DEBT_MANAGEMENT) {
            DebtManagementScreen(expenseViewModel, navController)
        }

        // 2. 个人债务详情页
        composable(
            route = Routes.DEBT_PERSON_DETAIL,
            arguments = listOf(navArgument("personName") { type = NavType.StringType })
        ) { backStackEntry ->
            val personName = backStackEntry.arguments?.getString("personName") ?: ""
            DebtPersonDetailScreen(expenseViewModel, navController, personName)
        }

        // 3. 收款/还款结算页 (核心修复：使用纯字符串定义，避免参数重复)
        composable(
            route = "settle_debt/{personName}/{isBorrow}/{maxAmount}", // ✅ 这里改成了固定字符串
            arguments = listOf(
                navArgument("personName") { type = NavType.StringType },
                navArgument("isBorrow") { type = NavType.BoolType },
                navArgument("maxAmount") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val personName = backStackEntry.arguments?.getString("personName") ?: ""
            val isBorrow = backStackEntry.arguments?.getBoolean("isBorrow") ?: false
            val maxAmountStr = backStackEntry.arguments?.getString("maxAmount") ?: "0.0"
            val maxAmount = maxAmountStr.toDoubleOrNull() ?: 0.0

            SettleDebtScreen(
                viewModel = expenseViewModel,
                navController = navController,
                personName = personName,
                isBorrow = isBorrow,
                maxAmount = maxAmount
            )
        }



        // === 信贷账户还款页面 ===
        composable(
            route = Routes.CREDIT_REPAY,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("maxAmount") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            val maxAmountStr = backStackEntry.arguments?.getString("maxAmount") ?: "0.0"
            val maxAmount = maxAmountStr.toDoubleOrNull() ?: 0.0

            val accounts by expenseViewModel.allAccounts.collectAsState()
            val accountName = accounts.firstOrNull { it.id == accountId }?.name ?: ""

            SettleDebtScreen(
                viewModel = expenseViewModel,
                navController = navController,
                personName = accountName,
                isBorrow = true,
                maxAmount = maxAmount,
                mode = "CREDIT",
                creditAccountId = accountId
            )
        }







        // 4. 借入页面 (支持可选姓名参数)
        composable(
            route = Routes.ADD_BORROW,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("personName") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            // 获取传递过来的姓名
            val personName = backStackEntry.arguments?.getString("personName")

            AddDebtScreen(
                viewModel = expenseViewModel,
                navController = navController,
                accountId = accountId,
                isBorrow = true,
                presetName = personName // [新增参数] 传递预设姓名
            )
        }

        // 5. 借出页面 (支持可选姓名参数)
        composable(
            route = Routes.ADD_LEND,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("personName") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            // 获取传递过来的姓名
            val personName = backStackEntry.arguments?.getString("personName")

            AddDebtScreen(
                viewModel = expenseViewModel,
                navController = navController,
                accountId = accountId,
                isBorrow = false,
                presetName = personName // [新增参数] 传递预设姓名
            )
        }
        composable(
            route = Routes.EDIT_DEBT,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L
            EditDebtScreen(expenseViewModel, navController, recordId)
        }
    }
}