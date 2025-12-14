package com.example.myapplication.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import com.example.myapplication.ui.navigation.expenseMainCategories
import com.example.myapplication.ui.navigation.incomeMainCategories
import com.example.myapplication.ui.navigation.MainCategory

// 转账计算模式
enum class TransferMode {
    SOURCE_FIXED, // 转出固定 (含手续费) -> 输入转出，算转入
    TARGET_FIXED  // 转入固定 (额外手续费) -> 输入转入，算转出
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    expenseId: Long? = null,
    dateMillis: Long? = null,
    initialTab: Int = 0
) {
    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("支出", "收入", "转账")

    // --- 基础状态 ---
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedDate by remember { mutableLongStateOf(dateMillis ?: System.currentTimeMillis()) }
    val remarks = remember { mutableStateListOf("", "", "") }

    // --- 账户数据 ---
    val defaultAccountId by viewModel.defaultAccountId.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }

    // --- 弹窗控制 ---
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showRemarkDialog by remember { mutableStateOf(false) }
    var showAccountPickerFor by remember { mutableStateOf<String?>(null) }

    // --- 金额状态 ---
    var amountStr by remember { mutableStateOf("0") }

    // --- 转账专用状态 ---
    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }

    // 独立状态
    var fromAmountStr by remember { mutableStateOf("0") } // 转出(扣款)
    var toAmountStr by remember { mutableStateOf("0") }   // 转入(到账)
    var transferFeeStr by remember { mutableStateOf("0") } // 手续费

    // 焦点与模式
    var focusedField by remember { mutableStateOf("amount") } // amount, from, to, fee
    var transferMode by remember { mutableStateOf(TransferMode.SOURCE_FIXED) }

    // 覆盖模式 (全选效果)
    var isInputOverwriteMode by remember { mutableStateOf(false) }

    // 手动模式 (断开汇率自动计算)
    var isManualMode by remember { mutableStateOf(false) }

    // [新增] 大类相关状态
    var selectedMainCategory by remember { mutableStateOf<MainCategory?>(null) }
    var showSubCategorySheet by remember { mutableStateOf(false) }

    // 辅助判断是否同币种
    val isSameCurrency = remember(fromAccount, toAccount) {
        val from = fromAccount
        val to = toAccount
        from != null && to != null && from.currency == to.currency
    }

    // --- 辅助数据 ---
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // 1. 回填逻辑
    LaunchedEffect(key1 = expenseId, key2 = expenses, key3 = accounts) {
        if (expenseId != null) {
            val expenseToEdit = expenses.find { it.id == expenseId }
            if (expenseToEdit != null) {
                selectedTab = if (expenseToEdit.category.startsWith("转账")) 2 else if (expenseToEdit.amount < 0) 0 else 1
                if (selectedTab != 2) {
                    selectedCategory = (expenseCategories + incomeCategories).find { it.title == expenseToEdit.category }
                    amountStr = abs(expenseToEdit.amount).toString()
                    selectedDate = expenseToEdit.date.time
                    selectedAccount = accounts.find { it.id == expenseToEdit.accountId }
                    remarks[selectedTab] = expenseToEdit.remark ?: ""
                } else {
                    selectedDate = expenseToEdit.date.time
                    fromAccount = accounts.find { it.id == expenseToEdit.accountId }
                    fromAmountStr = abs(expenseToEdit.amount).toString()
                    remarks[selectedTab] = expenseToEdit.remark ?: ""
                }
            }
        }
    }

    // 2. 默认账户
    LaunchedEffect(accounts, defaultAccountId) {
        if (selectedAccount == null) {
            val defaultAcc = accounts.find { it.id == defaultAccountId }
            selectedAccount = defaultAcc ?: accounts.firstOrNull()
        }
        if (expenseId == null && fromAccount == null && accounts.isNotEmpty()) {
            val defaultAcc = accounts.find { it.id == defaultAccountId }
            fromAccount = defaultAcc ?: accounts.firstOrNull()
            toAccount = accounts.firstOrNull { it.id != defaultAcc?.id }
        }
    }

    // 3. Tab 切换
    LaunchedEffect(selectedTab, expenseId) {
        if (expenseId == null) {
            val mainList = if (selectedTab == 0) expenseMainCategories else incomeMainCategories

            if (selectedMainCategory == null || selectedMainCategory !in mainList) {
                selectedMainCategory = mainList.firstOrNull()
            }

            if (selectedMainCategory != null) {
                val allSubInTab = mainList.flatMap { it.subCategories }
                if (selectedCategory == null || selectedCategory !in allSubInTab) {
                    selectedCategory = selectedMainCategory!!.subCategories.firstOrNull()
                }
            }
            if (amountStr == "0") amountStr = "0"
            if (fromAmountStr == "0") fromAmountStr = "0"
            if (toAmountStr == "0") toAmountStr = "0"
            if (transferFeeStr == "0") transferFeeStr = "0"

            if (selectedTab == 2) {
                focusedField = "from"
                transferMode = TransferMode.SOURCE_FIXED
                isInputOverwriteMode = true
            } else {
                focusedField = "amount"
                isInputOverwriteMode = false
            }
        }
    }

    LaunchedEffect(expenseId, selectedCategory) {
        if (expenseId != null && selectedCategory != null) {
            val mainList = if (selectedTab == 0) expenseMainCategories else incomeMainCategories
            val foundMain = mainList.find { main ->
                main.subCategories.any { sub -> sub.title == selectedCategory!!.title }
            }
            if (foundMain != null) {
                selectedMainCategory = foundMain
            }
        }
    }
    fun smartFormat(value: Double): String {
        // 判断是否为整数 (允许极小的浮点误差)
        val isInteger = abs(value - value.toLong()) < 0.001
        return if (isInteger) {
            value.toLong().toString() // 如果是整数，直接转 Long 显示 (例如 "100")
        } else {
            String.format(Locale.US, "%.2f", value) // 如果有小数，保留两位 (例如 "100.50")
        }
    }
    // --- 计算核心逻辑 ---
    fun recalculateTransfer() {
        if (isManualMode && !isSameCurrency) return

        val feeVal = try { evaluateExpression(transferFeeStr) } catch(e:Exception){0.0}

        if (fromAccount != null && toAccount != null) {
            if (transferMode == TransferMode.SOURCE_FIXED) {
                // 模式1：转出固定 -> 算转入
                val fromVal = try { evaluateExpression(fromAmountStr) } catch(e:Exception){0.0}

                // 计算净转出额 (扣除手续费)
                val baseInSource = fromVal - feeVal

                val toVal = if (isSameCurrency) {
                    if (baseInSource > 0) baseInSource else 0.0
                } else {
                    ExchangeRates.convert(baseInSource, fromAccount!!.currency, toAccount!!.currency)
                }

                // 【修改】传入目标账户的币种，进行智能格式化
                toAmountStr = smartFormat(
                    value = if(toVal > 0) toVal else 0.0,
                    currencyCode = toAccount!!.currency
                )

            } else {
                // 模式2：转入固定 -> 算转出
                val toVal = try { evaluateExpression(toAmountStr) } catch(e:Exception){0.0}

                val baseInSource = if (isSameCurrency) {
                    toVal
                } else {
                    ExchangeRates.convert(toVal, toAccount!!.currency, fromAccount!!.currency)
                }

                // 【修改】传入来源账户的币种，进行智能格式化
                fromAmountStr = smartFormat(
                    value = baseInSource + feeVal,
                    currencyCode = fromAccount!!.currency
                )
            }
        }
    }

    LaunchedEffect(transferMode) {
        if (selectedTab == 2) {
            if (transferMode == TransferMode.SOURCE_FIXED) {
                focusedField = "from"
            } else {
                focusedField = "to"
            }
            isInputOverwriteMode = true
            recalculateTransfer()
        }
    }

    fun handleInput(transform: (String) -> String) {
        // 1. 确定当前上下文的货币代码
        val currentCurrency = if (selectedTab == 2) {
            when (focusedField) {
                "from", "fee" -> fromAccount?.currency ?: ""
                "to" -> toAccount?.currency ?: ""
                else -> ""
            }
        } else {
            selectedAccount?.currency ?: ""
        }

        // 2. 获取当前的目标变量引用 (用于读取旧值和更新)
        // 使用一个 getter/setter 包装器来简化逻辑
        var currentValue = ""
        var updateValue: (String) -> Unit = {}

        if (selectedTab == 2) {
            when (focusedField) {
                "fee" -> {
                    if (isManualMode) return // 手动模式下禁用手续费输入(或根据需求)
                    currentValue = transferFeeStr
                    updateValue = { transferFeeStr = it; recalculateTransfer() }
                }
                "from" -> {
                    if ((!isManualMode || isSameCurrency) && transferMode != TransferMode.SOURCE_FIXED) {
                        transferMode = TransferMode.SOURCE_FIXED
                    }
                    currentValue = fromAmountStr
                    updateValue = { fromAmountStr = it; recalculateTransfer() }
                }
                "to" -> {
                    if ((!isManualMode || isSameCurrency) && transferMode != TransferMode.TARGET_FIXED) {
                        transferMode = TransferMode.TARGET_FIXED
                    }
                    currentValue = toAmountStr
                    updateValue = { toAmountStr = it; recalculateTransfer() }
                }
            }
        } else {
            currentValue = amountStr
            updateValue = { amountStr = it }
        }

        // 3. 计算新值
        val newValue = transform(currentValue)

        // 4. 【关键】验证精度
        // 如果是删除操作(变短)，或者是合法的精度，或者是中间算式(包含+-符号)，则允许更新
        // 注意：validateInputPrecision 只处理纯数字格式，如果包含运算符则放行(交给 evaluateExpression 处理)
        val isEquation = newValue.contains("+") || newValue.contains("-")

        if (isEquation || newValue.length < currentValue.length || validateInputPrecision(newValue, currentCurrency)) {
            updateValue(newValue)
        }
    }

    fun saveExpense(shouldFinish: Boolean) {
        if (selectedTab == 2) {
            val fromVal = try { evaluateExpression(fromAmountStr) } catch(e:Exception){0.0}
            val toVal = try { evaluateExpression(toAmountStr) } catch(e:Exception){0.0}

            if (fromAccount != null && toAccount != null && fromVal > 0 && fromAccount != toAccount) {
                viewModel.createTransfer(
                    fromAccountId = fromAccount!!.id,
                    toAccountId = toAccount!!.id,
                    fromAmount = fromVal,
                    toAmount = toVal,
                    date = Date(selectedDate)
                )
                navController.popBackStack()
            }
        } else {
            if (selectedCategory != null && selectedAccount != null && amountStr != "0") {
                val finalAmountStr = try { evaluateExpression(amountStr).toString() } catch(e: Exception) { amountStr }
                val rawAmount = finalAmountStr.toDoubleOrNull() ?: 0.0
                if (rawAmount == 0.0) return

                val finalAmount = if (selectedTab == 0) -rawAmount else rawAmount
                val expense = Expense(
                    id = expenseId ?: 0,
                    category = selectedCategory!!.title,
                    amount = finalAmount,
                    date = Date(selectedDate),
                    accountId = selectedAccount!!.id,
                    remark = remarks[selectedTab].takeIf { it.isNotBlank() }
                )

                if (expenseId != null && shouldFinish) {
                    viewModel.updateExpense(expense)
                } else {
                    viewModel.insert(expense)
                }

                if (shouldFinish) {
                    navController.popBackStack()
                } else {
                    amountStr = "0"
                    remarks[selectedTab] = ""
                }
            }
        }
    }

    // --- UI ---
    if (showAccountPickerFor != null) {
        // 【修改】不过滤，传入所有账户
        AccountPickerDialog(
            accounts = accounts,
            onAccountSelected = { selected ->
                when(showAccountPickerFor) {
                    "main" -> selectedAccount = selected
                    "from" -> {
                        fromAccount = selected
                        // 【逻辑】若转出==转入，则清空转入
                        if (fromAccount?.id == toAccount?.id) {
                            toAccount = null
                            toAmountStr = "0"
                        }
                    }
                    "to" -> {
                        toAccount = selected
                        // 【逻辑】若转入==转出，则清空转出
                        if (toAccount?.id == fromAccount?.id) {
                            fromAccount = null
                            fromAmountStr = "0"
                        }
                    }
                }

                if (showAccountPickerFor == "from" || showAccountPickerFor == "to") {
                    // 【关键修复】切换账户后，立即对已有金额进行精度修正
                    // 1. 清洗转出金额 (如果转出账户切到了日元，原来的 10.5 会变成 11)
                    if (fromAccount != null) {
                        val currentFromVal = try { fromAmountStr.toDouble() } catch(e: Exception) { 0.0 }
                        fromAmountStr = smartFormat(currentFromVal, fromAccount!!.currency)
                    }

                    // 2. 清洗转入金额 (同理)
                    if (toAccount != null) {
                        val currentToVal = try { toAmountStr.toDouble() } catch(e: Exception) { 0.0 }
                        toAmountStr = smartFormat(currentToVal, toAccount!!.currency)
                    }

                    // 3. 清洗手续费 (手续费通常跟随转出账户币种)
                    if (fromAccount != null) {
                        val currentFeeVal = try { transferFeeStr.toDouble() } catch(e: Exception) { 0.0 }
                        transferFeeStr = smartFormat(currentFeeVal, fromAccount!!.currency)
                    }

                    // 4. 最后再触发汇率重算，确保关联的一方金额也正确更新
                    recalculateTransfer()
                }

                showAccountPickerFor = null
            },
            onDismissRequest = { showAccountPickerFor = null },
            navController = navController
        )
    }

    // ... 其他弹窗保持不变 ...
    if (showAccountPicker) {
        AccountPickerDialog(
            accounts = accounts,
            onAccountSelected = { selectedAccount = it; showAccountPicker = false },
            onDismissRequest = { showAccountPicker = false },
            navController = navController
        )
    }
    if (showDatePicker) {
        CustomDateRangePicker(
            initialStartDate = selectedDate,
            initialEndDate = null,
            isSingleSelection = true,
            onConfirm = { startDate, _ -> if (startDate != null) selectedDate = startDate; showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showRemarkDialog) {
        RemarkInputDialog(
            initialRemark = remarks[selectedTab],
            onConfirm = { remarks[selectedTab] = it; showRemarkDialog = false },
            onDismiss = { showRemarkDialog = false }
        )
    }

    if (showSubCategorySheet && selectedMainCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { showSubCategorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = selectedMainCategory!!.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = selectedMainCategory!!.color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedMainCategory!!.subCategories) { sub ->
                        SubCategoryItem(
                            subCategory = sub,
                            mainColor = selectedMainCategory!!.color,
                            isSelected = selectedCategory?.title == sub.title,
                            onClick = {
                                selectedCategory = sub
                                showSubCategorySheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (expenseId == null) "记一笔" else "编辑") },
                navigationIcon = { TextButton(onClick = { navController.popBackStack() }) { Text("取消") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (expenseId == null) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                    }
                }
            }

            when (selectedTab) {
                0, 1 -> { // 支出/收入
                    Column(modifier = Modifier.fillMaxSize()) {
                        val displayColor = selectedMainCategory?.color ?: MaterialTheme.colorScheme.primary
                        NewAmountDisplay(
                            category = selectedCategory,
                            amount = amountStr,
                            backgroundColor = displayColor
                        )

                        val currentMainList = if (selectedTab == 0) expenseMainCategories else incomeMainCategories

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.weight(1f).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentMainList) { mainCat ->
                                MainCategoryItem(
                                    mainCategory = mainCat,
                                    isSelected = selectedMainCategory == mainCat,
                                    onClick = {
                                        selectedMainCategory = mainCat
                                        showSubCategorySheet = true
                                    }
                                )
                            }
                        }
                        KeyboardActionToolbar(
                            button1Icon = if (selectedAccount != null) IconMapper.getIcon(selectedAccount!!.iconName) else Icons.Default.CalendarToday,
                            button1Text = selectedAccount?.name ?: "选择账户",
                            button1OnClick = { showAccountPicker = true },
                            dateMillis = selectedDate,
                            onDateClick = { showDatePicker = true },
                            remark = remarks[selectedTab],
                            onRemarkClick = { showRemarkDialog = true }
                        )
                        NumericKeyboard(
                            onNumberClick = { num ->
                                handleInput { current ->
                                    if (current == "0") {
                                        if (num == ".") "0." else num
                                    } else {
                                        current + num
                                    }
                                }
                            },
                            onOperatorClick = { op -> handleInput { it + " $op " } },
                            onBackspaceClick = { handleInput { if (it.length > 1) it.dropLast(1) else "0" } },
                            onAgainClick = { saveExpense(false) },
                            onDoneClick = { saveExpense(true) },
                            onEqualsClick = { handleInput { try { evaluateExpression(it).toString() } catch (e: Exception) { "Error" } } },
                            isCalculation = amountStr.contains("+") || amountStr.contains("-"),
                            selectedDate = null
                        )
                    }
                }
                2 -> { // 转账
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TransferAccountCard(
                                account = fromAccount,
                                label = "转出",
                                displayValue = fromAmountStr,
                                amountColor = Color(0xFFE53935),
                                onClick = { showAccountPickerFor = "from" },
                                isFocused = focusedField == "from",
                                onCardClick = {
                                    focusedField = "from"
                                    transferMode = TransferMode.SOURCE_FIXED
                                    isInputOverwriteMode = true
                                }
                            )

                            // 【修改】添加了点击互换功能
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // 互换逻辑
                                        val tempAccount = fromAccount
                                        fromAccount = toAccount
                                        toAccount = tempAccount

                                        val tempAmount = fromAmountStr
                                        fromAmountStr = toAmountStr
                                        toAmountStr = tempAmount

                                        recalculateTransfer()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SwapVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            TransferAccountCard(
                                account = toAccount,
                                label = "转入",
                                displayValue = toAmountStr,
                                amountColor = Color(0xFF4CAF50),
                                onClick = { showAccountPickerFor = "to" },
                                isFocused = focusedField == "to",
                                onCardClick = {
                                    focusedField = "to"
                                    transferMode = TransferMode.TARGET_FIXED
                                    isInputOverwriteMode = true
                                }
                            )

                            FeeInputCard(
                                fee = transferFeeStr,
                                currency = fromAccount?.currency ?: "",
                                isFocused = focusedField == "fee",
                                enabled = !isManualMode,
                                onClick = {
                                    focusedField = "fee"
                                    isInputOverwriteMode = true
                                }
                            )

                            if (!isSameCurrency) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isManualMode = !isManualMode
                                            if (isManualMode && focusedField == "fee") {
                                                focusedField = "from"
                                            }
                                            if (!isManualMode) recalculateTransfer()
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isManualMode,
                                        onCheckedChange = {
                                            isManualMode = it
                                            if (it && focusedField == "fee") {
                                                focusedField = "from"
                                            }
                                            if (!it) recalculateTransfer()
                                        }
                                    )
                                    Text(
                                        text = "手动修改金额 (不自动换算)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (!(!isSameCurrency && isManualMode)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ModeSelectionButton(
                                        text = "转出固定(含手续费)",
                                        isSelected = transferMode == TransferMode.SOURCE_FIXED,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            transferMode = TransferMode.SOURCE_FIXED
                                            focusedField = "from"
                                            isInputOverwriteMode = true
                                            recalculateTransfer()
                                        }
                                    )
                                    ModeSelectionButton(
                                        text = "转入固定(额外手续费)",
                                        isSelected = transferMode == TransferMode.TARGET_FIXED,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            transferMode = TransferMode.TARGET_FIXED
                                            focusedField = "to"
                                            isInputOverwriteMode = true
                                            recalculateTransfer()
                                        }
                                    )
                                }
                            }
                        }

                        if (fromAccount != null && toAccount != null) {
                            SimpleKeyboardToolbar(
                                dateMillis = selectedDate,
                                onDateClick = { showDatePicker = true },
                                remark = remarks[selectedTab],
                                onRemarkClick = { showRemarkDialog = true }
                            )

                            NumericKeyboard(
                                onNumberClick = { num ->
                                    if (isInputOverwriteMode) {
                                        val newValue = if (num == ".") "0." else num
                                        handleInput { _ -> newValue }
                                        isInputOverwriteMode = false
                                    } else {
                                        handleInput { current ->
                                            if (current == "0") {
                                                if (num == ".") "0." else num
                                            } else {
                                                current + num
                                            }
                                        }
                                    }
                                },
                                onOperatorClick = { op ->
                                    isInputOverwriteMode = false
                                    handleInput { it + " $op " }
                                },
                                onBackspaceClick = {
                                    if (isInputOverwriteMode) {
                                        isInputOverwriteMode = false
                                        handleInput { "0" }
                                    } else {
                                        handleInput { if (it.length > 1) it.dropLast(1) else "0" }
                                    }
                                },
                                onAgainClick = null,
                                onDoneClick = { saveExpense(true) },
                                onEqualsClick = { handleInput { try { evaluateExpression(it).toString() } catch (e: Exception) { "Error" } } },
                                isCalculation = (if(focusedField=="fee") transferFeeStr else if(focusedField=="to") toAmountStr else fromAmountStr).let { it.contains("+")||it.contains("-") },
                                selectedDate = selectedDate
                            )
                        }
                    }
                }
            }
        }
    }
}