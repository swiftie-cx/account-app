package com.swiftiecx.timeledger.data.repository.datasource

import android.content.Context
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.local.dao.AccountDao
import com.swiftiecx.timeledger.data.local.dao.BudgetDao
import com.swiftiecx.timeledger.data.local.dao.CategoryDao
import com.swiftiecx.timeledger.data.local.dao.DebtRecordDao
import com.swiftiecx.timeledger.data.local.dao.ExpenseDao
import com.swiftiecx.timeledger.data.local.dao.PeriodicTransactionDao
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.local.entity.Budget
import com.swiftiecx.timeledger.data.local.entity.DebtRecord
import com.swiftiecx.timeledger.data.local.entity.Expense
import com.swiftiecx.timeledger.data.local.entity.PeriodicTransaction
import com.swiftiecx.timeledger.data.local.entity.RecordType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.abs

/**
 * 本地数据源（Room + 纯本地逻辑）
 *
 * 职责：
 * - 所有 DAO CRUD
 * - 周期记账执行（只依赖本地 DB）
 * - 多语言切换时强制刷新分类名称（更新 DB 中 title）
 *
 * ⚠️ 不包含 Firebase / Firestore。
 */
class LocalDataSource(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao,
    private val periodicDao: PeriodicTransactionDao,
    private val categoryDao: CategoryDao,
    private val debtRecordDao: DebtRecordDao,
    private val context: Context,
    /**
     * 注意：不要在函数类型参数上使用 @StringRes，否则会报
     * "This annotation is not applicable to target 'type usage'"
     */
    private val getString: (Int, Array<out Any>) -> String,
    private val transferOutCategoryKey: () -> String,
    private val transferInCategoryKey: () -> String
) {

    // --- Expense Flow ---
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    // --- Expense CRUD ---
    suspend fun insert(expense: Expense) {
        val newExpense = expense.copy(recordType = RecordType.INCOME_EXPENSE)
        expenseDao.insertExpense(newExpense)
    }

    suspend fun createTransfer(expenseOut: Expense, expenseIn: Expense) {
        val uniqueTransferId = System.currentTimeMillis()

        val finalOut = expenseOut.copy(
            recordType = RecordType.TRANSFER,
            transferId = uniqueTransferId,
            relatedAccountId = expenseIn.accountId
        )

        val finalIn = expenseIn.copy(
            recordType = RecordType.TRANSFER,
            transferId = uniqueTransferId,
            relatedAccountId = expenseOut.accountId
        )

        expenseDao.insertTransfer(finalOut, finalIn)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
        if (expense.debtId != null) {
            debtRecordDao.deleteById(expense.debtId)
        }
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    // --- Budget ---
    fun getBudgetsForMonth(year: Int, month: Int) = budgetDao.getBudgetsForMonth(year, month)
    suspend fun upsertBudget(budget: Budget) = budgetDao.upsertBudget(budget)
    suspend fun upsertBudgets(budgets: List<Budget>) = budgetDao.upsertBudgets(budgets)
    suspend fun getMostRecentBudget() = budgetDao.getMostRecentBudget()

    // --- Account ---
    val allAccountsRaw: Flow<List<Account>> = accountDao.getAllAccounts()
    suspend fun insertAccount(account: Account) = accountDao.insert(account)
    suspend fun updateAccount(account: Account) = accountDao.update(account)
    suspend fun deleteAccount(account: Account) = accountDao.delete(account)

    // --- Periodic ---
    val allPeriodicTransactions: Flow<List<PeriodicTransaction>> = periodicDao.getAll()

    suspend fun insertPeriodic(transaction: PeriodicTransaction) = periodicDao.insert(transaction)
    suspend fun updatePeriodic(transaction: PeriodicTransaction) = periodicDao.update(transaction)
    suspend fun deletePeriodic(transaction: PeriodicTransaction) = periodicDao.delete(transaction)
    suspend fun getPeriodicById(id: Long) = periodicDao.getById(id)

    /**
     * 周期记账：与原实现一致
     * - 取今天 23:59:59.999 为阈值
     * - nextExecutionDate <= endOfToday 则执行
     */
    suspend fun checkAndExecutePeriodicTransactions() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.time

        val allRules = periodicDao.getAllSync()
        allRules.forEach { rule ->
            if (rule.nextExecutionDate.time <= endOfToday.time) {
                executeRule(rule)
            }
        }
    }

    private suspend fun executeRule(rule: PeriodicTransaction) {
        // endMode=1：按日期截止
        if (rule.endMode == 1 && rule.endDate != null && rule.nextExecutionDate.after(rule.endDate)) return
        // endMode=2：按次数截止
        if (rule.endMode == 2 && rule.endCount != null && rule.endCount <= 0) return

        // type=2：转账
        if (rule.type == 2 && rule.targetAccountId != null) {
            val amountVal = abs(rule.amount)
            val feeVal = abs(rule.fee)

            val finalOut: Double
            val finalIn: Double
            // transferMode=0：手续费从转入扣；=1：手续费从转出加
            if (rule.transferMode == 0) {
                finalOut = -amountVal
                finalIn = amountVal - feeVal
            } else {
                finalOut = -(amountVal + feeVal)
                finalIn = amountVal
            }

            val uniqueTransferId = System.currentTimeMillis()
            val remark = rule.remark ?: getString(R.string.periodic_remark_transfer_default, emptyArray())

            val outExpense = Expense(
                accountId = rule.accountId,
                category = transferOutCategoryKey(),
                amount = finalOut,
                date = rule.nextExecutionDate,
                remark = remark,
                recordType = RecordType.TRANSFER,
                transferId = uniqueTransferId,
                relatedAccountId = rule.targetAccountId
            )
            val inExpense = Expense(
                accountId = rule.targetAccountId,
                category = transferInCategoryKey(),
                amount = finalIn,
                date = rule.nextExecutionDate,
                remark = remark,
                recordType = RecordType.TRANSFER,
                transferId = uniqueTransferId,
                relatedAccountId = rule.accountId
            )
            expenseDao.insertTransfer(outExpense, inExpense)
        } else {
            // 普通收支：type=0 代表支出（负），否则收入（正）
            val finalAmount = if (rule.type == 0) -abs(rule.amount) else abs(rule.amount)
            val remark = rule.remark ?: getString(R.string.periodic_remark_auto_default, emptyArray())

            val expense = Expense(
                category = rule.category,
                amount = finalAmount,
                date = rule.nextExecutionDate,
                accountId = rule.accountId,
                remark = remark,
                excludeFromBudget = rule.excludeFromBudget,
                recordType = RecordType.INCOME_EXPENSE
            )
            expenseDao.insertExpense(expense)
        }

        // 下一次执行时间（频率：日/周/月/年）
        val cal = Calendar.getInstance()
        cal.time = rule.nextExecutionDate
        when (rule.frequency) {
            0 -> cal.add(Calendar.DAY_OF_YEAR, 1)
            1 -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            2 -> cal.add(Calendar.MONTH, 1)
            3 -> cal.add(Calendar.YEAR, 1)
        }

        val newEndCount =
            if (rule.endMode == 2 && rule.endCount != null) rule.endCount - 1 else rule.endCount

        periodicDao.update(
            rule.copy(
                nextExecutionDate = cal.time,
                endCount = newEndCount
            )
        )
    }

    // --- Debt ---
    fun getDebtRecords(accountId: Long): Flow<List<DebtRecord>> = debtRecordDao.observeByAccount(accountId)

    suspend fun saveDebtWithTransaction(debt: DebtRecord, expense: Expense) {
        val debtId = debtRecordDao.insert(debt)
        expenseDao.insertExpense(expense.copy(debtId = debtId))
    }

    suspend fun insertDebtRecord(record: DebtRecord) = debtRecordDao.insert(record)
    suspend fun updateDebtRecord(record: DebtRecord) = debtRecordDao.update(record)
    suspend fun deleteDebtRecord(record: DebtRecord) = debtRecordDao.delete(record)

    fun getAllDebtRecords(): Flow<List<DebtRecord>> = debtRecordDao.getAllDebtRecords()
    suspend fun deleteAllDebtRecords() = debtRecordDao.deleteAll()

    // --- 多语言切换：强制刷新 DB 中分类 title（与原实现一致） ---
    private val mainCategoryMap = mapOf(
        "cat_food" to R.string.cat_food,
        "cat_shopping" to R.string.cat_shopping,
        "cat_transport" to R.string.cat_transport,
        "cat_home" to R.string.cat_home,
        "cat_entertainment" to R.string.cat_entertainment,
        "cat_medical_edu" to R.string.cat_medical_edu,
        "cat_financial" to R.string.cat_financial,
        "cat_income_job" to R.string.cat_income_job,
        "cat_income_other" to R.string.cat_income_other
    )

    private val subCategoryMap = mapOf(
        "sub_food" to R.string.sub_food,
        "sub_shopping" to R.string.sub_shopping,
        "sub_traffic" to R.string.sub_traffic,
        "sub_housing" to R.string.sub_housing,
        "sub_entertainment" to R.string.sub_entertainment,
        "sub_medical" to R.string.sub_medical,
        "sub_child" to R.string.sub_child,
        "sub_education" to R.string.sub_education,
        "sub_finance" to R.string.sub_finance,
        "sub_salary" to R.string.sub_salary,
        "sub_bonus" to R.string.sub_bonus,
        "sub_part_time" to R.string.sub_part_time,
        "sub_other" to R.string.sub_other,
        "sub_clothes" to R.string.sub_clothes,
        "sub_daily" to R.string.sub_daily,
        "sub_electronics" to R.string.sub_electronics,
        "sub_service" to R.string.sub_service,
        "sub_beauty" to R.string.sub_beauty,
        "sub_travel" to R.string.sub_travel,
        "sub_alcohol" to R.string.sub_alcohol,
        "sub_cigarette" to R.string.sub_cigarette,
        "sub_social" to R.string.sub_social,
        "sub_movie" to R.string.sub_movie,
        "sub_drama" to R.string.sub_drama,
        "sub_sports" to R.string.sub_sports,
        "sub_book" to R.string.sub_book,
        "sub_game" to R.string.sub_entertainment,
        "sub_pet" to R.string.sub_pets,
        "sub_car" to R.string.sub_car,
        "sub_repair" to R.string.sub_repair,
        "sub_gift" to R.string.sub_gift,
        "sub_red_packet" to R.string.sub_red_packet,
        "sub_donate" to R.string.sub_donate,
        "sub_gift_money" to R.string.sub_gift_money,
        "sub_reimbursement" to R.string.sub_reimbursement,
        "sub_second_hand" to R.string.sub_second_hand
    )

    suspend fun forceUpdateCategoryNames() {
        mainCategoryMap.forEach { (key, resId) ->
            categoryDao.updateMainCategoryName(key, context.getString(resId))
        }
        subCategoryMap.forEach { (key, resId) ->
            categoryDao.updateSubCategoryName(key, context.getString(resId))
        }
    }
}
