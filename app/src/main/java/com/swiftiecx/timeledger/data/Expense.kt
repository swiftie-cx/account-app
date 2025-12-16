package com.swiftiecx.timeledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date // 保持 Date 类型，以便使用 Converters

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
    val id: Long = 0, // 修改为 Long
    val accountId: Long, // 修改为 Long
    val category: String,
    val amount: Double,
    val date: Date, // *** 保持 Date 类型 ***
    val remark: String? = null,
    val excludeFromBudget: Boolean = false
)