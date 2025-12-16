package com.swiftiecx.timeledger

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.swiftiecx.timeledger.data.AppDatabase
import com.swiftiecx.timeledger.data.ExpenseRepository
import com.swiftiecx.timeledger.ui.screen.MainScreen
import com.swiftiecx.timeledger.ui.viewmodel.ExpenseViewModel
import com.swiftiecx.timeledger.ui.viewmodel.ThemeViewModel
import com.swiftiecx.timeledger.worker.PeriodicWorker
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. Firebase 安全初始化 ---
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("MainActivity", "Firebase 手动初始化成功")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Firebase 初始化失败: ${e.message}")
        }

        // --- 2. 开启 Edge-to-Edge ---
        enableEdgeToEdge()

        // --- 3. 初始化数据库和仓库 ---
        // 注意：这里使用的是 applicationContext，确保生命周期正确
        val database = AppDatabase.Companion.getDatabase(applicationContext)
        val repository = ExpenseRepository(
            expenseDao = database.expenseDao(),
            budgetDao = database.budgetDao(),
            accountDao = database.accountDao(),
            periodicDao = database.periodicDao(),
            context = applicationContext
        )

        // --- 4. 初始化 ViewModel (关键修复) ---

        // 4.1 ExpenseViewModel (注入 Repository)
        val expenseViewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ExpenseViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        val expenseViewModel = ViewModelProvider(this, expenseViewModelFactory)[ExpenseViewModel::class.java]

        // 4.2 ThemeViewModel (注入 Application)
        // 【关键修复】使用 AndroidViewModelFactory 自动传入 Application 实例
        // 解决了 Context != Application 的类型不匹配报错
        val themeViewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        val themeViewModel = ViewModelProvider(this, themeViewModelFactory)[ThemeViewModel::class.java]


        // --- 5. 启动后台周期任务 ---
        // 每 12 小时检查一次自动记账
        val workRequest = PeriodicWorkRequestBuilder<PeriodicWorker>(
            12, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "PeriodicBookkeepingWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        // --- 6. 设置 UI 内容 ---
        setContent {
            val themeColor by themeViewModel.themeColor.collectAsState()
            val isDarkTheme = isSystemInDarkTheme() // 这里也可以结合 themeViewModel.isDarkTheme 实现手动切换

            // 定义颜色方案
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
                    background = Color(0xFFF8F9FA),
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