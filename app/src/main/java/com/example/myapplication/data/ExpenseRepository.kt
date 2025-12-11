package com.example.myapplication.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

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

    // --- 用户认证逻辑 (模拟) ---

    // 1. 监听登录状态
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn = _isLoggedIn.asStateFlow()

    // 2. 监听当前用户邮箱 (仅用于显示)
    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail = _userEmail.asStateFlow()

    // 注册 (保存账号信息 + 自动登录)
    fun register(email: String, password: String) {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_password", password)
            .putBoolean("is_logged_in", true) // 注册后自动登录
            .apply()
        _userEmail.value = email
        _isLoggedIn.value = true
    }

    // 登录 (校验账号信息)
    fun login(email: String, password: String): Boolean {
        val savedEmail = prefs.getString("user_email", "")
        val savedPassword = prefs.getString("user_password", "")

        // 简单模拟：如果输入的账号密码和本地存的一致
        if (savedEmail == email && savedPassword == password) {
            prefs.edit().putBoolean("is_logged_in", true).apply()
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    // 退出登录 (只改状态，不删数据)
    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    // 注销账号 (删数据 + 退出)
    fun deleteUserAccount() {
        prefs.edit()
            .remove("user_email")
            .remove("user_password")
            .remove("is_logged_in")
            .apply()
        _userEmail.value = ""
        _isLoggedIn.value = false
    }

    // 检查邮箱是否被注册 (模拟：检查是否和本地存储的一样)
    fun isEmailRegistered(email: String): Boolean {
        val savedEmail = prefs.getString("user_email", "")
        return savedEmail == email
    }

    // 保存新密码 (修改密码/重置密码用)
    fun saveUserPassword(password: String) {
        prefs.edit().putString("user_password", password).apply()
    }

    fun verifyUserPassword(password: String): Boolean {
        val saved = prefs.getString("user_password", "")
        return saved == password
    }

    // 1. 默认账户 ID (StateFlow)
    private val _defaultAccountId = MutableStateFlow(prefs.getLong("default_account_id", -1L))
    val defaultAccountId = _defaultAccountId.asStateFlow()

    // 2. 账户排序 (存储 ID 列表)
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
                // 根据 order 列表中的 ID 顺序对 accounts 进行排序
                accounts.sortedBy { account ->
                    val index = order.indexOf(account.id)
                    if (index == -1) Int.MAX_VALUE else index
                }
            }
        }

    suspend fun insertAccount(account: Account) = accountDao.insert(account)
    suspend fun updateAccount(account: Account) = accountDao.update(account)
    suspend fun deleteAccount(account: Account) = accountDao.delete(account)

    // --- 新增功能方法 ---

    // 保存默认账户ID
    fun saveDefaultAccountId(id: Long) {
        prefs.edit().putLong("default_account_id", id).apply()
        _defaultAccountId.value = id
    }

    // 保存账户排序顺序
    fun saveAccountOrder(accounts: List<Account>) {
        val ids = accounts.map { it.id }
        val json = gson.toJson(ids)
        prefs.edit().putString("account_order", json).apply()
        _accountOrder.value = ids
    }

    // 清除所有数据逻辑
    suspend fun clearAllData() {
        // 1. 清空数据库表
        expenseDao.deleteAll()
        budgetDao.deleteAll()
        accountDao.deleteAll()

        // 2. 清空偏好设置
        prefs.edit().clear().apply()

        // 3. 重置内存中的状态
        _defaultAccountId.value = -1L
        _accountOrder.value = emptyList()
        _userEmail.value = ""
        _isLoggedIn.value = false
    }

    // --- 隐私设置 ---
    fun getPrivacyType(): String {
        return prefs.getString("privacy_type", "NONE") ?: "NONE"
    }

    fun savePrivacyType(type: String) {
        prefs.edit().putString("privacy_type", type).apply()
    }

    fun savePin(pin: String) {
        prefs.edit().putString("privacy_pin", pin).apply()
    }

    fun verifyPin(inputPin: String): Boolean {
        val savedPin = prefs.getString("privacy_pin", "")
        return savedPin == inputPin
    }

    fun savePattern(pattern: List<Int>) {
        val patternStr = pattern.joinToString(",")
        prefs.edit().putString("privacy_pattern", patternStr).apply()
    }

    fun verifyPattern(inputPattern: List<Int>): Boolean {
        val savedStr = prefs.getString("privacy_pattern", "")
        val inputStr = inputPattern.joinToString(",")
        return savedStr == inputStr
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("privacy_biometric", false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("privacy_biometric", enabled).apply()
    }

    // --- Periodic Transaction Methods (新增) ---
    val allPeriodicTransactions: Flow<List<PeriodicTransaction>> = periodicDao.getAll()
    suspend fun insertPeriodic(transaction: PeriodicTransaction) = periodicDao.insert(transaction)
    suspend fun updatePeriodic(transaction: PeriodicTransaction) = periodicDao.update(transaction)
    suspend fun deletePeriodic(transaction: PeriodicTransaction) = periodicDao.delete(transaction)
    suspend fun getPeriodicById(id: Long) = periodicDao.getById(id)
}