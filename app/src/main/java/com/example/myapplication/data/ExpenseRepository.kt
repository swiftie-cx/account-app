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
    context: Context // (关键) 构造函数接收 Context
) {
    // --- 偏好设置 (SharedPreferences) ---
    private val prefs = context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

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

    // --- Account methods (核心修改) ---

    // (修改) allAccounts 会结合数据库数据 + 排序设置，返回“已排序”的列表
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

    // (新增) 清除所有数据逻辑
    suspend fun clearAllData() {
        // 1. 清空数据库表
        expenseDao.deleteAll()
        budgetDao.deleteAll()
        accountDao.deleteAll()

        // 2. 清空偏好设置 (重置默认账户、排序等)
        prefs.edit().clear().apply()

        // 3. 重置内存中的状态
        _defaultAccountId.value = -1L
        _accountOrder.value = emptyList()
    }
    // 类型: "NONE", "PIN", "PATTERN"
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

    // 手势密码保存为字符串 "1,2,3,4"
    fun savePattern(pattern: List<Int>) {
        val patternStr = pattern.joinToString(",")
        prefs.edit().putString("privacy_pattern", patternStr).apply()
    }

    fun verifyPattern(inputPattern: List<Int>): Boolean {
        val savedStr = prefs.getString("privacy_pattern", "")
        val inputStr = inputPattern.joinToString(",")
        return savedStr == inputStr
    }

    // 指纹开关
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("privacy_biometric", false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("privacy_biometric", enabled).apply()
    }
}