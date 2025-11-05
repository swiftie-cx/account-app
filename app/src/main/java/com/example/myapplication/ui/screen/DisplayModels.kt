package com.example.myapplication.ui.screen

import com.example.myapplication.data.Account
import java.util.Date

/**
 * 用于在UI上显示一笔合并后的转账记录
 */
data class DisplayTransferItem(
    val date: Date,
    val fromAccount: Account,
    val toAccount: Account,
    val fromAmount: Double, // 负数
    val toAmount: Double   // 正数
)
