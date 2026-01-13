package com.swiftiecx.timeledger.ui.feature.settings.screen

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.ui.navigation.Category
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.viewmodel.CategoryType
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(navController: NavController, viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.type_expense), stringResource(R.string.type_income))

    val context = LocalContext.current
    val expenseMainCategories = remember(context) { CategoryData.getExpenseCategories(context) }
    val incomeMainCategories = remember(context) { CategoryData.getIncomeCategories(context) }

    val currentMainCategories = if (selectedTab == 0) expenseMainCategories else incomeMainCategories
    var selectedMainCategory by remember { mutableStateOf<MainCategory?>(null) }

    LaunchedEffect(currentMainCategories) {
        if (selectedMainCategory == null || selectedMainCategory !in currentMainCategories) {
            selectedMainCategory = currentMainCategories.firstOrNull()
        }
    }

    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<ImageVector?>(null) }

    LaunchedEffect(selectedTab) {
        selectedMainCategory = currentMainCategories.firstOrNull()
    }

    val themeColor = selectedMainCategory?.color ?: MaterialTheme.colorScheme.primary

    val availableIcons = remember(currentMainCategories) {
        currentMainCategories.flatMap { it.subCategories }.map { it.icon }.distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (categoryName.isNotBlank() && selectedIcon != null && selectedMainCategory != null) {

                                val type = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME

                                // ====== 核心修复点 ======
                                val stableKey = categoryName
                                    .trim()
                                    .replace(" ", "_")
                                    .lowercase()

                                val newSubCategory = Category(
                                    title = categoryName,
                                    icon = selectedIcon!!,
                                    key = stableKey
                                )
                                // ======================

                                viewModel.addSubCategory(
                                    selectedMainCategory!!,
                                    newSubCategory,
                                    type
                                )
                                navController.popBackStack()
                            }
                        },
                        enabled = categoryName.isNotBlank() && selectedIcon != null
                    ) {
                        Icon(
                            Icons.Default.Check,
                            stringResource(R.string.save),
                            tint = if (categoryName.isNotBlank() && selectedIcon != null)
                                MaterialTheme.colorScheme.primary
                            else Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            TabRow(
                selectedTabIndex = selectedTab,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    stringResource(R.string.category_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(currentMainCategories) { main ->
                        val isSelected = main == selectedMainCategory

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMainCategory = main },
                            label = { Text(main.title) },
                            leadingIcon = {
                                Icon(
                                    main.icon,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text(stringResource(R.string.account_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon != null)
                                        themeColor
                                    else Color.LightGray.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedIcon != null) {
                                Icon(
                                    selectedIcon!!,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    stringResource(R.string.select_icon),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                                .background(
                                    if (isSelected)
                                        themeColor.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) themeColor else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected)
                                    themeColor
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
