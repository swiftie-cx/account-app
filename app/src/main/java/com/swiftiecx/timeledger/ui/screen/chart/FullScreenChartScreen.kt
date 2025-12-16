package com.swiftiecx.timeledger.ui.screen.chart

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource // [新增] 引入资源引用
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增] 引入 R 类
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import kotlin.math.abs // [新增] 引入 abs
import kotlin.text.startsWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenChartScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    startDate: Long,
    endDate: Long,
    transactionType: Int // 0: 支出, 1: 收入, 2: 结余
) {
    // [新增] 跨币种所需数据
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val defaultCurrency by viewModel.defaultCurrency.collectAsState(initial = "CNY")
    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }

    val transferTypeString = stringResource(R.string.type_transfer) // [i18n]

    // 1. 强制横屏逻辑
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        // 保存原始方向
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
            // 恢复原始方向
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 2. 数据准备
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // 重新计算图表数据
    // [BUG 修复] 引入 accountMap 和 defaultCurrency
    val lineData = remember(allExpenses, startDate, endDate, transactionType, accountMap, defaultCurrency) {
        // 第一步：筛选日期范围
        val dateFiltered = allExpenses.filter { it.date.time in startDate..endDate }

        // 第二步：筛选交易类型
        val typeFiltered = dateFiltered.filter { expense ->
            when (transactionType) {
                0 -> expense.amount < 0 && !expense.category.startsWith(transferTypeString) // 支出
                1 -> expense.amount > 0 && !expense.category.startsWith(transferTypeString) // 收入
                else -> !expense.category.startsWith(transferTypeString) // 结余
            }
        }

        val typeEnum = when(transactionType) {
            1 -> TransactionType.INCOME
            2 -> TransactionType.BALANCE
            else -> TransactionType.EXPENSE
        }

        // 强制使用自定义逻辑, 并传入兑换参数
        prepareCustomLineChartData(
            data = typeFiltered,
            startDate = startDate,
            endDate = endDate,
            transactionType = typeEnum,
            accountMap = accountMap, // [传入]
            defaultCurrency = defaultCurrency // [传入]
        )
    }

    // 3. 计算画布宽度
    val pointWidth = 70.dp
    val minWidth = 600.dp

    val chartWidth = remember(lineData) {
        (pointWidth * lineData.size).coerceAtLeast(minWidth)
    }

    val scrollState = rememberScrollState()

    // [关键修改] 自动滚动到最右侧 (最新的日期)
    // 监听数据量变化，一旦数据准备好，立即滚动到底部
    LaunchedEffect(lineData.size) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // [i18n] 顶部标题
    val chartTitle = when(transactionType) {
        0 -> stringResource(R.string.chart_trend_expense)
        1 -> stringResource(R.string.chart_trend_income)
        else -> stringResource(R.string.chart_trend_balance)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.chart_fullscreen_title_format, chartTitle), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.chart_data_points_format, lineData.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (lineData.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.chart_no_data))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxHeight().width(chartWidth)) {
                        LineChart(
                            dataPoints = lineData,
                            modifier = Modifier.fillMaxSize(),
                            lineColor = MaterialTheme.colorScheme.primary,
                            showAllLabels = true,
                            onPointClick = { }
                        )
                    }
                }
            }
        }
    }
}