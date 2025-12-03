package com.example.myapplication.ui.viewmodel

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Account
import com.example.myapplication.data.Budget
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.data.Expense
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.incomeCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import kotlin.math.abs

enum class ExpenseTypeFilter { ALL, EXPENSE, INCOME, TRANSFER }
enum class CategoryType { EXPENSE, INCOME }

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val budgetUpdateMutex = Mutex()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            ExchangeRates.updateRates()
        }
    }

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultAccountId: StateFlow<Long> = repository.defaultAccountId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)

    fun setDefaultAccount(id: Long) {
        repository.saveDefaultAccountId(id)
    }

    fun reorderAccounts(newOrder: List<Account>) {
        repository.saveAccountOrder(newOrder)
    }

    // --- (新增) 清除所有数据 ---
    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
        }
    }

    // --- Category Management ---
    private val _expenseCategories = MutableStateFlow(expenseCategories)
    val expenseCategoriesState: StateFlow<List<Category>> = _expenseCategories.asStateFlow()

    private val _incomeCategories = MutableStateFlow(incomeCategories)
    val incomeCategoriesState: StateFlow<List<Category>> = _incomeCategories.asStateFlow()

    fun addCategory(name: String, icon: ImageVector, type: CategoryType) {
        val newCategory = Category(name, icon)
        if (type == CategoryType.EXPENSE) {
            _expenseCategories.value = _expenseCategories.value + newCategory
        } else {
            _incomeCategories.value = _incomeCategories.value + newCategory
        }
    }

    fun deleteCategory(category: Category, type: CategoryType) {
        if (type == CategoryType.EXPENSE) {
            _expenseCategories.value = _expenseCategories.value.filter { it.title != category.title }
        } else {
            _incomeCategories.value = _incomeCategories.value.filter { it.title != category.title }
        }
    }

    fun reorderCategories(categories: List<Category>, type: CategoryType) {
        if (type == CategoryType.EXPENSE) {
            _expenseCategories.value = categories
        } else {
            _incomeCategories.value = categories
        }
    }

    // --- Data Operations ---
    fun insert(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(expense)
        }
    }
    fun createTransfer(fromAccountId: Long, toAccountId: Long, fromAmount: Double, toAmount: Double, date: Date) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenseOut = Expense(accountId = fromAccountId, category = "转账 (转出)", amount = -abs(fromAmount), date = date, remark = null)
            val expenseIn = Expense(accountId = toAccountId, category = "转账 (转入)", amount = abs(toAmount), date = date, remark = null)
            repository.createTransfer(expenseOut, expenseIn)
        }
    }
    fun insertAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAccount(account)
        }
    }
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }
    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExpense(expense)
        }
    }

    // --- Search State ---
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText
    private val _selectedTypeFilter = MutableStateFlow(ExpenseTypeFilter.ALL)
    val selectedTypeFilter: StateFlow<ExpenseTypeFilter> = _selectedTypeFilter
    private val _selectedCategoryFilter = MutableStateFlow<String?>("全部")
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses, _searchText, _selectedTypeFilter, _selectedCategoryFilter
    ) { expenses, text, type, category ->
        expenses.filter { expense ->
            val matchesSearchText = text.isBlank() || (expense.remark?.contains(text, ignoreCase = true) ?: false) || expense.category.contains(text, ignoreCase = true)
            val matchesType = when (type) {
                ExpenseTypeFilter.ALL -> true
                ExpenseTypeFilter.EXPENSE -> expense.amount < 0 && !expense.category.startsWith("转账")
                ExpenseTypeFilter.INCOME -> expense.amount > 0 && !expense.category.startsWith("转账")
                ExpenseTypeFilter.TRANSFER -> expense.category.startsWith("转账")
            }
            val matchesCategory = category == "全部" || expense.category == category
            matchesSearchText && matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchText(text: String) { _searchText.value = text }
    fun updateTypeFilter(filter: ExpenseTypeFilter) { _selectedTypeFilter.value = filter }
    fun updateCategoryFilter(category: String?) { _selectedCategoryFilter.value = category ?: "全部" }

    // --- Budget Methods ---
    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>> {
        return repository.getBudgetsForMonth(year, month)
    }

    fun saveBudget(budget: Budget, allCategoryTitles: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetUpdateMutex.withLock {
                repository.upsertBudget(budget)
                if (budget.category != "总预算") {
                    val allBudgets = repository.getBudgetsForMonth(budget.year, budget.month).first()
                    val calculatedSum = allBudgets.filter { it.category in allCategoryTitles }.sumOf { it.amount }
                    val manualTotalBudget = allBudgets.find { it.category == "总预算" }
                    if (manualTotalBudget == null || manualTotalBudget.amount < calculatedSum) {
                        repository.upsertBudget(
                            Budget(id = manualTotalBudget?.id ?: 0, category = "总预算", amount = calculatedSum, year = budget.year, month = budget.month)
                        )
                    }
                }
            }
        }
    }

    fun syncBudgetsFor(year: Int, month: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetMonthBudgets = getBudgetsForMonth(year, month).first()
            if (targetMonthBudgets.isNotEmpty()) {
                return@launch
            }
            val recentBudget = repository.getMostRecentBudget() ?: return@launch
            if (recentBudget.year == year && recentBudget.month == month) {
                return@launch
            }
            val recentMonthBudgets = getBudgetsForMonth(recentBudget.year, recentBudget.month).first()
            val newBudgets = recentMonthBudgets.map {
                it.copy(id = 0, year = year, month = month)
            }
            if (newBudgets.isNotEmpty()) {
                repository.upsertBudgets(newBudgets)
            }
        }
    }
}