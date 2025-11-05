package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
// (不再需要 Migration)

// (***) (修改) 版本设为 1 (***)
@Database(entities = [Expense::class, Budget::class, Account::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // (***) (修改) 已删除所有 MIGRATION_... 代码 (***)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    // (***) (修改) 已删除 .addMigrations() (***)

                    // 我们保留这个，以便您将来修改（比如添加 v2）时，
                    // 它会自动帮您重构（删除旧 v1，创建新 v2）
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // Avoid if possible
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}