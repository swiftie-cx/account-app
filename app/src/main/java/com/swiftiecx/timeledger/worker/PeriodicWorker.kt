package com.swiftiecx.timeledger.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.swiftiecx.timeledger.data.local.db.AppDatabase
import com.swiftiecx.timeledger.data.repository.ExpenseRepository

class PeriodicWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)

            // ✅ 修复点：构造 Repository 时传入补全的 Dao 参数
            val repository = ExpenseRepository(
                expenseDao = database.expenseDao(),
                budgetDao = database.budgetDao(),
                accountDao = database.accountDao(),
                periodicDao = database.periodicDao(),
                categoryDao = database.categoryDao(),
                debtRecordDao = database.debtRecordDao(), // [补上这一行]
                context = applicationContext
            )

            repository.checkAndExecutePeriodicTransactions()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}