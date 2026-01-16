package com.swiftiecx.timeledger.data.repository

/**
 * 仅用于序列化/反序列化的 DTO。
 * - 不要放 UI 逻辑
 * - 字段名/结构尽量稳定（影响备份/云同步）
 */
data class CategoryDto(val title: String, val iconName: String)

// 嵌套结构 DTO（主类目 + 子类目）
data class SubCategoryDto(val title: String, val iconName: String)

data class MainCategoryDto(
    val title: String,
    val iconName: String,
    val colorInt: Int,
    val subs: List<SubCategoryDto>
)

/**
 * 同步冲突策略（与原实现一致）
 */
enum class SyncStrategy {
    OVERWRITE_CLOUD,   // 以本设备为准（覆盖云端）
    OVERWRITE_LOCAL,   // 以云端为准（覆盖本地）
    MERGE              // 智能合并
}
