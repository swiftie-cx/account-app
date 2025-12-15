package com.example.myapplication.data

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.myapplication.ui.navigation.Category
import com.example.myapplication.ui.navigation.IconMapper
import com.example.myapplication.ui.navigation.MainCategory
import com.example.myapplication.ui.navigation.expenseCategories
import com.example.myapplication.ui.navigation.expenseMainCategories
import com.example.myapplication.ui.navigation.incomeCategories
import com.example.myapplication.ui.navigation.incomeMainCategories
import com.example.myapplication.ui.viewmodel.CategoryType
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore // 新增
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first // 新增
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

// 用于序列化的简单数据类
data class CategoryDto(val title: String, val iconName: String)

// 用于序列化嵌套结构的 DTO
data class SubCategoryDto(val title: String, val iconName: String)
data class MainCategoryDto(val title: String, val iconName: String, val colorInt: Int, val subs: List<SubCategoryDto>)

// 定义同步冲突的三种策略 (新增)
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
    context: Context
) {
    // --- 偏好设置 (SharedPreferences) ---
    private val prefs = context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Firebase ---
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance() // 新增 Firestore 实例

    // 【新增】初始化块：启用语言自动识别
    init {
        // 这行代码会让 Firebase 自动使用用户当前设备的系统语言
        // 来发送验证邮件、重置密码邮件等。
        firebaseAuth.useAppLanguage()
    }

    // --- 【新增】首次启动标记 ---
    fun isFirstLaunch(): Boolean = prefs.getBoolean("is_first_launch", true)

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean("is_first_launch", false).apply()
    }

    // ===========================
    //  MainCategory Persistence (大类结构持久化)
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
                if (type == CategoryType.EXPENSE) expenseMainCategories else incomeMainCategories
            }
        } else {
            if (type == CategoryType.EXPENSE) expenseMainCategories else incomeMainCategories
        }
    }

    // ===========================
    //  Legacy Category Persistence (旧版/扁平化 分类持久化 - 保留以兼容旧逻辑)
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
                if (type == CategoryType.EXPENSE) expenseCategories else incomeCategories
            }
        } else {
            if (type == CategoryType.EXPENSE) expenseCategories else incomeCategories
        }
    }

    // ===========================
    //  Firebase 用户认证逻辑 (含中文错误处理)
    // ===========================

    // 监听 Firebase 用户状态变化
    val isLoggedIn: Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        // 初始发送
        trySend(firebaseAuth.currentUser != null)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    // 获取当前用户邮箱
    val userEmail: Flow<String> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.email ?: "")
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        trySend(firebaseAuth.currentUser?.email ?: "")
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    // 注册
    suspend fun register(email: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            // 注册成功后，发送验证邮件
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

    // 登录
    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthInvalidUserException -> "该账号不存在"
                is FirebaseAuthInvalidCredentialsException -> "邮箱或密码错误" // 解决你截图中的报错
                is FirebaseNetworkException -> "网络连接失败，请检查网络"
                else -> "登录失败: ${e.message}"
            }
            Result.failure(Exception(msg))
        }
    }

    // 登出
    fun logout() {
        firebaseAuth.signOut()
    }

    // 发送重置密码邮件
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

    // 修改密码 (需要重新认证)
    suspend fun changePassword(oldPass: String, newPass: String): Result<Boolean> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null || user.email == null) {
                return Result.failure(Exception("用户未登录"))
            }

            // 1. 创建认证凭证
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)

            // 2. 重新认证 (Re-authenticate)
            user.reauthenticate(credential).await()

            // 3. 更新密码
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

    // 注销账号 (从 Firebase 删除)
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
        // 清除本地偏好
        val editor = prefs.edit()
        editor.remove("default_account_id")
        editor.remove("account_order")
        editor.remove("privacy_type")
        editor.remove("privacy_pin")
        editor.remove("privacy_pattern")
        editor.remove("privacy_biometric")
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
        // 获取"今天结束"的时间点 (23:59:59.999)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.time

        // 获取所有规则
        val allRules = periodicDao.getAllSync()

        allRules.forEach { rule ->
            // 如果 "下次执行时间" <= "今天结束"，说明它属于今天（或之前的漏单），立即执行
            if (rule.nextExecutionDate.time <= endOfToday.time) {
                executeRule(rule)
            }
        }
    }

    private suspend fun executeRule(rule: PeriodicTransaction) {
        // A. 检查是否已结束
        if (rule.endMode == 1 && rule.endDate != null && rule.nextExecutionDate.after(rule.endDate)) {
            return
        }
        if (rule.endMode == 2 && rule.endCount != null && rule.endCount <= 0) {
            return
        }

        // B. 生成真实账单
        if (rule.type == 2 && rule.targetAccountId != null) {
            val amountVal = abs(rule.amount)
            val feeVal = abs(rule.fee)

            val finalOut: Double
            val finalIn: Double

            if (rule.transferMode == 0) {
                // 模式 0: 转出固定 (含手续费)
                finalOut = -amountVal
                finalIn = amountVal - feeVal
            } else {
                // 模式 1: 转入固定 (额外手续费)
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
            // (支出/收入逻辑保持不变)
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

        // C. 计算下一次执行时间
        val calendar = Calendar.getInstance()
        calendar.time = rule.nextExecutionDate

        when (rule.frequency) {
            0 -> calendar.add(Calendar.DAY_OF_YEAR, 1) // 每天
            1 -> calendar.add(Calendar.WEEK_OF_YEAR, 1) // 每周
            2 -> calendar.add(Calendar.MONTH, 1)       // 每月
            3 -> calendar.add(Calendar.YEAR, 1)        // 每年
        }
        val newNextDate = calendar.time

        // D. 更新规则
        val newEndCount = if (rule.endMode == 2 && rule.endCount != null) rule.endCount - 1 else rule.endCount

        val updatedRule = rule.copy(
            nextExecutionDate = newNextDate,
            endCount = newEndCount
        )

        periodicDao.update(updatedRule)
    }

    // ==========================================
    // 【核心】智能云同步逻辑 (新增)
    // ==========================================

    data class SyncCheckResult(
        val hasCloudData: Boolean,
        val hasLocalData: Boolean,
        val cloudTimestamp: Long
    )

    // 1. 检查云端状态
    suspend fun checkCloudStatus(): Result<SyncCheckResult> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("未登录"))
        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("backups").document("latest")
                .get().await()

            // 使用 Flow.first() 获取当前快照
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
            // 获取云端数据
            val doc = firestore.collection("users").document(uid)
                .collection("backups").document("latest")
                .get().await()

            // 获取本地数据 (Snapshot)
            val localExpenses = expenseDao.getAllExpenses().first()
            val localAccounts = accountDao.getAllAccounts().first()

            when (strategy) {
                SyncStrategy.OVERWRITE_CLOUD -> {
                    // 覆盖云端：直接上传本地数据
                    uploadData(uid, localExpenses, localAccounts)
                    Result.success("已成功备份到云端")
                }

                SyncStrategy.OVERWRITE_LOCAL -> {
                    // 覆盖本地：清空本地 -> 写入云端数据
                    if (doc.exists()) {
                        val data = doc.data ?: return Result.failure(Exception("云端数据为空"))
                        restoreDataLocally(data)
                        Result.success("已成功从云端恢复")
                    } else {
                        Result.failure(Exception("云端无数据"))
                    }
                }

                SyncStrategy.MERGE -> {
                    // 智能合并：两边加起来，去重
                    if (!doc.exists()) {
                        uploadData(uid, localExpenses, localAccounts)
                        return Result.success("云端为空，已自动上传本地数据")
                    }

                    val cloudData = doc.data!!
                    val cloudExpensesMap = cloudData["expenses"] as? List<Map<String, Any>> ?: emptyList()
                    val cloudAccountsMap = cloudData["accounts"] as? List<Map<String, Any>> ?: emptyList()

                    // --- 合并账户 ---
                    val accountIdMap = mutableMapOf<Long, Long>() // Old(Cloud) -> New(Local)

                    cloudAccountsMap.forEach { accMap ->
                        val name = accMap["name"] as String
                        val type = accMap["type"] as String
                        val currency = accMap["currency"] as? String ?: "CNY"

                        // 查找本地是否有同名同类型账户
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
                            val newId = accountDao.insert(newAccount) // 使用 accountDao
                            accountIdMap[cloudId] = newId
                        }
                    }

                    // --- 合并账单 ---
                    var addedCount = 0

                    cloudExpensesMap.forEach { expMap ->
                        val amount = (expMap["amount"] as Number).toDouble()
                        val date = Date(expMap["date"] as Long)
                        val remark = expMap["remark"] as? String ?: ""
                        val category = expMap["category"] as String
                        val cloudAccountId = (expMap["accountId"] as Number).toLong()

                        // 映射 ID
                        val targetAccountId = accountIdMap[cloudAccountId] ?: return@forEach

                        // 简单指纹去重 (金额+时间+账户+备注)
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

                    // 合并后，重新上传一份全量数据，保证云端也是最新的
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

    // 辅助：上传数据到 Firestore
    private suspend fun uploadData(uid: String, expenses: List<Expense>, accounts: List<Account>) {
        val backupData = hashMapOf(
            "version" to 1,
            "timestamp" to System.currentTimeMillis(),
            "device" to Build.MODEL,
            "expenses" to expenses,
            "accounts" to accounts
        )
        firestore.collection("users").document(uid)
            .collection("backups").document("latest")
            .set(backupData)
            .await()
    }

    // 辅助：将 Map 数据恢复到本地
    private suspend fun restoreDataLocally(data: Map<String, Any>) {
        // 1. 清空本地
        expenseDao.deleteAll()
        accountDao.deleteAll()
        // 可选：Budget 和 Periodic 要不要也同步？目前逻辑只同步了账单和账户。

        // 2. 恢复账户
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

        // 3. 恢复账单
        val expensesList = data["expenses"] as List<Map<String, Any>>
        expensesList.forEach { map ->
            val oldAccountId = (map["accountId"] as Number).toLong()
            val newAccountId = accountIdMap[oldAccountId] ?: return@forEach

            val expense = Expense(
                accountId = newAccountId,
                category = map["category"] as String,
                amount = (map["amount"] as Number).toDouble(),
                date = Date(map["date"] as Long),
                remark = map["remark"] as? String
            )
            expenseDao.insertExpense(expense)
        }
    }
}