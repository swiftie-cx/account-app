package com.swiftiecx.timeledger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,            // 账户名称
    val type: String,            // 账户类型（你现有的 key/文字）
    val initialBalance: Double,  // 初始余额（你现有逻辑：余额=initialBalance+流水合计）
    val currency: String,        // 货币
    val iconName: String,        // 图标名称
    val isLiability: Boolean,    // 旧字段：信用卡为 true（先保留，后面逐步废弃）

    // ===== v2 新增：三类账户（资金/信贷/债务） =====
    // FUNDS  : 资金账户（可收支/转账）
    // CREDIT : 信贷账户（正数=欠款=负债；可选支持溢缴为负数）
    // DEBT   : 债务账户（只能追加/还款/收款，不可用于收支/转账）
    val category: String = "FUNDS",

    // 仅 CREDIT 使用：额度
    val creditLimit: Double = 0.0,

    // 仅 DEBT 使用：PAYABLE(借入/应付) / RECEIVABLE(借出/应收)
    val debtType: String? = null,
    val billingDay: Int? = null,     // 出账日（1–31）
    val repaymentDay: Int? = null,   // 还款日（1–31）
)
