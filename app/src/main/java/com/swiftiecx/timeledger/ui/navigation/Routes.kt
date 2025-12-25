package com.swiftiecx.timeledger.ui.navigation

import android.net.Uri

object Routes {
    // === 欢迎与基础页面 ===
    const val WELCOME = "welcome"

    // === 交易记录相关 ===
    const val ADD_TRANSACTION = "add_transaction"
    fun addTransactionRoute(expenseId: Long? = null, dateMillis: Long? = null, type: Int = 0): String {
        val id = expenseId ?: -1L
        val date = dateMillis ?: -1L
        return "$ADD_TRANSACTION?expenseId=$id&dateMillis=$date&type=$type"
    }

    const val TRANSACTION_DETAIL = "transaction_detail/{expenseId}"
    fun transactionDetailRoute(expenseId: Long) = "transaction_detail/$expenseId"

    // === 预算相关 ===
    const val BUDGET_SETTINGS = "budget_settings/{year}/{month}"
    fun budgetSettingsRoute(year: Int, month: Int) = "budget_settings/$year/$month"

    // === 账户管理相关 ===
    const val ACCOUNT_MANAGEMENT = "account_management"
    const val ADD_ACCOUNT = "add_account"

    // 原有的添加账户逻辑
    fun addAccountRoute(
        accountId: Long? = null,
        category: String = "FUNDS",
        debtType: String? = null
    ): String {
        val id = accountId ?: -1L
        return if (debtType.isNullOrBlank()) {
            "$ADD_ACCOUNT?accountId=$id&category=$category"
        } else {
            "$ADD_ACCOUNT?accountId=$id&category=$category&debtType=$debtType"
        }
    }

    const val ACCOUNT_DETAIL = "account_detail/{accountId}"
    fun accountDetailRoute(accountId: Long) = "account_detail/$accountId"

    // === 债务管理相关 (新增/深度修改) ===
    const val DEBT_MANAGEMENT = "debt_management"
    const val DEBT_PERSON_DETAIL = "debt_person_detail/{personName}"
    const val SETTLE_DEBT = "settle_debt/{personName}/{isBorrow}" // 结算页：姓名 + 是否为还款

    // 借入/借出路由：路径参数 accountId + 可选查询参数 personName
    const val ADD_BORROW = "add_borrow/{accountId}?personName={personName}"
    const val ADD_LEND = "add_lend/{accountId}?personName={personName}"

    fun debtPersonDetailRoute(personName: String) = "debt_person_detail/$personName"

    fun settleDebtRoute(personName: String, isBorrow: Boolean) = "settle_debt/$personName/$isBorrow"

    /**
     * 生成借入页面路由。
     * 如果是普通的“新增借入”，不传 personName；
     * 如果是债务详情页里的“追加借入”，传入对应的姓名。
     */
    fun addBorrowRoute(accountId: Long, personName: String? = null): String {
        val base = "add_borrow/$accountId"
        return if (personName != null) "$base?personName=$personName" else base
    }

    /**
     * 生成借出页面路由
     */
    fun addLendRoute(accountId: Long, personName: String? = null): String {
        val base = "add_lend/$accountId"
        return if (personName != null) "$base?personName=$personName" else base
    }

    // === 日历与每日明细 ===
    const val CALENDAR = "calendar"
    const val DAILY_DETAILS = "daily_details/{dateMillis}"
    fun dailyDetailsRoute(dateMillis: Long) = "daily_details/$dateMillis"

    // === 搜索 ===
    const val SEARCH = "search?category={category}&startDate={startDate}&endDate={endDate}&type={type}"
    fun searchRoute(category: String? = null, startDate: Long? = null, endDate: Long? = null, type: Int = 0): String {
        val cat = if (category != null) Uri.encode(category) else ""
        val start = startDate ?: -1L
        val end = endDate ?: -1L
        return "search?category=$cat&startDate=$start&endDate=$end&type=$type"
    }

    // === 设置相关 ===
    const val SETTINGS = "settings"
    const val CURRENCY_SELECTION = "currency_selection"
    const val CATEGORY_SETTINGS = "category_settings"
    const val ADD_CATEGORY = "add_category"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val LOCK = "lock"
    const val THEME_SETTINGS = "theme_settings"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val SYNC = "sync_screen"

    // === 用户认证相关 ===
    const val USER_INFO = "user_info"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHANGE_PASSWORD = "change_password"
    const val FORGOT_PASSWORD = "forgot_password"

    // === 周期记账相关 ===
    const val PERIODIC_BOOKKEEPING = "periodic_bookkeeping"
    const val ADD_PERIODIC_TRANSACTION = "add_periodic_transaction?id={id}"
    fun addPeriodicTransactionRoute(id: Long? = null) = "add_periodic_transaction?id=${id ?: -1L}"

    // === 图表详情相关 ===
    const val CATEGORY_CHART_DETAIL = "category_chart_detail/{category}/{type}/{start}/{end}"
    fun categoryChartDetailRoute(category: String, type: Int, start: Long, end: Long): String {
        return "category_chart_detail/$category/$type/$start/$end"
    }
    const val EDIT_DEBT = "edit_debt/{recordId}"
    fun editDebtRoute(recordId: Long) = "edit_debt/$recordId"
}