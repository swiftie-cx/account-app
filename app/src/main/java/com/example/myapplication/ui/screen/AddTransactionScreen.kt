package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
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
// 务必导入 CustomDateRangePicker
import com.example.myapplication.ui.screen.CustomDateRangePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    expenseId: Long? = null,
    dateMillis: Long? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("支出", "收入", "转账")

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("0") }
    var selectedDate by remember { mutableLongStateOf(dateMillis ?: System.currentTimeMillis()) }

    val defaultAccountId by viewModel.defaultAccountId.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }

    var remark by remember { mutableStateOf("") }
    var showRemarkDialog by remember { mutableStateOf(false) }

    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var fromAmount by remember { mutableStateOf("0") }
    var toAmount by remember { mutableStateOf("0") }
    var focusedAmountField by remember { mutableStateOf("fromAmount") }
    var showAccountPickerFor by remember { mutableStateOf<String?>(null) }

    val isSameCurrency = remember(fromAccount, toAccount) {
        val from = fromAccount
        val to = toAccount
        from != null && to != null && from.currency == to.currency
    }

    LaunchedEffect(fromAccount, toAccount) {
        if (fromAccount != null && toAccount != null && fromAccount != toAccount && !isSameCurrency) {
            if (toAmount == "0" || toAmount == "") {
                focusedAmountField = "toAmount"
            }
        }
    }

    val isCalculation = remember(selectedTab, amount, fromAmount, toAmount, focusedAmountField, isSameCurrency) {
        when (selectedTab) {
            0, 1 -> amount.contains("+") || amount.contains("-")
            2 -> {
                if (isSameCurrency) {
                    fromAmount.contains("+") || fromAmount.contains("-")
                } else {
                    val targetAmount = if (focusedAmountField == "fromAmount") fromAmount else toAmount
                    targetAmount.contains("+") || targetAmount.contains("-")
                }
            }
            else -> false
        }
    }

    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())

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
                    remark = expenseToEdit.remark ?: ""
                }
            }
        }
    }

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
        }
    }

    LaunchedEffect(selectedTab) {
        if (expenseId == null) {
            val currentList = if (selectedTab == 0) expenseCategories else incomeCategories
            selectedCategory = currentList.firstOrNull()

            amount = "0"
            remark = ""
            val defaultAcc = accounts.find { it.id == defaultAccountId }
            selectedAccount = defaultAcc ?: accounts.firstOrNull()

            fromAccount = defaultAcc ?: accounts.firstOrNull()
            toAccount = null
            fromAmount = "0"
            toAmount = "0"
            focusedAmountField = "fromAmount"
            showAccountPickerFor = null
        }
    }

    fun handleInput(transform: (String) -> String) {
        if (selectedTab == 2) {
            if (isSameCurrency) {
                fromAmount = transform(fromAmount)
            } else {
                if (focusedAmountField == "fromAmount") {
                    fromAmount = transform(fromAmount)
                } else {
                    val newToAmount = transform(toAmount)
                    toAmount = newToAmount
                    try {
                        val toValue = evaluateExpression(newToAmount)
                        if (toValue > 0 && fromAccount != null && toAccount != null) {
                            val converted = ExchangeRates.convert(toValue, toAccount!!.currency, fromAccount!!.currency)
                            fromAmount = String.format(Locale.US, "%.2f", converted)
                        } else if (toValue == 0.0) {
                            fromAmount = "0"
                        }
                    } catch (e: Exception) { }
                }
            }
        } else {
            amount = transform(amount)
        }
    }

    // --- 核心逻辑提取：保存账单 ---
    // shouldFinish: true 表示保存并退出，false 表示保存后清空继续记
    fun saveExpense(shouldFinish: Boolean) {
        if (selectedCategory != null && selectedAccount != null && amount != "0") {
            val finalAmountStr = try { evaluateExpression(amount).toString() } catch(e: Exception) { amount }
            val rawAmount = finalAmountStr.toDoubleOrNull() ?: 0.0

            // 如果计算结果为0，不保存
            if (rawAmount == 0.0) return

            val finalAmount = if (selectedTab == 0) -rawAmount else rawAmount
            val expense = Expense(
                id = expenseId ?: 0, // 如果是再记，id 永远是 0 (新增)
                category = selectedCategory!!.title,
                amount = finalAmount,
                date = Date(selectedDate),
                accountId = selectedAccount!!.id,
                remark = remark.takeIf { it.isNotBlank() }
            )

            if (expenseId != null && shouldFinish) {
                // 编辑模式且点击完成 -> 更新
                viewModel.updateExpense(expense)
            } else {
                // 新增模式 或 编辑模式点击再记 -> 插入新记录
                viewModel.insert(expense)
            }

            if (shouldFinish) {
                navController.popBackStack()
            } else {
                // 再记模式：重置金额和备注，保持日期/账户/分类不变
                amount = "0"
                remark = ""
                // 提示用户已保存? (Optional: UI enhancement would be a Snackbar)
            }
        }
    }

    // 转账保存逻辑 (转账一般不涉及连续再记，暂保持原样，或仅支持完成)
    val onTransferDoneClick: () -> Unit = {
        val finalFromStr = try { evaluateExpression(fromAmount).toString() } catch(e: Exception) { fromAmount }
        val finalToStr = try { evaluateExpression(toAmount).toString() } catch(e: Exception) { toAmount }
        val fromAmountValue = finalFromStr.toDoubleOrNull() ?: 0.0
        val toAmountValue = if (isSameCurrency) fromAmountValue else (finalToStr.toDoubleOrNull() ?: 0.0)

        if (fromAccount != null && toAccount != null && fromAmountValue > 0 && toAmountValue > 0 && fromAccount != toAccount) {
            viewModel.createTransfer(
                fromAccountId = fromAccount!!.id,
                toAccountId = toAccount!!.id,
                fromAmount = fromAmountValue,
                toAmount = toAmountValue,
                date = Date(selectedDate)
            )
            navController.popBackStack()
        }
    }

    // --- Dialogs ---

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
                if (startDate != null) {
                    selectedDate = startDate
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showRemarkDialog) {
        RemarkInputDialog(
            initialRemark = remark,
            onConfirm = { newRemark ->
                remark = newRemark
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

            val categoriesToShow = when (selectedTab) {
                0 -> expenseCategories
                1 -> incomeCategories
                else -> emptyList()
            }

            when (selectedTab) {
                0, 1 -> { // 支出 和 收入
                    Column(modifier = Modifier.fillMaxSize()) {

                        // 1. 金额显示卡片
                        NewAmountDisplay(
                            category = selectedCategory,
                            amount = amount
                        )

                        // 2. 分类列表
                        CategoryGrid(
                            categories = categoriesToShow,
                            selectedCategory = selectedCategory,
                            onCategoryClick = { clickedCategory -> selectedCategory = clickedCategory },
                            modifier = Modifier.weight(1f)
                        )

                        // 3. 键盘上方的工具栏
                        KeyboardActionToolbar(
                            account = selectedAccount,
                            dateMillis = selectedDate,
                            remark = remark,
                            onAccountClick = { showAccountPicker = true },
                            onDateClick = { showDatePicker = true },
                            onRemarkClick = { showRemarkDialog = true }
                        )

                        // 4. 数字键盘 (常驻)
                        NumericKeyboard(
                            onNumberClick = { num -> handleInput { if (it == "0") num else it + num } },
                            onOperatorClick = { op -> handleInput { it + " $op " } },
                            onBackspaceClick = { handleInput { if (it.length > 1) it.dropLast(1) else "0" } },
                            onAgainClick = { saveExpense(shouldFinish = false) }, // (新增) 再记
                            onDoneClick = { saveExpense(shouldFinish = true) }, // (修改) 完成
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            TransferScreenContent(
                                fromAccount = fromAccount,
                                toAccount = toAccount,
                                fromAmount = fromAmount,
                                toAmount = toAmount,
                                isSameCurrency = isSameCurrency,
                                focusedField = focusedAmountField,
                                onFromAccountClick = { showAccountPickerFor = "from" },
                                onToAccountClick = { showAccountPickerFor = "to" },
                                onFromAmountClick = { focusedAmountField = "fromAmount" },
                                onToAmountClick = { focusedAmountField = "toAmount" }
                            )

                            OutlinedTextField(
                                value = remark,
                                onValueChange = { remark = it },
                                label = { Text("备注 (可选)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                singleLine = true
                            )
                        }

                        if (fromAccount != null && toAccount != null) {
                            // 转账界面暂时复用新键盘，但忽略"再记"功能，或者将其视为等同于完成
                            // 为了简单，转账界面 "再记" 这里不做特殊处理，或者您可以隐藏
                            // 这里我们简单地让 "再记" 也执行保存并退出，或者暂不绑定逻辑
                            NumericKeyboard(
                                onNumberClick = { num -> handleInput { if (it == "0") num else it + num } },
                                onOperatorClick = { op -> handleInput { it + " $op " } },
                                onBackspaceClick = { handleInput { if (it.length > 1) it.dropLast(1) else "0" } },
                                onAgainClick = { /* 转账通常不需要连续记，留空或做成保存 */ },
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

// 辅助组件 (CategoryGrid, KeyboardActionToolbar 等) 保持不变，请确保文件中保留了它们
// ...
// NewAmountDisplay, KeyboardActionToolbar, ActionChipItem, RemarkInputDialog,
// TransferScreenContent, AccountSelectorBox, AmountInputBox, evaluateExpression,
// CategoryGrid, CategoryItem, AccountPickerDialog 等请继续保留
// (为节省篇幅，此处省略这些未变动的代码，请使用上一次提供的完整代码块中的这些组件)

@Composable
fun NewAmountDisplay(
    category: Category?,
    amount: String
) {
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
    account: Account?,
    dateMillis: Long,
    remark: String,
    onAccountClick: () -> Unit,
    onDateClick: () -> Unit,
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
            icon = if (account != null) IconMapper.getIcon(account.iconName) else Icons.Default.CalendarToday,
            text = account?.name ?: "选择账户",
            onClick = onAccountClick,
            modifier = Modifier.weight(1f)
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
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

@Composable
fun TransferScreenContent( fromAccount: Account?, toAccount: Account?, fromAmount: String, toAmount: String, isSameCurrency: Boolean, focusedField: String, onFromAccountClick: () -> Unit, onToAccountClick: () -> Unit, onFromAmountClick: () -> Unit, onToAmountClick: () -> Unit ) {
    Column(Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AccountSelectorBox( modifier = Modifier.weight(1f), account = fromAccount, onClick = onFromAccountClick )
            Icon( Icons.Default.ArrowForward, contentDescription = "转账", modifier = Modifier.padding(horizontal = 8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant )
            AccountSelectorBox( modifier = Modifier.weight(1f), account = toAccount, onClick = onToAccountClick )
        }
        Spacer(Modifier.height(16.dp))
        if (isSameCurrency) {
            AmountInputBox( amount = fromAmount, currency = fromAccount?.currency ?: "---", label = "转账金额", isFocused = true, onClick = {} )
        } else {
            AmountInputBox( amount = fromAmount, currency = fromAccount?.currency ?: "---", label = "转出金额", isFocused = focusedField == "fromAmount", onClick = onFromAmountClick )
            Spacer(Modifier.height(8.dp))
            AmountInputBox( amount = toAmount, currency = toAccount?.currency ?: "---", label = "转入金额", isFocused = focusedField == "toAmount", onClick = onToAmountClick )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountInputBox( amount: String, currency: String, label: String, isFocused: Boolean, onClick: () -> Unit ) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row( modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(amount, style = MaterialTheme.typography.headlineSmall)
            }
            Text(currency, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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