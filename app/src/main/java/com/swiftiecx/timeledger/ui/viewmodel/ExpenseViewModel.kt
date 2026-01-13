package com.swiftiecx.timeledger.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swiftiecx.timeledger.data.Account
import com.swiftiecx.timeledger.data.Budget
import com.swiftiecx.timeledger.data.ExchangeRates
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.data.PeriodicTransaction
import com.swiftiecx.timeledger.data.SyncStrategy
import com.swiftiecx.timeledger.data.DebtRecord
import com.swiftiecx.timeledger.ui.navigation.Category
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.feature.chart.util.ChartMode
import com.swiftiecx.timeledger.ui.feature.chart.util.TransactionType
import com.swiftiecx.timeledger.ui.viewmodel.model.CategoryType
import com.swiftiecx.timeledger.ui.viewmodel.model.ExpenseTypeFilter
import com.swiftiecx.timeledger.ui.viewmodel.model.PersonDebtSummaryInfo
import com.swiftiecx.timeledger.ui.viewmodel.model.SyncUiState
import com.swiftiecx.timeledger.ui.viewmodel.parts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import com.swiftiecx.timeledger.data.ExpenseRepository

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    application: Application
) : AndroidViewModel(application) {

    // parts
    private val settingsPart = SettingsCurrencyPart(repository, viewModelScope)

    private val categoriesPart = CategoriesPart(
        repository = repository,
        scope = viewModelScope,
        appContextProvider = { getApplication<Application>() }
    )

    private val accountsPart = AccountsPart(
        repository = repository,
        scope = viewModelScope,
        afterClearAllData = { categoriesPart.refreshCategories() }
    )

    private val authPart = AuthPart(repository, viewModelScope)

    private val expensesPart = ExpensesPart(repository, viewModelScope)

    private val filterPart = ExpenseFilterPart(viewModelScope, expensesPart.allExpenses)

    private val budgetPart = BudgetPart(repository, viewModelScope)

    private val periodicPart = PeriodicPart(repository, viewModelScope)

    private val chartPart = ChartPart()

    private val syncPart = SyncPart(
        repository = repository,
        scope = viewModelScope,
        afterLocalOverwritten = { categoriesPart.refreshCategories() }
    )

    private val debtPart = DebtPart(repository, viewModelScope, getApplication())

    private val demoPart = DemoDataPart(
        repository = repository,
        scope = viewModelScope,
        app = getApplication(),
        defaultCurrencyProvider = { defaultCurrency.value },
        afterGenerated = { categoriesPart.refreshCategories() }
    )

    // -------------------------
    // init（保持原行为）
    // -------------------------
    init {
        viewModelScope.launch(Dispatchers.IO) { ExchangeRates.updateRates() }
        viewModelScope.launch(Dispatchers.IO) { repository.checkAndExecutePeriodicTransactions() }
        categoriesPart.refreshCategories()
    }

    // -------------------------
    // 0. Currency
    // -------------------------
    val defaultCurrency: StateFlow<String> = settingsPart.defaultCurrency
    fun setDefaultCurrency(currencyCode: String) = settingsPart.setDefaultCurrency(currencyCode)

    // -------------------------
    // onboarding
    // -------------------------
    val isFirstLaunch: Boolean get() = repository.isFirstLaunch()

    fun completeOnboarding(initialBalance: Double) {
        // 原逻辑保留在这里（它会创建默认账户、保存默认货币等）
        viewModelScope.launch(Dispatchers.IO) {
            val currencyCode = defaultCurrency.value
            repository.saveDefaultCurrency(currencyCode)

            val context = getApplication<Application>()
            val accountName = try {
                context.getString(com.swiftiecx.timeledger.R.string.default_account_name)
            } catch (_: Exception) {
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

    // -------------------------
    // 1. Auth / Privacy
    // -------------------------
    val isLoggedIn: StateFlow<Boolean> = authPart.isLoggedIn
    val userEmail: StateFlow<String> = authPart.userEmail

    fun register(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        authPart.register(email, password, onSuccess, onError)

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        authPart.login(email, password, onSuccess, onError)

    suspend fun refreshEmailVerification(): Boolean = authPart.refreshEmailVerification()

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        authPart.sendPasswordResetEmail(email, onSuccess, onError)

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        authPart.changePassword(oldPass, newPass, onSuccess, onError)

    fun logout() = authPart.logout()

    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) =
        authPart.deleteUserAccount(onSuccess, onError)

    fun verifyPin(pin: String): Boolean = authPart.verifyPin(pin)
    fun savePin(pin: String) = authPart.savePin(pin)
    fun verifyPattern(pattern: List<Int>) = authPart.verifyPattern(pattern)
    fun savePattern(pattern: List<Int>) = authPart.savePattern(pattern)

    fun getPrivacyType(): String = authPart.getPrivacyType()
    fun setPrivacyType(type: String) = authPart.setPrivacyType(type)
    fun setBiometricEnabled(enabled: Boolean) = authPart.setBiometricEnabled(enabled)
    fun isBiometricEnabled(): Boolean = authPart.isBiometricEnabled()

    // -------------------------
    // 3. Accounts
    // -------------------------
    val allAccounts: StateFlow<List<Account>> = accountsPart.allAccounts
    val defaultAccountId: StateFlow<Long> = accountsPart.defaultAccountId

    fun setDefaultAccount(id: Long) = accountsPart.setDefaultAccount(id)
    fun reorderAccounts(newOrder: List<Account>) = accountsPart.reorderAccounts(newOrder)
    fun insertAccount(account: Account) = accountsPart.insertAccount(account)
    fun updateAccount(account: Account) = accountsPart.updateAccount(account)
    fun deleteAccount(account: Account) = accountsPart.deleteAccount(account)
    fun updateAccountWithNewBalance(account: Account, newCurrentBalance: Double) =
        accountsPart.updateAccountWithNewBalance(account, newCurrentBalance)

    fun clearAllData() = accountsPart.clearAllData()

    // -------------------------
    // 4. Categories
    // -------------------------
    val expenseMainCategoriesState: StateFlow<List<MainCategory>> = categoriesPart.expenseMainCategoriesState
    val incomeMainCategoriesState: StateFlow<List<MainCategory>> = categoriesPart.incomeMainCategoriesState
    val expenseCategoriesState: StateFlow<List<Category>> = categoriesPart.expenseCategoriesState
    val incomeCategoriesState: StateFlow<List<Category>> = categoriesPart.incomeCategoriesState

    fun refreshCategories(specificContext: Context? = null) = categoriesPart.refreshCategories(specificContext)

    fun addSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) =
        categoriesPart.addSubCategory(mainCategory, subCategory, type)

    fun deleteSubCategory(mainCategory: MainCategory, subCategory: Category, type: CategoryType) =
        categoriesPart.deleteSubCategory(mainCategory, subCategory, type)

    fun reorderMainCategories(newOrder: List<MainCategory>, type: CategoryType) =
        categoriesPart.reorderMainCategories(newOrder, type)

    fun reorderSubCategories(mainCategory: MainCategory, newSubOrder: List<Category>, type: CategoryType) =
        categoriesPart.reorderSubCategories(mainCategory, newSubOrder, type)

    // 你原来就是空实现，这里保持空实现，不引入行为变化
    fun addCategory(name: String, icon: ImageVector, type: CategoryType) {}
    fun deleteCategory(category: Category, type: CategoryType) {}
    fun reorderCategories(categories: List<Category>, type: CategoryType) {}

    // -------------------------
    // 5. Expenses / Transfer
    // -------------------------
    val allExpenses: StateFlow<List<Expense>> = expensesPart.allExpenses
    fun insert(expense: Expense) = expensesPart.insert(expense)
    fun updateExpense(expense: Expense) = expensesPart.updateExpense(expense)
    fun deleteExpense(expense: Expense) = expensesPart.deleteExpense(expense)

    fun createTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        fromAmount: Double,
        toAmount: Double,
        date: Date,
        remark: String? = null
    ) = expensesPart.createTransfer(fromAccountId, toAccountId, fromAmount, toAmount, date, remark)

    // -------------------------
    // 6. Search & Filter
    // -------------------------
    val searchText: StateFlow<String> = filterPart.searchText
    val selectedTypeFilter: StateFlow<ExpenseTypeFilter> = filterPart.selectedTypeFilter
    val selectedCategoryFilter: StateFlow<String?> = filterPart.selectedCategoryFilter
    val filteredExpenses: StateFlow<List<Expense>> = filterPart.filteredExpenses

    fun updateSearchText(text: String) = filterPart.updateSearchText(text)
    fun updateTypeFilter(filter: ExpenseTypeFilter) = filterPart.updateTypeFilter(filter)
    fun updateCategoryFilter(category: String?) = filterPart.updateCategoryFilter(category)

    // -------------------------
    // 7. Budget
    // -------------------------
    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>> = budgetPart.getBudgetsForMonth(year, month)
    fun saveBudget(budget: Budget, allCategoryTitles: List<String>) = budgetPart.saveBudget(budget, allCategoryTitles)
    fun syncBudgetsFor(year: Int, month: Int) = budgetPart.syncBudgetsFor(year, month)

    // -------------------------
    // 9. Periodic
    // -------------------------
    val allPeriodicTransactions: StateFlow<List<PeriodicTransaction>> = periodicPart.allPeriodicTransactions
    fun insertPeriodic(transaction: PeriodicTransaction) = periodicPart.insertPeriodic(transaction)
    fun updatePeriodic(transaction: PeriodicTransaction) = periodicPart.updatePeriodic(transaction)
    fun deletePeriodic(transaction: PeriodicTransaction) = periodicPart.deletePeriodic(transaction)

    // -------------------------
    // 10. Chart
    // -------------------------
    val chartModeState = chartPart.chartModeState
    fun setChartMode(mode: ChartMode) = chartPart.setChartMode(mode)

    val chartTransactionTypeState = chartPart.chartTransactionTypeState
    fun setChartTransactionType(type: TransactionType) = chartPart.setChartTransactionType(type)

    val chartDateMillisState = chartPart.chartDateMillisState
    fun setChartDate(millis: Long) = chartPart.setChartDate(millis)

    val chartCustomDateRangeState = chartPart.chartCustomDateRangeState
    fun setChartCustomDateRange(start: Long?, end: Long?) = chartPart.setChartCustomDateRange(start, end)

    // -------------------------
    // 11. Sync
    // -------------------------
    val syncState: StateFlow<SyncUiState> = syncPart.syncState
    fun startSync() = syncPart.startSync()
    fun performSync(strategy: SyncStrategy) = syncPart.performSync(strategy)
    fun resetSyncState() = syncPart.resetSyncState()

    // -------------------------
    // 12. Debt
    // -------------------------
    fun getAllDebtRecords(): Flow<List<DebtRecord>> = debtPart.getAllDebtRecords()
    fun getDebtRecords(accountId: Long): Flow<List<DebtRecord>> = debtPart.getDebtRecords(accountId)
    fun getDebtRecordsByPerson(personName: String): Flow<List<DebtRecord>> = debtPart.getDebtRecordsByPerson(personName)
    fun getDebtRecordById(id: Long): Flow<DebtRecord?> = debtPart.getDebtRecordById(id)

    fun deleteDebtRecordsByPerson(personName: String) = debtPart.deleteDebtRecordsByPerson(personName)
    fun updateDebtWithTransaction(record: DebtRecord, oldDate: Date, oldAmount: Double) = debtPart.updateDebtWithTransaction(record, oldDate, oldAmount)
    fun updateDebtRecord(record: DebtRecord) = debtPart.updateDebtRecord(record)
    fun deleteDebtRecord(record: DebtRecord) = debtPart.deleteDebtRecord(record)
    fun insertDebtRecord(record: DebtRecord, countInStats: Boolean = true) = debtPart.insertDebtRecord(record, countInStats)

    fun settleDebt(
        personName: String,
        amount: Double,
        interest: Double,
        accountId: Long,
        isBorrow: Boolean,
        remark: String?,
        date: Date,
        generateBill: Boolean
    ) = debtPart.settleDebt(personName, amount, interest, accountId, isBorrow, remark, date, generateBill)

    fun getGlobalDebtSummary() = debtPart.getGlobalDebtSummary()
    fun getPersonDebtSummary(personName: String): Flow<PersonDebtSummaryInfo> = debtPart.getPersonDebtSummary(personName)

    // -------------------------
    // Demo Data
    // -------------------------
    fun generateDemoData() = demoPart.generateDemoData()
}
