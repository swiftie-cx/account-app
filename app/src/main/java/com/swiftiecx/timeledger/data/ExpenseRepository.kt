package com.swiftiecx.timeledger.data

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao,
    private val periodicDao: PeriodicTransactionDao,
    private val context: Context // 修改：添加 private val 使其成为成员变量，供后续 CategoryData 使用
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
                    SubCategoryDto(sub.title, IconMapper.getIconName(sub.icon))
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
                            Category(subDto.title, IconMapper.getIcon(subDto.iconName))
                        }
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 解析失败，回退到动态默认值
                getDefaultCategories(type)
            }
        } else {
            // 无缓存，使用动态默认值 (跟随系统语言)
            getDefaultCategories(type)
        }
    }

    // 【新增】辅助方法：获取默认的多语言分类
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
                // 解析失败，回退到默认扁平化列表
                getDefaultCategories(type).flatMap { it.subCategories }
            }
        } else {
            // 无缓存，回退到默认扁平化列表
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
                is FirebaseAuthUserCollisionException -> "该邮箱已被注册"
                is FirebaseAuthInvalidCredentialsException -> "邮箱格式不正确"
                is FirebaseNetworkException -> "网络连接失败，请检查网络"
                else -> "注册失败: ${e.message}"
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
                is FirebaseAuthInvalidUserException -> "该账号不存在"
                is FirebaseAuthInvalidCredentialsException -> "邮箱或密码错误"
                is FirebaseNetworkException -> "网络连接失败，请检查网络"
                else -> "登录失败: ${e.message}"
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
                is FirebaseAuthInvalidUserException -> "该邮箱未注册"
                is FirebaseAuthInvalidCredentialsException -> "邮箱格式不正确"
                is FirebaseNetworkException -> "网络连接失败"
                else -> "发送失败: ${e.message}"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun changePassword(oldPass: String, newPass: String): Result<Boolean> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null || user.email == null) {
                return Result.failure(Exception("用户未登录"))
            }
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
            user.reauthenticate(credential).await()
            user.updatePassword(newPass).await()
            Result.success(true)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthInvalidCredentialsException -> "旧密码不正确"
                is FirebaseAuthRecentLoginRequiredException -> "登录已过期，请重新登录后再试"
                is FirebaseNetworkException -> "网络连接失败"
                else -> {
                    if (e.message?.contains("weak-password") == true) "新密码强度太低"
                    else "修改失败: ${e.message}"
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
            val msg = when(e) {
                is FirebaseAuthRecentLoginRequiredException -> "安全验证过期，请重新登录后注销"
                is FirebaseNetworkException -> "网络连接失败"
                else -> "注销失败: ${e.message}"
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

    suspend fun insertAccount(account: Account) = accountDao.insert(account)
    suspend fun updateAccount(account: Account) = accountDao.update(account)
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

        val editor = prefs.edit()
        editor.remove("default_account_id")
        editor.remove("account_order")
        editor.remove("privacy_type")
        editor.remove("privacy_pin")
        editor.remove("privacy_pattern")
        editor.remove("privacy_biometric")

        // 同时清空分类
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
                category = "转账 (转出)",
                amount = finalOut,
                date = rule.nextExecutionDate,
                remark = rule.remark ?: "周期转账"
            )
            val expenseIn = Expense(
                accountId = rule.targetAccountId,
                category = "转账 (转入)",
                amount = finalIn,
                date = rule.nextExecutionDate,
                remark = rule.remark ?: "周期转账"
            )
            expenseDao.insertTransfer(expenseOut, expenseIn)
        } else {
            val finalAmount = if (rule.type == 0) -abs(rule.amount) else abs(rule.amount)
            val expense = Expense(
                category = rule.category,
                amount = finalAmount,
                date = rule.nextExecutionDate,
                accountId = rule.accountId,
                remark = rule.remark ?: "周期自动记账",
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

    // 辅助方法：智能解析日期
    private fun parseDate(obj: Any?): Date {
        return when (obj) {
            is Timestamp -> obj.toDate()
            is Long -> Date(obj)
            else -> Date()
        }
    }

    // 1. 检查云端状态
    suspend fun checkCloudStatus(): Result<SyncCheckResult> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("未登录"))
        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("backups").document("latest")
                .get().await()

            val localExpenseCount = expenseDao.getAllExpenses().first().size
            val localAccountCount = accountDao.getAllAccounts().first().size
            val localCount = localExpenseCount + localAccountCount

            Result.success(SyncCheckResult(
                hasCloudData = doc.exists(),
                hasLocalData = localCount > 0,
                cloudTimestamp = doc.getLong("timestamp") ?: 0L
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. 执行同步
    suspend fun executeSync(strategy: SyncStrategy): Result<String> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("未登录"))

        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("backups").document("latest")
                .get().await()

            val localExpenses = expenseDao.getAllExpenses().first()
            val localAccounts = accountDao.getAllAccounts().first()

            when (strategy) {
                SyncStrategy.OVERWRITE_CLOUD -> {
                    uploadData(uid, localExpenses, localAccounts)
                    Result.success("已成功备份到云端")
                }

                SyncStrategy.OVERWRITE_LOCAL -> {
                    if (doc.exists()) {
                        val data = doc.data ?: return Result.failure(Exception("云端数据为空"))
                        restoreDataLocally(data)
                        Result.success("已成功从云端恢复")
                    } else {
                        Result.failure(Exception("云端无数据"))
                    }
                }

                SyncStrategy.MERGE -> {
                    if (!doc.exists()) {
                        uploadData(uid, localExpenses, localAccounts)
                        return Result.success("云端为空，已自动上传本地数据")
                    }

                    val cloudData = doc.data!!
                    val cloudExpensesMap = cloudData["expenses"] as? List<Map<String, Any>> ?: emptyList()
                    val cloudAccountsMap = cloudData["accounts"] as? List<Map<String, Any>> ?: emptyList()

                    val accountIdMap = mutableMapOf<Long, Long>()

                    cloudAccountsMap.forEach { accMap ->
                        val name = accMap["name"] as String
                        val type = accMap["type"] as String
                        val currency = accMap["currency"] as? String ?: "CNY"

                        val existingLocalAccount = localAccounts.find { it.name == name && it.type == type }

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
                        val category = expMap["category"] as String
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

                    val finalExpenses = expenseDao.getAllExpenses().first()
                    val finalAccounts = accountDao.getAllAccounts().first()
                    uploadData(uid, finalExpenses, finalAccounts)

                    Result.success("同步完成，新增了 ${addedCount} 条记录")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun uploadData(uid: String, expenses: List<Expense>, accounts: List<Account>) {

        // 【新增】获取当前所有的分类设置
        val expenseCats = getMainCategories(CategoryType.EXPENSE)
        val incomeCats = getMainCategories(CategoryType.INCOME)

        // 转换成 DTO 结构，方便 JSON 序列化 (复用现有的 DTO 逻辑)
        val expenseCatsDto = expenseCats.map { main ->
            MainCategoryDto(
                title = main.title,
                iconName = IconMapper.getIconName(main.icon),
                colorInt = main.color.toArgb(),
                subs = main.subCategories.map { SubCategoryDto(it.title, IconMapper.getIconName(it.icon)) }
            )
        }
        val incomeCatsDto = incomeCats.map { main ->
            MainCategoryDto(
                title = main.title,
                iconName = IconMapper.getIconName(main.icon),
                colorInt = main.color.toArgb(),
                subs = main.subCategories.map { SubCategoryDto(it.title, IconMapper.getIconName(it.icon)) }
            )
        }

        // 转为 JSON 字符串，确保存储格式稳定
        val expenseCatsJson = gson.toJson(expenseCatsDto)
        val incomeCatsJson = gson.toJson(incomeCatsDto)

        val backupData = hashMapOf(
            "version" to 1,
            "timestamp" to System.currentTimeMillis(),
            "device" to Build.MODEL,
            "expenses" to expenses,
            "accounts" to accounts,
            // 【新增】把分类配置也传上去
            "categories_expense_json" to expenseCatsJson,
            "categories_income_json" to incomeCatsJson
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

        // 【新增】恢复分类配置
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
                type = map["type"] as String,
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
    }
}