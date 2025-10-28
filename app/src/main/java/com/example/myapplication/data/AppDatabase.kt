package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// (修改) 版本升级到 7, 添加 @TypeConverters
@Database(entities = [Expense::class, Budget::class, Account::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class) // *** 重新添加 TypeConverters ***
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // --- Added missing migrate implementations ---
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add date column as INTEGER (for Long timestamp via Converter)
                database.execSQL("ALTER TABLE expenses ADD COLUMN date INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `amount` REAL NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_category_year_month` ON `budgets` (`category`, `year`, `month`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create accounts table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `initialBalance` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `isLiability` INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2. Insert default account
                database.execSQL("""
                    INSERT INTO `accounts` (name, type, initialBalance, currency, iconName, isLiability)
                    VALUES ('现金', '现金', 0.0, 'CNY', 'AccountBalanceWallet', 0)
                """.trimIndent())

                // 3. Rebuild expenses table with INTEGER types for accountId and date
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expenses_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` INTEGER NOT NULL, -- INTEGER for Long
                        `category` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER, -- INTEGER for Date via Converter
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_new_accountId ON expenses_new (accountId)")

                // 3.2 Copy data, setting accountId to 1
                database.execSQL("""
                    INSERT INTO `expenses_new` (id, accountId, category, amount, date)
                    SELECT id, 1, category, amount, date FROM `expenses`
                """.trimIndent())

                // 3.3 Drop old table
                database.execSQL("DROP TABLE `expenses`")

                // 3.4 Rename new table
                database.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
            }
        }

        // MIGRATION_5_6 - Add remark (unchanged)
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN remark TEXT")
            }
        }

        // MIGRATION_6_7 - Change ID types (rebuild table, ensure correct types)
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create new table with INTEGER for id, accountId, date
                database.execSQL("""
                    CREATE TABLE expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, -- Stays INTEGER for Long PK
                        accountId INTEGER NOT NULL, -- Stays INTEGER for Long FK
                        category TEXT NOT NULL,
                        amount REAL NOT NULL,
                        date INTEGER, -- Stays INTEGER for Date via Converter
                        remark TEXT,
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_new_accountId ON expenses_new (accountId)")

                // 2. Copy data
                database.execSQL("""
                    INSERT INTO expenses_new (id, accountId, category, amount, date, remark)
                    SELECT id, accountId, category, amount, date, remark FROM expenses
                """.trimIndent())

                // 3. Drop old table
                database.execSQL("DROP TABLE expenses")

                // 4. Rename new table
                database.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
            }
        }
        // --- End of Migrations ---

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    // Ensure all migrations are listed
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration() // Use with caution
                    .allowMainThreadQueries() // Avoid if possible
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}