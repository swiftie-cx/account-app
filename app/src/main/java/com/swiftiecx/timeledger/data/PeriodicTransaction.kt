package com.swiftiecx.timeledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "periodic_transactions")
data class PeriodicTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int, // 0: 支出, 1: 收入, 2: 转账
    val amount: Double,
    val fee: Double = 0.0,
    val transferMode: Int = 0, // 【新增】0: 转出固定(含手续费), 1: 转入固定(额外手续费)
    val category: String,
    val accountId: Long,
    val targetAccountId: Long? = null,

    // --- 周期规则 ---
    val frequency: Int,
    val startDate: Date,

    // --- 结束规则 ---
    val nextExecutionDate: Date,

    val endMode: Int = 0,
    val endDate: Date? = null,
    val endCount: Int? = null,

    // --- 其他选项 ---
    val remark: String? = null,

    val excludeFromStats: Boolean = false,
    val excludeFromBudget: Boolean = false
)