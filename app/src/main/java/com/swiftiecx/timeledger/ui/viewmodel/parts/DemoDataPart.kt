package com.swiftiecx.timeledger.ui.viewmodel.parts

import android.app.Application
import com.swiftiecx.timeledger.data.*
import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.local.entity.Budget
import com.swiftiecx.timeledger.data.local.entity.DebtRecord
import com.swiftiecx.timeledger.data.local.entity.Expense
import com.swiftiecx.timeledger.data.local.entity.RecordType
import com.swiftiecx.timeledger.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.random.Random

class DemoDataPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope,
    private val app: Application,
    private val defaultCurrencyProvider: () -> String,
    private val afterGenerated: () -> Unit
) {
    fun generateDemoData() {
        scope.launch(Dispatchers.IO) {
            repository.clearAllData()

            val currency = defaultCurrencyProvider()

            val accCashId = repository.insertAccount(
                Account(
                    name = "Cash Wallet",
                    type = "Cash",
                    initialBalance = 1500.0,
                    currency = currency,
                    iconName = "Wallet",
                    isLiability = false,
                    category = "FUNDS"
                )
            )

            val accBankId = repository.insertAccount(
                Account(
                    name = "Chase Bank",
                    type = "Bank Card",
                    initialBalance = 25000.0,
                    currency = currency,
                    iconName = "AccountBalance",
                    isLiability = false,
                    category = "FUNDS"
                )
            )

            val accCreditId = repository.insertAccount(
                Account(
                    name = "Amex Platinum",
                    type = "Credit Card",
                    initialBalance = -3200.0,
                    currency = currency,
                    iconName = "CreditCard",
                    isLiability = true,
                    category = "CREDIT",
                    creditLimit = 50000.0
                )
            )

            val accInvestId = repository.insertAccount(
                Account(
                    name = "Stock Portfolio",
                    type = "Investment",
                    initialBalance = 100000.0,
                    currency = currency,
                    iconName = "TrendingUp",
                    isLiability = false,
                    category = "FUNDS"
                )
            )

            val cal = java.util.Calendar.getInstance()
            val currentYear = cal.get(java.util.Calendar.YEAR)
            val currentMonth = cal.get(java.util.Calendar.MONTH) + 1

            repository.upsertBudget(
                Budget(
                    category = "Food",
                    amount = 3000.0,
                    year = currentYear,
                    month = currentMonth
                )
            )
            repository.upsertBudget(
                Budget(
                    category = "Shopping",
                    amount = 2000.0,
                    year = currentYear,
                    month = currentMonth
                )
            )
            repository.upsertBudget(
                Budget(
                    category = "Traffic",
                    amount = 1000.0,
                    year = currentYear,
                    month = currentMonth
                )
            )
            repository.upsertBudget(
                Budget(
                    category = "Entertainment",
                    amount = 1500.0,
                    year = currentYear,
                    month = currentMonth
                )
            )
            repository.upsertBudget(
                Budget(
                    category = "Total Budget",
                    amount = 10000.0,
                    year = currentYear,
                    month = currentMonth
                )
            )

            val lendRecord = DebtRecord(
                accountId = -1,
                personName = "David",
                amount = 2000.0,
                borrowTime = Date(),
                outAccountId = accBankId,
                note = "Lend for rent"
            )
            val lendExpense = Expense(
                accountId = accBankId,
                category = "借出",
                amount = -2000.0,
                date = Date(),
                remark = "Lend to David"
            )
            repository.saveDebtWithTransaction(lendRecord, lendExpense)

            val borrowRecord = DebtRecord(
                accountId = -1,
                personName = "Mom",
                amount = 5000.0,
                borrowTime = Date(),
                inAccountId = accBankId,
                note = "Emergency fund"
            )
            val borrowExpense = Expense(
                accountId = accBankId,
                category = "借入",
                amount = 5000.0,
                date = Date(),
                remark = "Borrow from Mom"
            )
            repository.saveDebtWithTransaction(borrowRecord, borrowExpense)

            val random = Random.Default
            val expenseCats = listOf("Food", "Traffic", "Shopping", "Entertainment", "Housing", "Medical", "Daily", "Clothes")
            val incomeCats = listOf("Salary", "Bonus", "PartTime")
            val remarks = listOf("Lunch", "Taxi", "Grocery", "Movie", "Gas", "Coffee", "Netflix", "Gym")

            val daysBack = 90
            for (i in 0..daysBack) {
                cal.time = Date()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                val date = cal.time

                val count = random.nextInt(5)
                repeat(count) {
                    val isIncome = random.nextInt(10) < 1

                    if (isIncome) {
                        val amount = 2000 + random.nextDouble() * 5000
                        repository.insert(
                            Expense(
                                accountId = accBankId,
                                category = incomeCats.random(),
                                amount = amount,
                                date = date,
                                remark = "Income source",
                                recordType = RecordType.INCOME_EXPENSE
                            )
                        )
                    } else {
                        val accId = listOf(accCashId, accBankId, accCreditId).random()
                        val cat = expenseCats.random()
                        val baseAmount = 10 + random.nextDouble() * 100
                        val amount = if (random.nextInt(10) == 0) baseAmount * 5 else baseAmount

                        repository.insert(
                            Expense(
                                accountId = accId,
                                category = cat,
                                amount = -amount,
                                date = date,
                                remark = remarks.random(),
                                recordType = RecordType.INCOME_EXPENSE
                            )
                        )
                    }
                }
            }

            repeat(3) {
                cal.time = Date()
                cal.add(java.util.Calendar.MONTH, -it)
                val date = cal.time
                val amount = 1000.0 + random.nextInt(2000)

                repository.createTransfer(
                    Expense(
                        accountId = accBankId,
                        category = "category_transfer_out",
                        amount = -amount,
                        date = date,
                        remark = "Credit Card Repayment"
                    ),
                    Expense(
                        accountId = accCreditId,
                        category = "category_transfer_in",
                        amount = amount,
                        date = date,
                        remark = "Repayment Received"
                    )
                )
            }

            afterGenerated()
        }
    }
}
