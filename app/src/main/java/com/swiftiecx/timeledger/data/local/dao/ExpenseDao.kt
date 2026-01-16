package com.swiftiecx.timeledger.data.local.dao

import androidx.room.Dao
import androidx.room.Delete // (新) Import
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update // (新) Import
import com.swiftiecx.timeledger.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Transaction
    suspend fun insertTransfer(expenseOut: Expense, expenseIn: Expense) {
        insertExpense(expenseOut)
        insertExpense(expenseIn)
    }

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    // (新) 添加删除
    @Delete
    suspend fun deleteExpense(expense: Expense)

    // (新) 添加更新
    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}