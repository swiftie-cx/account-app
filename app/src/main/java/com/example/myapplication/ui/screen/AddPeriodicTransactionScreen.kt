package com.example.myapplication.ui.screen

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.PeriodicTransaction
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    // 焦点请求器
    val amountFocusRequester = remember { FocusRequester() }

    var remark by remember { mutableStateOf("") }
    // 【修改】移除了 excludeFromStats 状态变量，默认视为 false
    var excludeFromBudget by remember { mutableStateOf(false) }

    // 对象选择
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    // 弹窗控制
    var showFrequencySheet by remember { mutableStateOf(false) }
    var showEndRepeatSheet by remember { mutableStateOf(false) }
    var showCountInputDialog by remember { mutableStateOf(false) }
    var showAccountPickerFor by remember { mutableStateOf<String?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }

    // --- 日期和时间选择器 ---
    val calendar = Calendar.getInstance()

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
        startDatePicker.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(positiveColor)
        startDatePicker.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(negativeColor)
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
        timePicker.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(positiveColor)
        timePicker.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(negativeColor)
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
        endDatePicker.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(positiveColor)
        endDatePicker.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(negativeColor)
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
                remark = item.remark ?: ""
                // 【修改】不再加载 excludeFromStats
                excludeFromBudget = item.excludeFromBudget

                if (item.type != 2) {
                    val allCats = if(item.type == 0) expenseCategories else incomeCategories
                    selectedCategory = allCats.find { it.title == item.category }
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
                if (toAccount == null) toAccount = accounts.first()
            }
            if (selectedCategory == null && transactionType != 2) {
                val cats = if (transactionType == 0) expenseCategories else incomeCategories
                selectedCategory = cats.firstOrNull()
            }
        }
    }

    // --- 保存逻辑 ---
    fun save() {
        val amountVal = amount.toDoubleOrNull() ?: 0.0
        if (amountVal <= 0) return

        // 强制将秒和毫秒清零
        val cal = Calendar.getInstance()
        cal.time = startDate
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val cleanStartDate = cal.time

        val transaction = PeriodicTransaction(
            id = if (periodicId != null && periodicId != -1L) periodicId else 0,
            type = transactionType,
            amount = amountVal,
            category = if(transactionType == 2) "转账" else (selectedCategory?.title ?: "其他"),
            accountId = if(transactionType == 2) (fromAccount?.id ?: 0) else (selectedAccount?.id ?: 0),
            targetAccountId = if(transactionType == 2) toAccount?.id else null,
            frequency = frequency,
            startDate = cleanStartDate,

            // 下次执行时间初始值
            nextExecutionDate = cleanStartDate,

            endMode = endMode,
            endDate = if (endMode == END_MODE_DATE) endDate else null,
            endCount = if (endMode == END_MODE_COUNT) endCount else null,
            remark = remark,
            excludeFromStats = false, // 【修改】强制设为 false，因为界面入口已移除
            excludeFromBudget = excludeFromBudget
        )

        if (periodicId != null && periodicId != -1L) {
            viewModel.updatePeriodic(transaction)
        } else {
            viewModel.insertPeriodic(transaction)
        }
        navController.popBackStack()
    }

    // --- UI 渲染 ---
    val bgColor = MaterialTheme.colorScheme.surfaceContainerLow
    val cardColor = MaterialTheme.colorScheme.surface

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设置重复", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "取消")
                    }
                },
                actions = {
                    IconButton(onClick = { save() }) {
                        Icon(Icons.Default.Check, "保存")
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
                    val freqLabel = listOf("每天", "每周", "每月", "每年").getOrElse(frequency) { "每月" }
                    FormItem(label = "重复周期", value = freqLabel, onClick = { showFrequencySheet = true })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    val endLabel = when(endMode) {
                        END_MODE_NEVER -> "永不结束"
                        END_MODE_DATE -> endDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: "选择日期"
                        END_MODE_COUNT -> "${endCount ?: 0} 次后结束"
                        else -> "永不结束"
                    }
                    FormItem(label = "结束重复", value = endLabel, onClick = { showEndRepeatSheet = true })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    val startLabel = if (isToday(startDate)) "今天" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate)
                    FormItem(label = "生效日期", value = startLabel, onClick = { startDatePicker.show() })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(startDate)
                    FormItem(label = "账单时间", value = timeLabel, onClick = { timePicker.show() })
                }
            }

            // --- 第二组：账单详情 ---
            Card(colors = CardDefaults.cardColors(containerColor = cardColor)) {
                Column {
                    if (transactionType != 2) {
                        FormItem(
                            label = "分类",
                            value = selectedCategory?.title ?: "请选择",
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
                            text = "金额",
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

                    if (transactionType == 2) {
                        FormItem(label = "转出账户", value = fromAccount?.name ?: "选择账户", onClick = { showAccountPickerFor = "from" })
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        FormItem(label = "转入账户", value = toAccount?.name ?: "选择账户", onClick = { showAccountPickerFor = "to" })
                    } else {
                        FormItem(label = "账户", value = selectedAccount?.name ?: "选择账户", onClick = { showAccountPickerFor = "main" })
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("备注", style = MaterialTheme.typography.bodyLarge)
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
                                        Text(
                                            text = "输入备注",
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

            if (transactionType != 2) {
                Card(colors = CardDefaults.cardColors(containerColor = cardColor)) {
                    Column {
                        // 【修改】移除了“不计入收支”选项及其分割线
                        FormToggleItem(label = "不计入预算", checked = excludeFromBudget, onCheckedChange = { excludeFromBudget = it })
                    }
                }
            }
        }

        // --- 弹窗组件 ---

        if (showFrequencySheet) {
            ModalBottomSheet(onDismissRequest = { showFrequencySheet = false }) {
                Column(Modifier.padding(bottom = 32.dp)) {
                    listOf("每天", "每周", "每月", "每年").forEachIndexed { index, label ->
                        ListItem(
                            headlineContent = { Text(label) },
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
                    ListItem(
                        headlineContent = { Text("永不结束", style = MaterialTheme.typography.titleMedium) },
                        trailingContent = { if (endMode == END_MODE_NEVER) Icon(Icons.Default.Check, null) },
                        modifier = Modifier.clickable {
                            endMode = END_MODE_NEVER
                            showEndRepeatSheet = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("按日期结束重复", style = MaterialTheme.typography.titleMedium) },
                        trailingContent = { if (endMode == END_MODE_DATE) Icon(Icons.Default.Check, null) },
                        modifier = Modifier.clickable {
                            showEndRepeatSheet = false
                            endDatePicker.show()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("按次数结束重复", style = MaterialTheme.typography.titleMedium) },
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
                title = { Text("输入重复次数") },
                text = {
                    OutlinedTextField(
                        value = tempCount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tempCount = it },
                        label = { Text("次数") },
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
                    ) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { showCountInputDialog = false }) { Text("取消") } }
            )
        }

        if (showAccountPickerFor != null) {
            AccountPickerDialog(
                accounts = accounts,
                onAccountSelected = {
                    when(showAccountPickerFor) {
                        "main" -> selectedAccount = it
                        "from" -> fromAccount = it
                        "to" -> toAccount = it
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
                Column(Modifier.padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
                    Text("选择分类", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
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