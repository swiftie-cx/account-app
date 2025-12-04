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
// (新增) 导入 ThemeViewModel
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

        // 3. (新增) 初始化 ThemeViewModel
        val themeViewModel = ThemeViewModel(applicationContext)

        setContent {
            // 4. (新增) 监听主题颜色状态
            val themeColor by themeViewModel.themeColor.collectAsState()
            val isDarkTheme = isSystemInDarkTheme()

            // 5. (新增) 动态构建 ColorScheme
            // 这里我们用选中的颜色作为 primary，并简单推导其他颜色
            // 如果追求完美，可以使用 Material Color Utilities 库生成完整色盘
            val colorScheme = if (isDarkTheme) {
                darkColorScheme(
                    primary = themeColor,
                    onPrimary = Color.White,
                    primaryContainer = themeColor.copy(alpha = 0.3f), // 深色模式下容器色变暗
                    onPrimaryContainer = Color.White
                )
            } else {
                lightColorScheme(
                    primary = themeColor,
                    onPrimary = Color.White,
                    primaryContainer = themeColor.copy(alpha = 0.1f), // 浅色模式下容器色变淡
                    onPrimaryContainer = themeColor
                    // 其他颜色如 secondary, tertiary 也可以根据 themeColor 进行变换
                )
            }

            // 6. 应用动态主题
            MaterialTheme(
                colorScheme = colorScheme
            ) {
                // 传入 themeViewModel 以便在设置页调用
                MainScreen(expenseViewModel, themeViewModel)
            }
        }
    }
}