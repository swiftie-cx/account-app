package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    accountId: Long? = null
) {
    // 获取所有账户和账单数据，用于计算当前余额
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // --- 状态管理 ---
    var accountName by remember { mutableStateOf("") }

    // 【修改】这里存储的是“当前余额”的输入值
    var balanceInput by remember { mutableStateOf("") }

    val accountTypes = listOf("默认", "现金", "银行卡", "信用卡", "投资", "电子钱包")
    var selectedType by remember { mutableStateOf(accountTypes[0]) }

    val currencies = listOf(
        "CNY", "USD", "EUR", "JPY", "HKD", "GBP", "AUD", "CAD", "SGD", "TWD",
        "KRW", "THB", "MYR", "PHP", "IDR", "INR", "VND",
        "CHF", "SEK", "NOK", "DKK", "RUB", "TRY", "CZK", "HUF", "PLN", "BGN", "RON",
        "NZD", "BRL", "MXN", "ZAR", "ILS"
    )
    var selectedCurrency by remember { mutableStateOf(currencies[0]) }

    val icons = IconMapper.allIcons
    var selectedIcon by remember { mutableStateOf<String?>(null) }

    // 标记是否已加载初始数据
    var isDataLoaded by remember { mutableStateOf(false) }

    // 回填数据逻辑
    LaunchedEffect(accountId, allAccounts, allExpenses) {
        if (accountId != null && !isDataLoaded && allAccounts.isNotEmpty()) {
            val accountToEdit = allAccounts.find { it.id == accountId }
            if (accountToEdit != null) {
                accountName = accountToEdit.name

                // 【关键修改】计算当前余额用于显示
                val transactionSum = allExpenses
                    .filter { it.accountId == accountToEdit.id }
                    .sumOf { it.amount }
                val currentBalance = accountToEdit.initialBalance + transactionSum

                // 格式化显示：如果是整数去掉 .0，如果是 0 显示空方便输入
                balanceInput = when {
                    currentBalance == 0.0 -> ""
                    currentBalance % 1.0 == 0.0 -> currentBalance.toLong().toString()
                    else -> currentBalance.toString()
                }

                selectedType = accountToEdit.type
                selectedCurrency = accountToEdit.currency
                selectedIcon = accountToEdit.iconName
                isDataLoaded = true
            }
        }
    }

    // --- UI 界面 ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (accountId == null) "添加账户" else "编辑账户") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (accountId != null) {
                        IconButton(onClick = {
                            // 预留删除逻辑
                        }) {
                            // Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // 1. 账户名称
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("账户名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            // 2. 类型
            DropdownInput(
                label = "类型",
                options = accountTypes,
                selectedOption = selectedType,
                onOptionSelected = { selectedType = it }
            )
            Spacer(Modifier.height(16.dp))

            // 3. 货币
            DropdownInput(
                label = "货币",
                options = currencies,
                selectedOption = selectedCurrency,
                onOptionSelected = { selectedCurrency = it }
            )
            Spacer(Modifier.height(16.dp))

            // 4. 金额 (显示为"当前余额")
            OutlinedTextField(
                value = balanceInput,
                onValueChange = { balanceInput = it },
                label = { Text("当前余额") }, // 【修改】标签改为当前余额
                placeholder = { Text("0.0") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                supportingText = {
                    if (accountId != null) {
                        Text("修改此数值将自动调整账户初始余额以匹配流水")
                    }
                }
            )
            Spacer(Modifier.height(24.dp))

            // 5. 图标
            Text("图标", style = MaterialTheme.typography.labelLarge)
            IconSelector(
                icons = icons,
                selectedIconName = selectedIcon,
                onIconSelected = { iconName -> selectedIcon = iconName }
            )

            // 6. 添加/保存按钮
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val name = accountName.trim()
                    val newBalanceVal = balanceInput.toDoubleOrNull() ?: 0.0

                    if (name.isNotBlank() && selectedIcon != null) {
                        if (accountId == null) {
                            // 新增模式：初始余额 = 输入的当前余额 (因为还没有流水)
                            val account = Account(
                                name = name,
                                type = selectedType,
                                currency = selectedCurrency,
                                initialBalance = newBalanceVal,
                                iconName = selectedIcon!!,
                                isLiability = (selectedType == "信用卡")
                            )
                            viewModel.insertAccount(account)
                        } else {
                            // 编辑模式：调用特殊方法反算初始余额
                            val accountToUpdate = Account(
                                id = accountId,
                                name = name,
                                type = selectedType,
                                currency = selectedCurrency,
                                initialBalance = 0.0, // 占位，会被 recalculate 覆盖
                                iconName = selectedIcon!!,
                                isLiability = (selectedType == "信用卡")
                            )
                            viewModel.updateAccountWithNewBalance(accountToUpdate, newBalanceVal)
                        }

                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (accountId == null) "添加" else "保存", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ... DropdownInput 和 IconSelector 保持不变 ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownInput(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun IconSelector(
    icons: List<Pair<String, ImageVector>>,
    selectedIconName: String?,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(icons) { (name, icon) ->
            val isSelected = (name == selectedIconName)
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val background = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onIconSelected(name) }
                    .background(background)
                    .padding(8.dp)
            )
        }
    }
}