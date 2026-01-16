package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.data.local.entity.Budget
import com.swiftiecx.timeledger.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BudgetPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope
) {
    private val budgetUpdateMutex = Mutex()

    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>> = repository.getBudgetsForMonth(year, month)

    fun saveBudget(budget: Budget, allCategoryTitles: List<String>) {
        scope.launch(Dispatchers.IO) {
            budgetUpdateMutex.withLock {
                repository.upsertBudget(budget)
                if (budget.category != "总预算") {
                    val allBudgets = repository.getBudgetsForMonth(budget.year, budget.month).first()
                    val calculatedSum = allBudgets.filter { it.category in allCategoryTitles }.sumOf { it.amount }
                    val manualTotalBudget = allBudgets.find { it.category == "总预算" }
                    if (manualTotalBudget == null || manualTotalBudget.amount < calculatedSum) {
                        repository.upsertBudget(
                            Budget(
                                id = manualTotalBudget?.id ?: 0,
                                category = "总预算",
                                amount = calculatedSum,
                                year = budget.year,
                                month = budget.month
                            )
                        )
                    }
                }
            }
        }
    }

    fun syncBudgetsFor(year: Int, month: Int) {
        scope.launch(Dispatchers.IO) {
            val targetMonthBudgets = getBudgetsForMonth(year, month).first()
            if (targetMonthBudgets.isNotEmpty()) return@launch

            val recentBudget = repository.getMostRecentBudget() ?: return@launch
            if (recentBudget.year == year && recentBudget.month == month) return@launch

            val recentMonthBudgets = getBudgetsForMonth(recentBudget.year, recentBudget.month).first()
            val newBudgets = recentMonthBudgets.map { it.copy(id = 0, year = year, month = month) }
            if (newBudgets.isNotEmpty()) repository.upsertBudgets(newBudgets)
        }
    }
}
