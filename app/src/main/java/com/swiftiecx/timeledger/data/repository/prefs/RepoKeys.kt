package com.swiftiecx.timeledger.data.repository

/**
 * Repository 层“数据合同”集中处：
 * - SharedPreferences 的 key
 * - Firestore collection / document / field
 * - 特殊业务 category key（转账等）
 *
 * ⚠️ 这些一旦写入 DB/云端/备份，就尽量不要改名（封测期定下来最好）。
 */
object RepoKeys {

    // -------------------------
    // SharedPreferences
    // -------------------------
    const val PREFS_NAME = "expense_prefs"

    const val KEY_FIRST_LAUNCH = "is_first_launch"
    const val KEY_DEFAULT_CURRENCY = "key_default_currency"

    const val KEY_DEFAULT_ACCOUNT_ID = "default_account_id"
    const val KEY_ACCOUNT_ORDER = "account_order"

    // 分类 JSON（原实现就是用这些字符串）
    const val KEY_MAIN_CATS_EXPENSE = "main_cats_expense"
    const val KEY_MAIN_CATS_INCOME = "main_cats_income"
    const val KEY_CATS_EXPENSE = "cats_expense"
    const val KEY_CATS_INCOME = "cats_income"

    // 隐私锁
    const val KEY_PRIVACY_TYPE = "privacy_type"
    const val KEY_PRIVACY_PIN = "privacy_pin"
    const val KEY_PRIVACY_PATTERN = "privacy_pattern"
    const val KEY_PRIVACY_BIOMETRIC = "privacy_biometric"

    // -------------------------
    // 特殊业务语义分类 key（程序逻辑用）
    // -------------------------
    const val CATEGORY_TRANSFER_OUT = "category_transfer_out"
    const val CATEGORY_TRANSFER_IN = "category_transfer_in"

    // -------------------------
    // Firestore 路径
    // -------------------------
    const val COLLECTION_USERS = "users"
    const val COLLECTION_BACKUPS = "backups"
    const val DOCUMENT_LATEST = "latest"

    // -------------------------
    // 云端备份字段
    // -------------------------
    const val FIELD_VERSION = "version"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_DEVICE = "device"

    const val FIELD_EXPENSES = "expenses"
    const val FIELD_ACCOUNTS = "accounts"
    const val FIELD_DEBT_RECORDS = "debt_records"

    const val FIELD_CATEGORIES_EXPENSE = "categories_expense_json"
    const val FIELD_CATEGORIES_INCOME = "categories_income_json"
}
