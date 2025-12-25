package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtManagementScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    // 监听全局借贷记录和货币设置
    val allRecords by viewModel.getAllDebtRecords().collectAsState(initial = emptyList())
    val currency by viewModel.defaultCurrency.collectAsState()

    // 控制新增债务选择弹窗的状态
    var showAddSheet by remember { mutableStateOf(false) }

    // --- 数据逻辑处理：按姓名归类并计算净额 ---
    val personSummaries = remember(allRecords) {
        allRecords.groupBy { it.personName }.map { (name, records) ->
            val lendSum = records.filter { it.outAccountId != null }.sumOf { it.amount }
            val borrowSum = records.filter { it.inAccountId != null }.sumOf { it.amount }
            PersonDebtSummary(
                name = name,
                netBalance = lendSum - borrowSum // 正数为应收，负数为欠款
            )
        }
    }

    val receivablePeople = personSummaries.filter { it.netBalance > 0 }
    val payablePeople = personSummaries.filter { it.netBalance < 0 }

    // 总计数据
    val totalLend = personSummaries.filter { it.netBalance > 0 }.sumOf { it.netBalance }
    val totalBorrow = personSummaries.filter { it.netBalance < 0 }.sumOf { kotlin.math.abs(it.netBalance) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("债务管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        // --- 浮动操作按钮 ---
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. 顶部汇总卡片 (保持红绿色以区分状态)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DebtSummaryCard(
                            label = "总借出 (应收)",
                            amount = totalLend,
                            currency = currency,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        DebtSummaryCard(
                            label = "总借入 (欠款)",
                            amount = totalBorrow,
                            currency = currency,
                            color = Color(0xFFE53935),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. 应收明细框体
                if (receivablePeople.isNotEmpty()) {
                    item {
                        DebtGroupContainer(
                            title = "应收明细 (资产)",
                            people = receivablePeople,
                            currency = currency,
                            isReceivable = true,
                            onPersonClick = { name ->
                                navController.navigate(Routes.debtPersonDetailRoute(name))
                            }
                        )
                    }
                }

                // 3. 欠款明细框体
                if (payablePeople.isNotEmpty()) {
                    item {
                        DebtGroupContainer(
                            title = "欠款明细 (负债)",
                            people = payablePeople,
                            currency = currency,
                            isReceivable = false,
                            onPersonClick = { name ->
                                navController.navigate(Routes.debtPersonDetailRoute(name))
                            }
                        )
                    }
                }

                // 兜底提示
                if (personSummaries.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            Text("暂无借贷记录", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // --- 底部弹窗：选择借入或借出 ---
            if (showAddSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddSheet = false },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "选择债务类型",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // 借入选项 - [修改] 颜色统一为 Primary
                        AddOptionItem(
                            title = "新增借入",
                            subtitle = "记录您欠别人的款项",
                            icon = Icons.Default.ArrowCircleDown,
                            iconColor = MaterialTheme.colorScheme.primary, // 改为主题色
                            onClick = {
                                showAddSheet = false
                                navController.navigate(Routes.addBorrowRoute(-1L))
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        // 借出选项 - [修改] 颜色统一为 Primary
                        AddOptionItem(
                            title = "新增借出",
                            subtitle = "记录别人欠您的款项",
                            icon = Icons.Default.ArrowCircleUp,
                            iconColor = MaterialTheme.colorScheme.primary, // 改为主题色
                            onClick = {
                                showAddSheet = false
                                navController.navigate(Routes.addLendRoute(-1L))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 弹窗中的可点击条目
 */
@Composable
fun AddOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)), // 背景色会自动跟随 iconColor 变为主题色的浅色
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * 顶部统计小卡片
 */
@Composable
fun DebtSummaryCard(label: String, amount: Double, currency: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$currency ${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * 分组框体容器
 */
@Composable
fun DebtGroupContainer(
    title: String,
    people: List<PersonDebtSummary>,
    currency: String,
    isReceivable: Boolean,
    onPersonClick: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            color = if (isReceivable) Color(0xFF4CAF50) else Color(0xFFE53935)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column {
                people.forEachIndexed { index, person ->
                    DebtPersonRow(
                        person = person,
                        currency = currency,
                        isReceivable = isReceivable,
                        onClick = { onPersonClick(person.name) }
                    )
                    if (index < people.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个人员统计条目
 */
@Composable
fun DebtPersonRow(
    person: PersonDebtSummary,
    currency: String,
    isReceivable: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = person.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = person.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (isReceivable) "应收" else "欠款",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "$currency ${String.format("%.2f", kotlin.math.abs(person.netBalance))}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isReceivable) Color(0xFF4CAF50) else Color(0xFFE53935)
            )
        }

        Spacer(Modifier.width(4.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 数据辅助类
 */
data class PersonDebtSummary(
    val name: String,
    val netBalance: Double
)