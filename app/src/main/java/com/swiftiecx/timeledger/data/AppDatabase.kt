package com.swiftiecx.timeledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// [修改] entities 中加入了 MainCategory 和 SubCategory
@Database(
    entities = [
        Expense::class,
        Budget::class,
        Account::class,
        PeriodicTransaction::class,
        MainCategory::class, // 👈 新增
        SubCategory::class   // 👈 新增
    ],
    version = 1, // 既然卸载重装，版本号设为 1 即可
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun periodicDao(): PeriodicTransactionDao

    // [必须] 注册 CategoryDao，否则 Repository 无法获取实例
    abstract fun categoryDao(): CategoryDao

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
                    .fallbackToDestructiveMigration()
                    // .allowMainThreadQueries() // 建议移除此行，主线程查库会导致卡顿(ANR)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}