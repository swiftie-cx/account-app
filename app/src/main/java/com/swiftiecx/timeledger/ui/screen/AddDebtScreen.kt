package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.DebtRecord
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    accountId: Long, // 债务账户ID，如果是从资产页新建则为-1
    isBorrow: Boolean // true: 新增借入, false: 新增借出
) {
    val context = LocalContext.current
    // 从 ViewModel 获取所有账户
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    // 筛选出资产账户(FUNDS)和信贷账户(CREDIT)
    val filteredAccounts = remember(allAccounts) {
        allAccounts.filter { it.category == "FUNDS" || it.category == "CREDIT" }
    }

    // 表单状态
    var personName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var borrowTime by remember { mutableStateOf(Date()) }
    var settleTime by remember { mutableStateOf<Date?>(null) }

    // 选中的资金/支出账户ID
    var selectedFundsAccountId by remember { mutableStateOf<Long?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // 弹窗控制
    var showBorrowDatePicker by remember { mutableStateOf(false) }
    var showSettleDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBorrow) "新增借入" else "新增借出",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 姓名
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text("姓名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 金额
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金额") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 发生时间
            OutlinedTextField(
                value = dateFormat.format(borrowTime),
                onValueChange = {},
                label = { Text(if (isBorrow) "借入时间" else "借出时间") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { showBorrowDatePicker = true }) {
                        Icon(Icons.Default.DateRange, "选择日期")
                    }
                }
            )

            // 到期时间
            OutlinedTextField(
                value = settleTime?.let { dateFormat.format(it) } ?: "未设置",
                onValueChange = {},
                label = { Text(if (isBorrow) "还款时间" else "收款时间") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { showSettleDatePicker = true }) {
                        Icon(Icons.Default.DateRange, "选择日期")
                    }
                }
            )

            // --- 资金账户选择 (实装部分) ---
            val selectedAccount = filteredAccounts.find { it.id == selectedFundsAccountId }
            OutlinedTextField(
                value = selectedAccount?.name ?: "点击选择账户",
                onValueChange = {},
                label = { Text(if (isBorrow) "资金到账账户" else "资金支出账户") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAccountPicker = true },
                readOnly = true,
                enabled = false, // 禁用输入，仅响应点击
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
            )

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 保存按钮
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    val record = DebtRecord(
                        accountId = accountId,
                        personName = personName,
                        amount = amountVal,
                        note = note.takeIf { it.isNotBlank() },
                        borrowTime = borrowTime,
                        settleTime = settleTime,
                        inAccountId = if (isBorrow) selectedFundsAccountId else null,
                        outAccountId = if (!isBorrow) selectedFundsAccountId else null
                    )
                    viewModel.insertDebtRecord(record) //
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = personName.isNotBlank() && amount.isNotBlank() && selectedFundsAccountId != null
            ) {
                Text("确认保存", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // --- 日期选择器 ---
    if (showBorrowDatePicker) {
        DebtDatePicker(
            initialDate = borrowTime,
            onDateSelected = { borrowTime = it; showBorrowDatePicker = false },
            onDismiss = { showBorrowDatePicker = false }
        )
    }

    if (showSettleDatePicker) {
        DebtDatePicker(
            initialDate = settleTime ?: Date(),
            onDateSelected = { settleTime = it; showSettleDatePicker = false },
            onDismiss = { showSettleDatePicker = false }
        )
    }

    // --- 账户选择底部弹窗 (实装部分) ---
    if (showAccountPicker) {
        AccountSelectionBottomSheet(
            accounts = filteredAccounts,
            onSelect = {
                selectedFundsAccountId = it.id
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionBottomSheet(
    accounts: List<Account>,
    onSelect: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "选择资金账户",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts) { account ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(account) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIcon(account.iconName),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${account.currency} ${String.format("%.2f", account.initialBalance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无可用资金账户", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDatePicker(
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.time)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(Date(it)) }
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}