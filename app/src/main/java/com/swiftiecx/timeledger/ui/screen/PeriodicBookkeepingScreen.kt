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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.PeriodicTransaction
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicBookkeepingScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel
) {
    val periodicList by viewModel.allPeriodicTransactions.collectAsState()
    val context = LocalContext.current

    // [Fix] 动态构建图标映射表
    val categoryIconMap = remember(context) {
        val expenses = CategoryData.getExpenseCategories(context).flatMap { it.subCategories }
        val incomes = CategoryData.getIncomeCategories(context).flatMap { it.subCategories }
        (expenses + incomes).associate { it.title to it.icon }
    }

    var showTypeSheet by remember { mutableStateOf(false) }

    Scaffold(
        // ✅ [修改 1] 强制背景色为纯白，彻底去除默认紫色
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.periodic_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                // ✅ [修改 2] 顶部栏背景也强制为纯白
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTypeSheet = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.add))
            }
        }
    ) { padding ->
        if (periodicList.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Repeat, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.periodic_empty_hint), color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(periodicList) { item ->
                    PeriodicItem(
                        item = item,
                        icon = categoryIconMap[item.category],
                        onDelete = { viewModel.deletePeriodic(item) },
                        onClick = {
                            navController.navigate("add_periodic_transaction?id=${item.id}&type=${item.type}")
                        }
                    )
                }
            }
        }

        if (showTypeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTypeSheet = false },
                // ✅ [修改 3] 底部弹窗背景强制为纯白
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        stringResource(R.string.add),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                    )
                    // 周期支出
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.type_expense)) },
                        leadingContent = { Icon(Icons.Outlined.CreditCard, null, tint = Color(0xFFE53935)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            showTypeSheet = false
                            navController.navigate("add_periodic_transaction?id=-1&type=0")
                        }
                    )
                    // 周期收入
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.type_income)) },
                        leadingContent = { Icon(Icons.Outlined.Paid, null, tint = Color(0xFF4CAF50)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            showTypeSheet = false
                            navController.navigate("add_periodic_transaction?id=-1&type=1")
                        }
                    )
                    // 周期转账
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.type_transfer)) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            showTypeSheet = false
                            navController.navigate("add_periodic_transaction?id=-1&type=2")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodicItem(
    item: PeriodicTransaction,
    icon: ImageVector?,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    // [i18n] 动态获取频率文本
    val frequencyStr = when(item.frequency) {
        0 -> stringResource(R.string.freq_day)
        1 -> stringResource(R.string.freq_week)
        2 -> stringResource(R.string.freq_month)
        3 -> stringResource(R.string.freq_year)
        else -> ""
    }

    val typeStr = when(item.type) {
        0 -> stringResource(R.string.type_expense)
        1 -> stringResource(R.string.type_income)
        2 -> stringResource(R.string.type_transfer)
        else -> ""
    }

    // 结束规则描述 (简单处理日期格式)
    val endStr = when(item.endMode) {
        1 -> " · " + SimpleDateFormat("MM-dd", Locale.getDefault()).format(item.endDate!!)
        2 -> " · ${item.endCount}x"
        else -> ""
    }

    val color = when(item.type) {
        0 -> Color(0xFFE53935)
        1 -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null && item.type != 2) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                } else if (item.type == 2) {
                    Icon(Icons.Default.Repeat, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("$frequencyStr · $typeStr$endStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format(Locale.US, "%.2f", item.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}