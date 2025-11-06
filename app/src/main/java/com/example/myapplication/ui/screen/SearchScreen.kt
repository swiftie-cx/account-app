package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.Expense
// (新) 导入 VM 和相关类
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Date
import kotlin.math.abs
import androidx.compose.material3.MaterialTheme
import java.util.Calendar

/**
 * 搜索页面 (已连接基础搜索逻辑)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ExpenseViewModel, // (修改) 接收 ViewModel
    navController: NavHostController,
) {
    // --- 状态管理 ---

    // (修改) 搜索框的文字使用 *本地* 状态
    var localSearchText by remember { mutableStateOf("") }

    // (修改) 从 ViewModel 获取 *结果*
    val searchResults by viewModel.filteredExpenses.collectAsState()
    // (新) 从 ViewModel 获取账户，用于显示列表项
    val allAccounts by viewModel.allAccounts.collectAsState()

    // (保持本地) 其他过滤器暂时保持本地状态，不影响搜索
    val typeFilters = listOf("全部", "支出", "收入", "转账")
    var selectedTypeIndex by remember { mutableStateOf(0) }
    var selectedCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    val timeFilters = listOf("全部", "本周", "本月", "本年", "自定义")
    var selectedTimeIndex by remember { mutableStateOf(0) }

    // (新) 自定义时间范围的状态
    var showDateRangePicker by remember { mutableStateOf(false) }
    var customDateRangeMillis by remember { mutableStateOf<Pair<Long?, Long?>>(null to null) }

    val focusRequester = remember { FocusRequester() }

    // --- (新) 复制 DetailsScreen 的数据处理逻辑 ---
    // (需要这些来正确显示列表项)
    val accountMap = remember(allAccounts) {
        allAccounts.associateBy { it.id }
    }
    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    // (新) 预处理过滤后的列表，合并转账
    val displayItems = remember(searchResults, accountMap, selectedTypeIndex, selectedTimeIndex, customDateRangeMillis, selectedCategories) {
        // 1. 根据时间筛选
        val timeFilteredResults = when (selectedTimeIndex) {
            1 -> { // 本周
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.time
                searchResults.filter { !it.date.before(weekStart) }
            }
            2 -> { // 本月
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.time
                searchResults.filter { !it.date.before(monthStart) }
            }
            3 -> { // 本年
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val yearStart = calendar.time
                searchResults.filter { !it.date.before(yearStart) }
            }
            4 -> { // 自定义
                val (startMillis, endMillis) = customDateRangeMillis
                if (startMillis != null && endMillis != null) {
                    val endOfDayCalendar = Calendar.getInstance().apply {
                        timeInMillis = endMillis
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }
                    val endOfDayMillis = endOfDayCalendar.timeInMillis
                    searchResults.filter { it.date.time in startMillis..endOfDayMillis }
                } else {
                    searchResults
                }
            }
            else -> searchResults // "全部"
        }

        // 1.5. 根据类别筛选
        val categoryFilteredResults = if (selectedCategories.isNotEmpty()) {
            timeFilteredResults.filter { it.category in selectedCategories }
        } else {
            timeFilteredResults
        }

        // 2. 用筛选后的结果继续处理
        val transferExpenses = categoryFilteredResults.filter { it.category.startsWith("转账") }
        val regularExpenses = categoryFilteredResults.filter { !it.category.startsWith("转账") }

        val processedTransfers = transferExpenses
            .groupBy { it.date }
            .mapNotNull { (_, pair) ->
                val outTx = pair.find { it.amount < 0 }
                val inTx = pair.find { it.amount > 0 }
                if (outTx == null || inTx == null) return@mapNotNull null
                val fromAccount = accountMap[outTx.accountId]
                val toAccount = accountMap[inTx.accountId]
                if (fromAccount == null || toAccount == null) return@mapNotNull null
                DisplayTransferItem(
                    date = outTx.date,
                    fromAccount = fromAccount,
                    toAccount = toAccount,
                    fromAmount = outTx.amount,
                    toAmount = inTx.amount
                )
            }

        // 3. 根据类型筛选
        val finalItems: List<Any> = when (selectedTypeIndex) {
            1 -> regularExpenses.filter { it.amount < 0 } // 支出
            2 -> regularExpenses.filter { it.amount > 0 } // 收入
            3 -> processedTransfers // 转账
            else -> regularExpenses + processedTransfers // 全部
        }

        // 4. 排序
        finalItems.sortedByDescending {
            when (it) {
                is Expense -> it.date.time
                is DisplayTransferItem -> it.date.time
                else -> 0L
            }
        }
    }
    // --- 数据处理结束 ---

    if (showCategoryPicker) {
        Dialog(
            onDismissRequest = { showCategoryPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CategorySelectionScreen(
                onDismiss = { showCategoryPicker = false },
                onConfirm = { categories ->
                    showCategoryPicker = false
                    selectedCategories = categories
                }
            )
        }
    }

    // (新) 日期范围选择器
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDateRangePicker = false
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            customDateRangeMillis = startMillis to endMillis
                            selectedTimeIndex = 4 // 确认后，正式选择“自定义”
                        }
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DateRangePicker(state = dateRangePickerState)
        }
    }

    // 页面打开时，自动激活搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // (新) 页面销毁时 (例如按返回键)，重置 ViewModel 中的搜索词
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateSearchText("")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // (修改) 重置按钮
                    OutlinedButton(
                        onClick = {
                            // 重置本地状态
                            localSearchText = ""
                            selectedTypeIndex = 0
                            selectedCategories = emptyList()
                            selectedTimeIndex = 0
                            customDateRangeMillis = null to null // (新) 重置自定义日期
                            // (新) 重置 ViewModel
                            viewModel.updateSearchText("")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重置")
                    }
                    // (修改) 搜索按钮
                    Button(
                        onClick = {
                            // (新) 将本地搜索词提交给 ViewModel
                            viewModel.updateSearchText(localSearchText)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("搜索")
                    }
                }
            }
        }
    ) { innerPadding ->
        // (修改) 使用 Box 来容纳 Column 和 LazyColumn
        Box(modifier = Modifier.padding(innerPadding)) {
            Column {
                // --- 搜索和过滤条件 ---
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // (修改) 搜索框
                    OutlinedTextField(
                        value = localSearchText, // <-- 使用本地状态
                        onValueChange = { localSearchText = it }, // <-- 更新本地状态
                        placeholder = { Text("搜索备注、分类") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .focusRequester(focusRequester),
                        singleLine = true
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // (保持本地) 行 1: 类型
                    FilterChipRow(
                        title = "类型",
                        labels = typeFilters,
                        selectedIndex = selectedTypeIndex,
                        onChipSelected = { selectedTypeIndex = it }
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // (保持本地) 行 2: 类别
                    CategoryFilterRow(
                        selectedCategories = selectedCategories,
                        onClearCategories = { selectedCategories = emptyList() },
                        onAddClick = { showCategoryPicker = true }
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // (保持本地) 行 3: 时间
                    FilterChipRow(
                        title = "时间",
                        labels = timeFilters,
                        selectedIndex = selectedTimeIndex,
                        onChipSelected = { index ->
                            if (index == 4) { // "自定义"
                                showDateRangePicker = true
                            } else {
                                selectedTimeIndex = index
                                customDateRangeMillis = null to null
                            }
                        }
                    )
                }
                // --- 搜索和过滤条件 结束 ---

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // (新) --- 搜索结果列表 ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayItems) { item ->
                        when (item) {
                            is Expense -> {
                                val account = accountMap[item.accountId]
                                ExpenseItem(
                                    expense = item,
                                    icon = categoryIconMap[item.category],
                                    account = account,
                                    onClick = { navController.navigate(Routes.transactionDetailRoute(item.id)) }
                                )
                            }
                            is DisplayTransferItem -> TransferItem(
                                item = item,
                                onClick = { /* TODO: 决定合并转账的点击 */ }
                            )
                        }
                    }
                }
                // --- 搜索结果列表 结束 ---
            }
        }
    }
}


