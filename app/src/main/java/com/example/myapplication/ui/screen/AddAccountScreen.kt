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
    // 获取所有账户数据，用于查找回填
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    // --- 状态管理 ---
    var accountName by remember { mutableStateOf("") }

    // 初始值设为空字符串
    var initialAmount by remember { mutableStateOf("") }

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

    // 如果是编辑模式，回填数据
    LaunchedEffect(accountId, allAccounts) {
        if (accountId != null) {
            val accountToEdit = allAccounts.find { it.id == accountId }
            if (accountToEdit != null) {
                accountName = accountToEdit.name

                // 【关键修改】处理金额显示逻辑
                initialAmount = when {
                    accountToEdit.initialBalance == 0.0 -> "" // 0 -> 空
                    accountToEdit.initialBalance % 1.0 == 0.0 -> accountToEdit.initialBalance.toLong().toString() // 10000.0 -> 10000
                    else -> accountToEdit.initialBalance.toString() // 100.5 -> 100.5
                }

                selectedType = accountToEdit.type
                selectedCurrency = accountToEdit.currency
                selectedIcon = accountToEdit.iconName
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

            // 4. 金额
            OutlinedTextField(
                value = initialAmount,
                onValueChange = { initialAmount = it },
                label = { Text("初始余额") },
                placeholder = { Text("0.0") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
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
                    val amount = initialAmount.toDoubleOrNull() ?: 0.0

                    if (name.isNotBlank() && selectedIcon != null) {
                        val account = Account(
                            id = accountId ?: 0,
                            name = name,
                            type = selectedType,
                            currency = selectedCurrency,
                            initialBalance = amount,
                            iconName = selectedIcon!!,
                            isLiability = (selectedType == "信用卡")
                        )

                        if (accountId == null) {
                            viewModel.insertAccount(account)
                        } else {
                            viewModel.insertAccount(account)
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