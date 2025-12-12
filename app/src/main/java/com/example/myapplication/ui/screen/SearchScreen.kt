package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    initialType: Int = 0 // 0全部/结余, 1支出, 2收入
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
            else if (y1 == y2 && m1 == m2 && d1 == 1 && d2 == c2.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                sb.append(formatMonth.format(Date(start)))
            }
            // 3. 整年 (yyyy年)
            else if (y1 == y2 &&
                m1 == Calendar.JANUARY && d1 == 1 &&
                m2 == Calendar.DECEMBER && d2 == 31) {
                sb.append(formatYear.format(Date(start)))
            }
            // 4. 其他范围
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(dynamicTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        // 【关键修改】新增悬浮按钮
        floatingActionButton = {
            if (isFromChart) {
                FloatingActionButton(
                    onClick = {
                        // 映射逻辑：
                        // SearchScreen Type: 0=全部/结余, 1=支出, 2=收入
                        // AddTransactionScreen Tab: 0=支出, 1=收入, 2=转账

                        // 如果当前是收入(2)，跳转去收入Tab(1)
                        // 如果当前是支出(1)或结余(0)，跳转去支出Tab(0)
                        val targetTab = if (initialType == 2) 1 else 0

                        navController.navigate(
                            Routes.addTransactionRoute(
                                dateMillis = initialStartDate, // 使用进入该页面的筛选日期
                                type = targetTab
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加记录")
                }
            }
        },
        bottomBar = {
            if (!isFromChart) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
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
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("重置")
                        }
                        Button(
                            onClick = {
                                viewModel.updateSearchText(localSearchText)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("搜索")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 筛选区域 (白色背景卡片)
            if (!isFromChart) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // 搜索框
                        OutlinedTextField(
                            value = localSearchText,
                            onValueChange = { localSearchText = it },
                            placeholder = { Text("搜索备注、分类", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = if(localSearchText.isNotEmpty()) {
                                { IconButton(onClick = { localSearchText = "" }) { Icon(Icons.Default.Close, null) } }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        FilterChipRow(
                            title = "类型",
                            labels = typeFilters,
                            selectedIndex = selectedTypeIndex,
                            onChipSelected = { selectedTypeIndex = it }
                        )

                        Spacer(Modifier.height(8.dp))

                        CategoryFilterRow(
                            selectedCategories = selectedCategories,
                            onClearCategories = { selectedCategories = emptyList() },
                            onAddClick = { showCategoryPicker = true }
                        )

                        Spacer(Modifier.height(8.dp))

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
                }
            }

            // 结果列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // 增加底部边距，防止FAB遮挡
            ) {
                if (displayItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("没有找到相关记录", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onChipSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(labels) { index, label ->
                val isSelected = selectedIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { onChipSelected(index) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if(isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        enabled = true, selected = isSelected
                    )
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "类别",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedCategories.isEmpty(),
                    onClick = onClearCategories,
                    label = { Text("全部") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if(selectedCategories.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        enabled = true, selected = selectedCategories.isEmpty()
                    )
                )
            }

            items(selectedCategories) { category ->
                InputChip(
                    selected = true,
                    onClick = { /* Future: remove single category */ },
                    label = { Text(category) },
                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null
                )
            }

            item {
                FilledIconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "选择类别", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TransferItem(item: DisplayTransferItem, onClick: () -> Unit) {
    val transferColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(transferColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = "转账",
                modifier = Modifier.size(22.dp),
                tint = transferColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "内部转账",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.fromAccount.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item.toAccount.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = String.format("%.2f", item.toAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExpenseItem(
    expense: Expense,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    account: Account?,
    onClick: () -> Unit
) {
    val isExpense = expense.amount < 0
    val incomeColor = Color(0xFF4CAF50) // 绿色
    val expenseColor = Color(0xFFE53935) // 红色
    val amountColor = if (isExpense) expenseColor else incomeColor

    val iconContainerColor = if (isExpense) {
        expenseColor.copy(alpha = 0.1f)
    } else {
        incomeColor.copy(alpha = 0.1f)
    }

    val iconTintColor = if (isExpense) expenseColor else incomeColor

    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.CHINA) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (icon != null) iconContainerColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = expense.category,
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.size(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            val subText = buildString {
                append(dateFormat.format(expense.date))
                if (!expense.remark.isNullOrBlank()) {
                    append(" · ")
                    append(expense.remark)
                } else if (account != null) {
                    append(" · ")
                    append(account.name)
                }
            }
            Text(
                text = subText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = if (isExpense) String.format("%.2f", expense.amount) else "+${String.format("%.2f", expense.amount)}",
            color = amountColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}