package com.swiftiecx.timeledger.ui.screen

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleDebtScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    personName: String,
    isBorrow: Boolean
) {
    // ✅ 核心修复：确保这里全是 var，且使用 by remember 语法
    var amount by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var generateBill by remember { mutableStateOf(true) }
    var showAccountPicker by remember { mutableStateOf(false) }

    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val fundsAccounts = remember(allAccounts) {
        allAccounts.filter { it.category == "FUNDS" || it.category == "CREDIT" }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isBorrow) "还款" else "收款") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                actions = {
                    IconButton(
                        enabled = amount.isNotBlank() && selectedAccountId != null,
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
                        }
                    ) {
                        Icon(Icons.Default.Check, null)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("债务人", modifier = Modifier.width(80.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(personName, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("金额", modifier = Modifier.width(80.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextField(
                            value = amount,
                            onValueChange = { amount = it },
                            placeholder = { Text("请输入金额") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("利息", modifier = Modifier.width(80.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextField(
                            value = interest,
                            onValueChange = { interest = it },
                            placeholder = { Text("非必填") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("备注", modifier = Modifier.width(80.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextField(
                            value = remark,
                            onValueChange = { remark = it },
                            placeholder = { Text("请输入备注") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(if (isBorrow) "还款日期" else "收款日期") },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dateFormat.format(date))
                                Icon(Icons.Default.DateRange, null, Modifier.size(18.dp).padding(start = 4.dp))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        headlineContent = { Text(if (isBorrow) "结算账户" else "收款账户") },
                        trailingContent = {
                            val accName = fundsAccounts.find { it.id == selectedAccountId }?.name ?: "点击选择"
                            Text(accName, color = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable { showAccountPicker = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        headlineContent = { Text("生成账单明细") },
                        supportingContent = { Text("开启后将计入收支明细", fontSize = 11.sp) },
                        trailingContent = {
                            Switch(checked = generateBill, onCheckedChange = { generateBill = it })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    // --- 账户选择逻辑 ---
    if (showAccountPicker) {
        // ✅ 这里直接调用，它会寻找同包名下的定义
        AccountSelectionBottomSheet(
            accounts = fundsAccounts,
            onSelect = { account ->
                selectedAccountId = account.id
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }
}

