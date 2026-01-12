package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.data.ExpenseRepository
import com.swiftiecx.timeledger.data.PeriodicTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeriodicPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope
) {
    val allPeriodicTransactions: StateFlow<List<PeriodicTransaction>> =
        repository.allPeriodicTransactions.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertPeriodic(transaction: PeriodicTransaction) {
        scope.launch(Dispatchers.IO) {
            repository.insertPeriodic(transaction)
            repository.checkAndExecutePeriodicTransactions()
        }
    }

    fun updatePeriodic(transaction: PeriodicTransaction) {
        scope.launch(Dispatchers.IO) {
            repository.updatePeriodic(transaction)
            repository.checkAndExecutePeriodicTransactions()
        }
    }

    fun deletePeriodic(transaction: PeriodicTransaction) {
        scope.launch(Dispatchers.IO) { repository.deletePeriodic(transaction) }
    }
}
