package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,        // 账户名称 (e.g., "现金", "招商银行卡")
    val type: String,        // 账户类型 (e.g., "现金", "借记卡", "信用卡")
    val initialBalance: Double, // 初始余额
    val currency: String,    // 货币 (e.g., "CNY", "USD", "JPY")
    val iconName: String,    // 图标名称 (我们稍后在UI层映射)
    val isLiability: Boolean // 是否为负债 (信用卡为 true)
)