package com.example.myapplication.data

import android.content.Context
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

// 用于序列化的简单数据类 (因为 ImageVector 不能直接序列化)
data class CategoryDto(val title: String, val iconName: String)

// 用于序列化嵌套结构的 DTO
data class SubCategoryDto(val title: String, val iconName: String)
data class MainCategoryDto(val title: String, val iconName: String, val colorInt: Int, val subs: List<SubCategoryDto>)

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

    // --- 【新增】首次启动标记 ---
    fun isFirstLaunch(): Boolean = prefs.getBoolean("is_first_launch", true)

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean("is_first_launch", false).apply()
    }

    // ===========================
    //  MainCategory Persistence (大类结构持久化)
    // ===========================

    fun saveMainCategories(categories: List<MainCategory>, type: CategoryType) {
        // 1. 将 UI 的 MainCategory 对象转换为可序列化的 DTO (包含子类)
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
        // 2. 转 JSON
        val json = gson.toJson(dtoList)
        // 3. 存入 Prefs
        val key = if (type == CategoryType.EXPENSE) "main_cats_expense" else "main_cats_income"
        prefs.edit().putString(key, json).apply()
    }

    fun getMainCategories(type: CategoryType): List<MainCategory> {
        val key = if (type == CategoryType.EXPENSE) "main_cats_expense" else "main_cats_income"
        val json = prefs.getString(key, null)

        return if (json != null) {
            try {
                // 1. 读取 JSON
                val itemType = object : TypeToken<List<MainCategoryDto>>() {}.type
                val dtoList: List<MainCategoryDto> = gson.fromJson(json, itemType) ?: emptyList()

                // 2. 转回 UI 对象
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
                // 出错回退到默认
                if (type == CategoryType.EXPENSE) expenseMainCategories else incomeMainCategories
            }
        } else {
            // 3. 第一次运行，返回默认静态列表
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

    // --- 用户认证逻辑 (模拟) ---
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail = _userEmail.asStateFlow()

    fun register(email: String, password: String) {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_password", password)
            .putBoolean("is_logged_in", true)
            .apply()
        _userEmail.value = email
        _isLoggedIn.value = true
    }

    fun login(email: String, password: String): Boolean {
        val savedEmail = prefs.getString("user_email", "")
        val savedPassword = prefs.getString("user_password", "")
        if (savedEmail == email && savedPassword == password) {
            prefs.edit().putBoolean("is_logged_in", true).apply()
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    fun deleteUserAccount() {
        prefs.edit()
            .remove("user_email")
            .remove("user_password")
            .remove("is_logged_in")
            .apply()
        _userEmail.value = ""
        _isLoggedIn.value = false
    }

    fun isEmailRegistered(email: String): Boolean {
        val savedEmail = prefs.getString("user_email", "")
        return savedEmail == email
    }

    fun saveUserPassword(password: String) {
        prefs.edit().putString("user_password", password).apply()
    }

    fun verifyUserPassword(password: String): Boolean {
        val saved = prefs.getString("user_password", "")
        return saved == password
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
        prefs.edit().clear().apply()
        _defaultAccountId.value = -1L
        _accountOrder.value = emptyList()
        _userEmail.value = ""
        _isLoggedIn.value = false
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
        // 注意：这里生成的账单，date 用的是 rule.nextExecutionDate (比如今天 11:00)
        // 而不是 Date() (现在时间 06:00)。这样就保证了时间排序的正确性。
        if (rule.type == 2 && rule.targetAccountId != null) {
            val expenseOut = Expense(
                accountId = rule.accountId,
                category = "转账 (转出)",
                amount = -abs(rule.amount),
                date = rule.nextExecutionDate,
                remark = rule.remark ?: "周期转账"
            )
            val expenseIn = Expense(
                accountId = rule.targetAccountId,
                category = "转账 (转入)",
                amount = abs(rule.amount),
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
                // 【关键修改】将 PeriodicTransaction 的 excludeFromBudget 传递给生成的 Expense
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
}