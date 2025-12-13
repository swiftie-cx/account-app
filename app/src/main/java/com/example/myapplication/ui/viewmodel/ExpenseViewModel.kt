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
import com.example.myapplication.ui.navigation.MainCategory // [关键缺失]
import com.example.myapplication.ui.navigation.expenseMainCategories // [关键缺失]
import com.example.myapplication.ui.navigation.incomeMainCategories // [关键缺失]
import com.example.myapplication.ui.screen.chart.ChartMode
import com.example.myapplication.ui.screen.chart.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Currency
import java.util.Date
import java.util.Locale
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

        // 2. 启动时检查周期记账
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndExecutePeriodicTransactions()
        }
    }

    // ===========================
    // 0. 欢迎页与初始化逻辑 (新增)
    // ===========================

    val isFirstLaunch: Boolean
        get() = repository.isFirstLaunch()

    fun completeOnboarding(initialBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 自动检测系统币种
            val currencyCode = try {
                Currency.getInstance(Locale.getDefault()).currencyCode
            } catch (e: Exception) {
                "CNY"
            }

            // 2. 创建默认账户
            val defaultAccount = Account(
                name = "默认账户",
                type = "默认",
                initialBalance = initialBalance,
                currency = currencyCode,
                iconName = "AccountBalanceWallet",
                isLiability = false
            )

            // 插入账户并获取 ID (Repository 的 insertAccount 需返回 Long)
            val newId = repository.insertAccount(defaultAccount)
            repository.saveDefaultAccountId(newId)

            // 3. 标记完成
            repository.setFirstLaunchCompleted()
        }
    }

    // ===========================
    // 1. 用户认证与账号信息相关
    // ===========================

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userEmail: StateFlow<String> = repository.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun register(email: String, password: String) {
        repository.register(email, password)
    }

    fun login(email: String, password: String): Boolean {
        return repository.login(email, password)
    }

    fun isEmailRegistered(email: String): Boolean {
        return repository.isEmailRegistered(email)
    }

    fun verifyUserPassword(password: String) = repository.verifyUserPassword(password)
    fun saveUserPassword(password: String) = repository.saveUserPassword(password)
    fun logout() = repository.logout()
    fun deleteUserAccount() = repository.deleteUserAccount()

    // ===========================
    // 2. 邮箱验证码功能
    // ===========================

    private val verificationCodes = mutableMapOf<String, String>()

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

    fun verifyCode(email: String, inputCode: String): Boolean {
        val correctCode = verificationCodes[email]
        return correctCode != null && correctCode == inputCode
    }

    // ===========================
    // 3. 账单与账户数据流
    // ===========================

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

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
        }
    }

    // ===========================
    // 4. 分类管理 (Category) - [已升级为大类结构 & 支持持久化]
    // ===========================

    // 【关键修改】初始化时尝试从 Repository 读取 (如果本地没有存档，Repository 会自动返回默认列表)
    private val _expenseMainCategories = MutableStateFlow(repository.getMainCategories(CategoryType.EXPENSE))
    val expenseMainCategoriesState: StateFlow<List<MainCategory>> = _expenseMainCategories.asStateFlow()

    private val _incomeMainCategories = MutableStateFlow(repository.getMainCategories(CategoryType.INCOME))
    val incomeMainCategoriesState: StateFlow<List<MainCategory>> = _incomeMainCategories.asStateFlow()

    // 兼容旧的扁平化 State，供其他未修改的页面读取 (自动从 MainCategory 拍平)
    val expenseCategoriesState: StateFlow<List<Category>> = _expenseMainCategories.map { list ->
        list.flatMap { it.subCategories }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomeCategoriesState: StateFlow<List<Category>> = _incomeMainCategories.map { list ->
        list.flatMap { it.subCategories }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- 操作逻辑 ---

    // 1. 添加小类
    fun addSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map { main ->
                if (main.title == mainCategory.title) {
                    // 复制大类，并向其子类列表中添加新小类
                    main.copy(subCategories = main.subCategories + subCategory)
                } else {
                    main
                }
            }
        }
    }

    // 2. 删除小类
    fun deleteSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map { main ->
                if (main.title == mainCategory.title) {
                    main.copy(subCategories = main.subCategories.filter { it.title != subCategory.title })
                } else {
                    main
                }
            }
        }
    }

    // 3. 排序大类 (在主界面拖拽大类)
    fun reorderMainCategories(newOrder: List<MainCategory>, type: CategoryType) {
        updateMainCategoryList(type) { newOrder }
    }

    // 4. 排序小类 (在弹窗里拖拽小类)
    fun reorderSubCategories(mainCategory: MainCategory, newSubOrder: List<Category>, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map { main ->
                if (main.title == mainCategory.title) {
                    main.copy(subCategories = newSubOrder)
                } else {
                    main
                }
            }
        }
    }

    // 【关键辅助函数】统一更新逻辑并保存到 Repository
    private fun updateMainCategoryList(type: CategoryType, updateAction: (List<MainCategory>) -> List<MainCategory>) {
        if (type == CategoryType.EXPENSE) {
            val newList = updateAction(_expenseMainCategories.value)
            _expenseMainCategories.value = newList
            // 保存到本地
            repository.saveMainCategories(newList, type)
        } else {
            val newList = updateAction(_incomeMainCategories.value)
            _incomeMainCategories.value = newList
            // 保存到本地
            repository.saveMainCategories(newList, type)
        }
    }

    // (旧方法兼容：这些方法已废弃，留空即可)
    fun addCategory(name: String, icon: ImageVector, type: CategoryType) {}
    fun deleteCategory(category: Category, type: CategoryType) {}
    fun reorderCategories(categories: List<Category>, type: CategoryType) {}

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

    // 【新增】根据新的“当前余额”更新账户
    // 原理：NewInitialBalance = NewCurrentBalance - (Sum of all transactions for this account)
    fun updateAccountWithNewBalance(account: Account, newCurrentBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 获取当前所有账单快照
            val transactions = repository.allExpenses.first()
            // 2. 计算该账户所有历史流水总和
            val transactionSum = transactions
                .filter { it.accountId == account.id }
                .sumOf { it.amount }

            // 3. 反推新的初始余额
            val newInitialBalance = newCurrentBalance - transactionSum

            // 4. 更新账户
            val updatedAccount = account.copy(initialBalance = newInitialBalance)
            repository.updateAccount(updatedAccount)
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

    fun insertPeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPeriodic(transaction)
            repository.checkAndExecutePeriodicTransactions()
        }
    }

    fun updatePeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePeriodic(transaction)
            repository.checkAndExecutePeriodicTransactions()
        }
    }

    fun deletePeriodic(transaction: PeriodicTransaction) {
        viewModelScope.launch(Dispatchers.IO) { repository.deletePeriodic(transaction) }
    }

    // ===========================
    // 10. [新增] 图表页面 (ChartScreen) 专属状态
    // ===========================
    // 既然要持久化，就放在 ViewModel 里，横竖屏切换绝对不会丢

    // 1. 图表模式 (默认月视图)
    private val _chartMode = MutableStateFlow(ChartMode.MONTH)
    val chartModeState = _chartMode.asStateFlow()

    fun setChartMode(mode: ChartMode) {
        _chartMode.value = mode
    }

    // 2. 交易类型 (默认支出)
    private val _chartTransactionType = MutableStateFlow(TransactionType.EXPENSE)
    val chartTransactionTypeState = _chartTransactionType.asStateFlow()

    fun setChartTransactionType(type: TransactionType) {
        _chartTransactionType.value = type
    }

    // 3. 当前选中的日期 (时间戳)
    private val _chartDateMillis = MutableStateFlow(System.currentTimeMillis())
    val chartDateMillisState = _chartDateMillis.asStateFlow()

    fun setChartDate(millis: Long) {
        _chartDateMillis.value = millis
    }

    // 4. 自定义日期范围 (Start, End) - Pair<Long, Long>?
    private val _chartCustomDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val chartCustomDateRangeState = _chartCustomDateRange.asStateFlow()

    fun setChartCustomDateRange(start: Long?, end: Long?) {
        if (start != null && end != null) {
            _chartCustomDateRange.value = start to end
        } else {
            _chartCustomDateRange.value = null
        }
    }
}