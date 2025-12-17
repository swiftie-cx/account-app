package com.swiftiecx.timeledger.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// 定义主题颜色选项的数据结构
data class ThemeColorOption(val name: String, val color: Color)

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    // ===========================
    // 1. 语言设置 (核心修复部分)
    // ===========================

    // [新增] 必须暴露这个 StateFlow，SettingsScreen 才能监听
    // 初始化时，直接从 AppCompatDelegate 读取当前语言
    private val _language = MutableStateFlow(getCurrentLanguageCode())
    val language = _language.asStateFlow()

    // 获取当前语言代码 (如 "zh-CN", "en")，空字符串表示"跟随系统"
    // 这个方法改为 private 也可以，或者保留给其他逻辑使用
    fun getCurrentLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            // 只取第一个语言标签
            locales.get(0)?.toLanguageTag() ?: ""
        } else {
            "" // 空列表代表跟随系统
        }
    }

    // 设置语言
    fun setLanguage(code: String) {
        // 1. 立即更新 StateFlow，让 UI (SettingsScreen) 的勾选状态马上变化
        _language.value = code

        // 2. 调用 Android 原生 API 切换语言
        // 注意：code 为 "" 或 "system" 时，都视为跟随系统
        val localeList = if (code.isBlank() || code == "system") {
            LocaleListCompat.getEmptyLocaleList() // 空列表 = 跟随系统
        } else {
            LocaleListCompat.forLanguageTags(code)
        }

        AppCompatDelegate.setApplicationLocales(localeList)
        // 注意：调用此方法后，Activity 会自动重建 (Recreate)，App 语言会立即更新
    }

    // [可选] 获取用于 UI 显示的文字
    // 现在的 SettingsScreen 已经改用 stringResource 直接读取 xml 了，这个方法其实可以删掉，
    // 但为了防止其他地方用到，先保留。
    fun getCurrentLanguageDisplayName(): String {
        val current = _language.value // 使用 Flow 里的值
        return when {
            current.startsWith("zh") -> "简体中文"
            current.startsWith("en") -> "English"
            current.startsWith("ja") -> "日本語"
            current.startsWith("ko") -> "한국어"
            else -> "跟随系统"
        }
    }

    // ===========================
    // 2. 主题颜色设置 (保持不变)
    // ===========================

    val themeOptions = listOf(
        ThemeColorOption("默认紫", Color(0xFF6750A4)),
        ThemeColorOption("柔豆沙红", Color(0xFFC08886)),
        ThemeColorOption("玫瑰黯红", Color(0xFFA85656)),
        ThemeColorOption("枣泥红", Color(0xFF8E4444)),
        ThemeColorOption("深勃艮第", Color(0xFF5D2E2E)),
        ThemeColorOption("复古橘", Color(0xFFD69E68)),
        ThemeColorOption("暖土橙", Color(0xFFC48648)),
        ThemeColorOption("焦糖橙", Color(0xFFA66933)),
        ThemeColorOption("陶土棕", Color(0xFF754C24)),
        ThemeColorOption("复古芥末黄", Color(0xFFD6C047)),
        ThemeColorOption("暗金黄", Color(0xFFB59C24)),
        ThemeColorOption("赭黄", Color(0xFF8F7A1D)),
        ThemeColorOption("深黄铜", Color(0xFF615213)),
        ThemeColorOption("森林绿", Color(0xFF4CAF50)),
        ThemeColorOption("深海蓝", Color(0xFF2196F3)),
        ThemeColorOption("极致黑", Color(0xFF000000))
    )

    // 当前选中的主题颜色
    private val _themeColor = MutableStateFlow(loadThemeColor())
    val themeColor = _themeColor.asStateFlow()

    // Compose state
    var isDarkTheme = mutableStateOf(false)
        private set

    fun toggleTheme(isDark: Boolean) {
        isDarkTheme.value = isDark
    }

    fun updateThemeColor(color: Color) {
        _themeColor.value = color
        saveThemeColor(color)
    }

    private fun saveThemeColor(color: Color) {
        prefs.edit().putInt("theme_color_int", color.toArgb()).apply()
    }

    private fun loadThemeColor(): Color {
        val savedColorInt = prefs.getInt("theme_color_int", -1)
        return if (savedColorInt != -1) {
            Color(savedColorInt)
        } else {
            themeOptions.first().color
        }
    }
}