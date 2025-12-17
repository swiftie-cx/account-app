package com.swiftiecx.timeledger.ui.screen

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.ui.navigation.AccountTypeManager // [新增] 引入 AccountTypeManager
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
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

    // [修改] 使用 Manager 获取类型列表 (Key, ResId)
    val accountTypes = remember { AccountTypeManager.getAllTypes() }

    // --- 状态管理 ---
    var accountName by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }

    // [修改] 默认选中第一个类型的 Key
    var selectedTypeKey by remember { mutableStateOf(accountTypes.first().first) }

    val currencies = listOf(
        "CNY", "USD", "EUR", "JPY", "HKD", "GBP", "AUD", "CAD",
        "SGD", "TWD", "KRW"
    )
    var selectedCurrency by remember { mutableStateOf(currencies.first()) }

    // 获取图标列表 (Pair<String, ImageVector>)
    val icons = remember {
        listOf(
            "Wallet", "Bank", "CreditCard", "TrendingUp",
            "Smartphone", "AttachMoney", "Savings", "Payment",
            "CurrencyExchange", "Euro", "ShowChart", "PieChart"
        ).map { it to IconMapper.getIcon(it) }
    }

    // 默认选中第一个图标
    var selectedIcon by remember { mutableStateOf(icons.first().first) }

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
                val currentBalance = accountToEdit.initialBalance + transactionSum // 这里暂时只用于显示

                // 编辑模式下，初始余额显示为 Account 原始的 initialBalance
                // 或者如果您希望用户修改的是“当前余额”，逻辑会更复杂（需要反推初始余额），这里保持简单逻辑：
                // 通常修改账户信息时，改的是基本信息，余额建议通过“余额调整”功能（记一笔）来做。
                // 但为了保持您的逻辑一致：
                balanceInput = when {
                    accountToEdit.initialBalance == 0.0 -> ""
                    accountToEdit.initialBalance % 1.0 == 0.0 -> accountToEdit.initialBalance.toLong().toString()
                    else -> String.format(Locale.US, "%.2f", accountToEdit.initialBalance)
                }

                // [修改] 尝试把旧数据的中文转成 Key
                selectedTypeKey = AccountTypeManager.getStableKey(accountToEdit.type)
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
                title = { Text(stringResource(if (accountId == null) R.string.add_account_title else R.string.edit_account_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val name = accountName.trim()
                            val newBalanceVal = balanceInput.toDoubleOrNull() ?: 0.0

                            if (name.isNotBlank()) {
                                // 判断是否负债账户 (这里简单逻辑：信用卡即负债)
                                val isLiability = selectedTypeKey == "account_credit"

                                if (accountId == null) {
                                    val account = Account(
                                        name = name,
                                        type = selectedTypeKey, // [修改] 保存 Key
                                        currency = selectedCurrency,
                                        initialBalance = newBalanceVal,
                                        iconName = selectedIcon,
                                        isLiability = isLiability
                                    )
                                    viewModel.insertAccount(account)
                                } else {
                                    val accountToUpdate = Account(
                                        id = accountId,
                                        name = name,
                                        type = selectedTypeKey, // [修改] 保存 Key
                                        currency = selectedCurrency,
                                        initialBalance = newBalanceVal, // 这里直接更新初始余额
                                        iconName = selectedIcon,
                                        isLiability = isLiability
                                    )
                                    viewModel.updateAccount(accountToUpdate)
                                }
                                navController.popBackStack()
                            }
                        },
                        enabled = accountName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save), tint = primaryColor)
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
                name = if (accountName.isBlank()) stringResource(R.string.account_name_placeholder) else accountName,
                balance = balanceInput,
                currency = selectedCurrency,
                iconName = selectedIcon,
                typeKey = selectedTypeKey, // [修改] 传入 Key
                primaryColor = primaryColor
            )

            // 2. 表单区域
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 名称
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text(stringResource(R.string.account_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // 类型和货币 (并排)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            // [修改] 使用新的下拉组件
                            AccountTypeDropdown(
                                label = stringResource(R.string.account_type),
                                options = accountTypes,
                                selectedKey = selectedTypeKey,
                                onOptionSelected = { selectedTypeKey = it }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownInput(
                                label = stringResource(R.string.currency),
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
                        label = { Text(stringResource(R.string.current_balance)) },
                        placeholder = { Text(stringResource(R.string.balance_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = if (accountId != null) {
                            { Text(stringResource(R.string.balance_edit_hint), fontSize = 10.sp) }
                        } else null
                    )
                }
            }

            // 3. 图标选择器
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.select_icon),
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
                        modifier = Modifier.heightIn(max = 300.dp)
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
    typeKey: String,
    primaryColor: Color
) {
    val displayBalance = if (balance.isBlank()) "0.00" else balance

    // [修改] 获取类型显示名称
    val typeName = AccountTypeManager.getDisplayName(typeKey)

    val brush = Brush.verticalGradient(
        colors = listOf(primaryColor.copy(alpha = 0.8f), primaryColor)
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
            modifier = Modifier.fillMaxSize().background(brush)
        ) {
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
                        Text(text = typeName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.current_balance),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))

                    val balanceValue = remember(displayBalance) {
                        displayBalance.trim().replace(",", "").toDoubleOrNull() ?: 0.0
                    }

                    // [修改] 使用 stringResource 格式化金额，避免闪退
                    Text(
                        text = stringResource(R.string.currency_amount_format, currency, balanceValue),
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

// --- 组件：通用下拉选择框 ---
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
                    Icons.Default.ArrowDropDown,
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

// --- 组件：账户类型专用下拉框 (支持多语言) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTypeDropdown(
    label: String,
    options: List<Pair<String, Int>>, // Key, ResId
    selectedKey: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // 获取当前选中的显示文字
    val selectedResId = AccountTypeManager.getTypeResId(selectedKey)
    val displayText = if (selectedResId != null) stringResource(selectedResId) else selectedKey

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
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
            options.forEach { (key, resId) ->
                DropdownMenuItem(
                    text = { Text(stringResource(resId)) },
                    onClick = {
                        onOptionSelected(key)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp)
                )
            }
        }
    }
}

fun Modifier.rotate(degrees: Float) = this.then(
    Modifier.graphicsLayer(rotationZ = degrees)
)