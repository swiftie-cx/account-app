package com.example.myapplication.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb // 【关键】确保这个导入存在
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// 定义主题颜色选项的数据结构
data class ThemeColorOption(val name: String, val color: Color)

class ThemeViewModel(context: Context) : ViewModel() {

    private val prefs = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    // 主题颜色列表
    val themeOptions = listOf(
        ThemeColorOption("默认紫", Color(0xFF6750A4)), // App 默认紫色
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

    // 当前选中的主题颜色 (StateFlow 供 Compose 监听)
    private val _themeColor = MutableStateFlow(loadThemeColor())
    val themeColor = _themeColor.asStateFlow()

    // 也就是 Compose 中的 state，方便某些非 Flow 场景读取
    var isDarkTheme = mutableStateOf(false)
        private set

    // 切换并保存颜色
    fun updateThemeColor(color: Color) {
        _themeColor.value = color
        saveThemeColor(color)
    }

    // --- 内部持久化逻辑 ---

    private fun saveThemeColor(color: Color) {
        // 【修复】直接调用官方的 toArgb() 方法
        prefs.edit().putInt("theme_color_int", color.toArgb()).apply()
    }

    private fun loadThemeColor(): Color {
        // 读取存储的颜色，如果没有，默认返回列表里的第一个（默认紫）
        val savedColorInt = prefs.getInt("theme_color_int", -1)
        return if (savedColorInt != -1) {
            Color(savedColorInt)
        } else {
            themeOptions.first().color // 默认使用列表第一个颜色
        }
    }
}