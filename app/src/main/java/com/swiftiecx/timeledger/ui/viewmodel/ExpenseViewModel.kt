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
import com.swiftiecx.timeledger.data.RecordType
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

            // 根据支持的语言进行精准匹配
            when (language) {
                "zh" -> "CNY" // 简体中文 - 人民币
                "en" -> "USD" // 英语 - 美元 (通用)
                "ja" -> "JPY" // 日本语 - 日元
                "ko" -> "KRW" // 韩语 - 韩元
                "de" -> "EUR" // 德语 - 欧元
                "fr" -> "EUR" // 法语 - 欧元
                "it" -> "EUR" // 意大利语 - 欧元
                "es" -> "EUR" // 西班牙语 - 欧元
                "br" -> "BRL" // 葡萄牙语 (巴西) - 巴西雷亚尔
                "mx" -> "MXN" // 西班牙语 (墨西哥) - 墨西哥比索
                "pl" -> "PLN" // 波兰语 - 波兰兹罗提
                "ru" -> "RUB" // 俄语 - 俄罗斯卢布
                "id" -> "IDR" // 印尼语 - 印尼盾
                "vn" -> "VND" // 越南语 - 越南盾
                "vi" -> "VND" // 越南语 (备选)
                "tr" -> "TRY" // 土耳其语 - 土耳其里拉
                "th" -> "THB" // 泰语 - 泰铢
                "in" -> "INR" // 印地语 - 印度卢比
                else -> "USD"
            }
        } catch (e: Exception) {
            "USD"
        }
    }

    private val _defaultCurrency = MutableStateFlow(
        repository.getSavedCurrency() ?: detectAutoCurrency()
    )
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    fun setDefaultCurrency(currencyCode: String) {
        _defaultCurrency.value = currencyCode
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveDefaultCurrency(currencyCode)
        }
    }

    init {
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

    val isFirstLaunch: Boolean
        get() = repository.isFirstLaunch()

    fun completeOnboarding(initialBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val currencyCode = _defaultCurrency.value
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
                currency = currencyCode,
                iconName = "Wallet",
                isLiability = false
            )

            val newId = repository.insertAccount(defaultAccount)
            repository.saveDefaultAccountId(newId)
            repository.setFirstLaunchCompleted()
        }
    }

    // ===========================
    // 1. 用户认证
    // ===========================
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

    fun deleteAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAccount(account)
        }
    }

    fun deleteDebtRecordsByPerson(personName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 获取该联系人的所有记录并循环删除
            val records = repository.getAllDebtRecords().first().filter { it.personName == personName }
            records.forEach { repository.deleteDebtRecord(it) }
        }
    }

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
                category = "category_transfer_out", // 这里的 string 仅作兼容，逻辑走 RecordType
                amount = -abs(fromAmount),
                date = date,
                remark = null
            )
            val expenseIn = Expense(
                accountId = toAccountId,
                category = "category_transfer_in", // 这里的 string 仅作兼容，逻辑走 RecordType
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
        // Repository 中的 deleteExpense 已经实现了级联删除 debtId 关联记录的逻辑
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

    // ✅ [核心修改] 筛选逻辑：基于 RecordType 而非字符串
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses, _searchText, _selectedTypeFilter, _selectedCategoryFilter
    ) { expenses, text, type, category ->
        expenses.filter { expense ->
            // 1. 搜索匹配
            val matchesSearchText = text.isBlank() || (expense.remark?.contains(
                text,
                true
            ) == true) || expense.category.contains(text, true)

            // 2. 类型匹配 (基于 RecordType)
            val matchesType = when (type) {
                ExpenseTypeFilter.ALL -> {
                    // 在"全部"列表中：
                    // 如果是普通收支(0) -> 显示
                    // 如果是转账(1) -> 只显示"支出"(amount < 0)的那条，作为转账事务的代表。
                    if (expense.recordType == RecordType.TRANSFER) {
                        expense.amount < 0
                    } else {
                        true
                    }
                }
                ExpenseTypeFilter.EXPENSE -> {
                    // 仅显示普通支出
                    expense.recordType == RecordType.INCOME_EXPENSE && expense.amount < 0
                }
                ExpenseTypeFilter.INCOME -> {
                    // 仅显示普通收入
                    expense.recordType == RecordType.INCOME_EXPENSE && expense.amount > 0
                }
                ExpenseTypeFilter.TRANSFER -> {
                    // 仅显示转账 (同样只显示"支出"那条作为代表)
                    expense.recordType == RecordType.TRANSFER && expense.amount < 0
                }
            }

            // 3. 分类匹配
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

    // ===================================
    //  12. 债务管理核心逻辑 (修正)
    // ===================================

    fun getAllDebtRecords(): Flow<List<DebtRecord>> = repository.getAllDebtRecords()

    fun getDebtRecords(accountId: Long): Flow<List<DebtRecord>> = repository.getDebtRecords(accountId)

    fun getDebtRecordsByPerson(personName: String): Flow<List<DebtRecord>> {
        return repository.getAllDebtRecords().map { all ->
            all.filter { it.personName == personName }
        }
    }

    fun getDebtRecordById(id: Long): Flow<DebtRecord?> {
        return repository.getAllDebtRecords().map { all -> all.find { it.id == id } }
    }

    fun updateDebtWithTransaction(record: DebtRecord, oldDate: Date, oldAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 更新债务表
            repository.updateDebtRecord(record)

            // 2. 匹配并更新对应的流水记录 (通过旧日期、金额、对象姓名匹配)
            val expenses = repository.allExpenses.first()
            val matchedExpense = expenses.find {
                it.date.time == oldDate.time &&
                        abs(it.amount) == abs(oldAmount) &&
                        it.remark?.contains(record.personName) == true
            }

            matchedExpense?.let {
                // [最小化修改] 仅替换此处
                val context = getApplication<Application>()
                val relationLabel = try {
                    context.getString(R.string.label_related_person, record.personName)
                } catch (e: Exception) {
                    "Related: ${record.personName}" // 兜底
                }
                val noteSuffix = if (!record.note.isNullOrEmpty()) " | ${record.note}" else ""

                repository.updateExpense(it.copy(
                    date = record.borrowTime,
                    amount = if (it.amount < 0) -record.amount else record.amount,
                    remark = "$relationLabel$noteSuffix"
                ))
            }
        }
    }

    fun updateDebtRecord(record: DebtRecord) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateDebtRecord(record)
    }

    fun deleteDebtRecord(record: DebtRecord) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteDebtRecord(record)
    }

    /**
     * [最小化修改] 插入债务记录 + 多语言备注
     */
    fun insertDebtRecord(record: DebtRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val fundAccountId = record.inAccountId ?: record.outAccountId

            // 如果选择了资金账户，则生成流水
            if (fundAccountId != null && fundAccountId != -1L) {
                val isLending = record.outAccountId != null

                // [最小化修改] 仅替换此处
                val context = getApplication<Application>()
                val relationLabel = try {
                    context.getString(R.string.label_related_person, record.personName)
                } catch (e: Exception) {
                    "Related: ${record.personName}" // 兜底
                }
                val noteSuffix = if (record.note.isNullOrBlank()) "" else " | ${record.note}"
                val finalRemark = "$relationLabel$noteSuffix"

                val expense = Expense(
                    accountId = fundAccountId,
                    amount = if (isLending) -abs(record.amount) else abs(record.amount),
                    category = if (isLending) "借出" else "借入",
                    remark = finalRemark,
                    date = record.borrowTime,
                    recordType = RecordType.INCOME_EXPENSE // 债务相关流水目前视为收支
                )
                repository.saveDebtWithTransaction(record, expense)

            } else {
                repository.insertDebtRecord(record)
            }
        }
    }

    /**
     * [最小化修改] 处理债务结算 (收款/还款)
     */
    fun settleDebt(
        personName: String,
        amount: Double,   // 结算本金
        interest: Double, // 利息金额
        accountId: Long,
        isBorrow: Boolean, // true 为还款, false 为收款
        remark: String?,
        date: Date,
        generateBill: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 构造债务冲抵记录
            val settleRecord = DebtRecord(
                accountId = -1, // 结算记录本身不绑定特定债务账户ID
                personName = personName,
                amount = amount,
                borrowTime = date,
                note = "结算: $remark ${if(interest > 0) "|利息:$interest|" else ""}",
                inAccountId = if (!isBorrow) accountId else null,
                outAccountId = if (isBorrow) accountId else null
            )

            // 2. 同步生成收支流水
            if (generateBill && accountId != -1L) {
                // [最小化修改] 仅替换此处
                val context = getApplication<Application>()
                val relationLabel = try {
                    context.getString(R.string.label_related_person, personName)
                } catch (e: Exception) { "Related: $personName" }
                val noteSuffix = if (remark.isNullOrBlank()) "" else " | $remark"

                val finalRemark = "$relationLabel | 利息:$interest$noteSuffix"

                val expense = Expense(
                    accountId = accountId,
                    amount = if (!isBorrow) amount else -amount,
                    category = if (!isBorrow) "债务收款" else "债务还款",
                    remark = finalRemark,
                    date = date,
                    recordType = RecordType.INCOME_EXPENSE
                )

                repository.saveDebtWithTransaction(settleRecord, expense)

                // 第二笔：利息流水 (独立计入收支，不关联债务ID)
                if (interest > 0) {
                    val interestLabel = try { context.getString(R.string.label_interest) } catch(e:Exception){ "Interest" }
                    val actionLabel = if(!isBorrow) "收款" else "还款"

                    repository.insert(Expense(
                        accountId = accountId,
                        amount = if (!isBorrow) interest else -interest,
                        category = if (!isBorrow) "收入-其他" else "其他",
                        remark = "$personName $actionLabel$interestLabel",
                        date = date,
                        recordType = RecordType.INCOME_EXPENSE
                    ))
                }
            } else {
                repository.insertDebtRecord(settleRecord)
            }
        }
    }

    fun getGlobalDebtSummary(): Flow<Pair<Double, Double>> {
        return repository.getAllDebtRecords().map { all ->
            val totalLend = all.filter { it.outAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }
            val totalCollected = all.filter { it.inAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }

            val totalBorrow = all.filter { it.inAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }
            val totalPaid = all.filter { it.outAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }

            val receivable = (totalLend - totalCollected).coerceAtLeast(0.0)
            val payable = (totalBorrow - totalPaid).coerceAtLeast(0.0)

            receivable to payable
        }
    }

    fun getPersonDebtSummary(personName: String): Flow<PersonDebtSummaryInfo> {
        return repository.getAllDebtRecords().map { all ->
            val personRecords = all.filter { it.personName == personName }

            val lendTotal = personRecords.filter { it.outAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }
            val borrowTotal = personRecords.filter { it.inAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }

            val settledIn = personRecords.filter { it.inAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }
            val settledOut = personRecords.filter { it.outAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }

            val netOriginal = lendTotal - borrowTotal
            val isReceivable = netOriginal >= 0

            val remaining = if (isReceivable) (netOriginal - settledIn) else (abs(netOriginal) - settledOut)
            val collectedPrincipal = if (isReceivable) settledIn else settledOut

            val interestRegex = "\\|利息:([\\d.]+)\\|".toRegex()
            val totalInterest = personRecords.sumOf {
                interestRegex.find(it.note ?: "")?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            }

            PersonDebtSummaryInfo(
                totalAmount = remaining,
                collectedAmount = collectedPrincipal,
                interest = totalInterest
            )
        }
    }
    // 生成丰富的演示数据 (用于 Google Play 截图)
    fun generateDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            // 0. 清理旧数据
            repository.clearAllData()

            // 1. 创建多账户体系 (修复了 isLiability 参数缺失的问题)
            val currency = _defaultCurrency.value

            val accCashId = repository.insertAccount(Account(
                name = "Cash Wallet",
                type = "Cash",
                initialBalance = 1500.0,
                currency = currency,
                iconName = "Wallet",
                isLiability = false, // 现金账户：非负债
                category = "FUNDS"
            ))

            val accBankId = repository.insertAccount(Account(
                name = "Chase Bank",
                type = "Bank Card",
                initialBalance = 25000.0,
                currency = currency,
                iconName = "AccountBalance",
                isLiability = false, // 储蓄卡：非负债
                category = "FUNDS"
            ))

            val accCreditId = repository.insertAccount(Account(
                name = "Amex Platinum",
                type = "Credit Card",
                initialBalance = -3200.0,
                currency = currency,
                iconName = "CreditCard",
                isLiability = true,  // 信用卡：是负债
                category = "CREDIT",
                creditLimit = 50000.0
            ))

            val accInvestId = repository.insertAccount(Account(
                name = "Stock Portfolio",
                type = "Investment",
                initialBalance = 100000.0,
                currency = currency,
                iconName = "TrendingUp",
                isLiability = false, // 投资账户：非负债
                category = "FUNDS"
            ))

            // 2. 设置预算
            val calendar = java.util.Calendar.getInstance()
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1

            repository.upsertBudget(Budget(category = "Food", amount = 3000.0, year = currentYear, month = currentMonth))
            repository.upsertBudget(Budget(category = "Shopping", amount = 2000.0, year = currentYear, month = currentMonth))
            repository.upsertBudget(Budget(category = "Traffic", amount = 1000.0, year = currentYear, month = currentMonth))
            repository.upsertBudget(Budget(category = "Entertainment", amount = 1500.0, year = currentYear, month = currentMonth))
            repository.upsertBudget(Budget(category = "Total Budget", amount = 10000.0, year = currentYear, month = currentMonth))

            // 3. 生成债务记录
            val lendRecord = DebtRecord(
                accountId = -1, personName = "David", amount = 2000.0, borrowTime = Date(),
                outAccountId = accBankId, note = "Lend for rent"
            )
            // 注意：这里手动构建 Expense 同样需要注意 debtId 默认值问题，通常它有默认值 =null 所以这里没问题
            val lendExpense = Expense(accountId = accBankId, category = "借出", amount = -2000.0, date = Date(), remark = "Lend to David")
            repository.saveDebtWithTransaction(lendRecord, lendExpense)

            val borrowRecord = DebtRecord(
                accountId = -1, personName = "Mom", amount = 5000.0, borrowTime = Date(),
                inAccountId = accBankId, note = "Emergency fund"
            )
            val borrowExpense = Expense(accountId = accBankId, category = "借入", amount = 5000.0, date = Date(), remark = "Borrow from Mom")
            repository.saveDebtWithTransaction(borrowRecord, borrowExpense)

            // 4. 生成流水
            val random = java.util.Random()
            val expenseCats = listOf("Food", "Traffic", "Shopping", "Entertainment", "Housing", "Medical", "Daily", "Clothes")
            val incomeCats = listOf("Salary", "Bonus", "PartTime")
            val remarks = listOf("Lunch", "Taxi", "Grocery", "Movie", "Gas", "Coffee", "Netflix", "Gym")

            val daysBack = 90
            for (i in 0..daysBack) {
                calendar.time = Date()
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
                val date = calendar.time

                val count = random.nextInt(5)
                repeat(count) {
                    val isIncome = random.nextInt(10) < 1

                    if (isIncome) {
                        val amount = 2000 + random.nextDouble() * 5000
                        repository.insert(Expense(
                            accountId = accBankId,
                            category = incomeCats.random(),
                            amount = amount,
                            date = date,
                            remark = "Income source",
                            recordType = RecordType.INCOME_EXPENSE
                        ))
                    } else {
                        val accId = listOf(accCashId, accBankId, accCreditId).random()
                        val cat = expenseCats.random()
                        val baseAmount = 10 + random.nextDouble() * 100
                        val amount = if (random.nextInt(10) == 0) baseAmount * 5 else baseAmount

                        repository.insert(Expense(
                            accountId = accId,
                            category = cat,
                            amount = -amount,
                            date = date,
                            remark = remarks.random(),
                            recordType = RecordType.INCOME_EXPENSE
                        ))
                    }
                }
            }

            // 5. 转账记录
            repeat(3) {
                calendar.time = Date()
                calendar.add(java.util.Calendar.MONTH, -it)
                val date = calendar.time
                val amount = 1000.0 + random.nextInt(2000)

                // ✅ 使用 createTransfer 保证数据结构正确
                repository.createTransfer(
                    Expense(accountId = accBankId, category = "category_transfer_out", amount = -amount, date = date, remark = "Credit Card Repayment"),
                    Expense(accountId = accCreditId, category = "category_transfer_in", amount = amount, date = date, remark = "Repayment Received")
                )
            }

            refreshCategories()
        }
    }
}

data class PersonDebtSummaryInfo(
    val totalAmount: Double,
    val collectedAmount: Double,
    val interest: Double
)