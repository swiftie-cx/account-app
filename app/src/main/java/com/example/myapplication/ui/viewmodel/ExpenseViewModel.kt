package com.example.myapplication.ui.viewmodel

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Account
import com.example.myapplication.data.Budget
import com.example.myapplication.data.EmailSender // 确保您已经创建了 EmailSender.kt
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
        // 初始化时更新汇率 (在 IO 线程)
        viewModelScope.launch(Dispatchers.IO) {
            ExchangeRates.updateRates()
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
    // 2. 邮箱验证码功能 (新增核心)
    // ===========================

    // 临时存储验证码 (Key: 邮箱, Value: 验证码)
    // 注意：App完全关闭后内存会清空，这对于验证码场景是安全的
    private val verificationCodes = mutableMapOf<String, String>()

    /**
     * 发送验证码到指定邮箱
     * @param email 目标邮箱
     * @param onSuccess 发送成功回调
     * @param onError 发送失败回调 (带错误信息)
     */
    fun sendCodeToEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // 1. 生成6位随机数字验证码
            val code = (100000..999999).random().toString()

            // 2. 调用 EmailSender 工具类发送邮件
            // 请确保 EmailSender.kt 已正确配置了 SMTP 信息
            val isSuccess = EmailSender.sendVerificationCode(email, code)

            if (isSuccess) {
                // 3. 发送成功，将验证码保存到内存中用于后续校验
                verificationCodes[email] = code
                onSuccess()
            } else {
                onError("邮件发送失败，请检查网络设置或邮箱地址是否正确")
            }
        }
    }

    /**
     * 校验用户输入的验证码
     * @param email 用户邮箱
     * @param inputCode 用户输入的验证码
     * @return true 表示验证通过
     */
    fun verifyCode(email: String, inputCode: String): Boolean {
        // 获取内存中保存的正确验证码
        val correctCode = verificationCodes[email]
        // 比对是否一致
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

    // 清除所有数据 (危险操作)
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

    // 添加自定义分类
    fun addCategory(name: String, icon: ImageVector, type: CategoryType) {
        val newCategory = Category(name, icon)
        if (type == CategoryType.EXPENSE) {
            _expenseCategories.value = _expenseCategories.value + newCategory
        } else {
            _incomeCategories.value = _incomeCategories.value + newCategory
        }
    }

    // 删除分类
    fun deleteCategory(category: Category, type: CategoryType) {
        if (type == CategoryType.EXPENSE) {
            _expenseCategories.value = _expenseCategories.value.filter { it.title != category.title }
        } else {
            _incomeCategories.value = _incomeCategories.value.filter { it.title != category.title }
        }
    }

    // 分类重排序
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

    // 插入一笔账单
    fun insert(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(expense)
        }
    }

    // 创建转账记录 (同时生成一笔支出和一笔收入)
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

    // 插入账户
    fun insertAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAccount(account)
        }
    }

    // 删除账单
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    // 更新账单
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

    // 组合筛选结果流
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses, _searchText, _selectedTypeFilter, _selectedCategoryFilter
    ) { expenses, text, type, category ->
        expenses.filter { expense ->
            // 1. 文本搜索 (备注或分类名)
            val matchesSearchText = text.isBlank() ||
                    (expense.remark?.contains(text, ignoreCase = true) ?: false) ||
                    expense.category.contains(text, ignoreCase = true)

            // 2. 类型筛选
            val matchesType = when (type) {
                ExpenseTypeFilter.ALL -> true
                ExpenseTypeFilter.EXPENSE -> expense.amount < 0 && !expense.category.startsWith("转账")
                ExpenseTypeFilter.INCOME -> expense.amount > 0 && !expense.category.startsWith("转账")
                ExpenseTypeFilter.TRANSFER -> expense.category.startsWith("转账")
            }

            // 3. 分类筛选
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
                // 如果修改的是子分类预算，自动更新总预算以确保总预算 >= 所有子预算之和
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

    // 同步预算：如果本月没有预算，尝试从最近的一个月复制过来
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

    // 设置隐私锁类型 (PIN, PATTERN, NONE)
    fun setPrivacyType(type: String) = repository.savePrivacyType(type)

    fun savePin(pin: String) = repository.savePin(pin)
    fun verifyPin(pin: String): Boolean = repository.verifyPin(pin)

    fun savePattern(pattern: List<Int>) = repository.savePattern(pattern)
    fun verifyPattern(pattern: List<Int>): Boolean = repository.verifyPattern(pattern)

    fun setBiometricEnabled(enabled: Boolean) = repository.setBiometricEnabled(enabled)
    fun isBiometricEnabled(): Boolean = repository.isBiometricEnabled()

    // --- Periodic Transactions (新增) ---
    val allPeriodicTransactions: StateFlow<List<PeriodicTransaction>> = repository.allPeriodicTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertPeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) { repository.insertPeriodic(transaction) }
    }

    fun updatePeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) { repository.updatePeriodic(transaction) }
    }

    fun deletePeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) { repository.deletePeriodic(transaction) }
    }
}
