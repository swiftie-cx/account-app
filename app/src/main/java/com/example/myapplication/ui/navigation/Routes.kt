package com.example.myapplication.ui.navigation

import android.net.Uri

object Routes {
    // 交易相关
    const val ADD_TRANSACTION = "add_transaction"
    const val TRANSACTION_DETAIL = "transaction_detail/{expenseId}"
    fun transactionDetailRoute(expenseId: Long) = "transaction_detail/$expenseId"

    // 预算相关
    const val BUDGET_SETTINGS = "budget_settings/{year}/{month}"
    fun budgetSettingsRoute(year: Int, month: Int) = "budget_settings/$year/$month"

    // 账户管理相关
    const val ACCOUNT_MANAGEMENT = "account_management"
    const val ADD_ACCOUNT = "add_account?accountId={accountId}"
    fun addAccountRoute(accountId: Long? = null) = "add_account?accountId=${accountId ?: -1L}"

    // 其他功能页面
    const val CALENDAR = "calendar"
    const val DAILY_DETAILS = "daily_details/{dateMillis}"
    fun dailyDetailsRoute(dateMillis: Long) = "daily_details/$dateMillis"

    // 搜索
    const val SEARCH = "search?category={category}&startDate={startDate}&endDate={endDate}&type={type}"
    fun searchRoute(category: String? = null, startDate: Long? = null, endDate: Long? = null, type: Int = 0): String {
        val cat = if (category != null) Uri.encode(category) else ""
        val start = startDate ?: -1L
        val end = endDate ?: -1L
        return "search?category=$cat&startDate=$start&endDate=$end&type=$type"
    }

    // 设置相关
    const val SETTINGS = "settings"
    const val CURRENCY_SELECTION = "currency_selection"
    const val CATEGORY_SETTINGS = "category_settings"
    const val ADD_CATEGORY = "add_category"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val LOCK = "lock"
    const val THEME_SETTINGS = "theme_settings"

    // --- 用户信息相关 ---
    const val USER_INFO = "user_info"
    const val LOGIN = "login"          // 新增：登录
    const val REGISTER = "register"    // 新增：注册 (替代原来的 bind_email)
    const val CHANGE_PASSWORD = "change_password"
    const val FORGOT_PASSWORD = "forgot_password"

    // 周期记账相关
    const val PERIODIC_BOOKKEEPING = "periodic_bookkeeping"
    const val ADD_PERIODIC_TRANSACTION = "add_periodic_transaction?id={id}"
    fun addPeriodicTransactionRoute(id: Long? = null) = "add_periodic_transaction?id=${id ?: -1L}"
}