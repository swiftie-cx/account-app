package com.swiftiecx.timeledger.ui.screen

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增]
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleDebtScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    personName: String,
    isBorrow: Boolean, // true=还款(我付钱), false=收款(我收钱)
    maxAmount: Double
) {
    // 状态管理
    var amount by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var generateBill by remember { mutableStateOf(true) }

    // 弹窗控制
    var showAccountPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 数据获取
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val defaultCurrency by viewModel.defaultCurrency.collectAsState(initial = "CNY")

    val fundsAccounts = remember(allAccounts) {
        allAccounts.filter { it.category == "FUNDS" || it.category == "CREDIT" }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val currencySymbol = remember(selectedAccountId, allAccounts, defaultCurrency) {
        val account = allAccounts.find { it.id == selectedAccountId }
        val currencyCode = account?.currency ?: defaultCurrency
        try {
            java.util.Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            "¥"
        }
    }

    // [校验逻辑]
    val inputAmount = amount.toDoubleOrNull() ?: 0.0
    val inputInterest = interest.toDoubleOrNull() ?: 0.0
    val principalPart = if (inputAmount > inputInterest) inputAmount - inputInterest else 0.0

    val isOverLimit = principalPart > (maxAmount + 0.01)
    val isAmountValid = inputAmount > 0 && !isOverLimit

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val titleText = if (isBorrow) stringResource(R.string.title_repay) else stringResource(R.string.title_collect)
                        Text(titleText, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.label_related_person, personName), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(Modifier.width(8.dp))

                            val pendingText = if(isBorrow) stringResource(R.string.label_pending_repay, String.format("%.2f", maxAmount))
                            else stringResource(R.string.label_pending_collect, String.format("%.2f", maxAmount))

                            Text(
                                text = pendingText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. 金额输入 ---
            val amountLabel = if (isBorrow) stringResource(R.string.label_repay_amount) else stringResource(R.string.label_collect_amount)

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(amountLabel) },
                leadingIcon = {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                isError = isOverLimit,
                supportingText = if (isOverLimit) {
                    {
                        Text(
                            stringResource(R.string.error_settle_amount_exceeds, String.format("%.2f", principalPart), String.format("%.2f", maxAmount)),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else null
            )

            // --- 2. 利息输入 ---
            OutlinedTextField(
                value = interest,
                onValueChange = { interest = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_interest_optional)) },
                leadingIcon = {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // --- 3. 账户选择 ---
            Box(modifier = Modifier.fillMaxWidth()) {
                val accName = fundsAccounts.find { it.id == selectedAccountId }?.name ?: ""
                val accLabel = if (isBorrow) stringResource(R.string.label_payment_account) else stringResource(R.string.label_deposit_account)

                OutlinedTextField(
                    value = accName,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(accLabel) },
                    placeholder = { Text(stringResource(R.string.hint_select_account)) },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = selectedAccountId == null && amount.isNotEmpty()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showAccountPicker = true }
                )
            }

            // --- 4. 日期选择 ---
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateFormat.format(date),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_date)) },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            // --- 5. 备注输入 ---
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_note)) },
                leadingIcon = {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                placeholder = { Text(stringResource(R.string.hint_optional)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // --- 6. 生成流水开关 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_sync_transaction), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (isBorrow) stringResource(R.string.desc_sync_transaction_repay) else stringResource(R.string.desc_sync_transaction_collect),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = generateBill,
                    onCheckedChange = { generateBill = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- 7. 底部确认按钮 ---
            val btnText = if (isBorrow) stringResource(R.string.action_confirm_repay) else stringResource(R.string.action_confirm_collect)

            Button(
                onClick = {
                    viewModel.settleDebt(
                        personName = personName,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        interest = interest.toDoubleOrNull() ?: 0.0,
                        accountId = selectedAccountId ?: -1L,
                        isBorrow = isBorrow,
                        remark = remark,
                        date = date,
                        generateBill = generateBill
                    )
                    navController.popBackStack()
                },
                enabled = isAmountValid && selectedAccountId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(btnText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- 弹窗逻辑 ---

    if (showAccountPicker) {
        SettleAccountSelectionSheet(
            accounts = fundsAccounts,
            onSelect = { account ->
                selectedAccountId = account.id
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }

    if (showDatePicker) {
        SettleDatePicker(
            initialDate = date,
            onDateSelected = {
                date = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// [保持] 专属美化版 结算页账户选择弹窗 (纯白风格)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleAccountSelectionSheet(
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.select_account),
                style = MaterialTheme.typography.headlineSmall,
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIcon(account.iconName),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${account.currency} ${String.format("%.2f", account.initialBalance)}",
                                    style = MaterialTheme.typography.bodyMedium,
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
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.empty_available_accounts), color = Color.Gray)
                }
            }
        }
    }
}

// [保持] 专属美化版 结算页日期选择器 (纯白风格)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleDatePicker(
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
        colors = DatePickerDefaults.colors(containerColor = Color.White)
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(R.string.select_date),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        )
    }
}