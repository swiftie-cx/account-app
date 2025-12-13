package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import com.example.myapplication.ui.navigation.expenseMainCategories
import com.example.myapplication.ui.navigation.incomeMainCategories
import com.example.myapplication.ui.navigation.MainCategory
import com.example.myapplication.ui.navigation.SubCategory
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
            // 获取当前 Tab 对应的大类列表
            val mainList = if (selectedTab == 0) expenseMainCategories else incomeMainCategories

            // 默认选中第一个大类
            if (selectedMainCategory == null || selectedMainCategory !in mainList) {
                selectedMainCategory = mainList.firstOrNull()
            }

            // 默认选中该大类的第一个小类
            if (selectedMainCategory != null) {
                // 只有当当前选中的小类不在当前Tab列表里时，才重置
                // 这样可以避免切换Tab时重置用户已选的有效分类（虽然跨Tab通常不保留）
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

    // [新增] 编辑模式下的回填逻辑 (反查大类)
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

    // --- 计算核心逻辑 ---
    fun recalculateTransfer() {
        if (isManualMode && !isSameCurrency) return

        val feeVal = try { evaluateExpression(transferFeeStr) } catch(e:Exception){0.0}

        if (fromAccount != null && toAccount != null) {
            if (transferMode == TransferMode.SOURCE_FIXED) {
                val fromVal = try { evaluateExpression(fromAmountStr) } catch(e:Exception){0.0}
                val baseInSource = fromVal - feeVal
                val toVal = if (isSameCurrency) {
                    if (baseInSource > 0) baseInSource else 0.0
                } else {
                    ExchangeRates.convert(baseInSource, fromAccount!!.currency, toAccount!!.currency)
                }
                toAmountStr = String.format(Locale.US, "%.2f", if(toVal > 0) toVal else 0.0)
            } else {
                val toVal = try { evaluateExpression(toAmountStr) } catch(e:Exception){0.0}
                val baseInSource = if (isSameCurrency) {
                    toVal
                } else {
                    ExchangeRates.convert(toVal, toAccount!!.currency, fromAccount!!.currency)
                }
                fromAmountStr = String.format(Locale.US, "%.2f", baseInSource + feeVal)
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
        if (selectedTab == 2) {
            when (focusedField) {
                "fee" -> {
                    // 手动模式下，fee 应该被禁用，理论上不会走到这里，但加个判断更安全
                    if (!isManualMode) {
                        transferFeeStr = transform(transferFeeStr)
                        recalculateTransfer()
                    }
                }
                "from" -> {
                    if ((!isManualMode || isSameCurrency) && transferMode != TransferMode.SOURCE_FIXED) {
                        transferMode = TransferMode.SOURCE_FIXED
                    }
                    fromAmountStr = transform(fromAmountStr)
                    recalculateTransfer()
                }
                "to" -> {
                    if ((!isManualMode || isSameCurrency) && transferMode != TransferMode.TARGET_FIXED) {
                        transferMode = TransferMode.TARGET_FIXED
                    }
                    toAmountStr = transform(toAmountStr)
                    recalculateTransfer()
                }
            }
        } else {
            amountStr = transform(amountStr)
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
        AccountPickerDialog(
            accounts = accounts,
            onAccountSelected = { account ->
                if (showAccountPickerFor == "from") fromAccount = account else toAccount = account
                recalculateTransfer()
                showAccountPickerFor = null
            },
            onDismissRequest = { showAccountPickerFor = null },
            navController = navController
        )
    }
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

    // [新增] 小类选择弹窗
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
                    columns = GridCells.Fixed(5), // 一行5个，与原风格保持一致
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
                        // [修改] 顶部金额卡片：传入大类颜色
                        val displayColor = selectedMainCategory?.color ?: MaterialTheme.colorScheme.primary
                        NewAmountDisplay(
                            category = selectedCategory,
                            amount = amountStr,
                            backgroundColor = displayColor // 需要修改 NewAmountDisplay 接受此参数
                        )

                        // [修改] 大类选择网格
                        val currentMainList = if (selectedTab == 0) expenseMainCategories else incomeMainCategories

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4), // 大类一行4个比较合适
                            modifier = Modifier.weight(1f).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentMainList) { mainCat ->
                                MainCategoryItem(
                                    mainCategory = mainCat,
                                    // 只要当前选中的小类属于这个大类，这个大类就是“选中状态”
                                    isSelected = selectedMainCategory == mainCat,
                                    onClick = {
                                        selectedMainCategory = mainCat
                                        // 选中大类时，同时默认选中该大类的第一个小类 (可选体验)
                                        // selectedCategory = mainCat.subCategories.firstOrNull()
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

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SwapVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                enabled = !isManualMode, // 【关键修改】手动模式下禁用
                                onClick = {
                                    focusedField = "fee"
                                    isInputOverwriteMode = true
                                }
                            )

                            // 手动模式开关：仅跨币种显示
                            if (!isSameCurrency) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isManualMode = !isManualMode
                                            // 如果开启手动模式，且当前焦点是手续费，则强制移走焦点
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

                            // 模式选择按钮：只要不是(跨币种且开启了手动模式)，就显示。
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

// --- 组件定义 ---

@Composable
fun FeeInputCard(
    fee: String,
    currency: String,
    isFocused: Boolean,
    enabled: Boolean = true, // 【新增】控制是否可用
    onClick: () -> Unit
) {
    // 根据 enabled 状态调整颜色
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) // 禁用时变淡/灰
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // 禁用时文字变淡
    }
    val border = if (isFocused && enabled) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick), // 禁用时不可点击
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachMoney, null, tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else contentColor)
                Spacer(Modifier.width(8.dp))
                Text("手续费", style = MaterialTheme.typography.titleMedium, color = contentColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fee, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
                if (currency.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSelectionButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SimpleKeyboardToolbar(
    dateMillis: Long,
    onDateClick: () -> Unit,
    remark: String,
    onRemarkClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionChipItem(
            icon = Icons.Default.CalendarToday,
            text = dateFormat.format(Date(dateMillis)),
            onClick = onDateClick,
            modifier = Modifier.weight(1f)
        )
        ActionChipItem(
            icon = if(remark.isNotEmpty()) Icons.AutoMirrored.Filled.Note else Icons.Default.Edit,
            text = if (remark.isNotEmpty()) remark else "添加备注",
            onClick = onRemarkClick,
            modifier = Modifier.weight(1f),
            isHighlight = remark.isNotEmpty()
        )
    }
}

@Composable
fun TransferAccountCard(
    account: Account?,
    label: String,
    displayValue: String,
    amountColor: Color,
    onClick: () -> Unit,
    onCardClick: () -> Unit,
    isFocused: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (account != null) {
                        Icon(
                            imageVector = IconMapper.getIcon(account.iconName),
                            contentDescription = null,
                            tint = amountColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = account?.name ?: "选择",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (account != null) {
                    Text(
                        text = account.currency,
                        style = MaterialTheme.typography.labelSmall,
                        color = amountColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun NewAmountDisplay(
    category: Category?,
    amount: String,
    backgroundColor: Color = MaterialTheme.colorScheme.primary // [新增参数]
) {
    // 使用传入的 backgroundColor，而不是写死的 primary
    val contentColor = Color.White // 假定大类颜色都较深，文字用白色。如果颜色太浅可能需要判断亮度。

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(100.dp),
        shape = RoundedCornerShape(20.dp), // 圆角稍微加大一点
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category != null) {
                // 图标背景加一点半透明白色，增加层次感
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(text = "选择分类", style = MaterialTheme.typography.titleMedium, color = contentColor.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.weight(1f))
            // 自动调整字体大小防止溢出 (简单处理)
            val textStyle = if(amount.length > 8) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall
            Text(
                text = amount,
                style = textStyle,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun KeyboardActionToolbar(
    button1Icon: ImageVector,
    button1Text: String,
    button1OnClick: () -> Unit,
    button1Highlight: Boolean = false,
    dateMillis: Long,
    onDateClick: () -> Unit,
    remark: String,
    onRemarkClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionChipItem(icon = button1Icon, text = button1Text, onClick = button1OnClick, modifier = Modifier.weight(1f), isHighlight = button1Highlight)
        ActionChipItem(icon = Icons.Default.CalendarToday, text = dateFormat.format(Date(dateMillis)), onClick = onDateClick, modifier = Modifier.weight(1f))
        ActionChipItem(icon = if(remark.isNotEmpty()) Icons.AutoMirrored.Filled.Note else Icons.Default.Edit, text = if (remark.isNotEmpty()) remark else "添加备注", onClick = onRemarkClick, modifier = Modifier.weight(1.5f), isHighlight = remark.isNotEmpty())
    }
}

@Composable
fun ActionChipItem(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isHighlight: Boolean = false) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = modifier.height(40.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun RemarkInputDialog(initialRemark: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialRemark) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("添加备注") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("请输入备注内容...") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun CategoryGrid(categories: List<Category>, selectedCategory: Category?, onCategoryClick: (Category) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        gridItems(categories) { category -> CategoryItem(category = category, isSelected = category == selectedCategory, onClick = { onCategoryClick(category) }) }
    }
}

@Composable
fun CategoryItem(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp).aspectRatio(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(backgroundColor), contentAlignment = Alignment.Center) {
            Icon(imageVector = category.icon, contentDescription = category.title, tint = contentColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = category.title, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified)
    }
}

@Composable
fun AccountPickerDialog(accounts: List<Account>, onAccountSelected: (Account) -> Unit, onDismissRequest: () -> Unit, navController: NavHostController) {
    AlertDialog(
        onDismissRequest = onDismissRequest, title = { Text("选择账户") },
        text = { LazyColumn { items(accounts.size) { index -> val account = accounts[index]; Row(modifier = Modifier.fillMaxWidth().clickable { onAccountSelected(account) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { val icon = IconMapper.getIcon(account.iconName); Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp)); Text(account.name) } } } },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("取消") } },
        dismissButton = { TextButton(onClick = { navController.navigate("account_management"); onDismissRequest() }) { Text("账户管理") } }
    )
}

fun evaluateExpression(expression: String): Double {
    val tokens = expression.split(" ")
    if (tokens.isEmpty()) return 0.0
    var result = tokens[0].toDoubleOrNull() ?: 0.0
    for (i in 1 until tokens.size step 2) {
        if (i + 1 >= tokens.size) break
        val operator = tokens[i]
        val nextOperand = tokens[i + 1].toDoubleOrNull() ?: 0.0
        if (operator == "+") result += nextOperand else if (operator == "-") result -= nextOperand
    }
    return result
}

@Composable
fun MainCategoryItem(mainCategory: MainCategory, isSelected: Boolean, onClick: () -> Unit) {
    // 选中状态：颜色鲜艳；未选中：灰色
    val bgColor = if (isSelected) mainCategory.color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val contentColor = if (isSelected) mainCategory.color else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (isSelected) BorderStroke(1.dp, mainCategory.color.copy(alpha = 0.5f)) else null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(bgColor)
            .then(if (border != null) Modifier.padding(1.dp) else Modifier) // 防止边框占位跳动
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = mainCategory.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = mainCategory.title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun SubCategoryItem(
    subCategory: SubCategory,
    mainColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 小类选中样式：实心背景色
    val backgroundColor = if (isSelected) mainColor else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp) // 小类图标稍微大一点方便点击
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = subCategory.icon,
                contentDescription = subCategory.title,
                tint = if(isSelected) contentColor else mainColor, // 未选中时图标用大类颜色
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subCategory.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if(isSelected) mainColor else Color.Unspecified
        )
    }
}