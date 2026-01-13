package com.swiftiecx.timeledger.ui.feature.debt.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource // [新增]
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增]
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDebtScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    recordId: Long
) {
    val record by viewModel.getDebtRecordById(recordId).collectAsState(initial = null)

    // 获取所有账户，用于查找关联账户的货币
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    // 获取默认货币作为兜底
    val defaultCurrency by viewModel.defaultCurrency.collectAsState(initial = "CNY")

    // 状态管理
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var borrowDate by remember { mutableStateOf(Date()) }
    var settleDate by remember { mutableStateOf<Date?>(null) }

    // 弹窗控制
    var showBorrowPicker by remember { mutableStateOf(false) }
    var showSettlePicker by remember { mutableStateOf(false) }

    // 初始化数据
    LaunchedEffect(record) {
        record?.let {
            amount = it.amount.toString()
            note = it.note?.replace(Regex("\\|利息:[\\d.]+\\|"), "")?.trim() ?: ""
            borrowDate = it.borrowTime
            settleDate = it.settleTime
        }
    }

    if (record == null) return

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val isSettleRecord = record?.note?.contains("结算") == true

    // --- 动态计算货币符号逻辑 ---
    val currencySymbol = remember(record, allAccounts, defaultCurrency) {
        val accountId = record?.inAccountId ?: record?.outAccountId
        val account = allAccounts.find { it.id == accountId }
        val currencyCode = account?.currency ?: defaultCurrency
        try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            "¥"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // [修改] 标题多语言化
                    val titleText = if (isSettleRecord) stringResource(R.string.title_edit_settle) else stringResource(R.string.title_edit_debt)
                    Text(titleText, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        record?.let { viewModel.deleteDebtRecord(it) }
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. 金额输入 ---
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.amount_label)) },
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // --- 2. 日期选择 ---
            // 发生日期
            Box(modifier = Modifier.fillMaxWidth()) {
                val labelText = if (isSettleRecord) stringResource(R.string.label_settle_date) else stringResource(R.string.label_date)
                OutlinedTextField(
                    value = dateFormat.format(borrowDate),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(labelText) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showBorrowPicker = true }
                )
            }

            // 到期日期
            Box(modifier = Modifier.fillMaxWidth()) {
                val labelText = if (isSettleRecord) stringResource(R.string.label_due_date) else stringResource(R.string.label_expected_date)
                OutlinedTextField(
                    value = settleDate?.let { dateFormat.format(it) } ?: stringResource(R.string.not_set),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(labelText) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showSettlePicker = true }
                )
            }

            // --- 3. 备注输入 ---
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_note)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                placeholder = { Text(stringResource(R.string.remark_hint)) },
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- 4. 底部保存按钮 ---
            Button(
                onClick = {
                    val updated = record?.copy(
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        note = note,
                        borrowTime = borrowDate,
                        settleTime = settleDate
                    )
                    updated?.let {
                        viewModel.updateDebtRecord(it)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 日期选择弹窗 (复用 AddDebtScreen 的样式逻辑)
    if (showBorrowPicker) {
        DebtDatePicker(
            initialDate = borrowDate,
            onDateSelected = { borrowDate = it; showBorrowPicker = false },
            onDismiss = { showBorrowPicker = false }
        )
    }
    if (showSettlePicker) {
        DebtDatePicker(
            initialDate = settleDate ?: Date(),
            onDateSelected = { settleDate = it; showSettlePicker = false },
            onDismiss = { showSettlePicker = false }
        )
    }
}