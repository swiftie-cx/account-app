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
) {
    // --- 状态管理 ---

    // 输入框的状态
    var accountName by remember { mutableStateOf("") }
    var initialAmount by remember { mutableStateOf("0.0") }

    // “类型”下拉菜单的状态
    val accountTypes = listOf("默认", "现金", "银行卡", "信用卡", "投资", "电子钱包")
    var selectedType by remember { mutableStateOf(accountTypes[0]) }

    // (修改) “货币”下拉菜单的状态 - 调整顺序，主流在前
    val currencies = listOf(
        // 主流
        "CNY", "USD", "EUR", "JPY", "HKD", "GBP", "AUD", "CAD", "SGD", "TWD",
        // 其他
        "KRW", "THB", "MYR", "PHP", "IDR", "INR", "VND",
        "CHF", "SEK", "NOK", "DKK", "RUB", "TRY", "CZK", "HUF", "PLN", "BGN", "RON",
        "NZD", "BRL", "MXN", "ZAR", "ILS"
    )
    var selectedCurrency by remember { mutableStateOf(currencies[0]) }

    val icons = IconMapper.allIcons
    var selectedIcon by remember { mutableStateOf<String?>(null) }

    // --- UI 界面 ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("添加账户") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            // 6. 添加按钮
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val name = accountName.trim()
                    val amount = initialAmount.toDoubleOrNull() ?: 0.0

                    if (name.isNotBlank() && selectedIcon != null) {
                        val newAccount = Account(
                            name = name,
                            type = selectedType,
                            currency = selectedCurrency,
                            initialBalance = amount,
                            iconName = selectedIcon!!,
                            isLiability = (selectedType == "信用卡")
                        )
                        viewModel.insertAccount(newAccount)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("添加", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * 可重用的下拉菜单输入框 Composable
 */
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

/**
 * 可重用的图标选择器 Composable
 */
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