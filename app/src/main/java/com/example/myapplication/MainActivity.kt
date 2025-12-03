package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // (新) 导入这个
import androidx.activity.viewModels
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.screen.MainScreen
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import com.example.myapplication.ui.viewmodel.ExpenseViewModelFactory

class MainActivity : ComponentActivity() {
    private val vm: ExpenseViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val repo = ExpenseRepository(
            db.expenseDao(),
            db.budgetDao(),
            db.accountDao(),
            applicationContext
        )
        ExpenseViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (核心修改) 开启沉浸式（Edge-to-Edge）
        // 这会让内容延伸到状态栏和导航栏后面
        enableEdgeToEdge()

        setContent {
            // 注意：这里不需要再包裹 AppTheme，因为 Material3 默认适配较好，
            // 且 MainScreen 内部会处理颜色。如果您有自定义 Theme，请包裹在 MainScreen 外。
            MainScreen(expenseViewModel = vm)
        }
    }
}