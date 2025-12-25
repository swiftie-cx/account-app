package com.swiftiecx.timeledger.data

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.ui.navigation.Category
import com.swiftiecx.timeledger.ui.navigation.CategoryData
import com.swiftiecx.timeledger.ui.navigation.IconMapper
import com.swiftiecx.timeledger.ui.navigation.MainCategory
import com.swiftiecx.timeledger.ui.viewmodel.CategoryType
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

// 用于序列化的简单数据类
data class CategoryDto(val title: String, val iconName: String)

// 用于序列化嵌套结构的 DTO
data class SubCategoryDto(val title: String, val iconName: String)
data class MainCategoryDto(val title: String, val iconName: String, val colorInt: Int, val subs: List<SubCategoryDto>)

// 定义同步冲突的三种策略
enum class SyncStrategy {
    OVERWRITE_CLOUD,   // 以此设备为准（覆盖云端）
    OVERWRITE_LOCAL,   // 以云端为准（覆盖本地）
    MERGE              // 智能合并
}

private const val KEY_CATEGORY_TRANSFER_OUT = "category_transfer_out"
private const val KEY_CATEGORY_TRANSFER_IN = "category_transfer_in"

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao,
    private val periodicDao: PeriodicTransactionDao,
    private val categoryDao: CategoryDao,
    private val debtRecordDao: DebtRecordDao,
    private val context: Context
) {
    // --- 偏好设置 (SharedPreferences) ---
    private val prefs = context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Firebase ---
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        firebaseAuth.useAppLanguage()
    }

    // ===========================
    //  默认货币设置 (New)
    // ===========================

    fun getSavedCurrency(): String? {
        return prefs.getString("key_default_currency", null)
    }

    fun saveDefaultCurrency(currencyCode: String) {
        prefs.edit().putString("key_default_currency", currencyCode).apply()
    }

    // ===========================
    //  Localization helpers
    // ===========================

    private fun s(@StringRes id: Int, vararg args: Any): String {
        return if (args.isEmpty()) context.getString(id) else context.getString(id, *args)
    }

    fun getString(@StringRes id: Int, vararg args: Any): String = s(id, *args)

    fun normalizeCategoryValue(value: String): String {
        return value
    }

    fun localizeCategoryValue(value: String): String {
        return value
    }

    fun isTransferCategory(value: String): Boolean {
        return value == KEY_CATEGORY_TRANSFER_OUT || value == KEY_CATEGORY_TRANSFER_IN ||
                value == "category_transfer_out" || value == "category_transfer_in"
    }

    fun transferOutCategoryKey(): String = KEY_CATEGORY_TRANSFER_OUT
    fun transferInCategoryKey(): String = KEY_CATEGORY_TRANSFER_IN

    private val builtinAccountTypeKeys: Set<String> = setOf(
        "account_cash", "account_card", "account_credit", "account_investment", "account_ewallet", "account_default"
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

    // --- 首次启动标记 ---
    fun isFirstLaunch(): Boolean = prefs.getBoolean("is_first_launch", true)

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean("is_first_launch", false).apply()
    }

    // ===========================
    //  MainCategory Persistence
    // ===========================

    fun saveMainCategories(categories: List<MainCategory>, type: CategoryType) {
        val dtoList = categories.map { main ->
            MainCategoryDto(
                title = main.title,
                iconName = IconMapper.getIconName(main.icon),
                colorInt = main.color.toArgb(),
                subs = main.subCategories.map { sub ->
                    SubCategoryDto(sub.key, IconMapper.getIconName(sub.icon))
                }
            )
        }
        val json = gson.toJson(dtoList)
        val key = if (type == CategoryType.EXPENSE) "main_cats_expense" else "main_cats_income"
        prefs.edit().putString(key, json).apply()
    }

    fun getMainCategories(type: CategoryType): List<MainCategory> {
        val key = if (type == CategoryType.EXPENSE) "main_cats_expense" else "main_cats_income"
        val json = prefs.getString(key, null)

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
            } catch (e: Exception) {
                e.printStackTrace()
                getDefaultCategories(type)
            }
        } else {
            getDefaultCategories(type)
        }
    }

    private fun getDefaultCategories(type: CategoryType): List<MainCategory> {
        return if (type == CategoryType.EXPENSE) {
            CategoryData.getExpenseCategories(context)
        } else {
            CategoryData.getIncomeCategories(context)
        }
    }

    // ===========================
    //  Legacy Category Persistence
    // ===========================

    fun saveCategories(categories: List<Category>, type: CategoryType) {
        val dtoList = categories.map {
            CategoryDto(it.title, IconMapper.getIconName(it.icon))
        }
        val json = gson.toJson(dtoList)
        val key = if (type == CategoryType.EXPENSE) "cats_expense" else "cats_income"
        prefs.edit().putString(key, json).apply()
    }

    fun getCategories(type: CategoryType): List<Category> {
        val key = if (type == CategoryType.EXPENSE) "cats_expense" else "cats_income"
        val json = prefs.getString(key, null)

        return if (json != null) {
            try {
                val itemType = object : TypeToken<List<CategoryDto>>() {}.type
                val dtoList: List<CategoryDto> = gson.fromJson(json, itemType) ?: emptyList()
                dtoList.map {
                    Category(it.title, IconMapper.getIcon(it.iconName))
                }
            } catch (e: Exception) {
                getDefaultCategories(type).flatMap { it.subCategories }
            }
        } else {
            getDefaultCategories(type).flatMap { it.subCategories }
        }
    }

    // ===========================
    //  Firebase Auth
    // ===========================

    val isLoggedIn: Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        trySend(firebaseAuth.currentUser != null)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    val userEmail: Flow<String> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.email ?: "")
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        trySend(firebaseAuth.currentUser?.email ?: "")
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    suspend fun register(email: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            firebaseAuth.currentUser?.sendEmailVerification()?.await()
            Result.success(true)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthUserCollisionException -> s(R.string.error_email_already_registered)
                is FirebaseAuthInvalidCredentialsException -> s(R.string.error_email_invalid)
                is FirebaseNetworkException -> s(R.string.error_network_check)
                else -> s(R.string.error_register_failed, e.message ?: "")
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthInvalidUserException -> s(R.string.error_account_not_exist)
                is FirebaseAuthInvalidCredentialsException -> s(R.string.error_email_or_password_wrong)
                is FirebaseNetworkException -> s(R.string.error_network_check)
                else -> s(R.string.error_login_failed, e.message ?: "")
            }
            Result.failure(Exception(msg))
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthInvalidUserException -> s(R.string.error_email_not_registered)
                is FirebaseAuthInvalidCredentialsException -> s(R.string.error_email_invalid)
                is FirebaseNetworkException -> s(R.string.error_network)
                else -> s(R.string.error_send_failed, e.message ?: "")
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun changePassword(oldPass: String, newPass: String): Result<Boolean> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null || user.email == null) {
                return Result.failure(Exception(s(R.string.error_user_not_logged_in)))
            }
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
            user.reauthenticate(credential).await()
            user.updatePassword(newPass).await()
            Result.success(true)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthInvalidCredentialsException -> s(R.string.error_old_password_wrong)
                is FirebaseAuthRecentLoginRequiredException -> s(R.string.error_recent_login_required)
                is FirebaseNetworkException -> s(R.string.error_network)
                else -> {
                    if (e.message?.contains("weak-password") == true) s(R.string.error_new_password_weak)
                    else s(R.string.error_change_password_failed, e.message ?: "")
                }
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun deleteUserAccount(): Result<Boolean> {
        return try {
            val user = firebaseAuth.currentUser
            user?.delete()?.await()
            Result.success(true)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthRecentLoginRequiredException -> s(R.string.error_recent_login_required_delete)
                is FirebaseNetworkException -> s(R.string.error_network)
                else -> s(R.string.error_delete_account_failed, e.message ?: "")
            }
            Result.failure(Exception(msg))
        }
    }

    // --- 账户状态 ---
    private val _defaultAccountId = MutableStateFlow(prefs.getLong("default_account_id", -1L))
    val defaultAccountId = _defaultAccountId.asStateFlow()

    private val _accountOrder = MutableStateFlow<List<Long>>(loadAccountOrder())

    private fun loadAccountOrder(): List<Long> {
        val json = prefs.getString("account_order", null) ?: return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // --- Expense methods ---
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun insert(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun createTransfer(expenseOut: Expense, expenseIn: Expense) = expenseDao.insertTransfer(expenseOut, expenseIn)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    // --- Budget methods ---
    fun getBudgetsForMonth(year: Int, month: Int) = budgetDao.getBudgetsForMonth(year, month)
    suspend fun upsertBudget(budget: Budget) = budgetDao.upsertBudget(budget)
    suspend fun upsertBudgets(budgets: List<Budget>) = budgetDao.upsertBudgets(budgets)
    suspend fun getMostRecentBudget() = budgetDao.getMostRecentBudget()

    // --- Account methods ---
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
        .combine(_accountOrder) { accounts, order ->
            if (order.isEmpty()) {
                accounts
            } else {
                accounts.sortedBy { account ->
                    val index = order.indexOf(account.id)
                    if (index == -1) Int.MAX_VALUE else index
                }
            }
        }

    suspend fun insertAccount(account: Account) = accountDao.insert(account.copy(type = normalizeAccountTypeValue(account.type)))
    suspend fun updateAccount(account: Account) = accountDao.update(account.copy(type = normalizeAccountTypeValue(account.type)))
    suspend fun deleteAccount(account: Account) = accountDao.delete(account)

    // --- Helper methods ---
    fun saveDefaultAccountId(id: Long) {
        prefs.edit().putLong("default_account_id", id).apply()
        _defaultAccountId.value = id
    }

    fun saveAccountOrder(accounts: List<Account>) {
        val ids = accounts.map { it.id }
        val json = gson.toJson(ids)
        prefs.edit().putString("account_order", json).apply()
        _accountOrder.value = ids
    }

    suspend fun clearAllData() {
        expenseDao.deleteAll()
        budgetDao.deleteAll()
        accountDao.deleteAll()
        // ✅ [新增] 必须调用，以删除债务数据
        debtRecordDao.deleteAll()

        val editor = prefs.edit()
        editor.remove("default_account_id")
        editor.remove("account_order")
        editor.remove("privacy_type")
        editor.remove("privacy_pin")
        editor.remove("privacy_pattern")
        editor.remove("privacy_biometric")

        editor.remove("main_cats_expense")
        editor.remove("main_cats_income")
        editor.remove("cats_expense")
        editor.remove("cats_income")

        editor.apply()

        _defaultAccountId.value = -1L
        _accountOrder.value = emptyList()
    }

    // --- Privacy ---
    fun getPrivacyType(): String = prefs.getString("privacy_type", "NONE") ?: "NONE"
    fun savePrivacyType(type: String) = prefs.edit().putString("privacy_type", type).apply()
    fun savePin(pin: String) = prefs.edit().putString("privacy_pin", pin).apply()
    fun verifyPin(inputPin: String): Boolean = prefs.getString("privacy_pin", "") == inputPin
    fun savePattern(pattern: List<Int>) = prefs.edit().putString("privacy_pattern", pattern.joinToString(",")).apply()
    fun verifyPattern(inputPattern: List<Int>): Boolean = prefs.getString("privacy_pattern", "") == inputPattern.joinToString(",")
    fun isBiometricEnabled(): Boolean = prefs.getBoolean("privacy_biometric", false)
    fun setBiometricEnabled(enabled: Boolean) = prefs.edit().putBoolean("privacy_biometric", enabled).apply()

    // --- Periodic Transaction Methods ---
    val allPeriodicTransactions: Flow<List<PeriodicTransaction>> = periodicDao.getAll()
    suspend fun insertPeriodic(transaction: PeriodicTransaction) = periodicDao.insert(transaction)
    suspend fun updatePeriodic(transaction: PeriodicTransaction) = periodicDao.update(transaction)
    suspend fun deletePeriodic(transaction: PeriodicTransaction) = periodicDao.delete(transaction)
    suspend fun getPeriodicById(id: Long) = periodicDao.getById(id)

    // =========================================================
    //               核心逻辑：检查并执行周期记账
    // =========================================================

    suspend fun checkAndExecutePeriodicTransactions() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.time

        val allRules = periodicDao.getAllSync()

        allRules.forEach { rule ->
            if (rule.nextExecutionDate.time <= endOfToday.time) {
                executeRule(rule)
            }
        }
    }

    private suspend fun executeRule(rule: PeriodicTransaction) {
        if (rule.endMode == 1 && rule.endDate != null && rule.nextExecutionDate.after(rule.endDate)) {
            return
        }
        if (rule.endMode == 2 && rule.endCount != null && rule.endCount <= 0) {
            return
        }

        if (rule.type == 2 && rule.targetAccountId != null) {
            val amountVal = abs(rule.amount)
            val feeVal = abs(rule.fee)

            val finalOut: Double
            val finalIn: Double

            if (rule.transferMode == 0) {
                finalOut = -amountVal
                finalIn = amountVal - feeVal
            } else {
                finalOut = -(amountVal + feeVal)
                finalIn = amountVal
            }

            val expenseOut = Expense(
                accountId = rule.accountId,
                category = transferOutCategoryKey(),
                amount = finalOut,
                date = rule.nextExecutionDate,
                remark = rule.remark ?: s(R.string.periodic_remark_transfer_default)
            )
            val expenseIn = Expense(
                accountId = rule.targetAccountId,
                category = transferInCategoryKey(),
                amount = finalIn,
                date = rule.nextExecutionDate,
                remark = rule.remark ?: s(R.string.periodic_remark_transfer_default)
            )
            expenseDao.insertTransfer(expenseOut, expenseIn)
        } else {
            val finalAmount = if (rule.type == 0) -abs(rule.amount) else abs(rule.amount)
            val expense = Expense(
                category = rule.category,
                amount = finalAmount,
                date = rule.nextExecutionDate,
                accountId = rule.accountId,
                remark = rule.remark ?: s(R.string.periodic_remark_auto_default),
                excludeFromBudget = rule.excludeFromBudget
            )
            expenseDao.insertExpense(expense)
        }

        val calendar = Calendar.getInstance()
        calendar.time = rule.nextExecutionDate

        when (rule.frequency) {
            0 -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            1 -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            2 -> calendar.add(Calendar.MONTH, 1)
            3 -> calendar.add(Calendar.YEAR, 1)
        }
        val newNextDate = calendar.time

        val newEndCount = if (rule.endMode == 2 && rule.endCount != null) rule.endCount - 1 else rule.endCount

        val updatedRule = rule.copy(
            nextExecutionDate = newNextDate,
            endCount = newEndCount
        )

        periodicDao.update(updatedRule)
    }

    // ==========================================
    // 【核心】智能云同步逻辑
    // ==========================================

    data class SyncCheckResult(
        val hasCloudData: Boolean,
        val hasLocalData: Boolean,
        val cloudTimestamp: Long
    )

    private fun parseDate(obj: Any?): Date {
        return when (obj) {
            is Timestamp -> obj.toDate()
            is Long -> Date(obj)
            else -> Date()
        }
    }

    suspend fun checkCloudStatus(): Result<SyncCheckResult> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception(s(R.string.error_not_logged_in)))
        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("backups").document("latest")
                .get().await()

            val localExpenseCount = expenseDao.getAllExpenses().first().size
            val localAccountCount = accountDao.getAllAccounts().first().size
            val localCount = localExpenseCount + localAccountCount

            Result.success(
                SyncCheckResult(
                    hasCloudData = doc.exists(),
                    hasLocalData = localCount > 0,
                    cloudTimestamp = doc.getLong("timestamp") ?: 0L
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeSync(strategy: SyncStrategy): Result<String> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception(s(R.string.error_not_logged_in)))

        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("backups").document("latest")
                .get().await()

            val localExpenses = expenseDao.getAllExpenses().first()
            val localAccounts = accountDao.getAllAccounts().first()
            val localDebts = debtRecordDao.getAllDebtRecords().first()

            when (strategy) {
                SyncStrategy.OVERWRITE_CLOUD -> {
                    uploadData(uid, localExpenses, localAccounts)
                    Result.success(s(R.string.success_backup_to_cloud))
                }

                SyncStrategy.OVERWRITE_LOCAL -> {
                    if (doc.exists()) {
                        val data = doc.data ?: return Result.failure(Exception(s(R.string.error_cloud_data_empty)))
                        restoreDataLocally(data)
                        Result.success(s(R.string.success_restore_from_cloud))
                    } else {
                        Result.failure(Exception(s(R.string.error_cloud_no_data)))
                    }
                }

                SyncStrategy.MERGE -> {
                    if (!doc.exists()) {
                        uploadData(uid, localExpenses, localAccounts)
                        return Result.success(s(R.string.success_cloud_empty_uploaded))
                    }

                    val cloudData = doc.data!!
                    val cloudExpensesMap = cloudData["expenses"] as? List<Map<String, Any>> ?: emptyList()
                    val cloudAccountsMap = cloudData["accounts"] as? List<Map<String, Any>> ?: emptyList()
                    val cloudDebtsMap = cloudData["debt_records"] as? List<Map<String, Any>> ?: emptyList()

                    val accountIdMap = mutableMapOf<Long, Long>()

                    cloudAccountsMap.forEach { accMap ->
                        val name = accMap["name"] as String
                        val typeRaw = accMap["type"] as String
                        val type = normalizeAccountTypeValue(typeRaw)
                        val currency = accMap["currency"] as? String ?: "CNY"

                        val existingLocalAccount = localAccounts.find { it.name == name && normalizeAccountTypeValue(it.type) == type }

                        val cloudId = (accMap["id"] as Number).toLong()
                        if (existingLocalAccount != null) {
                            accountIdMap[cloudId] = existingLocalAccount.id
                        } else {
                            val newAccount = Account(
                                name = name,
                                type = type,
                                initialBalance = (accMap["initialBalance"] as Number).toDouble(),
                                currency = currency,
                                iconName = accMap["iconName"] as? String ?: "Wallet",
                                isLiability = accMap["isLiability"] as? Boolean ?: false
                            )
                            val newId = accountDao.insert(newAccount)
                            accountIdMap[cloudId] = newId
                        }
                    }

                    var addedCount = 0

                    cloudExpensesMap.forEach { expMap ->
                        val amount = (expMap["amount"] as Number).toDouble()
                        val date = parseDate(expMap["date"])
                        val remark = expMap["remark"] as? String ?: ""
                        val categoryRaw = expMap["category"] as String
                        val category = categoryRaw
                        val cloudAccountId = (expMap["accountId"] as Number).toLong()

                        val targetAccountId = accountIdMap[cloudAccountId] ?: return@forEach

                        val isDuplicate = localExpenses.any { local ->
                            local.amount == amount &&
                                    local.date.time == date.time &&
                                    local.remark == remark &&
                                    local.accountId == targetAccountId
                        }

                        if (!isDuplicate) {
                            val newExpense = Expense(
                                accountId = targetAccountId,
                                category = category,
                                amount = amount,
                                date = date,
                                remark = remark
                            )
                            expenseDao.insertExpense(newExpense)
                            addedCount++
                        }
                    }

                    // ✅ [新增] 3. 合并债务记录 (修正版)
                    cloudDebtsMap.forEach { debtMap ->
                        val personName = debtMap["personName"] as String
                        val amount = (debtMap["amount"] as Number).toDouble()
                        val borrowTime = parseDate(debtMap["borrowTime"])
                        val note = debtMap["note"] as? String

                        val isDuplicate = localDebts.any { local ->
                            local.personName == personName &&
                                    abs(local.amount - amount) < 0.01 &&
                                    local.borrowTime.time == borrowTime.time &&
                                    local.note == note
                        }

                        if (!isDuplicate) {
                            // 1. 映射 Account ID
                            val oldAccountId = (debtMap["accountId"] as? Number)?.toLong()
                            // 【修复】如果映射失败，使用 oldAccountId 或者默认值，避免记录丢失
                            val newAccountId = oldAccountId?.let { accountIdMap[it] } ?: oldAccountId ?: -1L

                            val oldInId = (debtMap["inAccountId"] as? Number)?.toLong()
                            val oldOutId = (debtMap["outAccountId"] as? Number)?.toLong()
                            val newInId = oldInId?.let { accountIdMap[it] }
                            val newOutId = oldOutId?.let { accountIdMap[it] }

                            val settleTimeRaw = debtMap["settleTime"]
                            val settleTime = if (settleTimeRaw != null) parseDate(settleTimeRaw) else null

                            val newRecord = com.swiftiecx.timeledger.data.DebtRecord(
                                id = 0L,
                                accountId = newAccountId,
                                personName = personName,
                                amount = amount,
                                note = note,
                                borrowTime = borrowTime,
                                settleTime = settleTime,
                                inAccountId = newInId,
                                outAccountId = newOutId
                            )
                            debtRecordDao.insert(newRecord)
                            addedCount++
                        }
                    }

                    val finalExpenses = expenseDao.getAllExpenses().first()
                    val finalAccounts = accountDao.getAllAccounts().first()
                    uploadData(uid, finalExpenses, finalAccounts)

                    Result.success(s(R.string.success_sync_merge_added, addedCount))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun uploadData(uid: String, expenses: List<Expense>, accounts: List<Account>) {
        val expenseCats = getMainCategories(CategoryType.EXPENSE)
        val incomeCats = getMainCategories(CategoryType.INCOME)

        val expenseCatsDto = expenseCats.map { main ->
            MainCategoryDto(
                title = main.title,
                iconName = IconMapper.getIconName(main.icon),
                colorInt = main.color.toArgb(),
                subs = main.subCategories.map { SubCategoryDto(it.key, IconMapper.getIconName(it.icon)) }
            )
        }
        val incomeCatsDto = incomeCats.map { main ->
            MainCategoryDto(
                title = main.title,
                iconName = IconMapper.getIconName(main.icon),
                colorInt = main.color.toArgb(),
                subs = main.subCategories.map { SubCategoryDto(it.key, IconMapper.getIconName(it.icon)) }
            )
        }

        val expenseCatsJson = gson.toJson(expenseCatsDto)
        val incomeCatsJson = gson.toJson(incomeCatsDto)

        // ✅ [新增] 获取所有债务记录
        val debtRecords = debtRecordDao.getAllDebtRecords().first()

        val backupData = hashMapOf(
            "version" to 1,
            "timestamp" to System.currentTimeMillis(),
            "device" to Build.MODEL,
            "expenses" to expenses,
            "accounts" to accounts,
            "categories_expense_json" to expenseCatsJson,
            "categories_income_json" to incomeCatsJson,
            "debt_records" to debtRecords
        )

        firestore.collection("users").document(uid)
            .collection("backups").document("latest")
            .set(backupData)
            .await()
    }

    private suspend fun restoreDataLocally(data: Map<String, Any>) {
        expenseDao.deleteAll()
        accountDao.deleteAll()
        budgetDao.deleteAll()
        // ✅ [新增] 先清空本地债务
        debtRecordDao.deleteAll()

        val expJson = data["categories_expense_json"] as? String
        val incJson = data["categories_income_json"] as? String

        if (expJson != null) {
            prefs.edit().putString("main_cats_expense", expJson).apply()
        }
        if (incJson != null) {
            prefs.edit().putString("main_cats_income", incJson).apply()
        }

        val accountsList = data["accounts"] as List<Map<String, Any>>
        val accountIdMap = mutableMapOf<Long, Long>()

        accountsList.forEach { map ->
            val account = Account(
                name = map["name"] as String,
                type = normalizeAccountTypeValue(map["type"] as String),
                initialBalance = (map["initialBalance"] as Number).toDouble(),
                currency = map["currency"] as? String ?: "CNY",
                iconName = map["iconName"] as? String ?: "Wallet",
                isLiability = map["isLiability"] as? Boolean ?: false
            )
            val newId = accountDao.insert(account)
            accountIdMap[(map["id"] as Number).toLong()] = newId
        }

        val expensesList = data["expenses"] as List<Map<String, Any>>
        expensesList.forEach { map ->
            val oldAccountId = (map["accountId"] as Number).toLong()
            val newAccountId = accountIdMap[oldAccountId] ?: return@forEach

            val date = parseDate(map["date"])

            val expense = Expense(
                accountId = newAccountId,
                category = map["category"] as String,
                amount = (map["amount"] as Number).toDouble(),
                date = date,
                remark = map["remark"] as? String
            )
            expenseDao.insertExpense(expense)
        }

        // ✅ [新增] 恢复债务记录 (修正版)
        val debtList = data["debt_records"] as? List<Map<String, Any>>
        debtList?.forEach { map ->
            // 1. 映射 accountId
            val oldAccountId = (map["accountId"] as? Number)?.toLong()
            // 【修复】如果映射失败，使用 oldAccountId 或者默认值，避免记录丢失
            val newAccountId = oldAccountId?.let { accountIdMap[it] } ?: oldAccountId ?: -1L

            // 2. 映射关联账户
            val oldInId = (map["inAccountId"] as? Number)?.toLong()
            val oldOutId = (map["outAccountId"] as? Number)?.toLong()

            val newInId = oldInId?.let { accountIdMap[it] }
            val newOutId = oldOutId?.let { accountIdMap[it] }

            // 3. 日期
            val borrowTime = parseDate(map["borrowTime"])
            val settleTimeRaw = map["settleTime"]
            val settleTime = if (settleTimeRaw != null) parseDate(settleTimeRaw) else null

            // 4. 构建对象 (匹配最新的 DebtRecord 结构)
            val record = com.swiftiecx.timeledger.data.DebtRecord(
                id = 0L, // 必填，0L 表示新增
                accountId = newAccountId,
                personName = map["personName"] as String,
                amount = (map["amount"] as Number).toDouble(),
                note = map["note"] as? String,
                borrowTime = borrowTime,
                settleTime = settleTime,
                inAccountId = newInId,
                outAccountId = newOutId
            )

            debtRecordDao.insert(record)
        }
    }
    // =================================================
    // [新增] 多语言切换支持
    // =================================================

    // 1. 定义映射表：数据库里的 Key -> strings.xml 里的资源ID
    private val mainCategoryMap = mapOf(
        "cat_food" to R.string.cat_food,
        "cat_shopping" to R.string.cat_shopping,
        "cat_transport" to R.string.cat_transport,
        "cat_home" to R.string.cat_home,
        "cat_entertainment" to R.string.cat_entertainment,
        "cat_medical_edu" to R.string.cat_medical_edu,
        "cat_financial" to R.string.cat_financial,
        "cat_income_job" to R.string.cat_income_job,
        "cat_income_other" to R.string.cat_income_other
    )

    private val subCategoryMap = mapOf(
        "sub_food" to R.string.sub_food,
        "sub_shopping" to R.string.sub_shopping,
        "sub_traffic" to R.string.sub_traffic,
        "sub_housing" to R.string.sub_housing,
        "sub_entertainment" to R.string.sub_entertainment,
        "sub_medical" to R.string.sub_medical,
        "sub_education" to R.string.sub_education,
        "sub_finance" to R.string.sub_finance,
        "sub_salary" to R.string.sub_salary,
        "sub_bonus" to R.string.sub_bonus,
        "sub_part_time" to R.string.sub_part_time,
        "sub_other" to R.string.sub_other,
        "sub_clothes" to R.string.sub_clothes,
        "sub_daily" to R.string.sub_daily,
        "sub_electronics" to R.string.sub_electronics,
        "sub_service" to R.string.sub_service,
        "sub_beauty" to R.string.sub_beauty,
        "sub_travel" to R.string.sub_travel,
        "sub_alcohol" to R.string.sub_alcohol,
        "sub_cigarette" to R.string.sub_cigarette,
        "sub_social" to R.string.sub_social,
        "sub_movie" to R.string.sub_movie,
        "sub_drama" to R.string.sub_drama,
        "sub_sports" to R.string.sub_sports,
        "sub_book" to R.string.sub_book,
        "sub_game" to R.string.sub_entertainment, // 假设
        "sub_pet" to R.string.sub_pets,
        "sub_car" to R.string.sub_car,
        "sub_repair" to R.string.sub_repair,
        "sub_gift" to R.string.sub_gift,
        "sub_red_packet" to R.string.sub_red_packet,
        "sub_donate" to R.string.sub_donate,
        "sub_gift_money" to R.string.sub_gift_money,
        "sub_reimbursement" to R.string.sub_reimbursement,
        "sub_second_hand" to R.string.sub_second_hand
    )

    suspend fun forceUpdateCategoryNames(context: Context) {
        // 1. 更新主分类
        mainCategoryMap.forEach { (key, resId) ->
            val newTitle = context.getString(resId)
            categoryDao.updateMainCategoryName(key, newTitle)
        }

        // 2. 更新子分类
        subCategoryMap.forEach { (key, resId) ->
            val newTitle = context.getString(resId)
            categoryDao.updateSubCategoryName(key, newTitle)
        }
    }
    // ===========================
    //  Debt Record Methods (新增)
    // ===========================

    fun getDebtRecords(accountId: Long): Flow<List<com.swiftiecx.timeledger.data.DebtRecord>> {
        return debtRecordDao.observeByAccount(accountId)
    }

    suspend fun insertDebtRecord(record: com.swiftiecx.timeledger.data.DebtRecord) = debtRecordDao.insert(record)

    suspend fun updateDebtRecord(record: com.swiftiecx.timeledger.data.DebtRecord) = debtRecordDao.update(record)

    suspend fun deleteDebtRecord(record: com.swiftiecx.timeledger.data.DebtRecord) = debtRecordDao.delete(record)

    fun getAllDebtRecords(): Flow<List<com.swiftiecx.timeledger.data.DebtRecord>> = debtRecordDao.getAllDebtRecords()

    suspend fun deleteAllDebtRecords() {
        debtRecordDao.deleteAll()
    }
}