/**
 * 筛选器行 (用于 "类型" 和 "时间")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onChipSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(labels) { index, label ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onChipSelected(index) },
                    label = { Text(label) }
                )
            }
        }
    }
}

/**
 * 筛选器行 (用于 "类别")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selectedCategories: List<String>,
    onClearCategories: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "类别",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp)
        )
        FilterChip(
            selected = selectedCategories.isEmpty(),
            onClick = onClearCategories,
            label = { Text("全部") }
        )
        if (selectedCategories.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedCategories) { category ->
                    InputChip(
                        selected = true,
                        onClick = { /* Future: remove single category */ },
                        label = { Text(category) }
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        FilledIconButton(
            onClick = onAddClick,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "选择类别")
        }
    }
}

// --- (新) 复制 DetailsScreen 的列表项 ---

@Composable
private fun TransferItem(item: DisplayTransferItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
            contentDescription = "转账",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(8.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = "${item.fromAccount.name} → ${item.toAccount.name}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Column(horizontalAlignment = Alignment.End) {
            val fromText = "${item.fromAccount.currency} ${String.format("%.2f", abs(item.fromAmount))}"
            val toText = "→ ${item.toAccount.currency} ${String.format("%.2f", item.toAmount)}"
            Text(
                text = fromText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = toText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpenseItem(
    expense: Expense,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    account: Account?,
    onClick: () -> Unit
) {
    val amountColor = if (expense.amount < 0) MaterialTheme.colorScheme.error else Color.Unspecified
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = expense.category,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        } else {
            Spacer(Modifier.size(40.dp))
        }

        val displayText = if (!expense.remark.isNullOrBlank()) expense.remark else expense.category
        Text(text = displayText ?: expense.category, modifier = Modifier.weight(1f))

        val amountText = "${account?.currency ?: ""} ${String.format("%.2f", abs(expense.amount))}"
        Text(
            text = amountText,
            color = amountColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
