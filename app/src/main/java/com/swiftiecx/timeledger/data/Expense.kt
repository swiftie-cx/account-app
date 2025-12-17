package com.swiftiecx.timeledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Ignore
import com.swiftiecx.timeledger.ui.viewmodel.CategoryType // 确保导入了 ViewMode 里的枚举，或者移到这个文件定义
import java.util.Date

// ===========================
// 1. 消费记录表 (Expense)
// ===========================
@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = Account::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["accountId"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val category: String,
    val amount: Double,
    val date: Date,
    val remark: String? = null,
    val excludeFromBudget: Boolean = false
)

// ===========================
// 2. 主分类表 (MainCategory)
// ===========================
// [关键修复] 添加 tableName="main_categories" 以匹配 DAO 中的 SQL
@Entity(tableName = "main_categories")
data class MainCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val iconName: String,
    val type: CategoryType, // 引用 CategoryType 枚举
    val key: String,        // [关键修复] 添加 key 字段用于多语言映射 (如 "cat_food")

    // UI 用的子分类列表，数据库不存，所以加 @Ignore
    @Ignore
    var subCategories: List<SubCategory> = emptyList()
) {
    // Room 需要一个无参构造函数或者匹配字段的构造函数
    // 这里提供一个辅助构造函数以防万一
    constructor(id: Long, title: String, iconName: String, type: CategoryType, key: String) : this(id, title, iconName, type, key, emptyList())
}

// ===========================
// 3. 子分类表 (SubCategory)
// ===========================
// [关键修复] 添加 tableName="sub_categories" 以匹配 DAO 中的 SQL
@Entity(tableName = "sub_categories")
data class SubCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val iconName: String,
    val key: String,        // [关键修复] 添加 key 字段用于多语言映射 (如 "sub_food")

    // 如果您需要将子分类关联到主分类，通常还需要这个字段：
    // val mainCategoryId: Long = 0
)