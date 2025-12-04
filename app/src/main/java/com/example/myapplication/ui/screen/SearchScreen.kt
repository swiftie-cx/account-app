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
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    initialCategory: String? = null,
    initialStartDate: Long? = null,
    initialEndDate: Long? = null,
    initialType: Int = 0 // 0全部, 1支出, 2收入
) {
    var localSearchText by remember { mutableStateOf("") }
    val searchResults by viewModel.filteredExpenses.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()

    val isFromChart = initialStartDate != null || initialCategory != null

    val typeFilters = listOf("全部", "支出", "收入", "转账")
    var selectedTypeIndex by remember { mutableStateOf(initialType) }

    var selectedCategories by remember {
        mutableStateOf(if (initialCategory != null) listOf(initialCategory) else emptyList<String>())
    }

    var showCategoryPicker by remember { mutableStateOf(false) }
    val timeFilters = listOf("全部", "本周", "本月", "本年", "自定义")

    var selectedTimeIndex by remember {
        mutableStateOf(if (initialStartDate != null) 4 else 0)
    }

    var showDateRangePicker by remember { mutableStateOf(false) }

    var customDateRangeMillis by remember {
        mutableStateOf(initialStartDate to initialEndDate)
    }

    val focusRequester = remember { FocusRequester() }

    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }
    val categoryIconMap = remember {
        (expenseCategories + incomeCategories).associate { it.title to it.icon }
    }

    // --- (修复) 智能标题生成逻辑 ---
    val dynamicTitle = remember(initialCategory, customDateRangeMillis) {
        val formatFullDay = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
        val formatMonth = SimpleDateFormat("yyyy年MM月", Locale.CHINA)
        val formatYear = SimpleDateFormat("yyyy年", Locale.CHINA)

        val sb = StringBuilder()

        if (initialCategory != null) {
            sb.append(initialCategory)
        }

        val start = customDateRangeMillis.first
        val end = customDateRangeMillis.second

        if (start != null && end != null) {
            if (sb.isNotEmpty()) sb.append(" - ")

            val c1 = Calendar.getInstance().apply { timeInMillis = start }
            val c2 = Calendar.getInstance().apply { timeInMillis = end }

            val y1 = c1.get(Calendar.YEAR)
            val m1 = c1.get(Calendar.MONTH)
            val d1 = c1.get(Calendar.DAY_OF_MONTH)

            val y2 = c2.get(Calendar.YEAR)
            val m2 = c2.get(Calendar.MONTH)
            val d2 = c2.get(Calendar.DAY_OF_MONTH)

            // 1. 同一天 (yyyy年MM月dd日)
            if (y1 == y2 && m1 == m2 && d1 == d2) {
                sb.append(formatFullDay.format(Date(start)))
            }
            // 2. 整月 (yyyy年MM月)
            // 条件：同年同月，Start是1号，End是该月最后一天
            else if (y1 == y2 && m1 == m2 && d1 == 1 && d2 == c2.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                sb.append(formatMonth.format(Date(start)))
            }
            // 3. 整年 (yyyy年)
            // 条件：Start是1月1日，End是12月31日
            else if (y1 == y2 &&
                m1 == Calendar.JANUARY && d1 == 1 &&
                m2 == Calendar.DECEMBER && d2 == 31) {
                sb.append(formatYear.format(Date(start)))
            }
            // 4. 其他范围 (yyyy年MM月dd日~yyyy年MM月dd日)
            else {
                sb.append("${formatFullDay.format(Date(start))}~${formatFullDay.format(Date(end))}")
            }
        }

        if (sb.isEmpty()) "搜索" else sb.toString()
    }

    val displayItems = remember(searchResults, accountMap, selectedTypeIndex, selectedTimeIndex, customDateRangeMillis, selectedCategories) {
        val timeFilteredResults = when (selectedTimeIndex) {
            1 -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.time
                searchResults.filter { !it.date.before(weekStart) }
            }
            2 -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.time
                searchResults.filter { !it.date.before(monthStart) }
            }
            3 -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val yearStart = calendar.time
                searchResults.filter { !it.date.before(yearStart) }
            }
            4 -> {
                val (startMillis, endMillis) = customDateRangeMillis
                if (startMillis != null && endMillis != null) {
                    val endOfDayCalendar = Calendar.getInstance().apply {
                        timeInMillis = endMillis
                        // 确保包含选定日期的最后一秒
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                    }
                    val endOfDayMillis = endOfDayCalendar.timeInMillis
                    searchResults.filter { it.date.time >= startMillis && it.date.time <= endOfDayMillis }
                } else {
                    searchResults
                }
            }
            else -> searchResults
        }

        val categoryFilteredResults = if (selectedCategories.isNotEmpty()) {
            timeFilteredResults.filter { it.category in selectedCategories }
        } else {
            timeFilteredResults
        }

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

        val finalItems: List<Any> = when (selectedTypeIndex) {
            1 -> regularExpenses.filter { it.amount < 0 }
            2 -> regularExpenses.filter { it.amount > 0 }
            3 -> processedTransfers
            else -> regularExpenses + processedTransfers
        }

        finalItems.sortedByDescending {
            when (it) {
                is Expense -> it.date.time
                is DisplayTransferItem -> it.date.time
                else -> 0L
            }
        }
    }

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

    if (showDateRangePicker) {
        CustomDateRangePicker(
            initialStartDate = customDateRangeMillis.first,
            initialEndDate = customDateRangeMillis.second,
            onDismiss = { showDateRangePicker = false },
            onConfirm = { start, end ->
                if (start != null && end != null) {
                    customDateRangeMillis = start to end
                    selectedTimeIndex = 4
                }
                showDateRangePicker = false
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!isFromChart) {
            focusRequester.requestFocus()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateSearchText("")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dynamicTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                // (修复) 使用主题色，与图表页保持一致
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (!isFromChart) {
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
                        OutlinedButton(
                            onClick = {
                                localSearchText = ""
                                selectedTypeIndex = 0
                                selectedCategories = emptyList()
                                selectedTimeIndex = 0
                                customDateRangeMillis = null to null
                                viewModel.updateSearchText("")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重置")
                        }
                        Button(
                            onClick = {
                                viewModel.updateSearchText(localSearchText)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("搜索")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column {
                if (!isFromChart) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = localSearchText,
                            onValueChange = { localSearchText = it },
                            placeholder = { Text("搜索备注、分类") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .focusRequester(focusRequester),
                            singleLine = true
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        FilterChipRow(
                            title = "类型",
                            labels = typeFilters,
                            selectedIndex = selectedTypeIndex,
                            onChipSelected = { selectedTypeIndex = it }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        CategoryFilterRow(
                            selectedCategories = selectedCategories,
                            onClearCategories = { selectedCategories = emptyList() },
                            onAddClick = { showCategoryPicker = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        FilterChipRow(
                            title = "时间",
                            labels = timeFilters,
                            selectedIndex = selectedTimeIndex,
                            onChipSelected = { index ->
                                if (index == 4) {
                                    showDateRangePicker = true
                                } else {
                                    selectedTimeIndex = index
                                    customDateRangeMillis = null to null
                                }
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

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
                                onClick = { /* TODO */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

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