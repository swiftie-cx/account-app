package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.* // Import * for layout
import androidx.compose.material3.* // Import * for material3
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavHostController, viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("支出", "收入", "转账")

    // --- 支出/收入 状态 ---
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("0") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var remark by remember { mutableStateOf("") } // <-- (新) 添加备注状态

    // --- 转账 状态 ---
    var fromAccount by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var fromAmount by remember { mutableStateOf("0") } // 转出金额
    var toAmount by remember { mutableStateOf("0") }   // 转入金额
    var focusedAmountField by remember { mutableStateOf("fromAmount") } // 焦点
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
        remark = "" // 重置备注
        selectedAccount = accounts.firstOrNull()
        fromAccount = null
        toAccount = null
        fromAmount = "0"
        toAmount = "0"
        focusedAmountField = "fromAmount"
        showAccountPickerFor = null
    }

    // --- (修改) 支出/收入 的 "完成" 逻辑 ---
    val onExpenseDoneClick: () -> Unit = {
        if (selectedCategory != null && selectedAccount != null && amount != "0") {
            val rawAmount = amount.toDouble()
            val finalAmount = if (selectedTab == 0) -rawAmount else rawAmount
            val expense = Expense(
                category = selectedCategory!!.title,
                amount = finalAmount,
                // --- (修复) 确保传递 Date 对象 ---
                date = Date(selectedDate), // selectedDate 是 Long 时间戳
                // --- (修复结束) ---
                accountId = selectedAccount!!.id, // ID 已经是 Long
                remark = remark.takeIf { it.isNotBlank() }
            )
            viewModel.insert(expense)
            navController.popBackStack()
        }
    }

    // --- (修改) 转账 的 "完成" 逻辑 ---
    val onTransferDoneClick: () -> Unit = {
        val fromAmountValue = fromAmount.toDoubleOrNull() ?: 0.0
        val toAmountValue = if (isSameCurrency) fromAmountValue else (toAmount.toDoubleOrNull() ?: 0.0)

        if (fromAccount != null && toAccount != null && fromAmountValue > 0 && toAmountValue > 0 && fromAccount != toAccount) {
            viewModel.createTransfer(
                fromAccountId = fromAccount!!.id, // ID 是 Long
                toAccountId = toAccount!!.id,   // ID 是 Long
                fromAmount = fromAmountValue,
                toAmount = toAmountValue,
                // --- (修复) 确保传递 Date 对象 ---
                date = Date() // 使用当前时间的 Date 对象
                // --- (修复结束) ---
            )
            navController.popBackStack()
        }
    }

    // --- 弹窗 (不变) ---
    if (showAccountPickerFor != null) { /* ... */ }
    if (showAccountPicker) { /* ... */ }
    if (showDatePicker) { /* ... */ }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("添加") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("取消") }
                }
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
                            category = selectedCategory,
                            amount = amount,
                            date = selectedDate,
                            account = selectedAccount,
                            onAccountClick = { showAccountPicker = true }
                        )

                        // (新) 添加备注输入框
                        OutlinedTextField(
                            value = remark,
                            onValueChange = { remark = it },
                            label = { Text("备注 (可选)") },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true
                        )

                        CategoryGrid(
                            categories = categoriesToShow,
                            onCategoryClick = { clickedCategory ->
                                selectedCategory = if (selectedCategory == clickedCategory) null else clickedCategory
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedCategory != null) {
                            NumericKeyboard(
                                onNumberClick = { if (amount == "0") amount = it else amount += it },
                                onBackspaceClick = { amount = if (amount.length > 1) amount.dropLast(1) else "0" },
                                onDateClick = { showDatePicker = true },
                                onDoneClick = onExpenseDoneClick
                            )
                        }
                    }
                }
                2 -> { // 转账
                    Column(modifier = Modifier.fillMaxSize()) {
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

                        // (新) 添加备注输入框 (Optional for transfers)
                        OutlinedTextField(
                            value = remark,
                            onValueChange = { remark = it },
                            label = { Text("备注 (可选)") },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true
                        )

                        Spacer(Modifier.weight(1f))
                        if (fromAccount != null && toAccount != null) {
                            NumericKeyboard(
                                onNumberClick = { num ->
                                    if (isSameCurrency) {
                                        fromAmount = if (fromAmount == "0") num else fromAmount + num
                                    } else {
                                        if (focusedAmountField == "fromAmount") {
                                            fromAmount = if (fromAmount == "0") num else fromAmount + num
                                        } else {
                                            toAmount = if (toAmount == "0") num else toAmount + num
                                        }
                                    }
                                },
                                onBackspaceClick = {
                                    if (isSameCurrency) {
                                        fromAmount = if (fromAmount.length > 1) fromAmount.dropLast(1) else "0"
                                    } else {
                                        if (focusedAmountField == "fromAmount") {
                                            fromAmount = if (fromAmount.length > 1) fromAmount.dropLast(1) else "0"
                                        } else {
                                            toAmount = if (toAmount.length > 1) toAmount.dropLast(1) else "0"
                                        }
                                    }
                                },
                                onDateClick = { /* 转账不使用日期选择 */ },
                                onDoneClick = onTransferDoneClick
                            )
                        }
                    }
                }
            }
        }
    }
}


// ... (TransferScreenContent, AccountSelectorBox, AmountInputBox, AmountDisplay, CategoryGrid, CategoryItem, AccountPickerDialog remain UNCHANGED) ...

// TransferScreenContent
@Composable
fun TransferScreenContent( fromAccount: Account?, toAccount: Account?, fromAmount: String, toAmount: String, isSameCurrency: Boolean, focusedField: String, onFromAccountClick: () -> Unit, onToAccountClick: () -> Unit, onFromAmountClick: () -> Unit, onToAmountClick: () -> Unit ) { /* ... */ }
// AccountSelectorBox
@OptIn(ExperimentalMaterial3Api::class) @Composable fun AccountSelectorBox( modifier: Modifier = Modifier, account: Account?, onClick: () -> Unit ) { /* ... */ }
// AmountInputBox
@OptIn(ExperimentalMaterial3Api::class) @Composable fun AmountInputBox( amount: String, currency: String, label: String, isFocused: Boolean, onClick: () -> Unit ) { /* ... */ }
// AmountDisplay
@Composable fun AmountDisplay( category: Category?, amount: String, date: Long, account: Account?, onAccountClick: () -> Unit ) { /* ... */ }
// CategoryGrid
@Composable fun CategoryGrid(categories: List<Category>, onCategoryClick: (Category) -> Unit, modifier: Modifier = Modifier) { /* ... */ }
// CategoryItem
@Composable fun CategoryItem(category: Category, onClick: () -> Unit) { /* ... */ }
// AccountPickerDialog
@Composable fun AccountPickerDialog( accounts: List<Account>, onAccountSelected: (Account) -> Unit, onDismissRequest: () -> Unit, navController: NavHostController ) { /* ... */ }