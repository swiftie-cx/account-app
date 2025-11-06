package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.ui.viewmodel.CategoryType
import com.example.myapplication.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(navController: NavController, viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<ImageVector?>(null) }
    val tabs = listOf("支出", "收入")

    val expenseCategories by viewModel.expenseCategoriesState.collectAsState()
    val incomeCategories by viewModel.incomeCategoriesState.collectAsState()
    val iconGroups = listOf(expenseCategories, incomeCategories)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加类别") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(
                        onClick = {
                            if (categoryName.isNotBlank() && selectedIcon != null) {
                                val type = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME
                                viewModel.addCategory(categoryName, selectedIcon!!, type)
                                navController.popBackStack()
                            }
                        },
                        enabled = categoryName.isNotBlank() && selectedIcon != null
                    ) {
                        Icon(Icons.Default.Check, "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("请输入类别名称") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray)) {
                        if (selectedIcon != null) {
                            Icon(selectedIcon!!, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 70.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(iconGroups[selectedTab].map { it.icon }.distinct()) { icon ->
                    Icon(
                        icon,
                        contentDescription = null, 
                        modifier = Modifier.size(40.dp).clickable { selectedIcon = icon },
                        tint = if(selectedIcon == icon) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }
            }
        }
    }
}