package com.example.myapplication.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // 使用 * 导入
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems // 重命名 grid 的 items
import androidx.compose.foundation.lazy.items // 导入 LazyColumn 的 items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.* // 使用 * 导入
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
// --- (修复) 添加 Import ---
import com.example.myapplication.ui.screen.Routes
// --- 修复结束 ---
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("支出", "收入", "转账")

    // --- 支出/收入 状态 ---
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("0") }
    var isCalculation by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var remark by remember { mutableStateOf("") }

    // --- 转账 状态 ---
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

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    LaunchedEffect(accounts) {
        if (selectedAccount == null || selectedAccount?.id !in accounts.map { it.id }) {
            selectedAccount = accounts.firstOrNull()
        }
    }

    LaunchedEffect(selectedTab) {
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

    // --- 完成按钮逻辑 ---
    val onExpenseDoneClick: () -> Unit = {
        if (selectedCategory != null && selectedAccount != null && amount != "0") {
            val rawAmount = amount.toDouble()
            val finalAmount = if (selectedTab == 0) -rawAmount else rawAmount
            val expense = Expense(
                category = selectedCategory!!.title,
                amount = finalAmount,
                date = Date(selectedDate), // 传递 Date 对象
                accountId = selectedAccount!!.id,
                remark = remark.takeIf { it.isNotBlank() }
            )
            viewModel.insert(expense)
            navController.popBackStack()
        }
    }

    val onTransferDoneClick: () -> Unit = {
        val fromAmountValue = fromAmount.toDoubleOrNull() ?: 0.0
        val toAmountValue = if (isSameCurrency) fromAmountValue else (toAmount.toDoubleOrNull() ?: 0.0)

        if (fromAccount != null && toAccount != null && fromAmountValue > 0 && toAmountValue > 0 && fromAccount != toAccount) {
            viewModel.createTransfer(
                fromAccountId = fromAccount!!.id,
                toAccountId = toAccount!!.id,
                fromAmount = fromAmountValue,
                toAmount = toAmountValue,
                date = Date() // 传递 Date 对象
            )
            navController.popBackStack()
        }
    }

    // --- 弹窗 ---
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
                title = { Text("添加") },
                navigationIcon = { TextButton(onClick = { navController.popBackStack() }) { Text("取消") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
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
                            category = selectedCategory, amount = amount, date = selectedDate, // Pass Long
                            account = selectedAccount, onAccountClick = { showAccountPicker = true }
                        )
                        OutlinedTextField(
                            value = remark, onValueChange = { remark = it }, label = { Text("备注 (可选)") },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true
                        )
                        CategoryGrid(
                            categories = categoriesToShow,
                            onCategoryClick = { clickedCategory -> selectedCategory = if (selectedCategory == clickedCategory) null else clickedCategory },
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedCategory != null) {
                            // (修改) 传递 selectedDate
                            NumericKeyboard(
                                onNumberClick = { if (amount == "0") amount = it else amount += it },
                                onOperatorClick = { operator -> amount += " $operator "; isCalculation = true },
                                onBackspaceClick = { amount = if (amount.length > 1) amount.dropLast(1) else "0"; isCalculation = amount.contains("+") || amount.contains("-") },
                                onDateClick = { showDatePicker = true },
                                onDoneClick = onExpenseDoneClick,
                                onEqualsClick = {
                                    try {
                                        val result = evaluateExpression(amount)
                                        amount = result.toString()
                                    } catch (e: Exception) {
                                        amount = "Error"
                                    }
                                    isCalculation = false
                                },
                                isCalculation = isCalculation,
                                selectedDate = selectedDate // <--- 传递当前选中的日期
                            )
                        }
                    }
                }
                2 -> { // 转账
                    Column(modifier = Modifier.fillMaxSize()) {
                        TransferScreenContent(
                            fromAccount = fromAccount, toAccount = toAccount, fromAmount = fromAmount, toAmount = toAmount,
                            isSameCurrency = isSameCurrency, focusedField = focusedAmountField,
                            onFromAccountClick = { showAccountPickerFor = "from" }, onToAccountClick = { showAccountPickerFor = "to" },
                            onFromAmountClick = { focusedAmountField = "fromAmount" }, onToAmountClick = { focusedAmountField = "toAmount" }
                        )
                        OutlinedTextField(
                            value = remark, onValueChange = { remark = it }, label = { Text("备注 (可选)") },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true
                        )
                        Spacer(Modifier.weight(1f))
                        if (fromAccount != null && toAccount != null) {
                            // (修改) 传递 selectedDate (虽然转账界面暂时不处理日期点击，但需要满足参数要求)
                            NumericKeyboard(
                                onNumberClick = { num ->
                                    if (isSameCurrency) fromAmount = if (fromAmount == "0") num else fromAmount + num
                                    else if (focusedAmountField == "fromAmount") fromAmount = if (fromAmount == "0") num else fromAmount + num
                                    else toAmount = if (toAmount == "0") num else toAmount + num
                                },
                                onOperatorClick = { operator ->
                                    if (isSameCurrency) { fromAmount += " $operator "; isCalculation = true }
                                    else if (focusedAmountField == "fromAmount") { fromAmount += " $operator "; isCalculation = true }
                                    else { toAmount += " $operator "; isCalculation = true }
                                },
                                onBackspaceClick = {
                                    if (isSameCurrency) fromAmount = if (fromAmount.length > 1) fromAmount.dropLast(1) else "0"
                                    else if (focusedAmountField == "fromAmount") fromAmount = if (fromAmount.length > 1) fromAmount.dropLast(1) else "0"
                                    else toAmount = if (toAmount.length > 1) toAmount.dropLast(1) else "0"
                                    isCalculation = fromAmount.contains("+") || fromAmount.contains("-") || toAmount.contains("+") || toAmount.contains("-")
                                },
                                onDateClick = { /* No date picker for transfer */ },
                                onDoneClick = onTransferDoneClick,
                                onEqualsClick = {
                                    try {
                                        if (isSameCurrency) fromAmount = evaluateExpression(fromAmount).toString()
                                        else if (focusedAmountField == "fromAmount") fromAmount = evaluateExpression(fromAmount).toString()
                                        else toAmount = evaluateExpression(toAmount).toString()
                                    } catch (e: Exception) {
                                        if (isSameCurrency) fromAmount = "Error"
                                        else if (focusedAmountField == "fromAmount") fromAmount = "Error"
                                        else toAmount = "Error"
                                    }
                                    isCalculation = false
                                },
                                isCalculation = isCalculation,
                                selectedDate = selectedDate // <--- 传递当前选中的日期
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
    var result = tokens[0].toDouble()
    for (i in 1 until tokens.size step 2) {
        val operator = tokens[i]
        val nextOperand = tokens[i + 1].toDouble()
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
    Column( modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp) ) {
        Row( modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically ) {
            if (category != null) {
                Icon(category.icon, contentDescription = category.title, modifier = Modifier.padding(end = 8.dp))
                Text(category.title, style = MaterialTheme.typography.bodyLarge)
            } else { Text("选择分类", style = MaterialTheme.typography.bodyLarge) }
            Text( text = amount, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.End )
        }
        Row( modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween ) {
            Row( modifier = Modifier.clickable(onClick = onAccountClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically ) {
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
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp).aspectRatio(1f),
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
                    Row( modifier = Modifier.fillMaxWidth().clickable { onAccountSelected(account) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically ) {
                        val icon = IconMapper.getIcon(account.iconName)
                        Icon(icon, contentDescription = account.name, modifier = Modifier.padding(end = 16.dp))
                        Text(account.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("取消") } },
        dismissButton = {
            TextButton(onClick = { navController.navigate(Routes.ACCOUNT_MANAGEMENT); onDismissRequest() }) {
                Text("账户管理")
            }
        }
    )
}