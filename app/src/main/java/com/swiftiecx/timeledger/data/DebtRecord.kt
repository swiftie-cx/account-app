package com.swiftiecx.timeledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "debt_records")
data class DebtRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    // 归属哪个“债务账户”
    val accountId: Long,

    // 对方姓名
    val personName: String,

    // 金额（统一用正数）
    val amount: Double,

    // 备注
    val note: String? = null,

    // 借入/借出发生时间（创建借款那天）
    val borrowTime: Date,

    // 还款/收款时间（可空，未结清）
    val settleTime: Date? = null,

    // 资金到账账户（借入/收款时钱进哪个资金账户）
    val inAccountId: Long? = null,

    // 资金支出账户（借出/还款时钱从哪个资金账户出）
    val outAccountId: Long? = null
)