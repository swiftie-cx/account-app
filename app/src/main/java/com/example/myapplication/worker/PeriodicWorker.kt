package com.example.myapplication.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository

class PeriodicWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)

            // 手动构造 Repository (因为 Worker 无法直接注入 ViewModel)
            val repository = ExpenseRepository(
                expenseDao = database.expenseDao(),
                budgetDao = database.budgetDao(),
                accountDao = database.accountDao(),
                periodicDao = database.periodicDao(),
                context = applicationContext
            )

            // 【关键】调用我们在 Repository 里写好的核心逻辑
            repository.checkAndExecutePeriodicTransactions()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // 如果失败，稍后重试
        }
    }
}