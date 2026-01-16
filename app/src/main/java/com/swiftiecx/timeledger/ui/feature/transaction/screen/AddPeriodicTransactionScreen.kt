package com.swiftiecx.timeledger.ui.feature.transaction.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.local.entity.PeriodicTransaction
import com.swiftiecx.timeledger.ui.common.Category
import com.swiftiecx.timeledger.ui.common.CategoryData // 动态分类数据源
import com.swiftiecx.timeledger.ui.feature.transaction.component.AccountPickerDialog
import com.swiftiecx.timeledger.ui.feature.transaction.component.ModeSelectionButton
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.find

// 结束模式枚举
private const val END_MODE_NEVER = 0
private const val END_MODE_DATE = 1
private const val END_MODE_COUNT = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPeriodicTransactionScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    periodicId: Long? = null,
    initialType: Int = 0
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    // [Fix] 动态获取分类列表
    val expenseCategories = remember(context) { CategoryData.getExpenseCategories(context).flatMap { it.subCategories } }
    val incomeCategories = remember(context) { CategoryData.getIncomeCategories(context).flatMap { it.subCategories } }

    // --- 准备按钮颜色 ---
    val positiveColor = MaterialTheme.colorScheme.primary.toArgb()
    val negativeColor = MaterialTheme.colorScheme.outline.toArgb()

    // --- 状态定义 ---
    var transactionType by remember { mutableIntStateOf(initialType) }

    // 周期规则
    var frequency by remember { mutableIntStateOf(2) } // 默认每月
    var startDate by remember { mutableStateOf(Date()) }

    // 结束规则
    var endMode by remember { mutableIntStateOf(END_MODE_NEVER) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var endCount by remember { mutableStateOf<Int?>(null) }

    // 账单详情
    var amount by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") } // 手续费

    // 转账模式状态 (0:转出固定, 1:转入固定)
    var transferMode by remember { mutableIntStateOf(0) }

    // 焦点请求器
    val amountFocusRequester = remember { FocusRequester() }

    var remark by remember { mutableStateOf("") }
    var excludeFromBudget by remember { mutableStateOf(false) }

    // 对象选择
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val selectableAccounts = remember(accounts) { accounts.filter { it.category != "DEBT" } }
    // 弹窗控制
    var showFrequencySheet by remember { mutableStateOf(false) }
    var showEndRepeatSheet by remember { mutableStateOf(false) }
    var showCountInputDialog by remember { mutableStateOf(false) }
    var showAccountPickerFor by remember { mutableStateOf<String?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }

    // --- 日期和时间选择器 ---
    val calendar = Calendar.getInstance()

    // [i18n] 确保对话框按钮使用主题色
    val dialogPositiveColor = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary).contentColor.toArgb()
    val dialogNegativeColor = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant).contentColor.toArgb()

    // 1. 生效日期选择器
    val startDatePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            val cal = Calendar.getInstance().apply { time = startDate }
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            startDate = cal.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    startDatePicker.setOnShowListener {
        startDatePicker.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(dialogPositiveColor)
        startDatePicker.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(dialogNegativeColor)
    }

    // 2. 账单时间选择器
    val timePicker = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val cal = Calendar.getInstance().apply { time = startDate }
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
            cal.set(Calendar.MINUTE, minute)
            startDate = cal.time
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )
    timePicker.setOnShowListener {
        timePicker.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(dialogPositiveColor)
        timePicker.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(dialogNegativeColor)
    }

    // 3. 结束日期选择器
    val endDatePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day)
            endDate = cal.time
            endMode = END_MODE_DATE
            endCount = null
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = System.currentTimeMillis()
    }
    endDatePicker.setOnShowListener {
        endDatePicker.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(dialogPositiveColor)
        endDatePicker.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(dialogNegativeColor)
    }

    // --- 加载数据 ---
    val periodicList by viewModel.allPeriodicTransactions.collectAsState()
    LaunchedEffect(periodicId, periodicList, accounts) {
        if (periodicId != null && periodicId != -1L) {
            val item = periodicList.find { it.id == periodicId }
            if (item != null) {
                transactionType = item.type
                frequency = item.frequency
                startDate = item.startDate
                endMode = item.endMode
                endDate = item.endDate
                endCount = item.endCount
                amount = item.amount.toString()
                fee = if (item.fee > 0) item.fee.toString() else ""
                transferMode = item.transferMode
                remark = item.remark ?: ""
                excludeFromBudget = item.excludeFromBudget

                if (item.type != 2) {
                    // [Fix] 动态获取分类列表 + 兼容旧数据(title)与新数据(key)
                    val allCats = if (item.type == 0) expenseCategories else incomeCategories
                    selectedCategory = allCats.find { it.key == item.category || it.title == item.category }
                    selectedAccount = accounts.find { it.id == item.accountId }
                } else {
                    fromAccount = accounts.find { it.id == item.accountId }
                    toAccount = accounts.find { it.id == item.targetAccountId }
                }
            }
        } else {
            // 新增模式默认值
            if (accounts.isNotEmpty()) {
                if (selectedAccount == null) selectedAccount = accounts.first()
                if (fromAccount == null) fromAccount = accounts.first()

                if (toAccount == null) {
                    toAccount = accounts.firstOrNull {
                        it.id != fromAccount?.id &&
                                it.currency == fromAccount?.currency
                    }
                }
            }
            if (selectedCategory == null && transactionType != 2) {
                // [Fix] 动态获取分类列表
                val cats = if (transactionType == 0) expenseCategories else incomeCategories
                selectedCategory = cats.firstOrNull()
            }
        }
    }

    // --- 保存逻辑 ---
    fun save() {
        val amountVal = amount.toDoubleOrNull() ?: 0.0
        if (amountVal <= 0) return

        val feeVal = fee.toDoubleOrNull() ?: 0.0

        val cal = Calendar.getInstance()
        cal.time = startDate
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val cleanStartDate = cal.time

        // [关键修复] 非转账：存 stable key，而不是存显示名(title)
        val stableCategoryKey = if (transactionType == 2) {
            // transfer 的 category 字段实际不会用于生成记录分类，这里随便存一个稳定值即可
            "Transfer"
        } else {
            val nameOrKey = selectedCategory?.key ?: selectedCategory?.title ?: context.getString(R.string.sub_other)
            CategoryData.getStableKey(nameOrKey, context)
        }

        val transaction = PeriodicTransaction(
            id = if (periodicId != null && periodicId != -1L) periodicId else 0,
            type = transactionType,
            amount = amountVal,
            fee = feeVal,
            transferMode = transferMode,
            category = stableCategoryKey,
            accountId = if (transactionType == 2) (fromAccount?.id ?: 0) else (selectedAccount?.id ?: 0),
            targetAccountId = if (transactionType == 2) toAccount?.id else null,
            frequency = frequency,
            startDate = cleanStartDate,
            nextExecutionDate = cleanStartDate,
            endMode = endMode,
            endDate = if (endMode == END_MODE_DATE) endDate else null,
            endCount = if (endMode == END_MODE_COUNT) endCount else null,
            remark = remark,
            excludeFromStats = false,
            // 只有支出类型才应用不计入预算
            excludeFromBudget = if (transactionType == 0) excludeFromBudget else false
        )

        if (periodicId != null && periodicId != -1L) {
            viewModel.updatePeriodic(transaction)
        } else {
            viewModel.insertPeriodic(transaction)
        }
        navController.popBackStack()
    }

    // --- 动态标题 ---
    val titleText = when (transactionType) {
        0 -> stringResource(R.string.type_expense) // 支出
        1 -> stringResource(R.string.type_income)  // 收入
        2 -> stringResource(R.string.type_transfer) // 转账
        else -> stringResource(R.string.periodic_title)
    }

    // --- UI 渲染 ---
    val bgColor = MaterialTheme.colorScheme.surfaceContainerLow
    val cardColor = MaterialTheme.colorScheme.surface

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // 添加 Snackbar 用于提示错误
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                // [i18n]
                title = { Text(titleText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { save() }) {
                        Icon(Icons.Default.Check, stringResource(R.string.save))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cardColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 第一组：重复规则 ---
            Card(colors = CardDefaults.cardColors(containerColor = cardColor)) {
                Column {
                    // [i18n] 频率文本
                    val freqLabel = listOf(
                        R.string.freq_day, R.string.freq_week, R.string.freq_month, R.string.freq_year
                    ).getOrElse(frequency) { R.string.freq_month }

                    // [i18n]
                    FormItem(
                        label = stringResource(R.string.repeat_frequency),
                        value = stringResource(freqLabel),
                        onClick = { showFrequencySheet = true }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    // [i18n] 结束规则文本
                    val endLabel = when (endMode) {
                        END_MODE_NEVER -> stringResource(R.string.end_never)
                        END_MODE_DATE -> endDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
                            ?: stringResource(R.string.date)
                        END_MODE_COUNT -> "${endCount ?: 0} ${stringResource(R.string.count_label_unit)}"
                        else -> stringResource(R.string.end_never)
                    }
                    FormItem(
                        label = stringResource(R.string.end_rule),
                        value = endLabel,
                        onClick = { showEndRepeatSheet = true }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    // [i18n] 生效日期文本
                    val startLabel =
                        if (isToday(startDate)) stringResource(R.string.today)
                        else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate)
                    FormItem(
                        label = stringResource(R.string.start_date),
                        value = startLabel,
                        onClick = { startDatePicker.show() }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    // [i18n] 账单时间文本
                    val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(startDate)
                    FormItem(
                        label = stringResource(R.string.time_day),
                        value = timeLabel,
                        onClick = { timePicker.show() }
                    )
                }
            }

            // --- 第二组：账单详情 ---
            Card(colors = CardDefaults.cardColors(containerColor = cardColor)) {
                Column {
                    if (transactionType != 2) {
                        FormItem(
                            label = stringResource(R.string.category_label),
                            value = selectedCategory?.title ?: stringResource(R.string.select_category),
                            icon = selectedCategory?.icon,
                            onClick = { showCategorySheet = true }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }

                    // --- 金额输入框 ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                amountFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.amount_label),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        BasicTextField(
                            value = amount,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    amount = input
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .width(150.dp)
                                .focusRequester(amountFocusRequester),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    if (amount.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    // --- 转账专用字段 ---
                    if (transactionType == 2) {
                        FormItem(
                            label = stringResource(R.string.transfer_out_account),
                            value = fromAccount?.name ?: stringResource(R.string.select_account),
                            onClick = { showAccountPickerFor = "from" }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        FormItem(
                            label = stringResource(R.string.transfer_in_account),
                            value = toAccount?.name ?: stringResource(R.string.select_account),
                            onClick = { showAccountPickerFor = "to" }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                        // 手续费输入框
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.fee_label),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            BasicTextField(
                                value = fee,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) fee = input
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(100.dp),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterEnd) {
                                        if (fee.isEmpty()) Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        inner()
                                    }
                                }
                            )
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                        // 转账模式选择
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModeSelectionButton(
                                text = stringResource(R.string.transfer_mode_out_fixed),
                                isSelected = transferMode == 0,
                                modifier = Modifier.weight(1f),
                                onClick = { transferMode = 0 }
                            )
                            ModeSelectionButton(
                                text = stringResource(R.string.transfer_mode_in_fixed),
                                isSelected = transferMode == 1,
                                modifier = Modifier.weight(1f),
                                onClick = { transferMode = 1 }
                            )
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    } else {
                        // 收入/支出
                        FormItem(
                            label = stringResource(R.string.select_account),
                            value = selectedAccount?.name ?: stringResource(R.string.select_account),
                            onClick = { showAccountPickerFor = "main" }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }

                    // --- 备注 ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.remark_label),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(16.dp))
                        BasicTextField(
                            value = remark,
                            onValueChange = { remark = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    if (remark.isEmpty()) {
                                        // [i18n]
                                        Text(
                                            text = stringResource(R.string.remark_hint),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            // --- 第三组：选项 ---
            if (transactionType == 0) {
                Card(colors = CardDefaults.cardColors(containerColor = cardColor)) {
                    Column {
                        // [i18n]
                        FormToggleItem(
                            label = stringResource(R.string.exclude_from_budget),
                            checked = excludeFromBudget,
                            onCheckedChange = { excludeFromBudget = it }
                        )
                    }
                }
            }

            // --- 底部提示 ---
            if (transactionType == 2) {
                // [i18n]
                Text(
                    text = "* " + stringResource(R.string.warning_cross_currency_transfer_periodic),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // --- 弹窗组件 ---
        if (showFrequencySheet) {
            ModalBottomSheet(onDismissRequest = { showFrequencySheet = false }) {
                Column(Modifier.padding(bottom = 32.dp)) {
                    // [i18n]
                    listOf(
                        R.string.freq_day, R.string.freq_week, R.string.freq_month, R.string.freq_year
                    ).forEachIndexed { index, id ->
                        ListItem(
                            headlineContent = { Text(stringResource(id)) },
                            trailingContent = { if (frequency == index) Icon(Icons.Default.Check, null) },
                            modifier = Modifier.clickable { frequency = index; showFrequencySheet = false }
                        )
                    }
                }
            }
        }

        if (showEndRepeatSheet) {
            ModalBottomSheet(onDismissRequest = { showEndRepeatSheet = false }) {
                Column(Modifier.padding(bottom = 32.dp)) {
                    // [i18n]
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.end_never), style = MaterialTheme.typography.titleMedium) },
                        trailingContent = { if (endMode == END_MODE_NEVER) Icon(Icons.Default.Check, null) },
                        modifier = Modifier.clickable {
                            endMode = END_MODE_NEVER
                            showEndRepeatSheet = false
                        }
                    )
                    // [i18n]
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.end_by_date), style = MaterialTheme.typography.titleMedium) },
                        trailingContent = { if (endMode == END_MODE_DATE) Icon(Icons.Default.Check, null) },
                        modifier = Modifier.clickable {
                            showEndRepeatSheet = false
                            endDatePicker.show()
                        }
                    )
                    // [i18n]
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.end_by_count), style = MaterialTheme.typography.titleMedium) },
                        trailingContent = { if (endMode == END_MODE_COUNT) Icon(Icons.Default.Check, null) },
                        modifier = Modifier.clickable {
                            showEndRepeatSheet = false
                            showCountInputDialog = true
                        }
                    )
                }
            }
        }

        if (showCountInputDialog) {
            var tempCount by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCountInputDialog = false },
                // [i18n]
                title = { Text(stringResource(R.string.input_repeat_count)) },
                text = {
                    OutlinedTextField(
                        value = tempCount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tempCount = it },
                        // [i18n]
                        label = { Text(stringResource(R.string.count)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val count = tempCount.toIntOrNull()
                            if (count != null && count > 0) {
                                endCount = count
                                endMode = END_MODE_COUNT
                                showCountInputDialog = false
                            }
                        }
                    ) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showCountInputDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        if (showAccountPickerFor != null) {
            AccountPickerDialog(
                accounts = selectableAccounts,
                onAccountSelected = { selected ->
                    val otherAccount =
                        if (showAccountPickerFor == "from") toAccount else fromAccount
                    var shouldClearOther = false

                    if (otherAccount != null) {
                        val isCurrencyMismatch = selected.currency != otherAccount.currency
                        val isSameAccount = selected.id == otherAccount.id

                        if (isCurrencyMismatch || isSameAccount) {
                            shouldClearOther = true
                        }
                    }

                    when (showAccountPickerFor) {
                        "main" -> selectedAccount = selected
                        "from" -> {
                            fromAccount = selected
                            if (shouldClearOther) toAccount = null
                        }

                        "to" -> {
                            toAccount = selected
                            if (shouldClearOther) fromAccount = null
                        }
                    }
                    showAccountPickerFor = null
                },
                onDismissRequest = { showAccountPickerFor = null },
                navController = navController
            )
        }

        if (showCategorySheet) {
            ModalBottomSheet(onDismissRequest = { showCategorySheet = false }) {
                val cats = if (transactionType == 0) expenseCategories else incomeCategories
                Column(
                    Modifier
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // [i18n]
                    Text(
                        stringResource(R.string.select_category),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    cats.forEach { cat ->
                        ListItem(
                            leadingContent = { Icon(cat.icon, null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text(cat.title) },
                            modifier = Modifier.clickable { selectedCategory = cat; showCategorySheet = false }
                        )
                    }
                }
            }
        }
    }
}

fun isToday(date: Date): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { time = date }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun FormItem(label: String, value: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FormToggleItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
