package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "periodic_transactions")
data class PeriodicTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int, // 0: 支出, 1: 收入, 2: 转账
    val amount: Double,
    val category: String,
    val accountId: Long,
    val targetAccountId: Long? = null,

    // --- 周期规则 ---
    val frequency: Int, // 0:每天, 1:每周, 2:每月, 3:每年
    val startDate: Date, // 生效日期

    // --- 结束规则 (新增) ---
    val endMode: Int = 0, // 0:永不, 1:按日期, 2:按次数
    val endDate: Date? = null, // 按日期结束时有效
    val endCount: Int? = null, // 按次数结束时有效 (例如: 重复 12 次)

    // --- 其他选项 ---
    val remark: String? = null,
    val excludeFromStats: Boolean = false, // 不计入收支
    val excludeFromBudget: Boolean = false // 不计入预算
)