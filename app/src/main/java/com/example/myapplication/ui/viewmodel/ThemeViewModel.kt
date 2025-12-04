package com.example.myapplication.ui.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 1. 定义主题数据模型
data class ThemeOption(val name: String, val color: Color)

// 2. 定义 32 款精选配色 (融合莫兰迪、大地色与经典色)
val AppThemeOptions = listOf(
    ThemeOption("柔豆沙红", Color(0xFFB67A74)),
    ThemeOption("玫瑰黯红", Color(0xFF9C5F5A)),
    ThemeOption("枣泥红", Color(0xFF814443)),
    ThemeOption("深勃艮第", Color(0xFF5A2C2C)),

    ThemeOption("复古橘", Color(0xFFD09363)),
    ThemeOption("暖土橙", Color(0xFFB97A4A)),
    ThemeOption("焦糖橙", Color(0xFF9A6038)),
    ThemeOption("陶土棕", Color(0xFF704933)),

    ThemeOption("复古芥末黄", Color(0xFFC5A544)),
    ThemeOption("暗金黄", Color(0xFFA5872E)),
    ThemeOption("赭黄", Color(0xFF8A6E26)),
    ThemeOption("深黄铜", Color(0xFF6A571E)),

    ThemeOption("鼠尾草绿", Color(0xFF8A9C84)),
    ThemeOption("灰苔绿", Color(0xFF6F7F6A)),
    ThemeOption("黛绿灰", Color(0xFF556354)),
    ThemeOption("深柏木绿", Color(0xFF3D493B)),

    ThemeOption("深松石青", Color(0xFF5F807C)),
    ThemeOption("灰蓝绿", Color(0xFF4C6B68)),
    ThemeOption("海青灰", Color(0xFF3D5554)),
    ThemeOption("深黛青", Color(0xFF2E4241)),

    ThemeOption("灰天青", Color(0xFF8192A4)),
    ThemeOption("钢蓝灰", Color(0xFF5F7284)),
    ThemeOption("深夜蓝", Color(0xFF3B4755)),
    ThemeOption("碳蓝灰", Color(0xFF2C333D)),

    ThemeOption("灰梅紫", Color(0xFF9C8A9E)),
    ThemeOption("葡萄紫灰", Color(0xFF79667C)),
    ThemeOption("深暮紫", Color(0xFF574A5A)),
    ThemeOption("暗紫灰", Color(0xFF3E3644)),

    ThemeOption("石英灰", Color(0xFF9B9B9B)),
    ThemeOption("玄岩灰", Color(0xFF6C6C6C)),
    ThemeOption("铁灰", Color(0xFF4A4A4A)),
    ThemeOption("极夜黑", Color(0xFF1F1F1F)),

    )

class ThemeViewModel(context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE)

    // 默认使用罗兰紫
    private val _themeColor = MutableStateFlow(loadColor())
    val themeColor: StateFlow<Color> = _themeColor.asStateFlow()

    fun setThemeColor(color: Color) {
        _themeColor.value = color
        saveColor(color)
    }

    private fun saveColor(color: Color) {
        prefs.edit().putInt("theme_color_int", color.toArgb()).apply()
    }

    private fun loadColor(): Color {
        val defaultColorInt = AppThemeOptions.find { it.name == "罗兰紫" }?.color?.toArgb()
            ?: Color(0xFF6750A4).toArgb()
        val savedColorInt = prefs.getInt("theme_color_int", defaultColorInt)
        return Color(savedColorInt)
    }
}