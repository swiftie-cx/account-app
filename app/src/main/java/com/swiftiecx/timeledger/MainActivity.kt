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

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 初始化失败: ${e.message}")
        }

        enableEdgeToEdge()

        try {
            val database = AppDatabase.getDatabase(applicationContext)

            val repository = ExpenseRepository(
                expenseDao = database.expenseDao(),
                budgetDao = database.budgetDao(),
                accountDao = database.accountDao(),
                periodicDao = database.periodicDao(),
                categoryDao = database.categoryDao(),
                debtRecordDao = database.debtRecordDao(),
                context = applicationContext
            )

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

            // ================================================================
            // 🔥🔥 [MOCK DATA GENERATION START] 🔥🔥
            // ⚠️ 警告：截图完成后请务必删除或注释掉以下这行代码！
            // ⚠️ WARNING: Delete or comment out this line after taking screenshots!
            // ================================================================
//            expenseViewModel.generateDemoData()
            // ================================================================
            // 🔥🔥 [MOCK DATA GENERATION END] 🔥🔥
            // ================================================================

            val themeViewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            val themeViewModel = ViewModelProvider(this, themeViewModelFactory)[ThemeViewModel::class.java]

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
                } catch (e: Exception) {
                    Log.e(TAG, "WorkManager 初始化失败: ${e.message}")
                }
            }

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
                        surface = Color(0xFF1E1E1E)
                    )
                } else {
                    // ✅ 【核心修改】在这里统一去除所有默认紫色
                    lightColorScheme(
                        // 1. 主题色
                        primary = themeColor,
                        onPrimary = Color.White,
                        primaryContainer = themeColor.copy(alpha = 0.15f),
                        onPrimaryContainer = themeColor, // 容器上的文字颜色

                        // 2. 页面与卡片背景
                        background = Color(0xFFF5F5F5), // 统一浅灰背景
                        onBackground = Color.Black,
                        surface = Color.White,          // 卡片表面纯白
                        onSurface = Color.Black,

                        // 3. 【关键】覆盖 M3 默认的紫色容器色 (用于 Dialog, BottomSheet, DatePicker 等)
                        surfaceContainerLowest = Color.White,
                        surfaceContainerLow = Color.White,
                        surfaceContainer = Color.White,
                        surfaceContainerHigh = Color.White,
                        surfaceContainerHighest = Color.White,

                        // 4. 【关键】输入框默认背景、Switch 未选中轨道等
                        // 默认是淡紫色，强制改为浅灰色
                        surfaceVariant = Color(0xFFE0E0E0),
                        onSurfaceVariant = Color.Gray,      // 图标颜色
                        outline = Color(0xFFBDBDBD),        // 边框颜色
                        outlineVariant = Color(0xFFE0E0E0)  // 次级边框
                    )
                }

                MaterialTheme(colorScheme = colorScheme) {
                    MainScreen(expenseViewModel, themeViewModel)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: MainActivity 初始化错误", e)
        }
    }
}