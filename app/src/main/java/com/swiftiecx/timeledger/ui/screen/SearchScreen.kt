package com.swiftiecx.timeledger.ui.screen

import android.net.Uri
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.text.startsWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    initialCategory: String? = null,
    initialStartDate: Long? = null,
    initialEndDate: Long? = null,
    initialType: Int = 0
) {
    val context = LocalContext.current
    var localSearchText by remember { mutableStateOf("") }
    val searchResults by viewModel.filteredExpenses.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()

    val isFromChart = initialStartDate != null || initialCategory != null

    // [i18n] 类型筛选器
    val typeFilters = listOf(
        stringResource(R.string.type_all),
        stringResource(R.string.type_expense),
        stringResource(R.string.type_income),
        stringResource(R.string.type_transfer)
    )
    var selectedTypeIndex by remember { mutableStateOf(initialType) }

    var selectedCategories by remember {
        mutableStateOf(if (initialCategory != null) listOf(initialCategory) else emptyList<String>())
    }

    var showCategoryPicker by remember { mutableStateOf(false) }

    // [i18n] 时间筛选器
    val timeFilters = listOf(
        stringResource(R.string.type_all),
        stringResource(R.string.time_week_current),
        stringResource(R.string.time_month_current),
        stringResource(R.string.time_year_current),
        stringResource(R.string.time_custom)
    )

    var selectedTimeIndex by remember {
        mutableStateOf(if (initialStartDate != null) 4 else 0)
    }

    var showDateRangePicker by remember { mutableStateOf(false) }

    var customDateRangeMillis by remember {
        mutableStateOf(initialStartDate to initialEndDate)
    }

    val focusRequester = remember { FocusRequester() }

    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }

    // ✅ 用“稳定 key(英文)”做映射：key -> icon
    val categoryIconMap = remember(context) {
        val expenseSubs = CategoryData.getExpenseCategories(context).flatMap { it.subCategories }
        val incomeSubs = CategoryData.getIncomeCategories(context).flatMap { it.subCategories }
        (expenseSubs + incomeSubs).associate { it.key to it.icon }
    }

    // ✅ key -> 主类颜色（用于搜索结果列表图标上色）
    val categoryColorMap = remember(context) {
        val expenseMains = CategoryData.getExpenseCategories(context)
        val incomeMains = CategoryData.getIncomeCategories(context)

        (expenseMains + incomeMains)
            .flatMap { main -> main.subCategories.map { sub -> sub.key to main.color } }
            .toMap()
    }

    val dynamicTitle = remember(initialCategory, customDateRangeMillis) {
        val formatFullDay = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val sb = StringBuilder()

        if (initialCategory != null) {
            sb.append(initialCategory)
        }

        val start = customDateRangeMillis.first
        val end = customDateRangeMillis.second

        if (start != null && end != null) {
            if (sb.isNotEmpty()) sb.append(" - ")
            sb.append("${formatFullDay.format(Date(start))}~${formatFullDay.format(Date(end))}")
        }

        if (sb.isEmpty()) context.getString(R.string.search) else sb.toString()
    }

    val displayItems = remember(
        searchResults,
        accountMap,
        selectedTypeIndex,
        selectedTimeIndex,
        customDateRangeMillis,
        selectedCategories
    ) {
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

                val feeRaw = abs(outTx.amount) - abs(inTx.amount)
                val fee = if (feeRaw < 0.01) 0.0 else feeRaw

                DisplayTransferItem(
                    expenseId = outTx.id,
                    date = outTx.date,
                    fromAccount = fromAccount,
                    toAccount = toAccount,
                    fromAmount = outTx.amount,
                    toAmount = inTx.amount,
                    fee = fee
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (isFromChart) {
                FloatingActionButton(
                    onClick = {
                        val targetTab = if (initialType == 2) 1 else 0
                        navController.navigate(
                            Routes.addTransactionRoute(
                                dateMillis = initialStartDate,
                                type = targetTab
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
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
                            Text(stringResource(R.string.reset))
                        }
                        Button(
                            onClick = {
                                viewModel.updateSearchText(localSearchText)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.search))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (!isFromChart) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        OutlinedTextField(
                            value = localSearchText,
                            onValueChange = { localSearchText = it },
                            placeholder = { Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = if (localSearchText.isNotEmpty()) {
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
                            title = stringResource(R.string.account_type),
                            labels = typeFilters,
                            selectedIndex = selectedTypeIndex,
                            onChipSelected = { selectedTypeIndex = it }
                        )

                        Spacer(Modifier.height(8.dp))

                        CategoryFilterRow(
                            selectedCategories = selectedCategories,
                            onClearCategories = { selectedCategories = emptyList() },
                            onRemoveCategory = { category ->
                                selectedCategories = selectedCategories - category
                            },
                            onAddClick = { showCategoryPicker = true }
                        )

                        Spacer(Modifier.height(8.dp))

                        FilterChipRow(
                            title = stringResource(R.string.date_label),
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (displayItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.no_records), color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                items(displayItems) { item ->
                    when (item) {
                        is Expense -> {
                            val account = accountMap[item.accountId]

                            // ✅ item.category 是 key（如 Food / Electronics）就能命中
                            val icon = categoryIconMap[item.category]
                            val categoryThemeColor = categoryColorMap[item.category] ?: MaterialTheme.colorScheme.primary

                            ExpenseItem(
                                expense = item,
                                icon = icon,
                                categoryColor = categoryThemeColor,
                                account = account,
                                onClick = { navController.navigate(Routes.transactionDetailRoute(item.id)) }
                            )
                        }

                        is DisplayTransferItem -> {
                            TransferItem(
                                item = item,
                                onClick = { navController.navigate(Routes.transactionDetailRoute(item.expenseId)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 以下为辅助组件定义 ----------------

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
                        borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
    onRemoveCategory: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.category_label),
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
                    label = { Text(stringResource(R.string.type_all)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (selectedCategories.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        enabled = true, selected = selectedCategories.isEmpty()
                    )
                )
            }

            items(selectedCategories) { category ->
                InputChip(
                    selected = true,
                    onClick = { /* 不做事 */ },
                    label = { Text(category) },
                    trailingIcon = {
                        IconButton(
                            onClick = { onRemoveCategory(category) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
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
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
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
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = transferColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.transfer_in_app),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (item.fee > 0.01) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.fee_label)} ${String.format("%.2f", item.fee)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

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
    icon: ImageVector?,
    categoryColor: Color,
    account: Account?,
    onClick: () -> Unit
) {
    val isExpense = expense.amount < 0
    val amountColor = if (isExpense) Color(0xFFE53935) else Color(0xFF4CAF50)

    val iconContainerColor = categoryColor.copy(alpha = 0.15f)
    val iconTintColor = categoryColor

    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }

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
