package com.swiftiecx.timeledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Ignore
import com.swiftiecx.timeledger.data.model.CategoryType
import java.util.Date

// ✅ [新增] 记录类型常量
object RecordType {
    const val INCOME_EXPENSE = 0 // 普通收支
    const val TRANSFER = 1       // 转账
}

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

    // 关联的债务ID
    val debtId: Long? = null,

    // ✅ [新增] 记录类型：显式区分 "收支"(0) 和 "转账"(1)
    val recordType: Int = RecordType.INCOME_EXPENSE,

    // ✅ [新增] 转账关联字段 (仅当 recordType = TRANSFER 时使用)
    val transferId: Long? = null,      // 唯一标识，用于关联转出和转入
    val relatedAccountId: Long? = null // 对方账户ID (直接存储，方便UI显示 "转账给->招商银行")
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