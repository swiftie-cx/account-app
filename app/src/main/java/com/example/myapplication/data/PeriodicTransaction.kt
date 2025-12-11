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

    // --- 结束规则 ---
    val nextExecutionDate: Date, // 下次执行时间 (必须有)

    val endMode: Int = 0, // 0:永不, 1:按日期, 2:按次数
    val endDate: Date? = null,
    val endCount: Int? = null,

    // --- 其他选项 ---
    val remark: String? = null,

    // 错就是因为缺了下面这两行
    val excludeFromStats: Boolean = false, // 不计入收支
    val excludeFromBudget: Boolean = false // 不计入预算
)