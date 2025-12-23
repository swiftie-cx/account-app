package com.swiftiecx.timeledger.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swiftiecx.timeledger.R
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
import com.swiftiecx.timeledger.data.DebtRecord
enum class ExpenseTypeFilter { ALL, EXPENSE, INCOME, TRANSFER }
enum class CategoryType { EXPENSE, INCOME }

sealed class SyncUiState {
    object Idle : SyncUiState()
    data class Loading(val msg: String) : SyncUiState()
    data class Success(val msg: String) : SyncUiState()
    data class Error(val err: String) : SyncUiState()
    data class Conflict(val cloudTime: Long) : SyncUiState()
}

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    application: Application
) : AndroidViewModel(application) {

    private val budgetUpdateMutex = Mutex()

    // ===========================
    // 0. 全局设置 & 自动货币检测
    // ===========================

    private fun detectAutoCurrency(): String {
        return try {
            val locale = Locale.getDefault()
            val language = locale.language
            val country = locale.country

            // 根据你支持的 16 种语言进行精准匹配
            when (language) {
                "zh" -> "CNY" // 简体中文 - 人民币
                "en" -> "USD" // 英语 - 美元 (通用)
                "ja" -> "JPY" // 日本语 - 日元
                "ko" -> "KRW" // 韩语 - 韩元
                "de" -> "EUR"    // 德语 - 欧元
                "fr" -> "EUR"    // 法语 - 欧元
                "it" -> "EUR"    // 意大利语 - 欧元
                "es" -> "EUR"    // 西班牙语 (西班牙) - 欧元
                "br" -> "BRL"    // 葡萄牙语 (巴西) - 巴西雷亚尔
                "mx" -> "MXN"    // 西班牙语 (墨西哥) - 墨西哥比索
                "pl" -> "PLN"    // 波兰语 - 波兰兹罗提
                "ru" -> "RUB"    // 俄语 - 俄罗斯卢布
                "id" -> "IDR"    // 印尼语 - 印尼盾
                "vn" -> "VND"    // 越南语 - 越南盾 (若资源文件夹名为 vi，请改为 vi)
                "vi" -> "VND"    // 越南语 (备选)
                "tr" -> "TRY"    // 土耳其语 - 土耳其里拉
                "th" -> "THB"    // 泰语 - 泰铢
                "in" -> "INR"    // 印地语 - 印度卢比
                // 如果不在上述语言定义内，返回 USD
                else -> "USD"
            }
        } catch (e: Exception) {
            "USD"
        }
    }

    // [逻辑修正]
    // 初始化时：优先读取用户存过的设置。
    // 只有当设置不存在（null）时，才使用自动检测。
    // 这样以后您手动改成了 CNY，这里就会读到 CNY，而不会被自动检测覆盖。
    private val _defaultCurrency = MutableStateFlow(
        repository.getSavedCurrency() ?: detectAutoCurrency()
    )
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    fun setDefaultCurrency(currencyCode: String) {
        _defaultCurrency.value = currencyCode
        // 手动修改时，保存到设置
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveDefaultCurrency(currencyCode)
        }
    }

    init {
        // [关键] 移除了之前的“强制覆盖”逻辑，避免影响后续使用

        // 1. 更新汇率
        viewModelScope.launch(Dispatchers.IO) {
            ExchangeRates.updateRates()
        }

        // 2. 检查周期记账
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndExecutePeriodicTransactions()
        }

        // 3. 刷新分类
        refreshCategories()
    }

    fun refreshCategories(specificContext: Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetContext = specificContext ?: getApplication<Application>()
            repository.forceUpdateCategoryNames(targetContext)
            val expenseCats = repository.getMainCategories(CategoryType.EXPENSE)
            val incomeCats = repository.getMainCategories(CategoryType.INCOME)
            _expenseMainCategories.value = expenseCats
            _incomeMainCategories.value = incomeCats
        }
    }

    // ===========================
    // 0. 欢迎页与初始化 (核心修复点)
    // ===========================
    val isFirstLaunch: Boolean
        get() = repository.isFirstLaunch()

    fun completeOnboarding(initialBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            // 获取当前 ViewModel 里的货币 (欢迎页显示的那个，比如 USD)
            val currencyCode = _defaultCurrency.value

            // [核心修复] 建账的同时，必须把这个货币保存为默认设置！
            // 这样下次进 App，init 里的 getSavedCurrency() 就能读到它了。
            repository.saveDefaultCurrency(currencyCode)

            val context = getApplication<Application>()
            val accountName = try {
                context.getString(R.string.default_account_name)
            } catch (e: Exception) {
                "Default Account"
            }

            val defaultAccount = Account(
                name = accountName,
                type = "account_default",
                initialBalance = initialBalance,
                currency = currencyCode, // 账户也是这个货币
                iconName = "Wallet",
                isLiability = false
            )

            val newId = repository.insertAccount(defaultAccount)
            repository.saveDefaultAccountId(newId)
            repository.setFirstLaunchCompleted()
        }
    }

    // ... (以下代码保持不变，省略以节省篇幅，请直接保留您现有的后续代码) ...

    // 为了方便您复制，这里列出后续所有方法名，您可以直接接在上面 completeOnboarding 后面：
    // isLoggedIn, userEmail, register, login, sendPasswordResetEmail, changePassword, logout, deleteUserAccount
    // verifyPin, savePin... 等等
    // allAccounts, defaultAccountId, setDefaultAccount...
    // addSubCategory, deleteSubCategory...
    // insert, createTransfer...
    // updateSearchText...
    // getBudgetsForMonth...
    // getPrivacyType...
    // insertPeriodic...
    // setChartMode...
    // startSync...

    // (如果需要完整代码请告诉我，不过只需要替换上面上半部分即可)

    // ===========================
    // ... 请保留原文件的剩余部分 ...
    // ===========================

    // 1. 用户认证
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userEmail: StateFlow<String> = repository.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun register(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.register(email, password)
            if (result.isSuccess) onSuccess() else onError(
                result.exceptionOrNull()?.message ?: "注册失败"
            )
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.login(email, password)
            if (result.isSuccess) onSuccess() else onError(
                result.exceptionOrNull()?.message ?: "登录失败"
            )
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            if (result.isSuccess) onSuccess() else onError(
                result.exceptionOrNull()?.message ?: "发送失败"
            )
        }
    }

    fun changePassword(
        oldPass: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.changePassword(oldPass, newPass)
            if (result.isSuccess) onSuccess() else onError(
                result.exceptionOrNull()?.message ?: "修改失败"
            )
        }
    }

    fun logout() = repository.logout()

    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteUserAccount()
            if (result.isSuccess) onSuccess() else onError(
                result.exceptionOrNull()?.message ?: "注销失败"
            )
        }
    }

    // Privacy
    fun verifyPin(pin: String): Boolean = repository.verifyPin(pin)
    fun savePin(pin: String) = repository.savePin(pin)
    fun verifyPattern(pattern: List<Int>) = repository.verifyPattern(pattern)
    fun savePattern(pattern: List<Int>) = repository.savePattern(pattern)

    // ===========================
    // 3. 账户数据
    // ===========================
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
            refreshCategories()
        }
    }

    // ===========================
    // 4. 分类管理
    // ===========================
    private val _expenseMainCategories = MutableStateFlow<List<MainCategory>>(emptyList())
    val expenseMainCategoriesState: StateFlow<List<MainCategory>> =
        _expenseMainCategories.asStateFlow()

    private val _incomeMainCategories = MutableStateFlow<List<MainCategory>>(emptyList())
    val incomeMainCategoriesState: StateFlow<List<MainCategory>> =
        _incomeMainCategories.asStateFlow()

    val expenseCategoriesState: StateFlow<List<Category>> = _expenseMainCategories.map { list ->
        list.flatMap { it.subCategories }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomeCategoriesState: StateFlow<List<Category>> = _incomeMainCategories.map { list ->
        list.flatMap { it.subCategories }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- 操作逻辑 ---
    fun addSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map { if (it.title == mainCategory.title) it.copy(subCategories = it.subCategories + subCategory) else it }
        }
    }

    fun deleteSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) {
        updateMainCategoryList(type) { list ->
            list.map { if (it.title == mainCategory.title) it.copy(subCategories = it.subCategories.filter { sub -> sub.title != subCategory.title }) else it }
        }
    }

    fun reorderMainCategories(newOrder: List<MainCategory>, type: CategoryType) {
        updateMainCategoryList(type) { newOrder }
    }

    fun reorderSubCategories(
        mainCategory: MainCategory,
        newSubOrder: List<Category>,
        type: CategoryType
    ) {
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
            repository.saveMainCategories(newList, CategoryType.EXPENSE)
        } else {
            val newList = updateAction(_incomeMainCategories.value)
            _incomeMainCategories.value = newList
            repository.saveMainCategories(newList, CategoryType.INCOME)
        }
    }

    fun addCategory(name: String, icon: ImageVector, type: CategoryType) {}
    fun deleteCategory(category: Category, type: CategoryType) {}
    fun reorderCategories(categories: List<Category>, type: CategoryType) {}

    // ===========================
    // 5. 账单操作
    // ===========================
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) { repository.insert(expense) }
    }

    fun createTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        fromAmount: Double,
        toAmount: Double,
        date: Date
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenseOut = Expense(
                accountId = fromAccountId,
                category = "category_transfer_out", // 使用 Key
                amount = -abs(fromAmount),
                date = date,
                remark = null
            )
            val expenseIn = Expense(
                accountId = toAccountId,
                category = "category_transfer_in", // 使用 Key
                amount = abs(toAmount),
                date = date,
                remark = null
            )
            repository.createTransfer(expenseOut, expenseIn)
        }
    }

    fun insertAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) { repository.insertAccount(account) }
    }

    // [新增] 必须包含此方法
    fun updateAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateAccount(account) }
    }

    fun updateAccountWithNewBalance(account: Account, newCurrentBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = repository.allExpenses.first()
            val transactionSum =
                transactions.filter { it.accountId == account.id }.sumOf { it.amount }
            val newInitialBalance = newCurrentBalance - transactionSum
            repository.updateAccount(account.copy(initialBalance = newInitialBalance))
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteExpense(expense) }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateExpense(expense) }
    }

    // ===========================
    // 6. 搜索与筛选
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
            val matchesSearchText = text.isBlank() || (expense.remark?.contains(
                text,
                true
            ) == true) || expense.category.contains(text, true)
            val matchesType = when (type) {
                ExpenseTypeFilter.ALL -> true
                ExpenseTypeFilter.EXPENSE -> expense.amount < 0 && !expense.category.startsWith("category_transfer")
                ExpenseTypeFilter.INCOME -> expense.amount > 0 && !expense.category.startsWith("category_transfer")
                ExpenseTypeFilter.TRANSFER -> expense.category.startsWith("category_transfer")
            }
            val matchesCategory = category == "全部" || expense.category == category
            matchesSearchText && matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun updateTypeFilter(filter: ExpenseTypeFilter) {
        _selectedTypeFilter.value = filter
    }

    fun updateCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category ?: "全部"
    }

    // ===========================
    // 7. 预算管理
    // ===========================
    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>> {
        return repository.getBudgetsForMonth(year, month)
    }

    fun saveBudget(budget: Budget, allCategoryTitles: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetUpdateMutex.withLock {
                repository.upsertBudget(budget)
                if (budget.category != "总预算") {
                    val allBudgets =
                        repository.getBudgetsForMonth(budget.year, budget.month).first()
                    val calculatedSum =
                        allBudgets.filter { it.category in allCategoryTitles }.sumOf { it.amount }
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
            if (targetMonthBudgets.isNotEmpty()) return@launch
            val recentBudget = repository.getMostRecentBudget() ?: return@launch
            if (recentBudget.year == year && recentBudget.month == month) return@launch
            val recentMonthBudgets =
                getBudgetsForMonth(recentBudget.year, recentBudget.month).first()
            val newBudgets = recentMonthBudgets.map { it.copy(id = 0, year = year, month = month) }
            if (newBudgets.isNotEmpty()) repository.upsertBudgets(newBudgets)
        }
    }

    // ===========================
    // 8. 隐私
    // ===========================
    fun getPrivacyType(): String = repository.getPrivacyType()
    fun setPrivacyType(type: String) = repository.savePrivacyType(type)
    fun setBiometricEnabled(enabled: Boolean) = repository.setBiometricEnabled(enabled)
    fun isBiometricEnabled(): Boolean = repository.isBiometricEnabled()

    // ===========================
    // 9. 周期记账
    // ===========================
    val allPeriodicTransactions: StateFlow<List<PeriodicTransaction>> =
        repository.allPeriodicTransactions
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
    // 10. 图表
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
        _chartCustomDateRange.value = if (start != null && end != null) start to end else null
    }

    // ===========================
    // 11. 同步逻辑
    // ===========================
    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState = _syncState.asStateFlow()

    fun startSync() {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Loading("正在检查云端数据...")
            val result = repository.checkCloudStatus()
            if (result.isFailure) {
                _syncState.value =
                    SyncUiState.Error(result.exceptionOrNull()?.message ?: "连接失败")
                return@launch
            }
            val status = result.getOrNull()!!
            if (status.hasCloudData && status.hasLocalData) {
                _syncState.value = SyncUiState.Conflict(status.cloudTimestamp)
            } else if (status.hasCloudData && !status.hasLocalData) {
                performSync(SyncStrategy.OVERWRITE_LOCAL)
            } else {
                performSync(SyncStrategy.OVERWRITE_CLOUD)
            }
        }
    }

    fun performSync(strategy: SyncStrategy) {
        viewModelScope.launch {
            _syncState.value =
                SyncUiState.Loading(if (strategy == SyncStrategy.MERGE) "正在智能合并..." else "正在同步...")
            val result = repository.executeSync(strategy)
            if (result.isSuccess) {
                _syncState.value = SyncUiState.Success(result.getOrNull() ?: "同步成功")
                if (strategy == SyncStrategy.OVERWRITE_LOCAL || strategy == SyncStrategy.MERGE) {
                    refreshCategories()
                }
            } else {
                _syncState.value =
                    SyncUiState.Error(result.exceptionOrNull()?.message ?: "同步失败")
            }
            delay(3000)
            _syncState.value = SyncUiState.Idle
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncUiState.Idle
    }

    // ===========================
    //  12. 借贷记录 (修正后)
    // ===========================

    fun getAllDebtRecords(): Flow<List<DebtRecord>> {
        return repository.getAllDebtRecords() // 需确保 Repository 中有此方法
    }

    fun getDebtRecords(accountId: Long): Flow<List<DebtRecord>> {
        return repository.getDebtRecords(accountId)
    }

    // [核心修改] 插入借贷记录并自动生成账单明细
    fun insertDebtRecord(record: DebtRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 插入原始借贷记录
            repository.insertDebtRecord(record)

            // 2. 自动生成对应的账单明细 (Expense)
            // 借入：资金到账，属于收入(+金额)；借出：资金支出，属于支出(-金额)
            val fundAccountId = record.inAccountId ?: record.outAccountId
            if (fundAccountId != null) {
                val expense = Expense(
                    accountId = fundAccountId,
                    amount = if (record.inAccountId != null) record.amount else -record.amount,
                    category = if (record.inAccountId != null) "借入" else "借出",
                    remark = "来自借贷对象: ${record.personName}${if (record.note != null) " (${record.note})" else ""}",
                    date = record.borrowTime
                )
                repository.insert(expense) // 同步到明细流水表
            }
        }
    }
}