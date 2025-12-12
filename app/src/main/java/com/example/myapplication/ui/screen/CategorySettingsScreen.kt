package com.example.myapplication.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.CategoryType
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(navController: NavController, viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("支出", "收入")

    val expenseCategories by viewModel.expenseCategoriesState.collectAsState()
    val incomeCategories by viewModel.incomeCategoriesState.collectAsState()

    val source = if (selectedTab == 0) expenseCategories else incomeCategories
    var items by remember(source) { mutableStateOf(source.toMutableList()) }

    // 监听数据源变化 (例如删除后)
    LaunchedEffect(source) {
        items = source.toMutableList()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Pair<Category, CategoryType>?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("类别设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ADD_CATEGORY) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加类别")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 美化后的 TabRow
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {} // 移除分割线
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                        },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val type = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME
            val reorderState = rememberReorderableLazyListState(
                onMove = { from, to ->
                    items = items.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
                },
                onDragEnd = { _, _ ->
                    viewModel.reorderCategories(items, type)
                }
            )

            LazyColumn(
                state = reorderState.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(reorderState)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.title }) { category ->
                    ReorderableItem(reorderState, key = category.title) { isDragging ->
                        val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)

                        CategorySettingCard(
                            category = category,
                            elevation = elevation.value,
                            onDelete = {
                                categoryToDelete = category to type
                                showDeleteDialog = true
                            },
                            // 拖拽把手 UI
                            handle = { modifier ->
                                IconButton(
                                    onClick = {},
                                    modifier = modifier.detectReorder(reorderState)
                                ) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "排序",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("您确定要删除 \"${categoryToDelete!!.first.title}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(categoryToDelete!!.first, categoryToDelete!!.second)
                        showDeleteDialog = false
                    }
                ) { Text("确定", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun CategorySettingCard(
    category: Category,
    elevation: androidx.compose.ui.unit.Dp,
    onDelete: () -> Unit,
    handle: @Composable (Modifier) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier.shadow(elevation, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 删除按钮
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }

            // 图标容器
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 类别名称
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // 拖拽把手
            handle(Modifier)
        }
    }
}