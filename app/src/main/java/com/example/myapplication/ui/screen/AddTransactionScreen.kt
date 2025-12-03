package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
// import com.example.myapplication.ui.navigation.Routes // (注释掉，避免报错)
import com.example.myapplication.data.ExchangeRates
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
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var remark by remember { mutableStateOf("") }

    // --- 转账状态 ---
    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var fromAmount by remember { mutableStateOf("0") }
    var toAmount by remember { mutableStateOf("0") }

    // (功能) 默认焦点
    var focusedAmountField by remember { mutableStateOf("fromAmount") }
    var showAccountPickerFor by remember { mutableStateOf<String?>(null) }

    val isSameCurrency = remember(fromAccount, toAccount) {
        val from = fromAccount
        val to = toAccount
        from != null && to != null && from.currency == to.currency
    }

    // (功能) 自动跳转焦点逻辑
    LaunchedEffect(fromAccount, toAccount) {
        if (fromAccount != null && toAccount != null && fromAccount != toAccount && !isSameCurrency) {
            if (toAmount == "0" || toAmount == "") {
                focusedAmountField = "toAmount"
            }
        }
    }

    // (功能) 自动推导键盘状态
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

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
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

    LaunchedEffect(accounts) {
        if (selectedAccount == null || selectedAccount?.id !in accounts.map { it.id }) {
            selectedAccount = accounts.firstOrNull()
        }
    }

    LaunchedEffect(selectedTab) {
        if (expenseId == null) {
            selectedCategory = null
            amount = "0"
            remark = ""
            selectedAccount = accounts.firstOrNull()
            fromAccount = null
            toAccount = null
            fromAmount = "0"
            toAmount = "0"
            focusedAmountField = "fromAmount"
            showAccountPickerFor = null
        }
    }

    // (功能) 核心输入处理：包含自动换算
    fun handleInput(transform: (String) -> String) {
        if (selectedTab == 2) {
            if (isSameCurrency) {
                fromAmount = transform(fromAmount)
            } else {
                if (focusedAmountField == "fromAmount") {
                    // 手动修改转出
                    fromAmount = transform(fromAmount)
                } else {
                    // 修改转入 -> 自动算转出
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

    val onExpenseDoneClick: () -> Unit = {
        if (selectedCategory != null && selectedAccount != null && amount != "0") {
            val finalAmountStr = try { evaluateExpression(amount).toString() } catch(e: Exception) { amount }
            val rawAmount = finalAmountStr.toDoubleOrNull() ?: 0.0

            val finalAmount = if (selectedTab == 0) -rawAmount else rawAmount
            val expense = Expense(
                id = expenseId ?: 0,
                category = selectedCategory!!.title,
                amount = finalAmount,
                date = Date(selectedDate),
                accountId = selectedAccount!!.id,
                remark = remark.takeIf { it.isNotBlank() }
            )
            if (expenseId == null) {
                viewModel.insert(expense)
            } else {
                viewModel.updateExpense(expense)
            }
            navController.popBackStack()
        }
    }

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
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (expenseId == null) "添加" else "编辑") },
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
                        AmountDisplay(
                            category = selectedCategory, amount = amount, date = selectedDate,
                            account = selectedAccount, onAccountClick = { showAccountPicker = true }
                        )
                        OutlinedTextField(
                            value = remark, onValueChange = { remark = it }, label = { Text("备注 (可选)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true
                        )
                        CategoryGrid(
                            categories = categoriesToShow,
                            onCategoryClick = { clickedCategory -> selectedCategory = if (selectedCategory == clickedCategory) null else clickedCategory },
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedCategory != null) {
                            NumericKeyboard(
                                onNumberClick = { num -> handleInput { if (it == "0") num else it + num } },
                                onOperatorClick = { op -> handleInput { it + " $op " } },
                                onBackspaceClick = { handleInput { if (it.length > 1) it.dropLast(1) else "0" } },
                                onDateClick = { showDatePicker = true },
                                onDoneClick = onExpenseDoneClick,
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
                2 -> { // 转账
                    Column(modifier = Modifier.fillMaxSize()) {

                        // (修复) 这里重新加上了滚动容器
                        // 使用 Column + weight(1f) + verticalScroll 包裹输入区域
                        // 这样即使内容超出，也可以滚动查看，不会挤压键盘
                        Column(
                            modifier = Modifier
                                .weight(1f) // 占据键盘上方的剩余空间
                                .verticalScroll(rememberScrollState()) // 允许垂直滚动
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

                        // 键盘固定在底部，不包含在 ScrollColumn 中
                        if (fromAccount != null && toAccount != null) {
                            NumericKeyboard(
                                onNumberClick = { num -> handleInput { if (it == "0") num else it + num } },
                                onOperatorClick = { op -> handleInput { it + " $op " } },
                                onBackspaceClick = { handleInput { if (it.length > 1) it.dropLast(1) else "0" } },
                                onDateClick = { showDatePicker = true },
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

fun evaluateExpression(expression: String): Double {
    val tokens = expression.split(" ")
    if (tokens.isEmpty()) return 0.0
    var result = tokens[0].toDoubleOrNull() ?: 0.0
    for (i in 1 until tokens.size step 2) {
        if (i + 1 >= tokens.size) break
        val operator = tokens[i]
        val nextOperand = tokens[i + 1].toDoubleOrNull() ?: 0.0
        if (operator == "+") {
            result += nextOperand
        } else if (operator == "-") {
            result -= nextOperand
        }
    }
    return result
}


// --- 其他 Composable 函数 ---

@Composable
fun TransferScreenContent( fromAccount: Account?, toAccount: Account?, fromAmount: String, toAmount: String, isSameCurrency: Boolean, focusedField: String, onFromAccountClick: () -> Unit, onToAccountClick: () -> Unit, onFromAmountClick: () -> Unit, onToAmountClick: () -> Unit ) {
    Column(Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AccountSelectorBox( modifier = Modifier.weight(1f), account = fromAccount, onClick = onFromAccountClick )
            Icon( Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "转账", modifier = Modifier.padding(horizontal = 8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant )
            AccountSelectorBox( modifier = Modifier.weight(1f), account = toAccount, onClick = onToAccountClick )
        }
        Spacer(Modifier.height(16.dp))
        if (isSameCurrency) {
            AmountInputBox( amount = fromAmount, currency = fromAccount?.currency ?: "---", label = "转账金额", isFocused = true, onClick = {} )
        } else {
            // 点击时更新 focusedField
            AmountInputBox( amount = fromAmount, currency = fromAccount?.currency ?: "---", label = "转出金额", isFocused = focusedField == "fromAmount", onClick = onFromAmountClick )
            Spacer(Modifier.height(8.dp))
            AmountInputBox( amount = toAmount, currency = toAccount?.currency ?: "---", label = "转入金额", isFocused = focusedField == "toAmount", onClick = onToAmountClick )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectorBox( modifier: Modifier = Modifier, account: Account?, onClick: () -> Unit ) {
    Card( onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium ) {
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically ) {
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
    Card( onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) ) {
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
fun AmountDisplay( category: Category?, amount: String, date: Long, // Receive Long
                   account: Account?, onAccountClick: () -> Unit ) {
    val formattedDate = SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(date)) // Format Date from Long
    Column( modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp) ) {
        Row( modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically ) {
            if (category != null) {
                Icon(category.icon, contentDescription = category.title, modifier = Modifier.padding(end = 8.dp))
                Text(category.title, style = MaterialTheme.typography.bodyLarge)
            } else { Text("选择分类", style = MaterialTheme.typography.bodyLarge) }
            Text( text = amount, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.End )
        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween ) {
            Row( modifier = Modifier
                .clickable(onClick = onAccountClick)
                .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically ) {
                if (account != null) {
                    val icon = IconMapper.getIcon(account.iconName)
                    Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 8.dp))
                    Text(account.name, style = MaterialTheme.typography.bodyMedium)
                } else { Text("选择账户", style = MaterialTheme.typography.bodyMedium) }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "选择账户")
            }
            Text( text = formattedDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant )
        }
    }
}

@Composable
fun CategoryGrid(categories: List<Category>, onCategoryClick: (Category) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridItems(categories) { category -> // Use gridItems here
            CategoryItem(category = category, onClick = { onCategoryClick(category) })
        }
    }
}

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(category.icon, contentDescription = category.title)
        Text(category.title, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AccountPickerDialog( accounts: List<Account>, onAccountSelected: (Account) -> Unit, onDismissRequest: () -> Unit, navController: NavHostController ) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("选择账户") },
        text = {
            LazyColumn {
                items(accounts) { account -> // Use LazyColumn's items
                    Row( modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAccountSelected(account) }
                        .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically ) {
                        val icon = IconMapper.getIcon(account.iconName)
                        Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp))
                        Text(account.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("取消") } },
        dismissButton = {
            // 使用字符串，避免 Route 报错
            TextButton(onClick = { navController.navigate("account_management"); onDismissRequest() }) {
                Text("账户管理")
            }
        }
    )
}