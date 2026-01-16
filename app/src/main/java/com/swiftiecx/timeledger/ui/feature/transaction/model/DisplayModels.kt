package com.swiftiecx.timeledger.ui.feature.transaction.model

import com.swiftiecx.timeledger.data.local.entity.Account
import java.util.Date

/**
 * 用于在UI上显示一笔合并后的转账记录
 */
data class DisplayTransferItem(
    val expenseId: Long, // 保存其中一笔账单的 ID (通常存转出那笔的ID)
    val date: Date,
    val fromAccount: Account,
    val toAccount: Account,
    val fromAmount: Double, // 负数
    val toAmount: Double,   // 正数
    val fee: Double = 0.0   // 【新增】手续费
)