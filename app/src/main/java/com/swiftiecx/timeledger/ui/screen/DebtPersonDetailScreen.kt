package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.data.DebtRecord
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPersonDetailScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    personName: String
) {
    val records by viewModel.getDebtRecordsByPerson(personName).collectAsState(initial = emptyList())
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val currency by viewModel.defaultCurrency.collectAsState()

    // --- 状态控制 ---
    var menuExpanded by remember { mutableStateOf(false) } // 控制下拉菜单显示
    var showDeleteConfirm by remember { mutableStateOf(false) } // 控制删除确认弹窗

    // --- 数据统计逻辑 ---
    val interestRegex = "\\|利息:([\\d.]+)\\|".toRegex()

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

    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$personName - ${if (isReceivable) "应收款" else "应付款"}", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // ✅ 右上角三个点菜单
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("删除债务", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirm = true // 触发确认弹窗
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
            // --- 顶部汇总卡片 (墨绿色) ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF67A18C))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isReceivable) "剩余待收" else "剩余待还", color = Color.White.copy(0.8f), fontSize = 13.sp)
                            Text(
                                text = String.format("%.2f", Math.abs(remainingAmount)),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isReceivable) "已收本金:" else "已还本金:", color = Color.White.copy(0.8f), fontSize = 13.sp)
                            Text(
                                text = String.format("%.2f", collectedPrincipal),
                                color = Color.White,
                                fontSize = 26.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${if (isReceivable) "已收利息" else "已还利息"}: ${String.format("%.2f", totalInterest)}",
                        color = Color.White.copy(0.8f),
                        fontSize = 12.sp
                    )
                    Text(text = "下次提醒: 未设置", color = Color.White.copy(0.8f), fontSize = 12.sp)
                }
            }

            // --- 操作按钮组 ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Add,
                    label = if (isReceivable) "追加借出" else "追加借入",
                    onClick = {
                        if (isReceivable) navController.navigate(Routes.addLendRoute(-1L, personName))
                        else navController.navigate(Routes.addBorrowRoute(-1L, personName))
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Payment,
                    label = if (isReceivable) "收款" else "还款",
                    onClick = {
                        navController.navigate(Routes.settleDebtRoute(personName, !isReceivable))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "往来明细",
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

    // ✅ 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除？") },
            text = { Text("这将永久删除与“$personName”相关的所有借贷记录，且无法撤销。对应的账单流水也会受到影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebtRecordsByPerson(personName)
                        showDeleteConfirm = false
                        navController.popBackStack() // 删除后返回上一页
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    val accountName = allAccounts.find { it.id == fundAccountId }?.name ?: "无账户"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = when {
                    isSettle && isIncoming -> "收款 ($accountName)"
                    isSettle && !isIncoming -> "还款 ($accountName)"
                    !isSettle && isIncoming -> "借入 ($accountName)"
                    else -> "借出 ($accountName)"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            val dateStr = if (android.text.format.DateUtils.isToday(record.borrowTime.time)) "今天"
            else dateFormat.format(record.borrowTime)
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
                    text = "利息: ${String.format("%.2f", interestAmount)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}