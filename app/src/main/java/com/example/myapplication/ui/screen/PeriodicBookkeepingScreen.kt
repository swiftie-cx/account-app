package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape // 确保导入了这个
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.PeriodicTransaction
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicBookkeepingScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel
) {
    val periodicList by viewModel.allPeriodicTransactions.collectAsState()
    val allCategories = expenseCategories + incomeCategories
    val categoryIconMap = remember { allCategories.associate { it.title to it.icon } }

    var showTypeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("周期记账") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            // 【关键修改】添加样式参数，使其变成圆形且颜色一致
            FloatingActionButton(
                onClick = { showTypeSheet = true },
                shape = CircleShape, // 变成圆形
                containerColor = MaterialTheme.colorScheme.primary, // 使用主题紫色
                contentColor = MaterialTheme.colorScheme.onPrimary // 图标变白
            ) {
                Icon(Icons.Default.Add, "添加")
            }
        }
    ) { padding ->
        if (periodicList.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Repeat, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("暂无周期记账任务", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(periodicList) { item ->
                    PeriodicItem(item, categoryIconMap[item.category], onDelete = { viewModel.deletePeriodic(item) }) {
                        navController.navigate("add_periodic_transaction?id=${item.id}&type=${item.type}")
                    }
                }
            }
        }

        if (showTypeSheet) {
            ModalBottomSheet(onDismissRequest = { showTypeSheet = false }) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        "选择类型",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                    )
                    ListItem(
                        headlineContent = { Text("周期支出") },
                        leadingContent = { Icon(Icons.Outlined.CreditCard, null, tint = Color(0xFFE53935)) },
                        modifier = Modifier.clickable {
                            showTypeSheet = false
                            navController.navigate("add_periodic_transaction?id=-1&type=0")
                        }
                    )
                    ListItem(
                        headlineContent = { Text("周期收入") },
                        leadingContent = { Icon(Icons.Outlined.Paid, null, tint = Color(0xFF4CAF50)) },
                        modifier = Modifier.clickable {
                            showTypeSheet = false
                            navController.navigate("add_periodic_transaction?id=-1&type=1")
                        }
                    )
                    ListItem(
                        headlineContent = { Text("周期转账") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = MaterialTheme.colorScheme.primary) },
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
    val frequencyStr = when(item.frequency) {
        0 -> "每天"
        1 -> "每周"
        2 -> "每月"
        3 -> "每年"
        else -> ""
    }

    val typeStr = when(item.type) {
        0 -> "支出"
        1 -> "收入"
        2 -> "转账"
        else -> ""
    }

    val endStr = when(item.endMode) {
        1 -> " · 至 " + java.text.SimpleDateFormat("MM-dd", Locale.getDefault()).format(item.endDate!!)
        2 -> " · 余 ${item.endCount} 次"
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
                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}