package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// (修改) 增加 PeriodicTransaction，版本号升级为 2
@Database(entities = [Expense::class, Budget::class, Account::class, PeriodicTransaction::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    // (新增)
    abstract fun periodicDao(): PeriodicTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    // (关键) 允许破坏性迁移，这会清空旧数据！
                    // 如果你想保留数据，需要写 Migration 脚本。
                    // 鉴于目前是开发阶段，为了方便直接用 fallbackToDestructiveMigration
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}