package com.swiftiecx.timeledger.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource // [新增]
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R // [新增]
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtManagementScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    val allRecords by viewModel.getAllDebtRecords().collectAsState(initial = emptyList())
    val currency by viewModel.defaultCurrency.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    val personSummaries = remember(allRecords) {
        allRecords.groupBy { it.personName }.map { (name, records) ->
            val lendSum = records.filter { it.outAccountId != null }.sumOf { it.amount }
            val borrowSum = records.filter { it.inAccountId != null }.sumOf { it.amount }
            PersonDebtSummary(
                name = name,
                netBalance = lendSum - borrowSum
            )
        }
    }

    val receivablePeople = personSummaries.filter { it.netBalance > 0 }
    val payablePeople = personSummaries.filter { it.netBalance < 0 }
    val totalLend = personSummaries.filter { it.netBalance > 0 }.sumOf { it.netBalance }
    val totalBorrow = personSummaries.filter { it.netBalance < 0 }.sumOf { kotlin.math.abs(it.netBalance) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_debt_management), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
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
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DebtSummaryCard(
                            label = stringResource(R.string.label_total_lend),
                            amount = totalLend,
                            currency = currency,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        DebtSummaryCard(
                            label = stringResource(R.string.label_total_borrow),
                            amount = totalBorrow,
                            currency = currency,
                            color = Color(0xFFE53935),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (receivablePeople.isNotEmpty()) {
                    item {
                        DebtGroupContainer(
                            title = stringResource(R.string.header_receivables),
                            people = receivablePeople,
                            currency = currency,
                            isReceivable = true,
                            onPersonClick = { name -> navController.navigate(Routes.debtPersonDetailRoute(name)) }
                        )
                    }
                }

                if (payablePeople.isNotEmpty()) {
                    item {
                        DebtGroupContainer(
                            title = stringResource(R.string.header_payables),
                            people = payablePeople,
                            currency = currency,
                            isReceivable = false,
                            onPersonClick = { name -> navController.navigate(Routes.debtPersonDetailRoute(name)) }
                        )
                    }
                }

                if (personSummaries.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.empty_debt_records), color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // --- 底部弹窗 ---
            if (showAddSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddSheet = false },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        // 顶部小把手
                        Box(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                .align(Alignment.CenterHorizontally)
                        )

                        Text(
                            text = stringResource(R.string.sheet_title_select_type),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // 借入选项 (红色系)
                        AddOptionItem(
                            title = stringResource(R.string.action_add_borrow),
                            subtitle = stringResource(R.string.subtitle_add_borrow),
                            icon = Icons.Default.ArrowCircleDown,
                            baseColor = Color(0xFFE53935), // 红色基调
                            onClick = {
                                showAddSheet = false
                                navController.navigate(Routes.addBorrowRoute(-1L))
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        // 借出选项 (绿色系)
                        AddOptionItem(
                            title = stringResource(R.string.action_add_lend),
                            subtitle = stringResource(R.string.subtitle_add_lend),
                            icon = Icons.Default.ArrowCircleUp,
                            baseColor = Color(0xFF4CAF50), // 绿色基调
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
 * [修改] 美化后的弹窗选项卡片
 * 使用基调颜色 (baseColor) 生成淡色背景和深色文字，提升视觉区分度
 */
@Composable
fun AddOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    baseColor: Color,
    onClick: () -> Unit
) {
    val containerColor = baseColor.copy(alpha = 0.08f)
    val iconBoxColor = baseColor.copy(alpha = 0.15f)
    val shape = RoundedCornerShape(20.dp) // 定义统一的形状

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, baseColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBoxColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = baseColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = baseColor.copy(alpha = 0.5f)
            )
        }
    }
}

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
                // [修改] 使用 stringResource
                text = if (isReceivable) stringResource(R.string.tag_receivable) else stringResource(R.string.tag_payable),
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

data class PersonDebtSummary(
    val name: String,
    val netBalance: Double
)