package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Upsert
    suspend fun upsertBudgets(budgets: List<Budget>)

    @Upsert
    suspend fun upsertBudget(budget: Budget)

    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month")
    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>>

    // New query to find the most recent month that has any budget entries.
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC LIMIT 1")
    suspend fun getMostRecentBudget(): Budget?

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
