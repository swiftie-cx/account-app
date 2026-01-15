package com.swiftiecx.timeledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swiftiecx.timeledger.R

private data class CategoryUi(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        stringResource(R.string.type_expense),
        stringResource(R.string.type_income)
    )

    val context = LocalContext.current

    // ✅ 保留“主分类颜色”，避免子分类全变同色
    val allCategories: List<List<CategoryUi>> = remember(context) {
        val expenses = CategoryData.getExpenseCategories(context)
            .flatMap { main ->
                main.subCategories.map { sub ->
                    CategoryUi(title = sub.title, icon = sub.icon, color = main.color)
                }
            }
        val incomes = CategoryData.getIncomeCategories(context)
            .flatMap { main ->
                main.subCategories.map { sub ->
                    CategoryUi(title = sub.title, icon = sub.icon, color = main.color)
                }
            }
        listOf(expenses, incomes)
    }

    var tempSelectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_label)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onConfirm(tempSelectedCategories.toList()) }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.confirm))
                    }
                }
            )
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentList = allCategories.getOrElse(selectedTab) { emptyList() }

                items(currentList) { category ->
                    CategoryItem(
                        title = category.title,
                        icon = category.icon,
                        baseColor = category.color,
                        isSelected = category.title in tempSelectedCategories,
                        onItemClick = { title ->
                            tempSelectedCategories = if (title in tempSelectedCategories) {
                                tempSelectedCategories - title
                            } else {
                                tempSelectedCategories + title
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    title: String,
    icon: ImageVector,
    baseColor: Color,
    isSelected: Boolean,
    onItemClick: (String) -> Unit
) {
    // ✅ 未选中：淡底 + 分类色图标
    // ✅ 选中：更明显的淡底 + 彩色描边 + 右上角勾
    val bg = if (isSelected) baseColor.copy(alpha = 0.18f) else baseColor.copy(alpha = 0.10f)
    val iconTint = baseColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onItemClick(title) }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bg)
                .then(
                    if (isSelected) Modifier.border(2.dp, baseColor, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(30.dp)
            )

            if (isSelected) {
                // 右上角勾：让“选中”一眼可见
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(baseColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
