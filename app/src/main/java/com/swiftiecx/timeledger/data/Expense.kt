package com.swiftiecx.timeledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Ignore
import com.swiftiecx.timeledger.ui.viewmodel.CategoryType
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
    val excludeFromBudget: Boolean = false,

    // ✅ [新增] 关联的债务ID (默认为空)
    // 当该条流水是由债务产生时，这里存储对应的 DebtRecord ID，以便删除流水时级联删除债务
    val debtId: Long? = null
)

// ===========================
// 2. 主分类表 (MainCategory)
// ===========================
@Entity(tableName = "main_categories")
data class MainCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val iconName: String,
    val type: CategoryType, // 引用 CategoryType 枚举
    val key: String,        // key 字段用于多语言映射

    // UI 用的子分类列表，数据库不存，所以加 @Ignore
    @Ignore
    var subCategories: List<SubCategory> = emptyList()
) {
    // Room 需要一个无参构造函数或者匹配字段的构造函数
    constructor(id: Long, title: String, iconName: String, type: CategoryType, key: String) : this(id, title, iconName, type, key, emptyList())
}

// ===========================
// 3. 子分类表 (SubCategory)
// ===========================
@Entity(tableName = "sub_categories")
data class SubCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val iconName: String,
    val key: String // key 字段用于多语言映射
)