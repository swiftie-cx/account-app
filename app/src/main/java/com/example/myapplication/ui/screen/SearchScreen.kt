package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/**
 * 搜索页面 (极简版)
 * 仅包含 UI 框架，无 ViewModel 逻辑
 * (修改版：统一类别行的按钮为 FilterChip)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
) {
    // --- 状态管理 (使用 remember) ---

    // 搜索框文字
    var searchText by remember { mutableStateOf("") }

    // 1. 类型 过滤器
    val typeFilters = listOf("全部", "支出", "收入", "转账")
    var selectedTypeIndex by remember { mutableStateOf(0) }

    // 2. 类别 过滤器
    // (修改) 状态依然是 String，"全部" 是一个特殊值
    var selectedCategory by remember { mutableStateOf("全部") }

    // 3. 时间 过滤器
    val timeFilters = listOf("全部", "本周", "本月", "本年", "自定义")
    var selectedTimeIndex by remember { mutableStateOf(0) }

    val focusRequester = remember { FocusRequester() }

    // 页面打开时，自动激活搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            // 顶部应用栏 (符合截图)
            TopAppBar(
                title = { Text("搜索") }, // 标题为 "搜索"
                navigationIcon = {
                    // 返回按钮
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            // 底部按钮栏 (符合截图)
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp // 加一点阴影
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 重置按钮 (符合截图)
                    OutlinedButton(
                        onClick = {
                            // 按要求：重置所有筛选为 "全部"
                            selectedTypeIndex = 0
                            selectedCategory = "全部" // (修改) 重置类别
                            selectedTimeIndex = 0
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重置")
                    }
                    // 搜索按钮 (符合截图)
                    Button(
                        onClick = { /* 暂时不做任何事 */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("搜索")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding) // 应用 Scaffold 的 padding
                .padding(horizontal = 16.dp) // 为内容区添加左右 padding
        ) {

            // 搜索框 (符合截图)
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("搜索备注、分类") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .focusRequester(focusRequester),
                singleLine = true
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // 行 1: 类型 (符合截图)
            FilterChipRow(
                title = "类型",
                labels = typeFilters,
                selectedIndex = selectedTypeIndex,
                onChipSelected = { selectedTypeIndex = it }
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // 行 2: 类别 (修改)
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                // (修改) 传入 "全部" 按钮是否被选中
                isAllSelected = (selectedCategory == "全部"),
                onCategoryChipClick = {
                    // (修改) 点击时，将状态设置回 "全部"
                    selectedCategory = "全部"
                },
                onAddClick = { /* 暂时不做任何事，后续跳转类别选择 */ }
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // 行 3: 时间 (符合截图)
            FilterChipRow(
                title = "时间",
                labels = timeFilters,
                selectedIndex = selectedTimeIndex,
                onChipSelected = { selectedTimeIndex = it }
            )
        }
    }
}


/**
 * 筛选器行 (用于 "类型" 和 "时间")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onChipSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 固定的标签
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp) // 保持标签对齐
        )
        // 横向滚动的按钮
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(labels) { index, label ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onChipSelected(index) },
                    label = { Text(label) }
                )
            }
        }
    }
}

/**
 * 筛选器行 (用于 "类别")
 * (已修改：使用 FilterChip)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selectedCategory: String,
    isAllSelected: Boolean, // (修改) 接收是否选中
    onCategoryChipClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 固定的标签
        Text(
            text = "类别",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp) // 保持标签对齐
        )

        // (修改) "全部" 按钮
        FilterChip( // <-- 已从 AssistChip 更改为 FilterChip
            selected = isAllSelected, // <-- 应用选中状态
            onClick = onCategoryChipClick,
            label = { Text(selectedCategory) } // 标签文本仍然显示当前选中的类别
        )

        // 间隔
        Spacer(Modifier.weight(1f))

        // 圆形 "+" 按钮 (符合截图)
        FilledIconButton(
            onClick = onAddClick,
            shape = CircleShape // 确保是圆形
        ) {
            Icon(Icons.Default.Add, contentDescription = "选择类别")
        }
    }
}