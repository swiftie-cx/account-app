package com.swiftiecx.timeledger.ui.feature.debt.screen

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource // [新增]
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增]
import com.swiftiecx.timeledger.data.DebtRecord
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPersonDetailScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    personName: String
) {
    val records by viewModel.getDebtRecordsByPerson(personName).collectAsState(initial = emptyList())
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    // --- 状态控制 ---
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // --- 数据统计逻辑 ---
    val interestRegex = "\\|利息:([\\d.]+)\\|".toRegex()
    // 注意：Regex 匹配逻辑依赖于写入时的字符串格式，如果是多语言环境写入的 Note 可能不匹配
    // 这里为了兼容性，暂时保持 Regex 不变，假设底层数据存储格式是固定的

    val totalInterest = records.sumOf { record ->
        interestRegex.find(record.note ?: "")?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    val originalLend = records.filter { it.outAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }
    val originalBorrow = records.filter { it.inAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }

    val isReceivable = (originalLend - originalBorrow) >= 0

    val settledIn = records.filter { it.inAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }
    val settledOut = records.filter { it.outAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }

    val collectedPrincipal = if (isReceivable) settledIn else settledOut
    val remainingAmount = if (isReceivable) (originalLend - originalBorrow - settledIn)
    else (originalBorrow - originalLend - settledOut)

    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) } // 改为国际通用格式

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "$personName - ${if (isReceivable) stringResource(R.string.title_receivable_detail) else stringResource(R.string.title_payable_detail)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = Color.White,
                            tonalElevation = 0.dp
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete_debt), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- 顶部汇总卡片 ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isReceivable) stringResource(R.string.label_remaining_receivable) else stringResource(R.string.label_remaining_payable),
                                color = Color.White.copy(0.8f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = String.format("%.2f", abs(remainingAmount)),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isReceivable) stringResource(R.string.label_collected_principal) else stringResource(R.string.label_repaid_principal),
                                color = Color.White.copy(0.8f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = String.format("%.2f", collectedPrincipal),
                                color = Color.White,
                                fontSize = 26.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${if (isReceivable) stringResource(R.string.label_collected_interest) else stringResource(R.string.label_repaid_interest)}: ${String.format("%.2f", totalInterest)}",
                        color = Color.White.copy(0.8f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${stringResource(R.string.label_next_reminder)}: ${stringResource(R.string.not_set)}",
                        color = Color.White.copy(0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            // --- 操作按钮组 ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Add,
                    label = if (isReceivable) stringResource(R.string.action_append_lend) else stringResource(R.string.action_append_borrow),
                    onClick = {
                        if (isReceivable) navController.navigate(Routes.addLendRoute(-1L, personName))
                        else navController.navigate(Routes.addBorrowRoute(-1L, personName))
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Payment,
                    label = if (isReceivable) stringResource(R.string.action_collect) else stringResource(R.string.action_repay),
                    onClick = {
                        val amountToPass = abs(remainingAmount)
                        val isBorrow = !isReceivable
                        navController.navigate("settle_debt/$personName/$isBorrow/$amountToPass")
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = stringResource(R.string.header_history_detail),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            )

            // --- 往来记录列表卡片 ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.5.dp)
            ) {
                Column {
                    records.forEachIndexed { index, record ->
                        DebtDetailSimpleItem(
                            record = record,
                            dateFormat = dateFormat,
                            allAccounts = allAccounts,
                            onClick = {
                                navController.navigate(Routes.editDebtRoute(record.id))
                            }
                        )
                        if (index < records.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_debt_msg, personName)) }, // 带参数的字符串
            containerColor = Color.White,
            tonalElevation = 0.dp,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebtRecordsByPerson(personName)
                        showDeleteConfirm = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(1.dp)
    ) {
        Icon(icon, null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.Black, fontSize = 14.sp)
    }
}

@Composable
fun DebtDetailSimpleItem(
    record: DebtRecord,
    dateFormat: SimpleDateFormat,
    allAccounts: List<Account>,
    onClick: () -> Unit
) {
    val interestRegex = "\\|利息:([\\d.]+)\\|".toRegex()
    val interestAmount = interestRegex.find(record.note ?: "")?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

    val isSettle = record.note.toString().contains("结算")
    val isIncoming = record.inAccountId != null

    val fundAccountId = record.inAccountId ?: record.outAccountId
    val accountName = allAccounts.find { it.id == fundAccountId }?.name ?: stringResource(R.string.no_account)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // 构建交易描述文本
            val typeText = when {
                isSettle && isIncoming -> stringResource(R.string.type_collection)
                isSettle && !isIncoming -> stringResource(R.string.type_repayment)
                !isSettle && isIncoming -> stringResource(R.string.type_borrow_in)
                else -> stringResource(R.string.type_lend_out)
            }
            Text(
                text = "$typeText ($accountName)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            val dateStr = if (DateUtils.isToday(record.borrowTime.time))
                stringResource(R.string.today)
            else
                dateFormat.format(record.borrowTime)

            Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncoming) "+" else "-"}${String.format("%.2f", record.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isIncoming) Color(0xFF4CAF50) else Color.Black
            )
            if (interestAmount > 0) {
                Text(
                    text = "${stringResource(R.string.label_interest)}: ${String.format("%.2f", interestAmount)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}