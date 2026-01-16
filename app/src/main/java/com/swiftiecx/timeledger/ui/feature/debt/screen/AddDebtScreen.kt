package com.swiftiecx.timeledger.ui.feature.debt.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource // [新增]
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增]
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.local.entity.DebtRecord
import com.swiftiecx.timeledger.ui.common.IconMapper
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    accountId: Long,
    isBorrow: Boolean, // true: 新增借入, false: 新增借出
    presetName: String? = null
) {
    // --- 状态与逻辑初始化 ---
    val isAppendMode = !presetName.isNullOrEmpty()

    var personName by remember { mutableStateOf(presetName ?: "") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var excludeFromStats by remember { mutableStateOf(false) } // 不计入收支
    var borrowTime by remember { mutableStateOf(Date()) }
    var settleTime by remember { mutableStateOf<Date?>(null) }

    var selectedFundsAccountId by remember { mutableStateOf<Long?>(null) }

    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val filteredAccounts = remember(allAccounts) {
        allAccounts.filter { it.category == "FUNDS" || it.category == "CREDIT" }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var showBorrowDatePicker by remember { mutableStateOf(false) }
    var showSettleDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }

    // [修改] 动态标题文本
    val screenTitle = if (isBorrow) stringResource(R.string.title_add_borrow) else stringResource(R.string.title_add_lend)

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
            // 姓名输入框
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    if (!isAppendMode) personName = it
                },
                label = { Text(stringResource(R.string.label_person_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                readOnly = isAppendMode,
                trailingIcon = {
                    if (isAppendMode) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.status_locked),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = if (isAppendMode) {
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                } else {
                    OutlinedTextFieldDefaults.colors()
                }
            )

            // 金额
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.amount_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 发生时间
            val borrowDateLabel = if (isBorrow) stringResource(R.string.label_borrow_time) else stringResource(R.string.label_lend_time)
            OutlinedTextField(
                value = dateFormat.format(borrowTime),
                onValueChange = {},
                label = { Text(borrowDateLabel) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { showBorrowDatePicker = true }) {
                        Icon(Icons.Default.DateRange, stringResource(R.string.select_date))
                    }
                }
            )

            // 到期时间 (选填)
            val settleDateLabel = if (isBorrow) stringResource(R.string.label_expected_repay_time) else stringResource(R.string.label_expected_collect_time)
            OutlinedTextField(
                value = settleTime?.let { dateFormat.format(it) } ?: stringResource(R.string.not_set),
                onValueChange = {},
                label = { Text(settleDateLabel) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { showSettleDatePicker = true }) {
                        Icon(Icons.Default.DateRange, stringResource(R.string.select_date))
                    }
                }
            )

            // 资金账户选择
            val selectedAccount = filteredAccounts.find { it.id == selectedFundsAccountId }
            val accountLabel = if (isBorrow) stringResource(R.string.label_target_fund_account) else stringResource(R.string.label_source_fund_account)

            OutlinedTextField(
                value = selectedAccount?.name ?: stringResource(R.string.hint_select_account),
                onValueChange = {},
                label = { Text(accountLabel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAccountPicker = true },
                readOnly = true,
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = Color.Transparent
                ),
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
            )

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.label_note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )



            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.exclude_from_income_expense),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = excludeFromStats,
                    onCheckedChange = { excludeFromStats = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    viewModel.insertDebtRecord(record, countInStats = !excludeFromStats)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = personName.isNotBlank() && amount.isNotBlank() && selectedFundsAccountId != null
            ) {
                Text(stringResource(R.string.confirm_save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // --- 日期选择器逻辑 ---
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

    // --- 账户选择弹窗 ---
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.title_select_fund_account),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts) { account ->
                    Card(
                        onClick = { onSelect(account) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIcon(account.iconName),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${account.currency} ${String.format("%.2f", account.initialBalance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.empty_fund_accounts), color = Color.Gray)
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
            }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        colors = DatePickerDefaults.colors(
            containerColor = Color.White
        )
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(R.string.select_date),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            )
        )
    }
}