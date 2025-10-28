package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
// (新) 导入真实的 Account 数据类
import com.example.myapplication.data.Account
// (新) 导入我们的 IconMapper
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
    val accountTypes = listOf("默认", "现金", "银行卡", "信用卡", "投资")
    var selectedType by remember { mutableStateOf(accountTypes[0]) }

    // “货币”下拉菜单的状态
    val currencies = listOf("CNY", "USD", "JPY", "EUR")
    var selectedCurrency by remember { mutableStateOf(currencies[0]) }

    // (修改) “图标”选择的状态
    // (修改) 从 IconMapper 获取图标列表 (Pair<String, ImageVector>)
    val icons = IconMapper.allIcons

    // (修改) selectedIcon 现在存储的是图标的名称(String)，而不是 ImageVector
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

            // 2. 类型 (使用可重用 Composable)
            DropdownInput(
                label = "类型",
                options = accountTypes,
                selectedOption = selectedType,
                onOptionSelected = { selectedType = it }
            )
            Spacer(Modifier.height(16.dp))

            // 3. 货币 (使用可重用 Composable)
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
                label = { Text("金额") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))

            // 5. 图标
            Text("图标", style = MaterialTheme.typography.labelLarge)
            IconSelector(
                icons = icons, // (修改) 传递 Pair 列表
                selectedIconName = selectedIcon, // (修改) 传递选中的图标名称
                onIconSelected = { iconName -> // (修改) 回调返回的是 String
                    selectedIcon = iconName
                }
            )

            // 6. 添加按钮
            // 使用 Spacer + weight 将按钮推到屏幕底部
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    // (修改) --- 实现保存逻辑 ---
                    val name = accountName.trim()
                    val amount = initialAmount.toDoubleOrNull() ?: 0.0

                    // 简单的验证
                    if (name.isNotBlank() && selectedIcon != null) {
                        val newAccount = Account(
                            // id 会自动生成
                            name = name,
                            type = selectedType,
                            currency = selectedCurrency,
                            initialBalance = amount,
                            iconName = selectedIcon!!, // (修改) 保存图标的名称
                            isLiability = (selectedType == "信用卡") // (新) 判断是否为负债
                        )

                        // (修改) 调用 ViewModel 保存
                        viewModel.insertAccount(newAccount)

                        navController.popBackStack()
                    } else {
                        // (可选) 在这里提示用户 "请填写名称并选择图标"
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
 * (此函数不变)
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
        // ... (内部代码不变) ...
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {}, // 不允许用户输入
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
 * (修改) 可重用的图标选择器 Composable
 */
@Composable
fun IconSelector(
    icons: List<Pair<String, ImageVector>>, // (修改) 接收 Pair 列表
    selectedIconName: String?, // (修改) 接收 String
    onIconSelected: (String) -> Unit, // (修改) 返回 String
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5), // 每行 5 个图标
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(icons) { (name, icon) -> // (修改) 解构 Pair
            val isSelected = (name == selectedIconName) // (修改) 比较 String
            // 根据是否选中来改变颜色
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val background = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent

            Icon(
                imageVector = icon, // (修改) 使用 icon
                contentDescription = null, // 图标是装饰性的
                tint = tint,
                modifier = Modifier
                    .size(48.dp) // 统一大小
                    .clip(CircleShape) // 圆形点击区域
                    .clickable { onIconSelected(name) } // (修改) 返回 name
                    .background(background)
                    .padding(8.dp) // 图标的内边距
            )
        }
    }
}