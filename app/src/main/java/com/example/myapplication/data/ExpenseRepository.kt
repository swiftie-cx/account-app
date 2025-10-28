package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao
) {

    // Expense related methods
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun insert(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun createTransfer(expenseOut: Expense, expenseIn: Expense) {
        expenseDao.insertTransfer(expenseOut, expenseIn)
    }

    // (新) 添加删除
    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    // (新) 添加更新
    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    // Budget related methods
    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(year, month)
    }

    suspend fun upsertBudget(budget: Budget) {
        budgetDao.upsertBudget(budget)
    }

    suspend fun upsertBudgets(budgets: List<Budget>) {
        budgetDao.upsertBudgets(budgets)
    }

    suspend fun getMostRecentBudget(): Budget? {
        return budgetDao.getMostRecentBudget()
    }

    // Account related methods
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun insertAccount(account: Account) {
        accountDao.insert(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.update(account)
    }

    suspend fun deleteAccount(account: Account) {
        accountDao.delete(account)
    }
}