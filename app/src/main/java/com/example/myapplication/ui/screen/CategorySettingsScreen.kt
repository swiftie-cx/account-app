package com.example.myapplication.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.viewmodel.CategoryType
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder     // ← 用这个
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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Pair<Category, CategoryType>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("类别设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            Button(onClick = { navController.navigate(Routes.ADD_CATEGORY) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("添加类别")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            val type = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME

            val reorderState = rememberReorderableLazyListState(
                onMove = { from, to ->
                    items = items.toMutableList().apply { move(from.index, to.index) }
                    viewModel.reorderCategories(items, type)
                }
            )

            LazyColumn(
                state = reorderState.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(reorderState)
            ) {
                items(
                    items = items,
                    key = { it.title } // 有 id 的话改为 it.id
                ) { category ->
                    ReorderableItem(reorderState, key = category.title) { _ ->
                        CategorySettingItem(
                            category = category,
                            onDelete = {
                                categoryToDelete = category to type
                                showDeleteDialog = true
                            },
                            modifier = Modifier.animateContentSize()
                        ) { handleModifier ->
                            IconButton(
                                onClick = {},
                                modifier = handleModifier
                                    .size(24.dp)
                                    .detectReorder(reorderState) // ← 把手在这里
                            ) {
                                Icon(Icons.Default.DragHandle, contentDescription = "排序")
                            }
                        }
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
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun CategorySettingItem(
    category: Category,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    handle: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(category.icon, contentDescription = category.title)
        }
        Text(category.title, modifier = Modifier.weight(1f))
        handle(Modifier)
    }
}

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    // 取出元素
    val item = removeAt(from)
    // 直接插入到目标位置，不需要额外计算
    add(to, item)
}
