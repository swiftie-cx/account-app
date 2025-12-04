package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.screen.MainScreen
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import com.example.myapplication.ui.viewmodel.ExpenseViewModelFactory
import com.example.myapplication.ui.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化数据库和 Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(
            database.expenseDao(),
            database.budgetDao(),
            database.accountDao(),
            applicationContext
        )

        // 2. 初始化 ExpenseViewModel
        val expenseViewModelFactory = ExpenseViewModelFactory(repository)
        val expenseViewModel = ViewModelProvider(this, expenseViewModelFactory)[ExpenseViewModel::class.java]

        // 3. 初始化 ThemeViewModel
        val themeViewModel = ThemeViewModel(applicationContext)

        setContent {
            // 4. 监听主题颜色状态
            val themeColor by themeViewModel.themeColor.collectAsState()
            val isDarkTheme = isSystemInDarkTheme()

            // 5. 动态构建 ColorScheme
            val colorScheme = if (isDarkTheme) {
                darkColorScheme(
                    primary = themeColor,
                    onPrimary = Color.White,
                    primaryContainer = themeColor.copy(alpha = 0.3f), // 深色模式
                    onPrimaryContainer = Color.White
                )
            } else {
                lightColorScheme(
                    primary = themeColor,
                    onPrimary = Color.White,
                    // (核心修复) 将透明度提升至 0.5f (50%)
                    // 您的莫兰迪色系比较浅，必须用 50% 的浓度才能在白色背景上显出颜色
                    // 这样“资产”、“预算”等卡片背景就会很清晰了
                    primaryContainer = themeColor.copy(alpha = 0.5f),
                    onPrimaryContainer = themeColor // 文字颜色直接用深色主题色，保证对比度
                )
            }

            // 6. 应用动态主题
            MaterialTheme(
                colorScheme = colorScheme
            ) {
                MainScreen(expenseViewModel, themeViewModel)
            }
        }
    }
}