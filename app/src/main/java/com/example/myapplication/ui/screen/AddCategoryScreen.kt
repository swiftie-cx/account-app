package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.MainCategory
import com.example.myapplication.ui.navigation.expenseMainCategories
import com.example.myapplication.ui.navigation.incomeMainCategories
import com.example.myapplication.ui.viewmodel.CategoryType
import com.example.myapplication.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(navController: NavController, viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("支出", "收入")

    // [关键修改] 1. 从 ViewModel 获取最新的大类列表
    val expenseMainCategories by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainCategories by viewModel.incomeMainCategoriesState.collectAsState()

    // [修改] 2. 动态决定当前使用哪个列表
    val currentMainCategories = if (selectedTab == 0) expenseMainCategories else incomeMainCategories

    // ... (后续代码 var selectedMainCategory ... 保持不变)
    // 状态
    var selectedMainCategory by remember { mutableStateOf(currentMainCategories.first()) }
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<ImageVector?>(null) }

    // 当切换 Tab 时，重置选中的大类
    LaunchedEffect(selectedTab) {
        selectedMainCategory = currentMainCategories.first()
    }

    // 获取当前选中大类颜色，用于 UI 反馈
    val themeColor = selectedMainCategory.color

    // 预设图标列表 (所有子类的图标集合，去重)
    val availableIcons = remember(currentMainCategories) {
        currentMainCategories.flatMap { it.subCategories }.map { it.icon }.distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加子类别") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(
                        onClick = {
                            if (categoryName.isNotBlank() && selectedIcon != null) {
                                val type = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME
                                val newSubCategory = Category(categoryName, selectedIcon!!)
                                viewModel.addSubCategory(selectedMainCategory, newSubCategory, type)
                                navController.popBackStack()
                            }
                        },
                        enabled = categoryName.isNotBlank() && selectedIcon != null
                    ) {
                        Icon(Icons.Default.Check, "保存", tint = if (categoryName.isNotBlank() && selectedIcon != null) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tab
            TabRow(
                selectedTabIndex = selectedTab,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = MaterialTheme.colorScheme.primary)
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // 1. 选择父级大类
                Text("所属大类", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(currentMainCategories) { main ->
                        val isSelected = main == selectedMainCategory
                        val bgColor = if (isSelected) main.color else MaterialTheme.colorScheme.surfaceVariant
                        val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMainCategory = main },
                            label = { Text(main.title) },
                            leadingIcon = {
                                Icon(
                                    main.icon,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = contentColor
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = main.color,
                                selectedLabelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 2. 输入名称
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("类别名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedIcon != null) themeColor else Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedIcon != null) {
                                Icon(selectedIcon!!, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                // 3. 选择图标
                Text("选择图标", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableIcons) { icon ->
                        val isSelected = selectedIcon == icon
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) themeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    width = if(isSelected) 2.dp else 0.dp,
                                    color = if(isSelected) themeColor else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}