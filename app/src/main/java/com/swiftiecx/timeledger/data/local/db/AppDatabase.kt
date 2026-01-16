package com.swiftiecx.timeledger.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.local.dao.AccountDao
import com.swiftiecx.timeledger.data.local.entity.Budget
import com.swiftiecx.timeledger.data.local.dao.BudgetDao
import com.swiftiecx.timeledger.data.local.dao.CategoryDao
import com.swiftiecx.timeledger.data.local.entity.DebtRecord
import com.swiftiecx.timeledger.data.local.dao.DebtRecordDao
import com.swiftiecx.timeledger.data.local.entity.Expense
import com.swiftiecx.timeledger.data.local.dao.ExpenseDao
import com.swiftiecx.timeledger.data.local.entity.MainCategory
import com.swiftiecx.timeledger.data.local.entity.PeriodicTransaction
import com.swiftiecx.timeledger.data.local.dao.PeriodicTransactionDao
import com.swiftiecx.timeledger.data.local.entity.SubCategory

@Database(
    entities = [
        Expense::class,
        Budget::class,
        Account::class,
        PeriodicTransaction::class,
        MainCategory::class,
        SubCategory::class,
        DebtRecord::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun periodicDao(): PeriodicTransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun debtRecordDao(): DebtRecordDao

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
                    .fallbackToDestructiveMigration() // ← 加这一行
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * v1 -> v2
         * - accounts 新增：category/creditLimit/debtType
         * - 旧字段 isLiability=true 的账户默认映射为 CREDIT，否则 FUNDS
         */
        private val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE accounts ADD COLUMN category TEXT NOT NULL DEFAULT 'FUNDS'")
            db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimit REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE accounts ADD COLUMN debtType TEXT")

            db.execSQL("UPDATE accounts SET category = 'CREDIT' WHERE isLiability = 1")
            db.execSQL("UPDATE accounts SET category = 'FUNDS'  WHERE isLiability = 0")
        }
    }
}
