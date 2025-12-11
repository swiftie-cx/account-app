package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy // 新增
import androidx.work.PeriodicWorkRequestBuilder // 新增
import androidx.work.WorkManager // 新增
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.screen.MainScreen
import com.example.myapplication.ui.viewmodel.ExpenseViewModel
import com.example.myapplication.ui.viewmodel.ExpenseViewModelFactory
import com.example.myapplication.ui.viewmodel.ThemeViewModel
import com.example.myapplication.worker.PeriodicWorker // 确保这一行没有红字，如果有，说明 PeriodicWorker 没创建
import java.util.concurrent.TimeUnit // 新增

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 开启 Edge-to-Edge
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(
            expenseDao = database.expenseDao(),
            budgetDao = database.budgetDao(),
            accountDao = database.accountDao(),
            periodicDao = database.periodicDao(),
            context = applicationContext,
        )

        val expenseViewModelFactory = ExpenseViewModelFactory(repository)
        val expenseViewModel = ViewModelProvider(this, expenseViewModelFactory)[ExpenseViewModel::class.java]
        val themeViewModel = ThemeViewModel(applicationContext)

        // --- 【新增】启动后台周期任务 ---
        // 定义任务：每 12 小时检查一次有没有需要自动记账的规则
        val workRequest = PeriodicWorkRequestBuilder<PeriodicWorker>(
            12, TimeUnit.HOURS
        ).build()

        // 加入队列：使用 KEEP 策略，如果任务已存在则不重复添加，避免重复启动
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "PeriodicBookkeepingWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        // ------------------------------

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