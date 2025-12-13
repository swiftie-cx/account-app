package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
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
import com.example.myapplication.data.Budget
import com.example.myapplication.ui.navigation.CategoryHelper
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    year: Int,
    month: Int
) {
    LaunchedEffect(year, month) {
        viewModel.syncBudgetsFor(year, month)
    }

    val budgets by viewModel.getBudgetsForMonth(year, month).collectAsState(initial = emptyList())
    val budgetMap = remember(budgets) { budgets.associateBy { it.category } }

    // 只显示支出分类用于设置预算
    val expenseCategoryTitles = remember { expenseCategories.map { it.title } }

    // 将总预算和全部分类分开处理
    val totalBudget = budgetMap["总预算"]

    var showBottomSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${year}年${month}月预算", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 顶部：总预算大卡片
            item {
                TotalBudgetSettingCard(
                    amount = totalBudget?.amount ?: 0.0,
                    onClick = {
                        editingCategory = "总预算"
                        showBottomSheet = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "分类预算",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            // 2. 列表：各个分类预算
            items(expenseCategoryTitles) { categoryTitle ->
                val amount = budgetMap[categoryTitle]?.amount ?: 0.0

                // 动态获取分类样式 (预算一般针对支出，所以 type 传 0)
                val color = CategoryHelper.getCategoryColor(categoryTitle, 0)
                val mainCat = CategoryHelper.getMainCategory(categoryTitle)
                // 尝试找具体的小类图标，找不到就用大类图标，再找不到用默认
                val icon = mainCat?.subCategories?.find { it.title == categoryTitle }?.icon
                    ?: mainCat?.icon
                    ?: Icons.Default.AccountBalanceWallet

                BudgetSettingItem(
                    title = categoryTitle,
                    amount = amount,
                    color = color,
                    icon = icon,
                    onClick = {
                        editingCategory = categoryTitle
                        showBottomSheet = true
                    }
                )
            }
        }
    }

    // --- 编辑弹窗 ---
    if (showBottomSheet && editingCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            val currentAmount = budgetMap[editingCategory]?.amount ?: 0.0
            // 格式化：如果是整数去掉小数点
            var amountStr by remember {
                mutableStateOf(
                    if (currentAmount == 0.0) ""
                    else if (currentAmount % 1.0 == 0.0) currentAmount.toLong().toString()
                    else currentAmount.toString()
                )
            }
            var isCalculation by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                // 弹窗标题栏
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "设置 ${editingCategory} 预算",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if(amountStr.isEmpty()) "0" else amountStr,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 数字键盘
                NumericKeyboard(
                    onNumberClick = { if (amountStr == "0") amountStr = it else amountStr += it },
                    onOperatorClick = { operator ->
                        amountStr += " $operator "
                        isCalculation = true
                    },
                    onBackspaceClick = {
                        amountStr = if (amountStr.length > 1) amountStr.dropLast(1) else ""
                        isCalculation = amountStr.contains("+") || amountStr.contains("-")
                    },
                    onAgainClick = null,
                    onDoneClick = {
                        val newAmount = try {
                            if (isCalculation) evaluateExpression(amountStr) else (amountStr.toDoubleOrNull() ?: 0.0)
                        } catch (e: Exception) { 0.0 }

                        val existingBudget = budgetMap[editingCategory!!]
                        val budget = Budget(
                            id = existingBudget?.id ?: 0,
                            category = editingCategory!!,
                            amount = newAmount,
                            year = year,
                            month = month
                        )
                        viewModel.saveBudget(budget, expenseCategoryTitles)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showBottomSheet = false
                        }
                    },
                    onEqualsClick = {
                        try {
                            val result = evaluateExpression(amountStr)
                            amountStr = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
                        } catch (e: Exception) {
                            amountStr = "Error"
                        }
                        isCalculation = false
                    },
                    isCalculation = isCalculation
                )
            }
        }
    }
}

// --- 组件：总预算卡片 ---
@Composable
fun TotalBudgetSettingCard(amount: Double, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "本月总预算",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%.0f", amount),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // 编辑图标背景
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// --- 组件：普通分类预算项 ---
@Composable
fun BudgetSettingItem(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp) // 平面风格
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 彩色图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            // 金额或提示
            if (amount > 0) {
                Text(
                    text = String.format(Locale.US, "%.0f", amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "未设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            }
        }
    }
}