package com.swiftiecx.timeledger.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.swiftiecx.timeledger.data.repository.*
import com.swiftiecx.timeledger.ui.common.Category
import com.swiftiecx.timeledger.ui.common.CategoryData
import com.swiftiecx.timeledger.ui.common.IconMapper
import com.swiftiecx.timeledger.ui.common.MainCategory
import com.swiftiecx.timeledger.data.model.CategoryType
import kotlinx.coroutines.flow.*
import kotlin.jvm.JvmName
/**
 * ExpenseRepository（Facade）
 * - 对外 API 不变
 * - 具体实现拆到 data/repository
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao,
    private val periodicDao: PeriodicTransactionDao,
    private val categoryDao: CategoryDao,
    private val debtRecordDao: DebtRecordDao,
    private val context: Context
) {

    private val prefs = context.getSharedPreferences(RepoKeys.PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getString(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    private val prefsStore = PrefsStore(prefs, gson)

    private val auth = AuthDataSource(
        firebaseAuth = firebaseAuth,
        getString = { id, args -> getString(id, *args) }
    )

    private val local = LocalDataSource(
        expenseDao = expenseDao,
        budgetDao = budgetDao,
        accountDao = accountDao,
        periodicDao = periodicDao,
        categoryDao = categoryDao,
        debtRecordDao = debtRecordDao,
        context = context,
        getString = { id, args -> getString(id, *args) },
        transferOutCategoryKey = { transferOutCategoryKey() },
        transferInCategoryKey = { transferInCategoryKey() }
    )

    private val cloud = CloudDataSource(
        firestore = firestore,
        gson = gson,
        debtRecordDao = debtRecordDao,
        expenseDao = expenseDao,
        accountDao = accountDao,
        budgetDao = budgetDao,
        normalizeAccountTypeValue = { normalizeAccountTypeValue(it) },
        getMainCategories = { getMainCategories(it) }
    )

    private val sync = SyncCoordinator(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        prefsStore = prefsStore,
        expenseDao = expenseDao,
        accountDao = accountDao,
        debtRecordDao = debtRecordDao,
        cloud = cloud,
        normalizeAccountTypeValue = { normalizeAccountTypeValue(it) },
        getString = { id, args -> getString(id, *args) }
    )

    // -------------------------
    // Auth State（保持字段名）
    // -------------------------
    val firebaseUser: StateFlow<FirebaseUser?> = auth.firebaseUser
    val emailVerified: StateFlow<Boolean> = auth.emailVerified
    val isLoggedIn: Flow<Boolean> = auth.isLoggedIn
    val userEmail: Flow<String> = firebaseUser.map { it?.email ?: "" }

    // -------------------------
    // currency / first launch
    // -------------------------
    fun getSavedCurrency(): String? = prefsStore.getSavedCurrency()
    fun saveDefaultCurrency(currencyCode: String) = prefsStore.saveDefaultCurrency(currencyCode)

    fun isFirstLaunch(): Boolean = prefsStore.isFirstLaunch()
    fun setFirstLaunchCompleted() = prefsStore.setFirstLaunchCompleted()

    // -------------------------
    // category / account type
    // -------------------------
    fun normalizeCategoryValue(value: String): String = value
    fun localizeCategoryValue(value: String): String = value

    fun transferOutCategoryKey(): String = RepoKeys.CATEGORY_TRANSFER_OUT
    fun transferInCategoryKey(): String = RepoKeys.CATEGORY_TRANSFER_IN

    fun isTransferCategory(value: String): Boolean {
        return value == RepoKeys.CATEGORY_TRANSFER_OUT || value == RepoKeys.CATEGORY_TRANSFER_IN
    }

    private val builtinAccountTypeKeys: Set<String> = setOf(
        "account_cash",
        "account_card",
        "account_credit",
        "account_investment",
        "account_ewallet",
        "account_default"
    )

    fun normalizeAccountTypeValue(value: String): String {
        if (builtinAccountTypeKeys.contains(value)) return value
        val id = context.resources.getIdentifier(value, "string", context.packageName)
        return if (id != 0) value else value
    }

    fun localizeAccountTypeValue(value: String): String {
        if (!builtinAccountTypeKeys.contains(value)) return value
        val id = context.resources.getIdentifier(value, "string", context.packageName)
        return if (id != 0) context.getString(id) else value
    }

    // -------------------------
    // 分类存取（保持原行为：prefs + gson + IconMapper）
    // -------------------------
    fun saveMainCategories(categories: List<MainCategory>, type: CategoryType) {
        val dtoList = categories.map { main ->
            MainCategoryDto(
                title = main.title,
                iconName = IconMapper.getIconName(main.icon),
                colorInt = main.color.toArgb(),
                subs = main.subCategories.map { SubCategoryDto(it.key, IconMapper.getIconName(it.icon)) }
            )
        }
        val json = gson.toJson(dtoList)
        val key = if (type == CategoryType.EXPENSE) RepoKeys.KEY_MAIN_CATS_EXPENSE else RepoKeys.KEY_MAIN_CATS_INCOME
        prefsStore.saveJson(key, json)
    }

    fun getMainCategories(type: CategoryType): List<MainCategory> {
        val key = if (type == CategoryType.EXPENSE) RepoKeys.KEY_MAIN_CATS_EXPENSE else RepoKeys.KEY_MAIN_CATS_INCOME
        val json = prefsStore.getJson(key)

        return if (json != null) {
            try {
                val itemType = object : TypeToken<List<MainCategoryDto>>() {}.type
                val dtoList: List<MainCategoryDto> = gson.fromJson(json, itemType) ?: emptyList()
                dtoList.map { dto ->
                    MainCategory(
                        title = dto.title,
                        icon = IconMapper.getIcon(dto.iconName),
                        color = Color(dto.colorInt),
                        subCategories = dto.subs.map { subDto ->
                            val stableKey = subDto.title
                            val displayTitle = CategoryData.getDisplayName(stableKey, context)
                            Category(displayTitle, IconMapper.getIcon(subDto.iconName), key = stableKey)
                        }
                    )
                }
            } catch (_: Exception) {
                getDefaultCategories(type)
            }
        } else {
            getDefaultCategories(type)
        }
    }

    fun saveCategories(categories: List<Category>, type: CategoryType) {
        val dtoList = categories.map { CategoryDto(it.title, IconMapper.getIconName(it.icon)) }
        val json = gson.toJson(dtoList)
        val key = if (type == CategoryType.EXPENSE) RepoKeys.KEY_CATS_EXPENSE else RepoKeys.KEY_CATS_INCOME
        prefsStore.saveJson(key, json)
    }

    fun getCategories(type: CategoryType): List<Category> {
        val key = if (type == CategoryType.EXPENSE) RepoKeys.KEY_CATS_EXPENSE else RepoKeys.KEY_CATS_INCOME
        val json = prefsStore.getJson(key)

        return if (json != null) {
            try {
                val itemType = object : TypeToken<List<CategoryDto>>() {}.type
                val dtoList: List<CategoryDto> = gson.fromJson(json, itemType) ?: emptyList()
                dtoList.map { Category(it.title, IconMapper.getIcon(it.iconName)) }
            } catch (_: Exception) {
                getDefaultCategories(type).flatMap { it.subCategories }
            }
        } else {
            getDefaultCategories(type).flatMap { it.subCategories }
        }
    }

    private fun getDefaultCategories(type: CategoryType): List<MainCategory> {
        return if (type == CategoryType.EXPENSE) {
            CategoryData.getExpenseCategories(context)
        } else {
            CategoryData.getIncomeCategories(context)
        }
    }

    // -------------------------
    // Auth API（保持方法名）
    // -------------------------
    suspend fun register(email: String, password: String): Result<Boolean> = auth.register(email, password)
    suspend fun login(email: String, password: String): Result<Boolean> = auth.login(email, password)
    suspend fun refreshEmailVerification(): Boolean = auth.refreshEmailVerification()
    fun logout() = auth.logout()
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> = auth.sendPasswordResetEmail(email)
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Boolean> = auth.changePassword(oldPassword, newPassword)
    suspend fun deleteUserAccount(): Result<Boolean> = auth.deleteUserAccount()

    // -------------------------
    // Account state（保持字段名）
    // -------------------------
    private val _defaultAccountId = MutableStateFlow(prefsStore.getDefaultAccountId())
    val defaultAccountId: StateFlow<Long> = _defaultAccountId.asStateFlow()

    private val _accountOrder = MutableStateFlow(prefsStore.loadAccountOrder())

    fun saveDefaultAccountId(id: Long) {
        prefsStore.saveDefaultAccountId(id)
        _defaultAccountId.value = id
    }

    fun saveAccountOrder(order: List<Long>) {
        prefsStore.saveAccountOrder(order)
        _accountOrder.value = order
    }

    @JvmName("saveAccountOrderAccounts")
    fun saveAccountOrder(accounts: List<Account>) {
        // 兼容旧调用：ViewModel 传 List<Account>
        saveAccountOrder(accounts.map { it.id })
    }

    // -------------------------
    // Flows（保持字段名）
    // -------------------------
    val allExpenses: Flow<List<Expense>> = local.allExpenses

    val allAccounts: Flow<List<Account>> =
        accountDao.getAllAccounts()
            .combine(_accountOrder) { accounts, order ->
                if (order.isEmpty()) accounts
                else accounts.sortedBy { acc ->
                    val idx = order.indexOf(acc.id)
                    if (idx == -1) Int.MAX_VALUE else idx
                }
            }

    val allPeriodicTransactions: Flow<List<PeriodicTransaction>> = local.allPeriodicTransactions

    // -------------------------
    // Expense API（保持方法名）
    // -------------------------
    suspend fun insert(expense: Expense) = local.insert(expense)
    suspend fun createTransfer(expenseOut: Expense, expenseIn: Expense) = local.createTransfer(expenseOut, expenseIn)
    suspend fun deleteExpense(expense: Expense) = local.deleteExpense(expense)
    suspend fun updateExpense(expense: Expense) = local.updateExpense(expense)

    // -------------------------
    // Budget
    // -------------------------
    fun getBudgetsForMonth(year: Int, month: Int) = local.getBudgetsForMonth(year, month)
    suspend fun upsertBudget(budget: Budget) = local.upsertBudget(budget)
    suspend fun upsertBudgets(budgets: List<Budget>) = local.upsertBudgets(budgets)
    suspend fun getMostRecentBudget() = local.getMostRecentBudget()

    // -------------------------
    // Account（写入前 normalize）
    // -------------------------
    suspend fun insertAccount(account: Account) =
        accountDao.insert(account.copy(type = normalizeAccountTypeValue(account.type)))

    suspend fun updateAccount(account: Account) =
        accountDao.update(account.copy(type = normalizeAccountTypeValue(account.type)))

    suspend fun deleteAccount(account: Account) =
        accountDao.delete(account)

    // -------------------------
    // Privacy（保持方法名）
    // -------------------------
    fun getPrivacyType(): String = prefsStore.getPrivacyType()
    fun savePrivacyType(type: String) = prefsStore.savePrivacyType(type)

    fun savePin(pin: String) = prefsStore.savePin(pin)
    fun verifyPin(inputPin: String): Boolean = prefsStore.verifyPin(inputPin)

    fun savePattern(pattern: List<Int>) = prefsStore.savePattern(pattern)
    fun verifyPattern(inputPattern: List<Int>): Boolean = prefsStore.verifyPattern(inputPattern)

    fun isBiometricEnabled(): Boolean = prefsStore.isBiometricEnabled()
    fun setBiometricEnabled(enabled: Boolean) = prefsStore.setBiometricEnabled(enabled)

    // -------------------------
    // Periodic
    // -------------------------
    suspend fun insertPeriodic(transaction: PeriodicTransaction) = local.insertPeriodic(transaction)
    suspend fun updatePeriodic(transaction: PeriodicTransaction) = local.updatePeriodic(transaction)
    suspend fun deletePeriodic(transaction: PeriodicTransaction) = local.deletePeriodic(transaction)
    suspend fun getPeriodicById(id: Long) = local.getPeriodicById(id)
    suspend fun checkAndExecutePeriodicTransactions() = local.checkAndExecutePeriodicTransactions()

    // -------------------------
    // Cloud Sync
    // -------------------------
    suspend fun checkCloudStatus(): Result<SyncCoordinator.SyncCheckResult> = sync.checkCloudStatus()
    suspend fun executeSync(strategy: SyncStrategy): Result<String> = sync.executeSync(strategy)

    // -------------------------
    // 多语言刷新分类名（无参版本）
    // -------------------------
    suspend fun forceUpdateCategoryNames() = local.forceUpdateCategoryNames()

    /**
     * 兼容旧调用：有的 ViewModel 会传 context 进来
     * - 参数不用，但保留签名避免 "Too many arguments"
     */
    suspend fun forceUpdateCategoryNames(@Suppress("UNUSED_PARAMETER") ctx: Context) {
        forceUpdateCategoryNames()
    }

    // -------------------------
    // Debt
    // -------------------------
    fun getDebtRecords(accountId: Long): Flow<List<DebtRecord>> = local.getDebtRecords(accountId)
    suspend fun saveDebtWithTransaction(debt: DebtRecord, expense: Expense) = local.saveDebtWithTransaction(debt, expense)
    suspend fun insertDebtRecord(record: DebtRecord) = local.insertDebtRecord(record)
    suspend fun updateDebtRecord(record: DebtRecord) = local.updateDebtRecord(record)
    suspend fun deleteDebtRecord(record: DebtRecord) = local.deleteDebtRecord(record)
    fun getAllDebtRecords(): Flow<List<DebtRecord>> = local.getAllDebtRecords()
    suspend fun deleteAllDebtRecords() = local.deleteAllDebtRecords()

    /**
     * 兼容旧 API：清空本地数据（用于测试/重置）
     * 只做 DB delete，不影响 UI 表现（与旧 clearAllData 语义一致）
     */
    suspend fun clearAllData() {
        expenseDao.deleteAll()
        accountDao.deleteAll()
        budgetDao.deleteAll()
        debtRecordDao.deleteAll()
        // periodicDao.deleteAll() 如果你项目里有这个方法再打开
    }
}
