package com.example.myapplication.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.data.Account
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    accountId: Long? = null
) {
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    // --- 状态管理 ---
    var accountName by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }

    val accountTypes = listOf("现金", "银行卡", "信用卡", "投资", "电子钱包", "默认")
    var selectedType by remember { mutableStateOf(accountTypes[0]) }

    val currencies = listOf(
        "CNY", "USD", "EUR", "JPY", "HKD", "GBP", "AUD", "CAD",
        "SGD", "TWD", "KRW"
    )
    var selectedCurrency by remember { mutableStateOf(currencies[0]) }

    val icons = IconMapper.allIcons
    // 默认选中第一个图标
    var selectedIcon by remember { mutableStateOf<String?>(icons.firstOrNull()?.first) }

    var isDataLoaded by remember { mutableStateOf(false) }

    // 回填数据逻辑
    LaunchedEffect(accountId, allAccounts, allExpenses) {
        if (accountId != null && !isDataLoaded && allAccounts.isNotEmpty()) {
            val accountToEdit = allAccounts.find { it.id == accountId }
            if (accountToEdit != null) {
                accountName = accountToEdit.name

                val transactionSum = allExpenses
                    .filter { it.accountId == accountToEdit.id }
                    .sumOf { it.amount }
                val currentBalance = accountToEdit.initialBalance + transactionSum

                balanceInput = when {
                    currentBalance == 0.0 -> ""
                    currentBalance % 1.0 == 0.0 -> currentBalance.toLong().toString()
                    else -> String.format(Locale.US, "%.2f", currentBalance)
                }

                selectedType = accountToEdit.type
                selectedCurrency = accountToEdit.currency
                selectedIcon = accountToEdit.iconName
                isDataLoaded = true
            }
        }
    }

    // --- 界面颜色 ---
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bgColor = MaterialTheme.colorScheme.surfaceContainerLow

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (accountId == null) "新建账户" else "编辑账户", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val name = accountName.trim()
                            val newBalanceVal = balanceInput.toDoubleOrNull() ?: 0.0

                            if (name.isNotBlank() && selectedIcon != null) {
                                if (accountId == null) {
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
                                    val accountToUpdate = Account(
                                        id = accountId,
                                        name = name,
                                        type = selectedType,
                                        currency = selectedCurrency,
                                        initialBalance = 0.0,
                                        iconName = selectedIcon!!,
                                        isLiability = (selectedType == "信用卡")
                                    )
                                    viewModel.updateAccountWithNewBalance(accountToUpdate, newBalanceVal)
                                }
                                navController.popBackStack()
                            }
                        },
                        enabled = accountName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = primaryColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 实时预览卡片 (核心美化点)
            AccountPreviewCard(
                name = if (accountName.isBlank()) "账户名称" else accountName,
                balance = balanceInput,
                currency = selectedCurrency,
                iconName = selectedIcon,
                type = selectedType,
                primaryColor = primaryColor
            )

            // 2. 表单区域
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp), // 平面风格，更现代
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 名称
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // 类型和货币 (并排)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownInput(
                                label = "类型",
                                options = accountTypes,
                                selectedOption = selectedType,
                                onOptionSelected = { selectedType = it }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownInput(
                                label = "货币",
                                options = currencies,
                                selectedOption = selectedCurrency,
                                onOptionSelected = { selectedCurrency = it }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 余额
                    OutlinedTextField(
                        value = balanceInput,
                        onValueChange = { balanceInput = it },
                        label = { Text("当前余额") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = if (accountId != null) {
                            { Text("修改余额将自动生成一笔修正流水", fontSize = 10.sp) }
                        } else null
                    )
                }
            }

            // 3. 图标选择器
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "选择图标",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 60.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.heightIn(max = 300.dp) // 限制高度
                    ) {
                        items(icons) { (name, icon) ->
                            IconSelectionItem(
                                icon = icon,
                                isSelected = name == selectedIcon,
                                primaryColor = primaryColor,
                                onClick = { selectedIcon = name }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// --- 组件：顶部预览卡片 ---
@Composable
fun AccountPreviewCard(
    name: String,
    balance: String,
    currency: String,
    iconName: String?,
    type: String,
    primaryColor: Color
) {
    val displayBalance = if (balance.isBlank()) "0.00" else balance

    // 渐变背景
    val brush = Brush.verticalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.8f),
            primaryColor
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = primaryColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        ) {
            // 装饰圆圈
            Box(
                modifier = Modifier
                    .offset(x = 200.dp, y = (-50).dp)
                    .size(200.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部：图标 + 类型
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = iconName?.let { IconMapper.getIcon(it) } ?: Icons.Default.AccountBalanceWallet
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = type, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                // 底部：余额
                Column {
                    Text(text = "当前余额", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$currency $displayBalance",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- 组件：图标选择项 ---
@Composable
fun IconSelectionItem(
    icon: ImageVector,
    isSelected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.Transparent,
        label = "bgColor"
    )
    val iconColor by animateColorAsState(
        if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "iconColor"
    )
    val border = if (isSelected) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(border, if(isSelected) primaryColor else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

// --- 组件：下拉选择框 (美化版) ---
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
            trailingIcon = {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    null,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp)
                )
            }
        }
    }
}

// 辅助扩展函数：旋转图标
fun Modifier.rotate(degrees: Float) = this.then(
    Modifier.graphicsLayer(rotationZ = degrees)
)