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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 开始初始化")

        // --- 1. Firebase 安全初始化 ---
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "Firebase 手动初始化成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 初始化失败: ${e.message}")
        }

        // --- 2. 开启 Edge-to-Edge ---
        enableEdgeToEdge()

        try {
            // --- 3. 初始化数据库和仓库 ---
            Log.d(TAG, "正在初始化数据库...")
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = ExpenseRepository(
                expenseDao = database.expenseDao(),
                budgetDao = database.budgetDao(),
                accountDao = database.accountDao(),
                periodicDao = database.periodicDao(),
                context = applicationContext
            )
            Log.d(TAG, "Repository 初始化完成")

            // --- 4. 初始化 ViewModel ---
            Log.d(TAG, "正在初始化 ViewModel...")

            // 4.1 ExpenseViewModel
            val expenseViewModelFactory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return ExpenseViewModel(repository, application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
            val expenseViewModel = ViewModelProvider(this, expenseViewModelFactory)[ExpenseViewModel::class.java]

            // 4.2 ThemeViewModel
            val themeViewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            val themeViewModel = ViewModelProvider(this, themeViewModelFactory)[ThemeViewModel::class.java]
            Log.d(TAG, "ViewModel 初始化完成")

            // --- 5. 启动后台周期任务 (放到协程中) ---
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val workRequest = PeriodicWorkRequestBuilder<PeriodicWorker>(
                        12, TimeUnit.HOURS
                    ).build()

                    WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                        "PeriodicBookkeepingWork",
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )
                    Log.d(TAG, "WorkManager 任务已入队")
                } catch (e: Exception) {
                    Log.e(TAG, "WorkManager 初始化失败: ${e.message}")
                }
            }

            // --- 6. 设置 UI 内容 ---
            setContent {
                val themeColor by themeViewModel.themeColor.collectAsState()
                val isDarkTheme = isSystemInDarkTheme()

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
            Log.d(TAG, "setContent 完成，UI 应该显示")

        } catch (e: Exception) {
            Log.e(TAG, "FATAL: MainActivity 初始化过程发生严重错误", e)
            // 这里虽然捕获了，但如果没有 UI，应用还是会黑屏或退出
            // 但至少我们能在 Logcat 里看到报错堆栈
        }
    }
}