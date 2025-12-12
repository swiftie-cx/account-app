package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.ui.screen.CustomDateRangePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// 转账手续费模式枚举
enum class TransferFeeMode {
    INCLUDE, // 扣款 = 输入值 (到账 = 输入 - 手续费)
    EXTRA    // 到账 = 输入值 (扣款 = 输入 + 手续费)
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
    // 初始Tab
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("支出", "收入", "转账")

    // --- 状态管理 ---
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("0") } // 在转账模式下，这代表“输入的主金额”
    var selectedDate by remember { mutableLongStateOf(dateMillis ?: System.currentTimeMillis()) }

    // 备注改为列表，分别对应 [支出, 收入, 转账]
    val remarks = remember { mutableStateListOf("", "", "") }

    val defaultAccountId by viewModel.defaultAccountId.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showRemarkDialog by remember { mutableStateOf(false) }

    // --- 转账专用状态 ---
    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var transferFee by remember { mutableStateOf("0") } // 手续费
    // 新增：手续费计算模式
    var feeMode by remember { mutableStateOf(TransferFeeMode.INCLUDE) }

    // 焦点控制：支持 "amount" (主金额), "fee" (手续费)
    // 注意：转账模式下，我们复用 `amount` 作为主输入框的值，不再区分 fromAmount/toAmount 字符串，而是通过逻辑计算
    var focusedField by remember { mutableStateOf("amount") }

    var showAccountPickerFor by remember { mutableStateOf<String?>(null) }

    // 计算逻辑判断
    val isCalculation = remember(selectedTab, amount, transferFee, focusedField) {
        val targetAmount = if (focusedField == "fee") transferFee else amount
        targetAmount.contains("+") || targetAmount.contains("-")
    }

    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // 编辑模式回填数据
    LaunchedEffect(key1 = expenseId, key2 = expenses, key3 = accounts) {
        if (expenseId != null) {
            val expenseToEdit = expenses.find { it.id == expenseId }
            if (expenseToEdit != null) {
                selectedTab = if (expenseToEdit.category.startsWith("转账")) 2 else if (expenseToEdit.amount < 0) 0 else 1
                if (selectedTab != 2) {
                    selectedCategory = (expenseCategories + incomeCategories).find { it.title == expenseToEdit.category }
                    amount = abs(expenseToEdit.amount).toString()
                    selectedDate = expenseToEdit.date.time
                    selectedAccount = accounts.find { it.id == expenseToEdit.accountId }
                    remarks[selectedTab] = expenseToEdit.remark ?: ""
                }
            }
        }
    }

    // 默认账户初始化
    LaunchedEffect(accounts, defaultAccountId) {
        if (selectedAccount == null) {
            if (expenseId == null) {
                val defaultAcc = accounts.find { it.id == defaultAccountId }
                selectedAccount = defaultAcc ?: accounts.firstOrNull()
            }
        }
        if (expenseId == null && fromAccount == null && accounts.isNotEmpty()) {
            val defaultAcc = accounts.find { it.id == defaultAccountId }
            fromAccount = defaultAcc ?: accounts.firstOrNull()
            // 尝试找一个不同的账户作为接收方，如果没有则为空
            toAccount = accounts.firstOrNull { it.id != defaultAcc?.id }
        }
    }

    // Tab 切换重置逻辑
    LaunchedEffect(selectedTab) {
        if (expenseId == null) {
            val currentList = if (selectedTab == 0) expenseCategories else incomeCategories
            if (selectedCategory == null || selectedCategory !in currentList) {
                selectedCategory = currentList.firstOrNull()
            }
            if (amount == "0") amount = "0"
            if (selectedAccount == null) {
                val defaultAcc = accounts.find { it.id == defaultAccountId }
                selectedAccount = defaultAcc ?: accounts.firstOrNull()
            }
            // 转账模式默认焦点在主金额
            if (selectedTab == 2) {
                focusedField = "amount"
            }
        }
    }

    // --- 输入处理 ---
    fun handleInput(transform: (String) -> String) {
        if (selectedTab == 2 && focusedField == "fee") {
            transferFee = transform(transferFee)
        } else {
            amount = transform(amount)
        }
    }

    // --- 保存逻辑 ---
    fun saveExpense(shouldFinish: Boolean) {
        if (selectedCategory != null && selectedAccount != null && amount != "0") {
            val finalAmountStr = try { evaluateExpression(amount).toString() } catch(e: Exception) { amount }
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
                amount = "0"
                remarks[selectedTab] = ""
            }
        }
    }

    // 转账完成逻辑
    val onTransferDoneClick: () -> Unit = {
        val inputValStr = try { evaluateExpression(amount).toString() } catch(e: Exception) { amount }
        val feeValStr = try { evaluateExpression(transferFee).toString() } catch(e: Exception) { transferFee }

        val inputValue = inputValStr.toDoubleOrNull() ?: 0.0
        val feeValue = feeValStr.toDoubleOrNull() ?: 0.0

        // 根据模式计算最终的转出和转入金额
        val finalFromAmount = if (feeMode == TransferFeeMode.INCLUDE) inputValue else inputValue + feeValue
        val finalToAmount = if (feeMode == TransferFeeMode.INCLUDE) inputValue - feeValue else inputValue

        if (fromAccount != null && toAccount != null && finalFromAmount > 0 && finalToAmount >= 0 && fromAccount != toAccount) {
            viewModel.createTransfer(
                fromAccountId = fromAccount!!.id,
                toAccountId = toAccount!!.id,
                fromAmount = finalFromAmount,
                toAmount = finalToAmount,
                date = Date(selectedDate)
            )
            navController.popBackStack()
        }
    }

    // --- 弹窗组件 ---

    if (showAccountPickerFor != null) {
        AccountPickerDialog(
            accounts = accounts,
            onAccountSelected = { account ->
                if (showAccountPickerFor == "from") fromAccount = account else toAccount = account
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
            onConfirm = { startDate, _ ->
                if (startDate != null) selectedDate = startDate
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showRemarkDialog) {
        RemarkInputDialog(
            initialRemark = remarks[selectedTab],
            onConfirm = { newRemark ->
                remarks[selectedTab] = newRemark
                showRemarkDialog = false
            },
            onDismiss = { showRemarkDialog = false }
        )
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
                0, 1 -> { // 支出 和 收入
                    val categoriesToShow = if (selectedTab == 0) expenseCategories else incomeCategories

                    Column(modifier = Modifier.fillMaxSize()) {
                        NewAmountDisplay(
                            category = selectedCategory,
                            amount = amount
                        )

                        CategoryGrid(
                            categories = categoriesToShow,
                            selectedCategory = selectedCategory,
                            onCategoryClick = { clickedCategory -> selectedCategory = clickedCategory },
                            modifier = Modifier.weight(1f)
                        )

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
                            onNumberClick = { num -> handleInput { if (it == "0") num else it + num } },
                            onOperatorClick = { op -> handleInput { it + " $op " } },
                            onBackspaceClick = { handleInput { if (it.length > 1) it.dropLast(1) else "0" } },
                            onAgainClick = { saveExpense(shouldFinish = false) },
                            onDoneClick = { saveExpense(shouldFinish = true) },
                            onEqualsClick = {
                                handleInput {
                                    try {
                                        evaluateExpression(it).toString()
                                    } catch (e: Exception) { "Error" }
                                }
                            },
                            isCalculation = isCalculation,
                            selectedDate = null
                        )
                    }
                }
                2 -> { // 转账
                    // 计算显示的数值
                    val inputVal = try { evaluateExpression(amount) } catch(e: Exception) { 0.0 }
                    val feeVal = try { evaluateExpression(transferFee) } catch(e: Exception) { 0.0 }

                    val displayFrom = if (feeMode == TransferFeeMode.INCLUDE) inputVal else inputVal + feeVal
                    val displayTo = if (feeMode == TransferFeeMode.INCLUDE) inputVal - feeVal else inputVal

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp) // 增加间距
                        ) {
                            // 1. 转出账户卡片
                            TransferAccountCard(
                                account = fromAccount,
                                label = "转出",
                                amount = -displayFrom, // 显示负数
                                amountColor = Color(0xFFE53935), // 红色
                                onClick = { showAccountPickerFor = "from" },
                                isFocused = focusedField == "amount"
                            )

                            // 交换箭头 (居中)
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SwapVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // 2. 转入账户卡片
                            TransferAccountCard(
                                account = toAccount,
                                label = "转入",
                                amount = displayTo, // 显示正数
                                amountColor = Color(0xFF4CAF50), // 绿色
                                onClick = { showAccountPickerFor = "to" },
                                isFocused = false // 转入卡片不直接接受焦点，只显示结果
                            )

                            Spacer(Modifier.height(8.dp))

                            // 3. 手续费逻辑选择
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 模式 A: 扣款(Total) = 到账 + 手续费
                                // 也就是：扣款金额由 到账+手续费 决定。此时输入的是“扣款金额”。
                                // 按照图片逻辑：
                                // 选项1：扣款(505) = 到账(500) + 手续费(5) -> 输入的是到账金额
                                // 选项2：扣款(500) = 到账(495) + 手续费(5) -> 输入的是扣款金额

                                val format = "%.0f" // 简化显示，去除多余小数

                                // 选项 A: 扣款金额固定 (输入值)
                                FeeOptionRow(
                                    selected = feeMode == TransferFeeMode.INCLUDE,
                                    onClick = { feeMode = TransferFeeMode.INCLUDE },
                                    text = "扣款(${String.format(format, inputVal)}) = 到账(${String.format(format, inputVal - feeVal)}) + 手续费(${String.format(format, feeVal)})"
                                )

                                // 选项 B: 到账金额固定 (输入值)
                                FeeOptionRow(
                                    selected = feeMode == TransferFeeMode.EXTRA,
                                    onClick = { feeMode = TransferFeeMode.EXTRA },
                                    text = "扣款(${String.format(format, inputVal + feeVal)}) = 到账(${String.format(format, inputVal)}) + 手续费(${String.format(format, feeVal)})"
                                )
                            }
                        }

                        if (fromAccount != null && toAccount != null) {
                            // 转账页面的键盘工具栏
                            // 这里点击主金额区域（上方的卡片），focusedField 变回 "amount"
                            // 点击手续费区域，focusedField 变为 "fee"
                            KeyboardActionToolbar(
                                button1Icon = Icons.Default.AttachMoney,
                                button1Text = "手续费: $transferFee",
                                button1OnClick = { focusedField = "fee" },
                                button1Highlight = focusedField == "fee",
                                dateMillis = selectedDate,
                                onDateClick = { showDatePicker = true },
                                remark = remarks[selectedTab],
                                onRemarkClick = { showRemarkDialog = true }
                            )

                            // 当焦点不在手续费时，键盘输入给主金额
                            if (focusedField != "fee" && focusedField != "amount") {
                                focusedField = "amount"
                            }

                            NumericKeyboard(
                                onNumberClick = { num -> handleInput { if (it == "0") num else it + num } },
                                onOperatorClick = { op -> handleInput { it + " $op " } },
                                onBackspaceClick = { handleInput { if (it.length > 1) it.dropLast(1) else "0" } },
                                onAgainClick = null, // 去掉“再记”
                                onDoneClick = onTransferDoneClick,
                                onEqualsClick = {
                                    handleInput {
                                        try {
                                            evaluateExpression(it).toString()
                                        } catch (e: Exception) { "Error" }
                                    }
                                },
                                isCalculation = isCalculation,
                                selectedDate = selectedDate
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 新增转账 UI 组件 ---

@Composable
fun TransferAccountCard(
    account: Account?,
    label: String,
    amount: Double,
    amountColor: Color,
    onClick: () -> Unit,
    isFocused: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            // 左侧：图标
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
                        tint = amountColor, // 图标颜色跟随金额颜色
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.width(16.dp))

            // 中间：账户名
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account?.name ?: "选择账户",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // 这里可以显示余额，如果已有数据的话
                if (account != null) {
                    // 简单显示初始余额，或者如果不准确可以隐藏
                    // Text("余额: ${account.currency} ...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // 右侧：金额
            Text(
                text = String.format("%.2f", amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
fun FeeOptionRow(selected: Boolean, onClick: () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- 辅助组件 ---

@Composable
fun NewAmountDisplay(category: Category?, amount: String) {
    val cardColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category != null) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "选择分类",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = amount,
                style = MaterialTheme.typography.displaySmall,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionChipItem(
            icon = button1Icon,
            text = button1Text,
            onClick = button1OnClick,
            modifier = Modifier.weight(1f),
            isHighlight = button1Highlight
        )
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
            modifier = Modifier.weight(1.5f),
            isHighlight = remark.isNotEmpty()
        )
    }
}

@Composable
fun ActionChipItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RemarkInputDialog(
    initialRemark: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialRemark) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加备注") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("请输入备注内容...") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun CategoryGrid(categories: List<Category>, selectedCategory: Category?, onCategoryClick: (Category) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridItems(categories) { category ->
            CategoryItem(category = category, isSelected = category == selectedCategory, onClick = { onCategoryClick(category) })
        }
    }
}

@Composable
fun CategoryItem(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp).aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(backgroundColor), contentAlignment = Alignment.Center) {
            Icon(imageVector = category.icon, contentDescription = category.title, tint = contentColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = category.title, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectorBox( modifier: Modifier = Modifier, account: Account?, onClick: () -> Unit ) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Row( modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically ) {
            if (account != null) {
                val icon = IconMapper.getIcon(account.iconName)
                Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(account.name, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("选择账户", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AccountPickerDialog( accounts: List<Account>, onAccountSelected: (Account) -> Unit, onDismissRequest: () -> Unit, navController: NavHostController ) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("选择账户") },
        text = {
            LazyColumn {
                items(accounts) { account ->
                    Row( modifier = Modifier.fillMaxWidth().clickable { onAccountSelected(account) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically ) {
                        val icon = IconMapper.getIcon(account.iconName)
                        Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp))
                        Text(account.name)
                    }
                }
            }
        },
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