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

            // ✅ 修复点：传入所有 6 个 Dao 参数
            val repository = ExpenseRepository(
                expenseDao = database.expenseDao(),
                budgetDao = database.budgetDao(),
                accountDao = database.accountDao(),
                periodicDao = database.periodicDao(),
                categoryDao = database.categoryDao(),
                debtRecordDao = database.debtRecordDao(), // [补上这一行]
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
                    lightColorScheme(
                        primary = themeColor,
                        onPrimary = Color.White,
                        primaryContainer = themeColor.copy(alpha = 0.15f),
                        onPrimaryContainer = themeColor,
                        background = Color(0xFFF8F9FA),
                        surface = Color.White
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