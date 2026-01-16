package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.data.local.entity.Account
import com.swiftiecx.timeledger.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class AccountsPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope,
    private val afterClearAllData: () -> Unit
) {
    val allAccounts: StateFlow<List<Account>> =
        repository.allAccounts.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultAccountId: StateFlow<Long> =
        repository.defaultAccountId.stateIn(scope, SharingStarted.WhileSubscribed(5000), -1L)

    fun setDefaultAccount(id: Long) = repository.saveDefaultAccountId(id)

    fun reorderAccounts(newOrder: List<Account>) = repository.saveAccountOrder(newOrder)

    fun insertAccount(account: Account) {
        scope.launch(Dispatchers.IO) { repository.insertAccount(account) }
    }

    fun updateAccount(account: Account) {
        scope.launch(Dispatchers.IO) { repository.updateAccount(account) }
    }

    fun deleteAccount(account: Account) {
        scope.launch(Dispatchers.IO) { repository.deleteAccount(account) }
    }

    fun updateAccountWithNewBalance(account: Account, newCurrentBalance: Double) {
        scope.launch(Dispatchers.IO) {
            val transactions = repository.allExpenses.first()
            val transactionSum = transactions.filter { it.accountId == account.id }.sumOf { it.amount }
            val newInitialBalance = newCurrentBalance - transactionSum
            repository.updateAccount(account.copy(initialBalance = newInitialBalance))
        }
    }

    fun clearAllData() {
        scope.launch(Dispatchers.IO) {
            repository.clearAllData()
            afterClearAllData()
        }
    }
}
