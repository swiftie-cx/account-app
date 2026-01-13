package com.swiftiecx.timeledger.ui.feature.settings.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource // [新增] 引入资源引用
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.swiftiecx.timeledger.R // [新增] 引入 R 类
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.navigation.Routes
import com.swiftiecx.timeledger.ui.viewmodel.CategoryType
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.collections.toMutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(navController: NavController, viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    // [i18n] Tab 标题使用资源
    val tabs = listOf(stringResource(R.string.type_expense), stringResource(R.string.type_income))
    val currentType = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME

    // 获取大类数据源
    val expenseMainList by viewModel.expenseMainCategoriesState.collectAsState()
    val incomeMainList by viewModel.incomeMainCategoriesState.collectAsState()
    val sourceList = if (selectedTab == 0) expenseMainList else incomeMainList

    // 大类排序用的本地状态
    var mainItems by remember(sourceList) { mutableStateOf(sourceList) }

    // 监听数据源变化
    LaunchedEffect(sourceList) {
        mainItems = sourceList
    }

    // 控制小类编辑弹窗
    var editingMainCategory by remember { mutableStateOf<MainCategory?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.category_settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ADD_CATEGORY) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tab 栏
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
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 大类拖拽列表
            val reorderState = rememberReorderableLazyListState(
                onMove = { from, to ->
                    // [修改] 使用更安全的写法
                    val list = mainItems.toMutableList()
                    val item = list.removeAt(from.index)
                    list.add(to.index, item)
                    mainItems = list
                },
                onDragEnd = { _, _ ->
                    viewModel.reorderMainCategories(mainItems, currentType)
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
                items(mainItems, key = { it.title }) { mainCategory ->
                    ReorderableItem(reorderState, key = mainCategory.title) { isDragging ->
                        val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)

                        MainCategorySettingCard(
                            mainCategory = mainCategory,
                            elevation = elevation.value,
                            onClick = { editingMainCategory = mainCategory },
                            handle = { modifier ->
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = stringResource(R.string.category_reorder),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = modifier.detectReorder(reorderState)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // --- 小类管理弹窗 ---
    if (editingMainCategory != null) {
        SubCategorySheet(
            mainCategory = editingMainCategory!!,
            type = currentType,
            viewModel = viewModel,
            onDismiss = { editingMainCategory = null }
        )
    }
}

// --- 组件：大类卡片 ---
@Composable
fun MainCategorySettingCard(
    mainCategory: MainCategory,
    elevation: Dp,
    onClick: () -> Unit,
    handle: @Composable (Modifier) -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier.shadow(elevation, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 大类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(mainCategory.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    mainCategory.icon,
                    contentDescription = null,
                    tint = mainCategory.color
                )
            }

            Spacer(Modifier.width(16.dp))

            // 大类名称 + 小类数量提示
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mainCategory.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.subcategory_count_label, mainCategory.subCategories.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 拖拽把手
            handle(Modifier)

            Spacer(Modifier.width(16.dp))

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

// --- 组件：小类管理底部面板 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategorySheet(
    mainCategory: MainCategory,
    type: CategoryType,
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    // 小类排序用的本地状态
    var subItems by remember(mainCategory) { mutableStateOf(mainCategory.subCategories) }

    // 监听外部数据变化(比如删除了一个子类)
    LaunchedEffect(mainCategory) {
        subItems = mainCategory.subCategories
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxHeight(0.6f) // 限制最大高度
        ) {
            // 标题栏
            Text(
                text = stringResource(R.string.subcategory_management_title, mainCategory.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = mainCategory.color,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )

            Text(
                text = stringResource(R.string.subcategory_reorder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
            )

            // 小类拖拽列表
            val subReorderState = rememberReorderableLazyListState(
                onMove = { from, to ->
                    // [修改] 使用更安全的写法
                    val list = subItems.toMutableList()
                    val item = list.removeAt(from.index)
                    list.add(to.index, item)
                    subItems = list
                },
                onDragEnd = { _, _ ->
                    viewModel.reorderSubCategories(mainCategory, subItems, type)
                }
            )

            LazyColumn(
                state = subReorderState.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(subReorderState),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(subItems, key = { it.title }) { sub ->
                    ReorderableItem(subReorderState, key = sub.title) { isDragging ->
                        val bgColor = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent

                        ListItem(
                            modifier = Modifier.background(bgColor),
                            leadingContent = {
                                Icon(
                                    sub.icon,
                                    null,
                                    tint = mainCategory.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            headlineContent = { Text(sub.title) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 删除按钮
                                    IconButton(
                                        onClick = { viewModel.deleteSubCategory(mainCategory, sub, type) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    // 拖拽把手
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = stringResource(R.string.category_reorder),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.detectReorder(subReorderState)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}