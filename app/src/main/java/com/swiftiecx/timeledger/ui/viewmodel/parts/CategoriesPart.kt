package com.swiftiecx.timeledger.ui.viewmodel.parts

import android.content.Context
import com.swiftiecx.timeledger.data.ExpenseRepository
import com.swiftiecx.timeledger.ui.navigation.Category
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.viewmodel.model.CategoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoriesPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope,
    private val appContextProvider: () -> Context
) {
    private val _expenseMainCategories = MutableStateFlow<List<MainCategory>>(emptyList())
    val expenseMainCategoriesState: StateFlow<List<MainCategory>> = _expenseMainCategories.asStateFlow()

    private val _incomeMainCategories = MutableStateFlow<List<MainCategory>>(emptyList())
    val incomeMainCategoriesState: StateFlow<List<MainCategory>> = _incomeMainCategories.asStateFlow()

    val expenseCategoriesState: StateFlow<List<Category>> =
        _expenseMainCategories
            .map { it.flatMap { main -> main.subCategories } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val incomeCategoriesState: StateFlow<List<Category>> =
        _incomeMainCategories
            .map { it.flatMap { main -> main.subCategories } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun refreshCategories(specificContext: Context? = null) {
        scope.launch(Dispatchers.IO) {
            val targetContext = specificContext ?: appContextProvider()
            // 这里保持你原来的行为：切语言时强制更新 DB 内的显示名
            repository.forceUpdateCategoryNames(targetContext)

            val expenseCats = repository.getMainCategories(CategoryType.EXPENSE)
            val incomeCats = repository.getMainCategories(CategoryType.INCOME)

            _expenseMainCategories.value = expenseCats
            _incomeMainCategories.value = incomeCats
        }
    }

    fun addSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map {
                if (it.title == mainCategory.title) it.copy(subCategories = it.subCategories + subCategory) else it
            }
        }
    }

    fun deleteSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map {
                if (it.title == mainCategory.title) it.copy(subCategories = it.subCategories.filter { sub -> sub.title != subCategory.title })
                else it
            }
        }
    }

    fun reorderMainCategories(newOrder: List<MainCategory>, type: CategoryType) {
        updateMainCategoryList(type) { newOrder }
    }

    fun reorderSubCategories(mainCategory: MainCategory, newSubOrder: List<Category>, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map { if (it.title == mainCategory.title) it.copy(subCategories = newSubOrder) else it }
        }
    }

    private fun updateMainCategoryList(
        type: CategoryType,
        updateAction: (List<MainCategory>) -> List<MainCategory>
    ) {
        if (type == CategoryType.EXPENSE) {
            val newList = updateAction(_expenseMainCategories.value)
            _expenseMainCategories.value = newList
            repository.saveMainCategories(newList, com.swiftiecx.timeledger.ui.viewmodel.model.CategoryType.EXPENSE)
        } else {
            val newList = updateAction(_incomeMainCategories.value)
            _incomeMainCategories.value = newList
            repository.saveMainCategories(newList, com.swiftiecx.timeledger.ui.viewmodel.model.CategoryType.INCOME)
        }
    }
}
