package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.screen.MainScreen
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import com.example.myapplication.ui.viewmodel.ExpenseViewModelFactory

class MainActivity : ComponentActivity() {
    private val vm: ExpenseViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        // (修改) 同时传入三个 DAO
        val repo = ExpenseRepository(db.expenseDao(), db.budgetDao(), db.accountDao())
        ExpenseViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen(expenseViewModel = vm) }
    }
}