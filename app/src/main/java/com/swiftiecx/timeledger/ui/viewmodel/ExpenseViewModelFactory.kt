package com.swiftiecx.timeledger.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.swiftiecx.timeledger.data.ExpenseRepository

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository,
    private val context: Context // [新增] 接收 Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // [新增] 传入 context
            return ExpenseViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}