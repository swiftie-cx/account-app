package com.swiftiecx.timeledger.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import com.swiftiecx.timeledger.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// 定义主题颜色选项的数据结构
data class ThemeColorOption(
    val nameResId: Int,   // ✅ 多语言：名称走 strings.xml
    val color: Color
)

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = application.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    // ===========================
    // 1. 语言设置 (多语言化)
    // ===========================

    private val _language = MutableStateFlow(getCurrentLanguageCode())
    val language = _language.asStateFlow()

    fun getCurrentLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales.get(0)?.toLanguageTag() ?: ""
        } else {
            "" // 跟随系统
        }
    }

    fun setLanguage(code: String) {
        _language.value = code

        val localeList = if (code.isBlank() || code == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(code)
        }

        AppCompatDelegate.setApplicationLocales(localeList)
    }




    // ===========================
    // 2. 主题颜色设置 (多语言化)
    // ===========================

    // ✅ 名称全部走 strings.xml
    val themeOptions = listOf(
        ThemeColorOption(R.string.theme_color_retro_gold, Color(0xFFFED040)),
        ThemeColorOption(R.string.theme_color_classic_purple, Color(0xFF6750A4)),
        ThemeColorOption(R.string.theme_color_cinnabar_red, Color(0xFFB71C1C)),
        ThemeColorOption(R.string.theme_color_rouge_pink, Color(0xFFC2185B)),
        ThemeColorOption(R.string.theme_color_maple_orange, Color(0xFFE65100)),
        ThemeColorOption(R.string.theme_color_amber_brown, Color(0xFF8D6E63)),
        ThemeColorOption(R.string.theme_color_olive_green, Color(0xFF558B2F)),
        ThemeColorOption(R.string.theme_color_teal_green, Color(0xFF00695C)),
        ThemeColorOption(R.string.theme_color_peacock_blue, Color(0xFF00838F)),
        ThemeColorOption(R.string.theme_color_sky_blue, Color(0xFF0277BD)),
        ThemeColorOption(R.string.theme_color_indigo_blue, Color(0xFF283593)),
        ThemeColorOption(R.string.theme_color_lilac_purple, Color(0xFF8E24AA)),
        ThemeColorOption(R.string.theme_color_lavender, Color(0xFF9575CD)),
        ThemeColorOption(R.string.theme_color_cool_gray, Color(0xFF546E7A)),
        ThemeColorOption(R.string.theme_color_coffee_brown, Color(0xFF4E342E)),
        ThemeColorOption(R.string.theme_color_midnight_blue, Color(0xFF263238))
    )

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
