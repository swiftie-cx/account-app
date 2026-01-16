package com.swiftiecx.timeledger.ui.feature.account.screen

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
import androidx.compose.ui.res.stringResource // [新增]
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增]
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.ui.common.AccountTypeManager
import com.swiftiecx.timeledger.ui.common.IconMapper
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel,
    accountId: Long? = null,
    presetCategory: String = "FUNDS",
    presetDebtType: String? = null
) {
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    // ✅ 新建账户时：默认货币跟随「设置-默认货币」
    val defaultCurrency by viewModel.defaultCurrency.collectAsState(initial = "CNY")

    // 类型列表 (Key, ResId)
    val accountTypes = remember { AccountTypeManager.getAllTypes() }

    // --- 基础输入状态 ---
    var accountName by remember { mutableStateOf("") }

    // FUNDS：余额；CREDIT：欠款；DEBT：未结清金额
    var amountInput by remember { mutableStateOf("") }

    // CREDIT 专用：额度
    var creditLimitInput by remember { mutableStateOf("") }

    // 类别 / 债务方向：新建时用路由 preset；编辑时会被数据库覆盖
    var category by remember { mutableStateOf(presetCategory) }

    // 如果是 DEBT 且没传 debtType，默认 PAYABLE（借入/应付）
    var debtType by remember {
        mutableStateOf(
            when (presetCategory) {
                "DEBT" -> presetDebtType ?: "PAYABLE"
                else -> presetDebtType
            }
        )
    }

    // FUNDS/CREDIT 用的“账户类型”
    var selectedTypeKey by remember { mutableStateOf(accountTypes.first().first) }

    val currencies = listOf(
        "CNY", "USD", "EUR", "JPY", "HKD", "GBP", "AUD", "CAD",
        "SGD", "TWD", "KRW"
    )
    var selectedCurrency by remember(defaultCurrency) {
        mutableStateOf(
            if (currencies.contains(defaultCurrency)) defaultCurrency else currencies.first()
        )
    }

    val icons = remember {
        listOf(
            "Wallet", "Bank", "CreditCard", "Savings",
            "Calculate", "AttachMoney", "ShoppingCart", "Home",
            "DirectionsCar", "Flight", "Restaurant", "MedicalServices",
            "School", "Pets", "Redeem", "MoreHoriz"
        ).map { it to IconMapper.getIcon(it) }
    }

    var selectedIcon by remember {
        mutableStateOf(
            if (icons.any { it.first == "Wallet" }) "Wallet" else icons.first().first
        )
    }

    var isDataLoaded by remember { mutableStateOf(false) }

    // --- 编辑回填 ---
    LaunchedEffect(accountId, allAccounts) {
        if (accountId != null && !isDataLoaded && allAccounts.isNotEmpty()) {
            val acc = allAccounts.find { it.id == accountId } ?: return@LaunchedEffect

            accountName = acc.name
            selectedCurrency = acc.currency
            selectedIcon = acc.iconName

            category = acc.category
            debtType = when (acc.category) {
                "DEBT" -> acc.debtType ?: "PAYABLE"
                else -> acc.debtType
            }

            selectedTypeKey = AccountTypeManager.getStableKey(acc.type)

            when (acc.category) {
                "CREDIT" -> {
                    val debt = max(acc.initialBalance, 0.0)
                    amountInput = formatNumberForInput(debt)
                    val limit = max((acc.creditLimit ?: 0.0), 0.0)
                    creditLimitInput = formatNumberForInput(limit)
                }
                "DEBT" -> {
                    amountInput = formatNumberForInput(max(acc.initialBalance, 0.0))
                }
                else -> {
                    amountInput = formatNumberForInput(acc.initialBalance)
                }
            }

            isDataLoaded = true
        }
    }

    // --- UI 颜色 ---
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bgColor = Color.White

    // [修改] 预览标签多语言化
    val previewLabel = when (category) {
        "CREDIT" -> stringResource(R.string.preview_label_debt)
        "DEBT" -> stringResource(R.string.preview_label_unsettled)
        else -> stringResource(R.string.preview_label_balance)
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(if (accountId == null) R.string.add_account_title else R.string.edit_account_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val name = accountName.trim()
                            if (name.isBlank()) return@IconButton

                            val amountVal = amountInput.toDoubleOrNull() ?: 0.0
                            val limitVal = creditLimitInput.toDoubleOrNull() ?: 0.0

                            val isLiability = category == "CREDIT"

                            val finalType = when (category) {
                                "DEBT" -> "debt"
                                else -> selectedTypeKey
                            }

                            val finalInitialBalance = when (category) {
                                "CREDIT" -> max(amountVal, 0.0)
                                "DEBT" -> max(amountVal, 0.0)
                                else -> amountVal
                            }

                            val finalDebtType = if (category == "DEBT") {
                                (debtType ?: "PAYABLE")
                            } else {
                                null
                            }

                            val toSave = Account(
                                id = accountId ?: 0L,
                                name = name,
                                type = finalType,
                                currency = selectedCurrency,
                                initialBalance = finalInitialBalance,
                                iconName = selectedIcon,
                                isLiability = isLiability,

                                category = category,
                                creditLimit = if (category == "CREDIT") max(limitVal, 0.0) else 0.0,
                                debtType = finalDebtType
                            )

                            if (accountId == null) {
                                viewModel.insertAccount(toSave.copy(id = 0L))
                            } else {
                                viewModel.updateAccount(toSave)
                            }

                            navController.popBackStack()
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
            // 预览卡片
            AccountPreviewCard(
                name = if (accountName.isBlank()) stringResource(R.string.account_name_placeholder) else accountName,
                balance = amountInput,
                currency = selectedCurrency,
                iconName = selectedIcon,
                typeKey = selectedTypeKey,
                primaryColor = primaryColor,
                overrideTypeText = CategoryBadgeText(category, debtType), // [修改] 使用 Composable 获取文本
                overrideBalanceLabel = previewLabel
            )

            // 表单区域
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

                    CategoryRow(category = category, debtType = debtType)

                    Spacer(Modifier.height(12.dp))

                    if (category != "DEBT") {
                        AccountTypeDropdown(
                            label = stringResource(R.string.account_type),
                            options = accountTypes,
                            selectedKey = selectedTypeKey,
                            onOptionSelected = { selectedTypeKey = it }
                        )
                        Spacer(Modifier.height(16.dp))
                    } else {
                        DebtTypeSelector(
                            debtType = debtType,
                            onChange = { debtType = it }
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    DropdownInput(
                        label = stringResource(R.string.currency),
                        options = currencies,
                        selectedOption = selectedCurrency,
                        onOptionSelected = { selectedCurrency = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    // [修改] 金额输入框标签动态化
                    val amountLabel = when (category) {
                        "CREDIT" -> stringResource(R.string.preview_label_debt)
                        "DEBT" -> stringResource(R.string.preview_label_unsettled)
                        else -> stringResource(R.string.current_balance)
                    }

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text(amountLabel) },
                        placeholder = { Text(stringResource(R.string.balance_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (category == "CREDIT") {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = creditLimitInput,
                            onValueChange = { creditLimitInput = it },
                            label = { Text(stringResource(R.string.label_credit_limit)) },
                            placeholder = { Text(stringResource(R.string.placeholder_credit_limit)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 图标选择器
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
                        modifier = Modifier.heightIn(max = 350.dp)
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

// ===== 小组件：类别显示 =====
@Composable
private fun CategoryRow(category: String, debtType: String?) {
    val text = CategoryBadgeText(category, debtType)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.label_account_category), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        AssistChip(
            onClick = { /* 不可切换 */ },
            label = { Text(text) }
        )
    }
}

// [修改] 改为 Composable 以支持 stringResource
@Composable
private fun CategoryBadgeText(category: String, debtType: String?): String {
    return when (category) {
        "CREDIT" -> stringResource(R.string.category_credit_account)
        "DEBT" -> if ((debtType ?: "PAYABLE") == "RECEIVABLE") stringResource(R.string.category_lend_receivable) else stringResource(R.string.category_borrow_payable)
        else -> stringResource(R.string.category_funds_account)
    }
}

@Composable
private fun DebtTypeSelector(
    debtType: String?,
    onChange: (String) -> Unit
) {
    val current = debtType ?: "PAYABLE"
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.label_debt_type), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = current == "PAYABLE",
                onClick = { onChange("PAYABLE") },
                label = { Text(stringResource(R.string.chip_payable)) }
            )
            FilterChip(
                selected = current == "RECEIVABLE",
                onClick = { onChange("RECEIVABLE") },
                label = { Text(stringResource(R.string.chip_receivable)) }
            )
        }
    }
}

// ===== 预览卡片（可覆盖类型/余额标签）=====
@Composable
fun AccountPreviewCard(
    name: String,
    balance: String,
    currency: String,
    iconName: String?,
    typeKey: String,
    primaryColor: Color,
    overrideTypeText: String? = null,
    overrideBalanceLabel: String? = null
) {
    val displayBalance = if (balance.isBlank()) "0.00" else balance
    // 如果有 override 则用 override，否则去 AccountTypeManager 找翻译
    val typeName = overrideTypeText ?: run {
        val resId = AccountTypeManager.getTypeResId(typeKey)
        if (resId != null) stringResource(resId) else typeKey
    }

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
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
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
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = typeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Column {
                    Text(
                        text = overrideBalanceLabel ?: stringResource(R.string.current_balance),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))

                    val balanceValue = remember(displayBalance) {
                        displayBalance.trim().replace(",", "").toDoubleOrNull() ?: 0.0
                    }

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

// --- 图标选择项 ---
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
            .border(border, if (isSelected) primaryColor else Color.Transparent, RoundedCornerShape(16.dp))
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

// --- 通用下拉选择框 ---
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

// --- 账户类型专用下拉框 (支持多语言) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTypeDropdown(
    label: String,
    options: List<Pair<String, Int>>, // Key, ResId
    selectedKey: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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

private fun formatNumberForInput(v: Double): String {
    return when {
        v == 0.0 -> ""
        v % 1.0 == 0.0 -> v.toLong().toString()
        else -> String.format(Locale.US, "%.2f", v)
    }
}