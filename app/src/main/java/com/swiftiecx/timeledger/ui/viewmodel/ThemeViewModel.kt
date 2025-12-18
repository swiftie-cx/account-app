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
        ThemeColorOption("复古金", Color(0xFFFED040)), // 第一位：提取自 App 图标的黄色
        ThemeColorOption("经典紫", Color(0xFF6750A4)), // 第二位：原默认紫
        ThemeColorOption("朱砂红", Color(0xFFB71C1C)),
        ThemeColorOption("胭脂粉", Color(0xFFC2185B)),
        ThemeColorOption("枫叶橙", Color(0xFFE65100)),
        ThemeColorOption("琥珀棕", Color(0xFF8D6E63)),
        ThemeColorOption("橄榄绿", Color(0xFF558B2F)),
        ThemeColorOption("青黛绿", Color(0xFF00695C)),
        ThemeColorOption("孔雀蓝", Color(0xFF00838F)),
        ThemeColorOption("晴空蓝", Color(0xFF0277BD)),
        ThemeColorOption("靛青蓝", Color(0xFF283593)),
        ThemeColorOption("罗兰紫", Color(0xFF8E24AA)),
        ThemeColorOption("丁香色", Color(0xFF9575CD)),
        ThemeColorOption("冷岩灰", Color(0xFF546E7A)),
        ThemeColorOption("咖啡色", Color(0xFF4E342E)),
        ThemeColorOption("夜幕蓝", Color(0xFF263238))
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