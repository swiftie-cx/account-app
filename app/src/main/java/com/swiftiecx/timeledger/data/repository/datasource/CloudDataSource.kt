package com.swiftiecx.timeledger.data.repository.datasource

import android.os.Build
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.swiftiecx.timeledger.data.local.dao.AccountDao
import com.swiftiecx.timeledger.data.local.dao.BudgetDao
import com.swiftiecx.timeledger.data.local.dao.DebtRecordDao
import com.swiftiecx.timeledger.data.local.dao.ExpenseDao
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.local.entity.DebtRecord
import com.swiftiecx.timeledger.data.local.entity.Expense
import com.swiftiecx.timeledger.data.local.entity.RecordType
import com.swiftiecx.timeledger.ui.common.IconMapper
import com.swiftiecx.timeledger.ui.common.MainCategory
import com.swiftiecx.timeledger.data.model.CategoryType
import com.swiftiecx.timeledger.data.repository.MainCategoryDto
import com.swiftiecx.timeledger.data.repository.PrefsStore
import com.swiftiecx.timeledger.data.repository.RepoKeys
import com.swiftiecx.timeledger.data.repository.SubCategoryDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Firestore I/O 层（保持原实现）
 */
class CloudDataSource(
    private val firestore: FirebaseFirestore,
    private val gson: Gson,
    private val debtRecordDao: DebtRecordDao,
    private val expenseDao: ExpenseDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val normalizeAccountTypeValue: (String) -> String,
    private val getMainCategories: (CategoryType) -> List<MainCategory>
) {

    private fun parseDate(obj: Any?): Date {
        return when (obj) {
            is Timestamp -> obj.toDate()
            is Long -> Date(obj)
            else -> Date()
        }
    }

    suspend fun uploadData(uid: String, expenses: List<Expense>, accounts: List<Account>) {
        val expenseCats = getMainCategories(CategoryType.EXPENSE)
        val incomeCats = getMainCategories(CategoryType.INCOME)

        val expenseCatsDto = expenseCats.map { main ->
            MainCategoryDto(
                title = main.title,
                iconName = IconMapper.getIconName(main.icon),
                colorInt = main.color.toArgb(),
                subs = main.subCategories.map {
                    SubCategoryDto(
                        it.key,
                        IconMapper.getIconName(it.icon)
                    )
                }
            )
        }
        val incomeCatsDto = incomeCats.map { main ->
            MainCategoryDto(
                title = main.title,
                iconName = IconMapper.getIconName(main.icon),
                colorInt = main.color.toArgb(),
                subs = main.subCategories.map {
                    SubCategoryDto(
                        it.key,
                        IconMapper.getIconName(it.icon)
                    )
                }
            )
        }

        val expenseCatsJson = gson.toJson(expenseCatsDto)
        val incomeCatsJson = gson.toJson(incomeCatsDto)

        val debtRecords = debtRecordDao.getAllDebtRecords().first()

        val backupData = hashMapOf(
            RepoKeys.FIELD_VERSION to 1,
            RepoKeys.FIELD_TIMESTAMP to System.currentTimeMillis(),
            RepoKeys.FIELD_DEVICE to Build.MODEL,
            RepoKeys.FIELD_EXPENSES to expenses,
            RepoKeys.FIELD_ACCOUNTS to accounts,
            RepoKeys.FIELD_CATEGORIES_EXPENSE to expenseCatsJson,
            RepoKeys.FIELD_CATEGORIES_INCOME to incomeCatsJson,
            RepoKeys.FIELD_DEBT_RECORDS to debtRecords
        )

        firestore.collection(RepoKeys.COLLECTION_USERS).document(uid)
            .collection(RepoKeys.COLLECTION_BACKUPS).document(RepoKeys.DOCUMENT_LATEST)
            .set(backupData)
            .await()
    }

    suspend fun restoreDataLocally(data: Map<String, Any>, prefsStore: PrefsStore) {
        expenseDao.deleteAll()
        accountDao.deleteAll()
        budgetDao.deleteAll()
        debtRecordDao.deleteAll()

        val expJson = data[RepoKeys.FIELD_CATEGORIES_EXPENSE] as? String
        val incJson = data[RepoKeys.FIELD_CATEGORIES_INCOME] as? String
        if (expJson != null) prefsStore.saveJson(RepoKeys.KEY_MAIN_CATS_EXPENSE, expJson)
        if (incJson != null) prefsStore.saveJson(RepoKeys.KEY_MAIN_CATS_INCOME, incJson)

        val accountsList = data[RepoKeys.FIELD_ACCOUNTS] as List<Map<String, Any>>
        val accountIdMap = mutableMapOf<Long, Long>()

        accountsList.forEach { map ->
            val account = Account(
                name = map["name"] as String,
                type = normalizeAccountTypeValue(map["type"] as String),
                initialBalance = (map["initialBalance"] as Number).toDouble(),
                currency = map["currency"] as? String ?: "CNY",
                iconName = map["iconName"] as? String ?: "Wallet",
                isLiability = map["isLiability"] as? Boolean ?: false
            )
            val newId = accountDao.insert(account)
            accountIdMap[(map["id"] as Number).toLong()] = newId
        }

        val expensesList = data[RepoKeys.FIELD_EXPENSES] as List<Map<String, Any>>
        expensesList.forEach { map ->
            val oldAccountId = (map["accountId"] as Number).toLong()
            val newAccountId = accountIdMap[oldAccountId] ?: return@forEach

            val date = parseDate(map["date"])
            val recordType = (map["recordType"] as? Number)?.toInt() ?: RecordType.INCOME_EXPENSE
            val transferId = (map["transferId"] as? Number)?.toLong()

            val relatedAccountIdRaw = (map["relatedAccountId"] as? Number)?.toLong()
            val relatedAccountId = relatedAccountIdRaw?.let { accountIdMap[it] }

            val expense = Expense(
                accountId = newAccountId,
                category = map["category"] as String,
                amount = (map["amount"] as Number).toDouble(),
                date = date,
                remark = map["remark"] as? String,
                recordType = recordType,
                transferId = transferId,
                relatedAccountId = relatedAccountId
            )
            expenseDao.insertExpense(expense)
        }

        val debtList = data[RepoKeys.FIELD_DEBT_RECORDS] as? List<Map<String, Any>>
        debtList?.forEach { map ->
            val oldAccountId = (map["accountId"] as? Number)?.toLong()
            val newAccountId = oldAccountId?.let { accountIdMap[it] } ?: oldAccountId ?: -1L

            val oldInId = (map["inAccountId"] as? Number)?.toLong()
            val oldOutId = (map["outAccountId"] as? Number)?.toLong()
            val newInId = oldInId?.let { accountIdMap[it] }
            val newOutId = oldOutId?.let { accountIdMap[it] }

            val borrowTime = parseDate(map["borrowTime"])
            val settleTimeRaw = map["settleTime"]
            val settleTime = if (settleTimeRaw != null) parseDate(settleTimeRaw) else null

            val record = DebtRecord(
                id = 0L,
                accountId = newAccountId,
                personName = map["personName"] as String,
                amount = (map["amount"] as Number).toDouble(),
                note = map["note"] as? String,
                borrowTime = borrowTime,
                settleTime = settleTime,
                inAccountId = newInId,
                outAccountId = newOutId
            )
            debtRecordDao.insert(record)
        }
    }
}
