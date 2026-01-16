package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.data.local.entity.Expense
import com.swiftiecx.timeledger.data.local.entity.RecordType
import com.swiftiecx.timeledger.ui.viewmodel.model.ExpenseTypeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ExpenseFilterPart(
    scope: CoroutineScope,
    allExpenses: StateFlow<List<Expense>>
) {
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    private val _selectedTypeFilter = MutableStateFlow(ExpenseTypeFilter.ALL)
    val selectedTypeFilter: StateFlow<ExpenseTypeFilter> = _selectedTypeFilter

    private val _selectedCategoryFilter = MutableStateFlow<String?>("全部")
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter

    val filteredExpenses: StateFlow<List<Expense>> =
        combine(allExpenses, _searchText, _selectedTypeFilter, _selectedCategoryFilter) { expenses, text, type, category ->
            expenses.filter { expense ->
                val matchesSearchText =
                    text.isBlank() ||
                            (expense.remark?.contains(text, true) == true) ||
                            expense.category.contains(text, true)

                val matchesType = when (type) {
                    ExpenseTypeFilter.ALL -> {
                        if (expense.recordType == RecordType.TRANSFER) expense.amount < 0 else true
                    }
                    ExpenseTypeFilter.EXPENSE -> expense.recordType == RecordType.INCOME_EXPENSE && expense.amount < 0
                    ExpenseTypeFilter.INCOME -> expense.recordType == RecordType.INCOME_EXPENSE && expense.amount > 0
                    ExpenseTypeFilter.TRANSFER -> expense.recordType == RecordType.TRANSFER && expense.amount < 0
                }

                val matchesCategory = category == "全部" || expense.category == category
                matchesSearchText && matchesType && matchesCategory
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchText(text: String) { _searchText.value = text }
    fun updateTypeFilter(filter: ExpenseTypeFilter) { _selectedTypeFilter.value = filter }
    fun updateCategoryFilter(category: String?) { _selectedCategoryFilter.value = category ?: "全部" }
}
