package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // 核心：启用边到边显示
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.screen.MainScreen
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import com.example.myapplication.ui.viewmodel.ExpenseViewModelFactory
import com.example.myapplication.ui.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 开启 Edge-to-Edge (沉浸式)，让内容延伸到状态栏后面
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(
            expenseDao = database.expenseDao(),
            budgetDao = database.budgetDao(),
            accountDao = database.accountDao(),
            periodicDao = database.periodicDao(), // (新增) 传入 periodicDao
            context = applicationContext,
        )

        val expenseViewModelFactory = ExpenseViewModelFactory(repository)
        val expenseViewModel = ViewModelProvider(this, expenseViewModelFactory)[ExpenseViewModel::class.java]
        val themeViewModel = ThemeViewModel(applicationContext)

        setContent {
            val themeColor by themeViewModel.themeColor.collectAsState()
            val isDarkTheme = isSystemInDarkTheme()

            // 定义颜色方案 (保持之前的高级灰调)
            val colorScheme = if (isDarkTheme) {
                darkColorScheme(
                    primary = themeColor,
                    onPrimary = Color.White,
                    primaryContainer = themeColor.copy(alpha = 0.3f),
                    onPrimaryContainer = Color.White,
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    surfaceContainerLow = Color(0xFF1E1E1E)
                )
            } else {
                lightColorScheme(
                    primary = themeColor,
                    onPrimary = Color.White,
                    primaryContainer = themeColor.copy(alpha = 0.15f),
                    onPrimaryContainer = themeColor,
                    background = Color(0xFFF8F9FA), // 极简灰白背景
                    surface = Color.White,
                    surfaceContainerLow = Color(0xFFF2F4F7),
                    onSurface = Color(0xFF191C1E),
                    onSurfaceVariant = Color(0xFF44474E),
                    outline = Color(0xFF74777F),
                    outlineVariant = Color(0xFFC4C7C5)
                )
            }

            MaterialTheme(
                colorScheme = colorScheme
            ) {
                MainScreen(expenseViewModel, themeViewModel)
            }
        }
    }
}