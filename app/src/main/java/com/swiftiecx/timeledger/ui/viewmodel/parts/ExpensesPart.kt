package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.data.Expense
import com.swiftiecx.timeledger.data.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.abs

class ExpensesPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope
) {
    val allExpenses: StateFlow<List<Expense>> =
        repository.allExpenses.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(expense: Expense) {
        scope.launch(Dispatchers.IO) { repository.insert(expense) }
    }

    fun updateExpense(expense: Expense) {
        scope.launch(Dispatchers.IO) { repository.updateExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        scope.launch(Dispatchers.IO) { repository.deleteExpense(expense) }
    }

    fun createTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        fromAmount: Double,
        toAmount: Double,
        date: Date,
        remark: String?
    ) {
        scope.launch(Dispatchers.IO) {
            val out = Expense(
                accountId = fromAccountId,
                category = "category_transfer_out",
                amount = -abs(fromAmount),
                date = date,
                remark = remark
            )
            val inn = Expense(
                accountId = toAccountId,
                category = "category_transfer_in",
                amount = abs(toAmount),
                date = date,
                remark = remark
            )
            repository.createTransfer(out, inn)
        }
    }
}
