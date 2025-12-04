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
    // === 系列一：现代都市 & 莫兰迪 (参考截图风格) ===
    ThemeOption("雾霾蓝", Color(0xFF547578)),
    ThemeOption("香槟金", Color(0xFF9E8436)),
    ThemeOption("燕麦灰", Color(0xFF8D7B68)),
    ThemeOption("赤陶色", Color(0xFFA05D44)),
    ThemeOption("豆沙红", Color(0xFF944E43)), 
    ThemeOption("大象灰", Color(0xFF53565A)),
    ThemeOption("岩石灰", Color(0xFF444746)),
    ThemeOption("紫藤灰", Color(0xFF705D75)),

    // === 系列二：大地与暖阳 (黄色/橙色/棕色系 - 重点补充) ===
    ThemeOption("姜黄色", Color(0xFF7E6800)),
    ThemeOption("琥珀金", Color(0xFF966300)),
    ThemeOption("落日黄", Color(0xFFB37E00)),
    ThemeOption("活力橙", Color(0xFFC44D00)),
    ThemeOption("脏橘色", Color(0xFF9E4908)),
    ThemeOption("焦糖棕", Color(0xFF8D4F00)),
    ThemeOption("可可棕", Color(0xFF6D4C41)),
    ThemeOption("暖咖色", Color(0xFF5D4037)),

    // === 系列三：自然与森林 (绿色/青色系) ===
    ThemeOption("橄榄绿", Color(0xFF556500)),
    ThemeOption("抹茶绿", Color(0xFF5F7142)),
    ThemeOption("青苔绿", Color(0xFF4C662B)),
    ThemeOption("森林绿", Color(0xFF006C4C)),
    ThemeOption("祖母绿", Color(0xFF006D40)),
    ThemeOption("薄荷绿", Color(0xFF006C51)),
    ThemeOption("松石绿", Color(0xFF006A6A)),
    ThemeOption("孔雀青", Color(0xFF006978)),

    // === 系列四：经典与深邃 (蓝色/紫色/红色系) ===
    ThemeOption("罗兰紫", Color(0xFF6750A4)),
    ThemeOption("葡萄紫", Color(0xFF6E3987)),
    ThemeOption("克莱因", Color(0xFF283593)),
    ThemeOption("海洋蓝", Color(0xFF00618E)),
    ThemeOption("午夜蓝", Color(0xFF0B2548)),
    ThemeOption("勃艮第", Color(0xFF920025)),
    ThemeOption("朱砂红", Color(0xFFB3261E)),
    ThemeOption("极夜黑", Color(0xFF191C1C))
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