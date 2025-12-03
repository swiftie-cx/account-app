package com.example.myapplication.ui.navigation

object Routes {
    // 交易相关
    const val ADD_TRANSACTION = "add_transaction"
    const val TRANSACTION_DETAIL = "transaction_detail/{expenseId}"
    fun transactionDetailRoute(expenseId: Long) = "transaction_detail/$expenseId"

    // 预算相关
    const val BUDGET_SETTINGS = "budget_settings/{year}/{month}"
    fun budgetSettingsRoute(year: Int, month: Int) = "budget_settings/$year/$month"

    // 账户管理相关 (修复报错的关键)
    const val ACCOUNT_MANAGEMENT = "account_management"
    const val ADD_ACCOUNT = "add_account"

    // 其他功能页面
    const val SEARCH = "search"
    const val CALENDAR = "calendar"
    const val DAILY_DETAILS = "daily_details/{dateMillis}"
    fun dailyDetailsRoute(dateMillis: Long) = "daily_details/$dateMillis"

    // 设置相关
    const val SETTINGS = "settings"
    const val CURRENCY_SELECTION = "currency_selection"
    const val CATEGORY_SETTINGS = "category_settings"
    const val ADD_CATEGORY = "add_category"
}