package com.example.myapplication.ui.viewmodel

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Account
import com.example.myapplication.data.Budget
import com.example.myapplication.data.EmailSender
import com.example.myapplication.data.ExchangeRates
import com.example.myapplication.data.Expense
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.data.PeriodicTransaction
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

// 定义筛选类型枚举
enum class ExpenseTypeFilter { ALL, EXPENSE, INCOME, TRANSFER }
// 定义分类类型枚举
enum class CategoryType { EXPENSE, INCOME }

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // 用于预算更新的互斥锁，防止并发冲突
    private val budgetUpdateMutex = Mutex()

    init {
        // 1. 初始化时更新汇率 (在 IO 线程)
        viewModelScope.launch(Dispatchers.IO) {
            ExchangeRates.updateRates()
        }

        // 2. 【关键】启动时检查周期记账
        // 只要 App 一打开，立刻检查是否有到期的任务并自动执行
        // (注意：如果这里爆红，说明 ExpenseRepository.kt 还没更新 checkAndExecutePeriodicTransactions 方法)
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndExecutePeriodicTransactions()
        }
    }

    // ===========================
    // 1. 用户认证与账号信息相关
    // ===========================

    // 监听登录状态
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 监听当前用户邮箱
    val userEmail: StateFlow<String> = repository.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // 注册账号
    fun register(email: String, password: String) {
        repository.register(email, password)
    }

    // 登录
    fun login(email: String, password: String): Boolean {
        return repository.login(email, password)
    }

    // 检查邮箱是否已被注册
    fun isEmailRegistered(email: String): Boolean {
        return repository.isEmailRegistered(email)
    }

    // 验证旧密码
    fun verifyUserPassword(password: String) = repository.verifyUserPassword(password)

    // 保存新密码
    fun saveUserPassword(password: String) = repository.saveUserPassword(password)

    // 退出登录
    fun logout() = repository.logout()

    // 注销/删除账号
    fun deleteUserAccount() = repository.deleteUserAccount()

    // ===========================
    // 2. 邮箱验证码功能
    // ===========================

    // 临时存储验证码 (Key: 邮箱, Value: 验证码)
    private val verificationCodes = mutableMapOf<String, String>()

    /**
     * 发送验证码到指定邮箱
     */
    fun sendCodeToEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val code = (100000..999999).random().toString()
            val isSuccess = EmailSender.sendVerificationCode(email, code)

            if (isSuccess) {
                verificationCodes[email] = code
                onSuccess()
            } else {
                onError("邮件发送失败，请检查网络设置或邮箱地址是否正确")
            }
        }
    }

    /**
     * 校验用户输入的验证码
     */
    fun verifyCode(email: String, inputCode: String): Boolean {
        val correctCode = verificationCodes[email]
        return correctCode != null && correctCode == inputCode
    }

    // ===========================
    // 3. 账单与账户数据流
    // ===========================

    // 所有账单列表
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 所有账户列表
    val allAccounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 默认账户 ID
    val defaultAccountId: StateFlow<Long> = repository.defaultAccountId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)

    // 设置默认账户
    fun setDefaultAccount(id: Long) {
        repository.saveDefaultAccountId(id)
    }

    // 重新排序账户
    fun reorderAccounts(newOrder: List<Account>) {
        repository.saveAccountOrder(newOrder)
    }

    // 清除所有数据
    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
        }
    }

    // ===========================
    // 4. 分类管理 (Category)
    // ===========================

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

    // ===========================
    // 5. 账单操作 (CRUD)
    // ===========================

    fun insert(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(expense)
        }
    }

    fun createTransfer(fromAccountId: Long, toAccountId: Long, fromAmount: Double, toAmount: Double, date: Date) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenseOut = Expense(
                accountId = fromAccountId,
                category = "转账 (转出)",
                amount = -abs(fromAmount),
                date = date,
                remark = null
            )
            val expenseIn = Expense(
                accountId = toAccountId,
                category = "转账 (转入)",
                amount = abs(toAmount),
                date = date,
                remark = null
            )
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

    // ===========================
    // 6. 搜索与筛选状态
    // ===========================

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
            val matchesSearchText = text.isBlank() ||
                    (expense.remark?.contains(text, ignoreCase = true) ?: false) ||
                    expense.category.contains(text, ignoreCase = true)

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

    // ===========================
    // 7. 预算管理 (Budget)
    // ===========================

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
                            Budget(
                                id = manualTotalBudget?.id ?: 0,
                                category = "总预算",
                                amount = calculatedSum,
                                year = budget.year,
                                month = budget.month
                            )
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

    // ===========================
    // 8. 隐私与安全 (Privacy)
    // ===========================

    fun getPrivacyType(): String = repository.getPrivacyType()
    fun setPrivacyType(type: String) = repository.savePrivacyType(type)
    fun savePin(pin: String) = repository.savePin(pin)
    fun verifyPin(pin: String): Boolean = repository.verifyPin(pin)
    fun savePattern(pattern: List<Int>) = repository.savePattern(pattern)
    fun verifyPattern(pattern: List<Int>): Boolean = repository.verifyPattern(pattern)
    fun setBiometricEnabled(enabled: Boolean) = repository.setBiometricEnabled(enabled)
    fun isBiometricEnabled(): Boolean = repository.isBiometricEnabled()

    // ===========================
    // 9. 周期记账 (Periodic)
    // ===========================

    val allPeriodicTransactions: StateFlow<List<PeriodicTransaction>> = repository.allPeriodicTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 新增规则
    fun insertPeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 保存
            repository.insertPeriodic(transaction)
            // 2. 【关键】保存后立即检查是否需要今天执行，给用户即时反馈
            repository.checkAndExecutePeriodicTransactions()
        }
    }

    // 修改规则
    fun updatePeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 更新
            repository.updatePeriodic(transaction)
            // 2. 【关键】更新后也立即检查 (万一用户把时间改到了今天之前)
            repository.checkAndExecutePeriodicTransactions()
        }
    }

    fun deletePeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) { repository.deletePeriodic(transaction) }
    }
}