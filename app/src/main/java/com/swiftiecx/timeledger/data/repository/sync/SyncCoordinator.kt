package com.swiftiecx.timeledger.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.local.dao.AccountDao
import com.swiftiecx.timeledger.data.local.dao.DebtRecordDao
import com.swiftiecx.timeledger.data.local.dao.ExpenseDao
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.local.entity.DebtRecord
import com.swiftiecx.timeledger.data.local.entity.Expense
import com.swiftiecx.timeledger.data.local.entity.RecordType
import com.swiftiecx.timeledger.data.repository.datasource.CloudDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

/**
 * 同步编排器：
 * - checkCloudStatus：判断云端是否存在 latest + 本地是否有数据
 * - executeSync：三种策略（覆盖云/覆盖本地/合并）
 *
 * ⚠️ 逻辑来源于原 ExpenseRepository.kt，不改变去重条件与流程。
 */
class SyncCoordinator(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefsStore: PrefsStore,
    private val expenseDao: ExpenseDao,
    private val accountDao: AccountDao,
    private val debtRecordDao: DebtRecordDao,
    private val cloud: CloudDataSource,
    private val normalizeAccountTypeValue: (String) -> String,
    private val getString: (Int, Array<out Any>) -> String
) {

    data class SyncCheckResult(
        val hasCloudData: Boolean,
        val hasLocalData: Boolean,
        val cloudTimestamp: Long
    )

    suspend fun checkCloudStatus(): Result<SyncCheckResult> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return Result.failure(Exception(getString(R.string.error_not_logged_in, emptyArray())))

        return try {
            val doc = firestore.collection(RepoKeys.COLLECTION_USERS).document(uid)
                .collection(RepoKeys.COLLECTION_BACKUPS).document(RepoKeys.DOCUMENT_LATEST)
                .get().await()

            val localExpenseCount = expenseDao.getAllExpenses().first().size
            val localAccountCount = accountDao.getAllAccounts().first().size
            val localCount = localExpenseCount + localAccountCount

            Result.success(
                SyncCheckResult(
                    hasCloudData = doc.exists(),
                    hasLocalData = localCount > 0,
                    cloudTimestamp = doc.getLong(RepoKeys.FIELD_TIMESTAMP) ?: 0L
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeSync(strategy: SyncStrategy): Result<String> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return Result.failure(Exception(getString(R.string.error_not_logged_in, emptyArray())))

        return try {
            val doc = firestore.collection(RepoKeys.COLLECTION_USERS).document(uid)
                .collection(RepoKeys.COLLECTION_BACKUPS).document(RepoKeys.DOCUMENT_LATEST)
                .get().await()

            val localExpenses = expenseDao.getAllExpenses().first()
            val localAccounts = accountDao.getAllAccounts().first()
            val localDebts = debtRecordDao.getAllDebtRecords().first()

            when (strategy) {
                SyncStrategy.OVERWRITE_CLOUD -> {
                    cloud.uploadData(uid, localExpenses, localAccounts)
                    Result.success(getString(R.string.success_backup_to_cloud, emptyArray()))
                }

                SyncStrategy.OVERWRITE_LOCAL -> {
                    if (!doc.exists()) {
                        Result.failure(Exception(getString(R.string.error_cloud_no_data, emptyArray())))
                    } else {
                        val data = doc.data ?: return Result.failure(Exception(getString(R.string.error_cloud_data_empty, emptyArray())))
                        cloud.restoreDataLocally(data, prefsStore)
                        Result.success(getString(R.string.success_restore_from_cloud, emptyArray()))
                    }
                }

                SyncStrategy.MERGE -> {
                    if (!doc.exists()) {
                        cloud.uploadData(uid, localExpenses, localAccounts)
                        return Result.success(getString(R.string.success_cloud_empty_uploaded, emptyArray()))
                    }

                    val cloudData = doc.data!!
                    val cloudExpensesMap = cloudData[RepoKeys.FIELD_EXPENSES] as? List<Map<String, Any>> ?: emptyList()
                    val cloudAccountsMap = cloudData[RepoKeys.FIELD_ACCOUNTS] as? List<Map<String, Any>> ?: emptyList()
                    val cloudDebtsMap = cloudData[RepoKeys.FIELD_DEBT_RECORDS] as? List<Map<String, Any>> ?: emptyList()

                    // 云端 accountId -> 本地 accountId 映射
                    val accountIdMap = mutableMapOf<Long, Long>()

                    // 先合并 accounts（按 name + type 判断重复）
                    cloudAccountsMap.forEach { accMap ->
                        val name = accMap["name"] as String
                        val typeRaw = accMap["type"] as String
                        val type = normalizeAccountTypeValue(typeRaw)
                        val currency = accMap["currency"] as? String ?: "CNY"

                        val existing = localAccounts.find {
                            it.name == name && normalizeAccountTypeValue(it.type) == type
                        }

                        val cloudId = (accMap["id"] as Number).toLong()
                        if (existing != null) {
                            accountIdMap[cloudId] = existing.id
                        } else {
                            val newAccount = Account(
                                name = name,
                                type = type,
                                initialBalance = (accMap["initialBalance"] as Number).toDouble(),
                                currency = currency,
                                iconName = accMap["iconName"] as? String ?: "Wallet",
                                isLiability = accMap["isLiability"] as? Boolean ?: false
                            )
                            val newId = accountDao.insert(newAccount)
                            accountIdMap[cloudId] = newId
                        }
                    }

                    var addedCount = 0

                    // 合并 expenses（去重条件：amount + date + remark + accountId）
                    cloudExpensesMap.forEach { expMap ->
                        val amount = (expMap["amount"] as Number).toDouble()
                        val date = when (val d = expMap["date"]) {
                            is com.google.firebase.Timestamp -> d.toDate()
                            is Long -> java.util.Date(d)
                            else -> java.util.Date()
                        }
                        val remark = expMap["remark"] as? String ?: ""
                        val category = expMap["category"] as String
                        val cloudAccountId = (expMap["accountId"] as Number).toLong()

                        val recordType = (expMap["recordType"] as? Number)?.toInt() ?: RecordType.INCOME_EXPENSE
                        val transferId = (expMap["transferId"] as? Number)?.toLong()
                        val relatedAccountIdRaw = (expMap["relatedAccountId"] as? Number)?.toLong()
                        val relatedAccountId = relatedAccountIdRaw?.let { accountIdMap[it] }

                        val targetAccountId = accountIdMap[cloudAccountId] ?: return@forEach

                        val isDuplicate = localExpenses.any { local ->
                            local.amount == amount &&
                                    local.date.time == date.time &&
                                    (local.remark ?: "") == remark &&
                                    local.accountId == targetAccountId
                        }

                        if (!isDuplicate) {
                            val newExpense = Expense(
                                accountId = targetAccountId,
                                category = category,
                                amount = amount,
                                date = date,
                                remark = remark,
                                recordType = recordType,
                                transferId = transferId,
                                relatedAccountId = relatedAccountId
                            )
                            expenseDao.insertExpense(newExpense)
                            addedCount++
                        }
                    }

                    // 合并 debt_records（去重条件：personName + amount(误差<0.01) + borrowTime + note）
                    cloudDebtsMap.forEach { debtMap ->
                        val personName = debtMap["personName"] as String
                        val amount = (debtMap["amount"] as Number).toDouble()
                        val borrowTime = when (val d = debtMap["borrowTime"]) {
                            is com.google.firebase.Timestamp -> d.toDate()
                            is Long -> java.util.Date(d)
                            else -> java.util.Date()
                        }
                        val note = debtMap["note"] as? String

                        val isDuplicate = localDebts.any { local ->
                            local.personName == personName &&
                                    abs(local.amount - amount) < 0.01 &&
                                    local.borrowTime.time == borrowTime.time &&
                                    local.note == note
                        }

                        if (!isDuplicate) {
                            val oldAccountId = (debtMap["accountId"] as? Number)?.toLong()
                            val newAccountId = oldAccountId?.let { accountIdMap[it] } ?: oldAccountId ?: -1L

                            val oldInId = (debtMap["inAccountId"] as? Number)?.toLong()
                            val oldOutId = (debtMap["outAccountId"] as? Number)?.toLong()
                            val newInId = oldInId?.let { accountIdMap[it] }
                            val newOutId = oldOutId?.let { accountIdMap[it] }

                            val settleTimeRaw = debtMap["settleTime"]
                            val settleTime = if (settleTimeRaw != null) {
                                when (settleTimeRaw) {
                                    is com.google.firebase.Timestamp -> settleTimeRaw.toDate()
                                    is Long -> java.util.Date(settleTimeRaw)
                                    else -> java.util.Date()
                                }
                            } else null

                            val newRecord = DebtRecord(
                                id = 0L,
                                accountId = newAccountId,
                                personName = personName,
                                amount = amount,
                                note = note,
                                borrowTime = borrowTime,
                                settleTime = settleTime,
                                inAccountId = newInId,
                                outAccountId = newOutId
                            )
                            debtRecordDao.insert(newRecord)
                            addedCount++
                        }
                    }

                    // 合并后再上传一次（与原实现一致）
                    val finalExpenses = expenseDao.getAllExpenses().first()
                    val finalAccounts = accountDao.getAllAccounts().first()
                    cloud.uploadData(uid, finalExpenses, finalAccounts)

                    Result.success(getString(R.string.success_sync_merge_added, arrayOf(addedCount)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
