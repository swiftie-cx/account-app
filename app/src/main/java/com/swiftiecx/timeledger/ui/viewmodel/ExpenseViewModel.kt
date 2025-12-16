package com.swiftiecx.timeledger.ui.viewmodel

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.Budget
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.data.ExpenseRepository
import com.swiftiecx.timeledger.data.PeriodicTransaction
import com.swiftiecx.timeledger.data.SyncStrategy
import com.swiftiecx.timeledger.ui.navigation.Category
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.screen.chart.ChartMode
import com.swiftiecx.timeledger.ui.screen.chart.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.math.abs

// 定义筛选类型枚举
enum class ExpenseTypeFilter { ALL, EXPENSE, INCOME, TRANSFER }
// 定义分类类型枚举
enum class CategoryType { EXPENSE, INCOME }

// 同步状态 UI State
sealed class SyncUiState {
    object Idle : SyncUiState()
    data class Loading(val msg: String) : SyncUiState()
    data class Success(val msg: String) : SyncUiState()
    data class Error(val err: String) : SyncUiState()
    // 冲突状态：需要 UI 弹窗处理
    data class Conflict(val cloudTime: Long) : SyncUiState()
}

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // 用于预算更新的互斥锁，防止并发冲突
    private val budgetUpdateMutex = Mutex()

    // ===========================
    // 0. 全局设置 (Currency) - [修改] 临时本地管理，解决 Repository 报错
    // ===========================

    // 使用 MutableStateFlow 在内存中管理，默认 CNY
    private val _defaultCurrency = MutableStateFlow("CNY")
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    fun setDefaultCurrency(currencyCode: String) {
        _defaultCurrency.value = currencyCode
        // TODO: 将来需要在 Repository 中实现 saveDefaultCurrency(currencyCode)
    }

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
    // 0. 欢迎页与初始化逻辑
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

            // [修改] 更新本地状态
            _defaultCurrency.value = currencyCode
            // TODO: repository.saveDefaultCurrency(currencyCode)

            // 2. 创建默认账户
            val defaultAccount = Account(
                name = "默认账户",
                type = "默认",
                initialBalance = initialBalance,
                currency = currencyCode,
                iconName = "AccountBalanceWallet",
                isLiability = false
            )

            // 插入账户并获取 ID
            val newId = repository.insertAccount(defaultAccount)
            repository.saveDefaultAccountId(newId)

            // 3. 标记完成
            repository.setFirstLaunchCompleted()
        }
    }

    // ===========================
    // 1. 用户认证与账号信息相关 (Firebase)
    // ===========================

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userEmail: StateFlow<String> = repository.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun register(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.register(email, password)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "注册失败")
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.login(email, password)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "登录失败")
            }
        }
    }

    // 发送重置密码邮件
    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "发送失败")
            }
        }
    }

    // 修改密码
    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.changePassword(oldPass, newPass)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "修改失败")
            }
        }
    }

    fun logout() = repository.logout()

    // 注销账号：增加回调，确保 UI 能收到成功或失败的通知
    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteUserAccount()
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "注销失败")
            }
        }
    }

    // 旧的隐私锁验证逻辑 (本地 PIN/Pattern)
    fun verifyPin(pin: String): Boolean = repository.verifyPin(pin)
    fun savePin(pin: String) = repository.savePin(pin)
    fun verifyPattern(pattern: List<Int>) = repository.verifyPattern(pattern)
    fun savePattern(pattern: List<Int>) = repository.savePattern(pattern)

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
            // 重新加载默认分类到 UI
            _expenseMainCategories.value = repository.getMainCategories(CategoryType.EXPENSE)
            _incomeMainCategories.value = repository.getMainCategories(CategoryType.INCOME)
        }
    }

    // ===========================
    // 4. 分类管理 (Category)
    // ===========================

    private val _expenseMainCategories = MutableStateFlow(repository.getMainCategories(CategoryType.EXPENSE))
    val expenseMainCategoriesState: StateFlow<List<MainCategory>> = _expenseMainCategories.asStateFlow()

    private val _incomeMainCategories = MutableStateFlow(repository.getMainCategories(CategoryType.INCOME))
    val incomeMainCategoriesState: StateFlow<List<MainCategory>> = _incomeMainCategories.asStateFlow()

    val expenseCategoriesState: StateFlow<List<Category>> = _expenseMainCategories.map { list ->
        list.flatMap { it.subCategories }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomeCategoriesState: StateFlow<List<Category>> = _incomeMainCategories.map { list ->
        list.flatMap { it.subCategories }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- 操作逻辑 ---

    fun addSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map { main ->
                if (main.title == mainCategory.title) {
                    main.copy(subCategories = main.subCategories + subCategory)
                } else {
                    main
                }
            }
        }
    }

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

    fun reorderMainCategories(newOrder: List<MainCategory>, type: CategoryType) {
        updateMainCategoryList(type) { newOrder }
    }

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

    private fun updateMainCategoryList(type: CategoryType, updateAction: (List<MainCategory>) -> List<MainCategory>) {
        if (type == CategoryType.EXPENSE) {
            val newList = updateAction(_expenseMainCategories.value)
            _expenseMainCategories.value = newList
            repository.saveMainCategories(newList, type)
        } else {
            val newList = updateAction(_incomeMainCategories.value)
            _incomeMainCategories.value = newList
            repository.saveMainCategories(newList, type)
        }
    }

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

    fun updateAccountWithNewBalance(account: Account, newCurrentBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = repository.allExpenses.first()
            val transactionSum = transactions
                .filter { it.accountId == account.id }
                .sumOf { it.amount }

            val newInitialBalance = newCurrentBalance - transactionSum
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
    // 10. 图表页面状态
    // ===========================

    private val _chartMode = MutableStateFlow(ChartMode.MONTH)
    val chartModeState = _chartMode.asStateFlow()

    fun setChartMode(mode: ChartMode) {
        _chartMode.value = mode
    }

    private val _chartTransactionType = MutableStateFlow(TransactionType.EXPENSE)
    val chartTransactionTypeState = _chartTransactionType.asStateFlow()

    fun setChartTransactionType(type: TransactionType) {
        _chartTransactionType.value = type
    }

    private val _chartDateMillis = MutableStateFlow(System.currentTimeMillis())
    val chartDateMillisState = _chartDateMillis.asStateFlow()

    fun setChartDate(millis: Long) {
        _chartDateMillis.value = millis
    }

    private val _chartCustomDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val chartCustomDateRangeState = _chartCustomDateRange.asStateFlow()

    fun setChartCustomDateRange(start: Long?, end: Long?) {
        if (start != null && end != null) {
            _chartCustomDateRange.value = start to end
        } else {
            _chartCustomDateRange.value = null
        }
    }

    // ===========================
    // 11. 【新增】同步相关逻辑
    // ===========================

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState = _syncState.asStateFlow()

    // 第一步：点击“同步”按钮调用此方法
    fun startSync() {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Loading("正在检查云端数据...")

            val result = repository.checkCloudStatus()

            if (result.isFailure) {
                _syncState.value = SyncUiState.Error(result.exceptionOrNull()?.message ?: "连接失败")
                return@launch
            }

            val status = result.getOrNull()!!

            // 核心逻辑：判断是否冲突
            if (status.hasCloudData && status.hasLocalData) {
                _syncState.value = SyncUiState.Conflict(status.cloudTimestamp)
            } else if (status.hasCloudData && !status.hasLocalData) {
                // 本地空，云端有 -> 自动下载
                performSync(SyncStrategy.OVERWRITE_LOCAL)
            } else {
                // 云端空，本地有 -> 自动上传
                performSync(SyncStrategy.OVERWRITE_CLOUD)
            }
        }
    }

    // 第二步：执行具体的同步策略
    fun performSync(strategy: SyncStrategy) {
        viewModelScope.launch {
            val strategyName = when(strategy) {
                SyncStrategy.MERGE -> "正在智能合并..."
                SyncStrategy.OVERWRITE_CLOUD -> "正在上传备份..."
                SyncStrategy.OVERWRITE_LOCAL -> "正在恢复数据..."
            }
            _syncState.value = SyncUiState.Loading(strategyName)

            val result = repository.executeSync(strategy)

            if (result.isSuccess) {
                _syncState.value = SyncUiState.Success(result.getOrNull() ?: "同步成功")

                // 【新增】如果执行了 恢复 或 合并，需要重新加载分类配置到 UI
                if (strategy == SyncStrategy.OVERWRITE_LOCAL || strategy == SyncStrategy.MERGE) {
                    _expenseMainCategories.value = repository.getMainCategories(CategoryType.EXPENSE)
                    _incomeMainCategories.value = repository.getMainCategories(CategoryType.INCOME)
                }

            } else {
                _syncState.value = SyncUiState.Error(result.exceptionOrNull()?.message ?: "同步失败")
            }

            delay(3000)
            _syncState.value = SyncUiState.Idle
        }
    }

    // 重置状态
    fun resetSyncState() {
        _syncState.value = SyncUiState.Idle
    }
}