package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.data.DebtRecord
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

    // 状态管理
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var borrowDate by remember { mutableStateOf(Date()) }
    var settleDate by remember { mutableStateOf<Date?>(null) }

    // 用于同步修改流水的旧数据备份
    var originalDate by remember { mutableStateOf(Date()) }
    var originalAmount by remember { mutableStateOf(0.0) }

    // 弹窗控制
    var showBorrowPicker by remember { mutableStateOf(false) }
    var showSettlePicker by remember { mutableStateOf(false) }

    LaunchedEffect(record) {
        record?.let {
            amount = it.amount.toString()
            note = it.note?.replace(Regex("\\|利息:[\\d.]+\\|"), "")?.trim() ?: ""
            borrowDate = it.borrowTime
            settleDate = it.settleTime
            originalDate = it.borrowTime
            originalAmount = it.amount
        }
    }

    if (record == null) return
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }
    val isSettleRecord = record?.note?.contains("结算") == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(isSettleRecord) "编辑收款/还款" else "编辑借出/借入", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.Close, null) } },
                actions = {
                    IconButton(onClick = {
                        record?.let { viewModel.deleteDebtRecord(it) }
                        navController.popBackStack()
                    }) { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }

                    IconButton(onClick = {
                        val updated = record?.copy(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            note = note,
                            borrowTime = borrowDate,
                            settleTime = settleDate
                        )
                        updated?.let { viewModel.updateDebtWithTransaction(it, originalDate, originalAmount) }
                        navController.popBackStack()
                    }) { Icon(Icons.Default.Check, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            // 金额与备注卡片
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row {
                        Text("金额", modifier = Modifier.width(80.dp))
                        TextField(
                            value = amount,
                            onValueChange = { amount = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                    }
                    Row {
                        Text("备注", modifier = Modifier.width(80.dp))
                        TextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 日期编辑卡片
            Card(shape = RoundedCornerShape(12.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text(if(isSettleRecord) "结算日期" else "借入/借出日期") },
                        supportingContent = { Text("此日期将同步更新到明细页流水") },
                        trailingContent = { Text(dateFormat.format(borrowDate), color = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { showBorrowPicker = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    ListItem(
                        headlineContent = { Text(if(isSettleRecord) "到期日期" else "预计还款/收款日期") },
                        trailingContent = { Text(settleDate?.let { dateFormat.format(it) } ?: "未设置 >") },
                        modifier = Modifier.clickable { showSettlePicker = true }
                    )
                }
            }
        }
    }

    // 日期选择弹窗
    if (showBorrowPicker) {
        DebtDatePicker(initialDate = borrowDate, onDateSelected = { borrowDate = it; showBorrowPicker = false }, onDismiss = { showBorrowPicker = false })
    }
    if (showSettlePicker) {
        DebtDatePicker(initialDate = settleDate ?: Date(), onDateSelected = { settleDate = it; showSettlePicker = false }, onDismiss = { showSettlePicker = false })
    }
}