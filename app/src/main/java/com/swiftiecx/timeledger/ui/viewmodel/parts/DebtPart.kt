package com.swiftiecx.timeledger.ui.viewmodel.parts

import android.app.Application
import com.swiftiecx.timeledger.R
import com.swiftiecx.timeledger.data.DebtRecord
import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.data.ExpenseRepository
import com.swiftiecx.timeledger.data.RecordType
import com.swiftiecx.timeledger.ui.viewmodel.model.PersonDebtSummaryInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.abs

class DebtPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope,
    private val app: Application
) {
    fun getAllDebtRecords(): Flow<List<DebtRecord>> = repository.getAllDebtRecords()
    fun getDebtRecords(accountId: Long): Flow<List<DebtRecord>> = repository.getDebtRecords(accountId)

    fun getDebtRecordsByPerson(personName: String): Flow<List<DebtRecord>> =
        repository.getAllDebtRecords().map { all -> all.filter { it.personName == personName } }

    fun getDebtRecordById(id: Long): Flow<DebtRecord?> =
        repository.getAllDebtRecords().map { all -> all.find { it.id == id } }

    fun deleteDebtRecordsByPerson(personName: String) {
        scope.launch(Dispatchers.IO) {
            val records = repository.getAllDebtRecords().first().filter { it.personName == personName }
            records.forEach { repository.deleteDebtRecord(it) }
        }
    }

    fun updateDebtWithTransaction(record: DebtRecord, oldDate: Date, oldAmount: Double) {
        scope.launch(Dispatchers.IO) {
            repository.updateDebtRecord(record)

            val expenses = repository.allExpenses.first()
            val matchedExpense = expenses.find {
                it.date.time == oldDate.time &&
                        abs(it.amount) == abs(oldAmount) &&
                        it.remark?.contains(record.personName) == true
            }

            matchedExpense?.let {
                val relationLabel = try {
                    app.getString(R.string.label_related_person, record.personName)
                } catch (_: Exception) {
                    "Related: ${record.personName}"
                }
                val noteSuffix = if (!record.note.isNullOrEmpty()) " | ${record.note}" else ""
                repository.updateExpense(
                    it.copy(
                        date = record.borrowTime,
                        amount = if (it.amount < 0) -record.amount else record.amount,
                        remark = "$relationLabel$noteSuffix"
                    )
                )
            }
        }
    }

    fun updateDebtRecord(record: DebtRecord) {
        scope.launch(Dispatchers.IO) { repository.updateDebtRecord(record) }
    }

    fun deleteDebtRecord(record: DebtRecord) {
        scope.launch(Dispatchers.IO) { repository.deleteDebtRecord(record) }
    }

    fun insertDebtRecord(record: DebtRecord, countInStats: Boolean = true) {
        scope.launch(Dispatchers.IO) {
            val fundAccountId = record.inAccountId ?: record.outAccountId

            if (fundAccountId != null && fundAccountId != -1L) {
                val isLending = record.outAccountId != null

                val relationLabel = try {
                    app.getString(R.string.label_related_person, record.personName)
                } catch (_: Exception) {
                    "Related: ${record.personName}"
                }
                val noteSuffix = if (record.note.isNullOrBlank()) "" else " | ${record.note}"
                val finalRemark = "$relationLabel$noteSuffix"

                val expense = Expense(
                    accountId = fundAccountId,
                    amount = if (isLending) -abs(record.amount) else abs(record.amount),
                    category = if (isLending) "借出" else "借入",
                    remark = finalRemark,
                    date = record.borrowTime,
                    recordType = if (countInStats) RecordType.INCOME_EXPENSE else RecordType.TRANSFER
                )
                repository.saveDebtWithTransaction(record, expense)
            } else {
                repository.insertDebtRecord(record)
            }
        }
    }

    fun settleDebt(
        personName: String,
        amount: Double,
        interest: Double,
        accountId: Long,
        isBorrow: Boolean,
        remark: String?,
        date: Date,
        generateBill: Boolean
    ) {
        scope.launch(Dispatchers.IO) {
            val settleRecord = DebtRecord(
                accountId = -1,
                personName = personName,
                amount = amount,
                borrowTime = date,
                note = "结算: $remark ${if (interest > 0) "|利息:$interest|" else ""}",
                inAccountId = if (!isBorrow) accountId else null,
                outAccountId = if (isBorrow) accountId else null
            )

            if (generateBill && accountId != -1L) {
                val relationLabel = try {
                    app.getString(R.string.label_related_person, personName)
                } catch (_: Exception) {
                    "Related: $personName"
                }
                val noteSuffix = if (remark.isNullOrBlank()) "" else " | $remark"
                val finalRemark = "$relationLabel | 利息:$interest$noteSuffix"

                val expense = Expense(
                    accountId = accountId,
                    amount = if (!isBorrow) abs(amount) else -abs(amount),
                    category = if (!isBorrow) "债务收款" else "债务还款",
                    remark = finalRemark,
                    date = date,
                    recordType = RecordType.INCOME_EXPENSE
                )

                repository.saveDebtWithTransaction(settleRecord, expense)

                if (interest > 0) {
                    val interestLabel = try { app.getString(R.string.label_interest) } catch (_: Exception) { "Interest" }
                    val actionLabel = if (!isBorrow) "收款" else "还款"

                    repository.insert(
                        Expense(
                            accountId = accountId,
                            amount = if (!isBorrow) interest else -interest,
                            category = if (!isBorrow) "收入-其他" else "其他",
                            remark = "$personName $actionLabel$interestLabel",
                            date = date,
                            recordType = RecordType.INCOME_EXPENSE
                        )
                    )
                }
            } else {
                repository.insertDebtRecord(settleRecord)
            }
        }
    }

    fun getGlobalDebtSummary(): Flow<Pair<Double, Double>> {
        return repository.getAllDebtRecords().map { all ->
            val totalLend = all.filter { it.outAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }
            val totalCollected = all.filter { it.inAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }

            val totalBorrow = all.filter { it.inAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }
            val totalPaid = all.filter { it.outAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }

            val receivable = (totalLend - totalCollected).coerceAtLeast(0.0)
            val payable = (totalBorrow - totalPaid).coerceAtLeast(0.0)

            receivable to payable
        }
    }

    fun getPersonDebtSummary(personName: String): Flow<PersonDebtSummaryInfo> {
        return repository.getAllDebtRecords().map { all ->
            val personRecords = all.filter { it.personName == personName }

            val lendTotal = personRecords.filter { it.outAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }
            val borrowTotal = personRecords.filter { it.inAccountId != null && !it.note.toString().contains("结算") }.sumOf { it.amount }

            val settledIn = personRecords.filter { it.inAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }
            val settledOut = personRecords.filter { it.outAccountId != null && it.note.toString().contains("结算") }.sumOf { it.amount }

            val netOriginal = lendTotal - borrowTotal
            val isReceivable = netOriginal >= 0

            val remaining = if (isReceivable) (netOriginal - settledIn) else (abs(netOriginal) - settledOut)
            val collectedPrincipal = if (isReceivable) settledIn else settledOut

            val interestRegex = "\\|利息:([\\d.]+)\\|".toRegex()
            val totalInterest = personRecords.sumOf {
                interestRegex.find(it.note ?: "")?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            }

            PersonDebtSummaryInfo(
                totalAmount = remaining,
                collectedAmount = collectedPrincipal,
                interest = totalInterest
            )
        }
    }
}
